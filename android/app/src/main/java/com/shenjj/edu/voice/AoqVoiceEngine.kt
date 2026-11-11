package com.shenjj.edu.voice

import android.content.Context
import android.util.Log
import com.alibaba.aoq.clientsdk.AoqClientEngine
import com.alibaba.aoq.clientsdk.AoqClientListener
import com.shenjj.edu.BuildConfig
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

        override fun onWarning(code: Int, msg: String?) {
            Log.w(TAG, "AOQ warning $code：$msg")
        }

        override fun onError(code: Int, msg: String?) {
            Log.e(TAG, "AOQ error $code：$msg")
            emit(AoqEvent.Failure("AOQ 错误 $code：$msg"))
        }

        /**
         * 下行有没有音频只看这一个数字：audioSubscribeStats.bytes 不涨 = 服务端没发，
         * 涨了却没声音 = 本机播放/路由问题。没有它，「没声音」永远只能靠猜。
         */
        override fun onStats(stats: AoqClientEngine.AoqStats) {
            val pub = stats.audioPublishStats?.firstOrNull()
            val sub = stats.audioSubscribeStats?.firstOrNull()
            val dataSub = stats.dataMsgSubscribeStats?.firstOrNull()
            val net = stats.networkStats
            Log.i(
                TAG,
                "stats ↑pub=${pub?.bytes ?: -1}B/${pub?.bitrate ?: -1}kbps encVol=${pub?.encodeVolume ?: -1} " +
                    "↓sub=${sub?.bytes ?: -1}B/${sub?.bitrate ?: -1}kbps playVol=${sub?.playVolume ?: -1} " +
                    "data=${dataSub?.bytes ?: -1}B net recv=${net?.recvBytes ?: -1} send=${net?.sendBytes ?: -1} " +
                    "loss=${net?.loss ?: -1} rtt=${net?.rtt ?: -1}",
            )
        }

        /** 采集/播放设备起停失败会走这里（RecordFail / PlayFail），是无声问题的直接证据 */
        override fun onAudioDeviceStateChanged(state: AoqClientEngine.AoqAudioDeviceState) {
            Log.i(TAG, "audio device state=${state.state} reason=${state.reason}")
            when (state.state) {
                AoqClientEngine.AoqAudioDeviceStateCode.AoqAudioDeviceRecordFail ->
                    emit(AoqEvent.Failure("麦克风启动失败，请检查录音权限或其他应用占用"))

                AoqClientEngine.AoqAudioDeviceStateCode.AoqAudioDevicePlayFail ->
                    emit(AoqEvent.Failure("音频播放设备启动失败"))

                else -> Unit
            }
        }

        override fun onAudioDeviceRouteChanged(routeType: Int) {
            Log.i(TAG, "audio route=$routeType (${AoqClientEngine.AoqAudioDeviceRouteType.fromValue(routeType)})")
        }

        /** 通话被系统/其他应用打断（如来电）：不然后台恢复后表现为一直静音 */
        override fun onAudioDeviceInterrupted(interrupt: Boolean) {
            Log.i(TAG, "audio interrupted=$interrupt")
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

        // 上下行传输都用 Opus + 16k（Stream 模式的要求，见 SAMPLE_RATE 说明）
        logRet("setAudioEncoderConfig", eng.setAudioEncoderConfig(opusCodec(SAMPLE_RATE)))
        logRet("setAudioDecoderConfig", eng.setAudioDecoderConfig(opusCodec(SAMPLE_RATE)))

        // 先起播放设备，保证下行音频一到就能出声
        val playback = AoqClientEngine.AoqAudioPlaybackConfig().apply {
            isVoipMode = true
            isDefaultSpeaker = true
            isExternal = false
            channel = 1
        }
        logRet("startAudioPlayer", eng.startAudioPlayer(playback))

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

    /**
     * 发送 session.update。字段必须严格遵循 Qwen-Audio-Realtime 的客户端事件协议
     * （https://help.aliyun.com/zh/model-studio/fun-audiochat-client-events），
     * **不要照 OpenAI Realtime 的写法套**，两者字段名与取值并不相同：
     *
     * - `input_audio_format` 只支持 `"pcm"`（16kHz 16bit 单声道）；OpenAI 那套 `"pcm16"` 在这里是非法值。
     * - `output_audio_format` 只支持 `"pcm"`（24kHz 16bit 单声道）。
     *   这两个字段描述的是**网关与模型之间**的音频，与 AOQ 传输层用 Opus 无关，
     *   文档明确「网关会将音频转换为模型所需的格式」，所以 [SAMPLE_RATE] 仍按 SDK 要求写 16000。
     * - `input_audio_transcription` 只有 `language` 一个字段，**没有 `model`**：
     *   转写模型（`fun-asr`）由服务端在 session.created 里下发，客户端设它只会报 invalid_value。
     *   这里整体不传，用服务端默认配置。
     * - `voice`：3.1 Plus/Flash 默认 `longanqian_v3.1`，`longanqian` 只属于 3.0 系列，
     *   且「仅可在第一次 session.update 中设置」。
     *
     * `turn_detection` 与 `input_audio_format` 一样，只在首次发送音频前（IDLE 态）允许改，
     * 因此必须在 enableSendMediaStream 之前发出去。
     */
    private fun sendSessionUpdate() {
        if (sessionUpdateSent) return
        sessionUpdateSent = true
        val session = JSONObject().apply {
            put("modalities", JSONArray().put("text").put("audio"))
            put("voice", pendingVoice)
            put("instructions", pendingInstructions)
            put("input_audio_format", "pcm")
            put("output_audio_format", "pcm")
            put(
                "turn_detection",
                JSONObject()
                    .put("type", "server_vad")
                    .put("threshold", VAD_THRESHOLD)
                    .put("silence_duration_ms", VAD_SILENCE_MS),
            )
        }
        val json = JSONObject().put("type", "session.update").put("session", session).toString()
        Log.i(TAG, "session.update → voice=$pendingVoice model=${BuildConfig.AOQ_MODEL}")
        sendRaw(json)
    }

    private fun handleData(bytes: ByteArray?) {
        if (bytes == null || bytes.isEmpty()) return
        val obj = try {
            JSONObject(String(bytes, Charsets.UTF_8))
        } catch (e: Exception) {
            return
        }
        val type = obj.optString("type")
        when (type) {
            "session.updated" -> {
                val eng = engine ?: return
                // 握手完成：开上行 + 起麦克风采集（VAD 需要持续上行才有轮次）
                logRet("enableSendMediaStream(audio)", eng.enableSendMediaStream(
                    AoqClientEngine.AoqTrackType.AoqTrackTypeAudio, true,
                ))
                val capture = AoqClientEngine.AoqAudioCaptureConfig().apply {
                    isExternal = false
                    isVoipMode = true
                    channel = 1
                }
                logRet("startAudioCapture", eng.startAudioCapture(capture))
                emit(AoqEvent.SessionReady)
            }

            // ASR 中间结果：本协议里增量字段叫 text（已确定）+ stash（未确定），不是 OpenAI 的 delta
            "conversation.item.input_audio_transcription.delta" -> {
                val partial = obj.optString("text") + obj.optString("stash")
                if (partial.isNotEmpty()) emit(AoqEvent.UserTranscript(partial, false))
            }

            "conversation.item.input_audio_transcription.completed" ->
                emit(AoqEvent.UserTranscript(obj.optString("transcript"), true))

            // 转写失败只影响用户这侧的字幕，AI 语音不受影响：记日志，不打断通话
            "conversation.item.input_audio_transcription.failed" ->
                Log.w(TAG, "ASR 转写失败：${obj.optJSONObject("error")?.optString("message")}")

            "response.audio_transcript.delta" ->
                emit(AoqEvent.AiDelta(obj.optString("delta")))

            "response.audio_transcript.done" ->
                emit(AoqEvent.AiDone(obj.optString("transcript")))

            // VAD 只报事件、不触发推理 = 模型认为这不是有效轮次，是"没声音"最常见的形态
            "input_audio_buffer.speech_started" -> Log.i(TAG, "VAD 检测到语音开始")
            "input_audio_buffer.speech_stopped" ->
                Log.i(TAG, "VAD 检测到语音结束 reason=${obj.optString("reason")}")

            "response.created" -> Log.i(TAG, "模型开始生成响应")

            "response.done" -> emit(AoqEvent.ResponseDone)

            "error" -> {
                val err = obj.optJSONObject("error")
                // 客户端错误（invalid_request_error）不中断连接，只吞掉本轮，所以必须显式暴露出来
                Log.e(TAG, "模型错误 type=${err?.optString("type")} " +
                    "code=${err?.optString("code")} param=${err?.optString("param")} " +
                    "message=${err?.optString("message")}")
                emit(AoqEvent.Failure(err?.optString("message").takeUnless { it.isNullOrEmpty() } ?: "模型返回错误"))
            }

            // 未识别的事件一律打出来：事件名与 OpenAI 有差异时，静默丢弃会表现成"一切正常但界面空白"
            else -> Log.i(TAG, "未处理的事件 $type")
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

    /** SDK 的 int 返回值：非 0 即失败。吞掉返回值会让"没声音"变成无解的谜题。 */
    private fun logRet(api: String, ret: Int) {
        if (ret != 0) Log.e(TAG, "$api 失败 ret=$ret") else Log.i(TAG, "$api ok")
    }

    companion object {
        private const val TAG = "AoqVoice"

        /**
         * TTS 音色。据客户端事件文档：3.1 Plus/Flash 默认 `longanqian_v3.1`，
         * `longanqian` 只属于 3.0 Plus/Flash；且音色「仅可在第一次 session.update 中设置」，
         * 后续传入将被忽略。切换老师/音色的持久化入口见个人设置。
         */
        const val DEFAULT_VOICE = "longanqian_v3.1"

        /**
         * server_vad 灵敏度与句尾静音判定时长。文档默认 0.5 / 800ms，
         * 对话场景推荐 silence 400-800；threshold 越低越容易把背景噪音当语音。
         */
        private const val VAD_THRESHOLD = 0.5
        private const val VAD_SILENCE_MS = 800

        /**
         * 音频**传输层**采样率：上下行都用 16000。
         *
         * 这是 AOQ Stream 模式对传输编码的要求，与模型侧采样率无关。文档写明
         * 「这些参数用于网络传输，网关会将音频转换为模型所需的格式」，
         * 「传输编码配置与模型事件中的音频格式参数应分别按 SDK 和目标模型的要求设置」：
         * 模型侧固定 input 16k / output 24k，由 session.update 里的
         * `input_audio_format` / `output_audio_format`（两者都只支持 `"pcm"`）描述，
         * 不需要、也不能靠改这个数字来对齐。
         *
         * 实测：decoder 设成 24000 时 `connect` 直接失败——
         * ```
         * Connect param invalid: subscribe audio sampleRate 24000 is not supported
         * in Stream mode; use Segment mode or change decoder sampleRate
         * ```
         * 真要原样播放 24kHz，得把音频轨 [AoqClientEngine.AoqTrackMode] 换成 Segment
         * （分段传输、延迟更高，不适合边说边听），而不是只改这里。
         *
         * 注：`AoqAudioCaptureConfig` 没有 sampleRate 字段，麦克风采集采样率
         * 由传给 `setAudioEncoderConfig` 的 [AoqClientEngine.AoqAudioCodecConfig] 决定。
         */
        private const val SAMPLE_RATE = 16000

        private const val OPUS_BITRATE = 32000
    }
}
