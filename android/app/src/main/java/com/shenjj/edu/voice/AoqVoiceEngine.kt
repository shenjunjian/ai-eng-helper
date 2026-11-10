package com.shenjj.edu.voice

import android.content.Context
import com.alibaba.aoq.clientsdk.AoqClientEngine
import com.alibaba.aoq.clientsdk.AoqClientListener
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONArray
import org.json.JSONObject

/** 引擎向 UI 抛出的事件（字幕、状态、错误）。 */
sealed interface AoqEvent {
    /** session.updated 已收到、上行已开，可以开始说话 */
    data object SessionReady : AoqEvent

    /** 用户说话的 ASR 转写；isFinal=false 为中间结果 */
    data class UserTranscript(val text: String, val isFinal: Boolean) : AoqEvent

    /** AI 回复的流式文本增量（字幕逐字） */
    data class AiDelta(val text: String) : AoqEvent

    /** AI 一轮回复的完整文本 */
    data class AiDone(val text: String) : AoqEvent

    /** AI 一轮结束（response.done） */
    data object ResponseDone : AoqEvent

    /** 连接状态变化 */
    data class Status(val connected: Boolean) : AoqEvent

    /** 出错（建连/模型/引擎） */
    data class Failure(val reason: String) : AoqEvent
}

/**
 * AOQ 实时语音引擎封装。
 *
 * 严格遵循官方时序：createEngine → 配 Opus 编解码 → 起播放设备 → 上行保持关闭 →
 * connect → onConnectionStatusChange(Connected) 发 session.update →
 * onDataMsg(session.updated) 后开上行 + 起采集 → 收发 Data 事件与音频 → stop 时 disconnect/destroy。
 *
 * 音频走 Audio 轨由 SDK 自动采集/播放；对话事件走 Data 轨（JSON）。
 * 服务端 VAD 自动分轮，无需客户端手动 commit。
 */
class AoqVoiceEngine(private val context: Context) {

    private var engine: AoqClientEngine? = null

    @Volatile
    private var started = false

    @Volatile
    private var sessionUpdateSent = false

    private var pendingInstructions = ""
    private var pendingVoice = DEFAULT_VOICE

    private val _events = MutableSharedFlow<AoqEvent>(extraBufferCapacity = 128)
    val events: SharedFlow<AoqEvent> = _events.asSharedFlow()

    private val listener = object : AoqClientListener() {
        override fun onConnectionStatusChange(status: AoqClientEngine.AoqConnectionStatus) {
            when (status) {
                AoqClientEngine.AoqConnectionStatus.AoqConnectionStatusConnected -> {
                    emit(AoqEvent.Status(true))
                    sendSessionUpdate()
                }

                AoqClientEngine.AoqConnectionStatus.AoqConnectionStatusFailed ->
                    emit(AoqEvent.Failure("AOQ 连接失败"))

                AoqClientEngine.AoqConnectionStatus.AoqConnectionStatusDisconnected ->
                    emit(AoqEvent.Status(false))

                else -> Unit
            }
        }

        override fun onDataMsg(msg: AoqClientEngine.AoqDataMsg) {
            handleData(msg.data)
        }

        override fun onError(code: Int, msg: String?) {
            emit(AoqEvent.Failure("AOQ 错误 $code：$msg"))
        }
    }

    /**
     * 建连并开始通话。[credential] 必须由 [AoqTokenClient] 现取（一次性）。
     * [instructions] 是本次对话的系统提示（老师人设 + 对话模式 + 生词内联标记规则）。
     */
    fun start(credential: AoqCredential, instructions: String, voice: String = DEFAULT_VOICE) {
        if (started) return
        started = true
        sessionUpdateSent = false
        pendingInstructions = instructions
        pendingVoice = voice

        val createConfig = AoqClientEngine.AoqCreateConfig().apply {
            workDir = context.filesDir.absolutePath + "/aoq"
            pauseByOtherAppCall = true
        }
        // 声明为可空：native 库加载失败时 createEngine 会返回 null
        val eng: AoqClientEngine? = AoqClientEngine.createEngine(context, createConfig, listener)
        engine = eng
        if (eng == null) {
            emit(AoqEvent.Failure("AOQ 引擎创建失败"))
            started = false
            return
        }

        // 上下行都用 Opus + 16k（Stream 模式的硬约束，见 SAMPLE_RATE 说明）
        eng.setAudioEncoderConfig(opusCodec(SAMPLE_RATE))
        eng.setAudioDecoderConfig(opusCodec(SAMPLE_RATE))

        // 先起播放设备，保证下行音频一到就能出声
        val playback = AoqClientEngine.AoqAudioPlaybackConfig().apply {
            isVoipMode = true
            isDefaultSpeaker = true
            isExternal = false
            channel = 1
        }
        eng.startAudioPlayer(playback)

        // 上行保持关闭，等 session.updated 再开（避免握手前推流）
        eng.enableSendMediaStream(AoqClientEngine.AoqTrackType.AoqTrackTypeAudio, false)

        val connectConfig = AoqClientEngine.AoqConnectConfig().apply {
            token = credential.token
            sid = credential.sid
            certFingerprint = credential.certFingerprint
            workspaceIdHash = credential.workspaceIdHash
            relayEndpoints = credential.endpoints.map { ep ->
                AoqClientEngine.AoqRelayEndpoint().apply {
                    routeIndex = ep.routeIndex
                    endpoint = ep.endpoint
                    port = ep.port
                    tcpPort = ep.tcpPort
                }
            }
            publishTracks = listOf(streamTrack(AoqClientEngine.AoqTrackType.AoqTrackTypeAudio),
                streamTrack(AoqClientEngine.AoqTrackType.AoqTrackTypeData))
            subscribeTracks = listOf(streamTrack(AoqClientEngine.AoqTrackType.AoqTrackTypeAudio),
                streamTrack(AoqClientEngine.AoqTrackType.AoqTrackTypeData))
        }
        val ret = eng.connect(connectConfig)
        if (ret != 0) emit(AoqEvent.Failure("connect 返回 $ret"))
    }

    /** 挂断：停采集 → 断连 → 停播放 → 销毁引擎。 */
    fun stop() {
        if (!started) return
        started = false
        runCatching { engine?.stopAudioCapture() }
        runCatching { engine?.enableSendMediaStream(AoqClientEngine.AoqTrackType.AoqTrackTypeAudio, false) }
        runCatching { engine?.disconnect() }
        runCatching { engine?.stopAudioPlayer() }
        runCatching { AoqClientEngine.destroy() }
        engine = null
    }

    private fun sendSessionUpdate() {
        if (sessionUpdateSent) return
        sessionUpdateSent = true
        val session = JSONObject().apply {
            put("modalities", JSONArray().put("text").put("audio"))
            put("voice", pendingVoice)
            put("instructions", pendingInstructions)
            // AOQ 传输层用 Opus，模型侧仍按 pcm16 交换，SDK 负责编解码
            put("input_audio_format", "pcm16")
            put("output_audio_format", "pcm16")
            put("input_audio_transcription", JSONObject().put("model", "gpt-4o-transcribe"))
            put("turn_detection", JSONObject().put("type", "server_vad"))
        }
        sendRaw(JSONObject().put("type", "session.update").put("session", session).toString())
    }

    private fun handleData(bytes: ByteArray?) {
        if (bytes == null || bytes.isEmpty()) return
        val obj = try {
            JSONObject(String(bytes, Charsets.UTF_8))
        } catch (e: Exception) {
            return
        }
        when (obj.optString("type")) {
            "session.updated" -> {
                val eng = engine ?: return
                // 握手完成：开上行 + 起麦克风采集
                eng.enableSendMediaStream(AoqClientEngine.AoqTrackType.AoqTrackTypeAudio, true)
                val capture = AoqClientEngine.AoqAudioCaptureConfig().apply {
                    isExternal = false
                    isVoipMode = true
                    channel = 1
                }
                eng.startAudioCapture(capture)
                emit(AoqEvent.SessionReady)
            }

            "conversation.item.input_audio_transcription.delta" ->
                emit(AoqEvent.UserTranscript(obj.optString("delta"), false))

            "conversation.item.input_audio_transcription.completed" ->
                emit(AoqEvent.UserTranscript(obj.optString("transcript"), true))

            "response.audio_transcript.delta" ->
                emit(AoqEvent.AiDelta(obj.optString("delta")))

            "response.audio_transcript.done" ->
                emit(AoqEvent.AiDone(obj.optString("transcript")))

            "response.done" -> emit(AoqEvent.ResponseDone)

            "error" -> {
                val reason = obj.optJSONObject("error")?.optString("message") ?: "模型返回错误"
                emit(AoqEvent.Failure(reason))
            }
        }
    }

    private fun sendRaw(json: String) {
        val msg = AoqClientEngine.AoqDataMsg().apply { data = json.toByteArray(Charsets.UTF_8) }
        runCatching { engine?.sendDataMsg(msg) }
    }

    private fun opusCodec(sampleRate: Int) = AoqClientEngine.AoqAudioCodecConfig().apply {
        trackType = AoqClientEngine.AoqTrackType.AoqTrackTypeAudio
        codecType = AoqClientEngine.AoqEncoderType.AoqEncoderTypeAudioOpus
        this.sampleRate = sampleRate
        channel = 1
        bitrate = OPUS_BITRATE
    }

    private fun streamTrack(type: AoqClientEngine.AoqTrackType) =
        AoqClientEngine.AoqTrackParam().apply {
            trackType = type
            trackMode = AoqClientEngine.AoqTrackMode.AoqTrackModeStream
        }

    private fun emit(event: AoqEvent) {
        _events.tryEmit(event)
    }

    companion object {
        /** qwen-audio-realtime 默认音色之一；可在个人设置/通话页切换 */
        const val DEFAULT_VOICE = "longanqian"
        /**
         * 音频采样率：上行采集与下行播放都用 16000，Qwen-Audio-Realtime 的要求。
         *
         * **不要改成 24000。** AOQ 的 Stream 模式（实时对话必须用它，低延迟连续推流）
         * 只接受 16000；下行 decoder 一旦设成 24000，`connect` 会直接失败：
         *
         * ```
         * Connect param invalid: subscribe audio sampleRate 24000 is not supported
         * in Stream mode; use Segment mode or change decoder sampleRate
         * ```
         *
         * 如果将来模型确实输出 24kHz 音频、必须原样播放，那就要把音频轨的
         * [AoqClientEngine.AoqTrackMode] 从 Stream 换成 Segment（分段传输，延迟更高、
         * 不适合边说边听），而不是只改采样率。
         *
         * 注：`AoqAudioCaptureConfig` 没有 sampleRate 字段，麦克风采集的采样率
         * 完全由这里传给 `setAudioEncoderConfig` 的 [AoqClientEngine.AoqAudioCodecConfig] 决定。
         */
        private const val SAMPLE_RATE = 16000

        private const val OPUS_BITRATE = 32000
    }
}
