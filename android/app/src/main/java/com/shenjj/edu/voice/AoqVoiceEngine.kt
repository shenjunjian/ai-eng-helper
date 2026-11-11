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
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap

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

    /** 每个 stage 独立限流，避免一个 stage 的日志挤掉其他 stage 的对照数据。 */
    private val frameLogMs = ConcurrentHashMap<String, Long>()

    /** 限流窗口内的帧数与峰值：只取窗口第一帧无法区分"没帧""帧都是静音""偶发有声音"。 */
    private val frameCount = ConcurrentHashMap<String, Int>()
    private val framePeak = ConcurrentHashMap<String, Int>()

    /** 下行播放 PCM 落盘（诊断音质用，见 [enableMicProbe]）。 */
    private var pcmDump: RandomAccessFile? = null
    private var dumpBytes = 0L
    private var dumpSampleRate = 24000
    private var dumpChannels = 1

    private val _events = MutableSharedFlow<AoqEvent>(extraBufferCapacity = 128)
    val events: SharedFlow<AoqEvent> = _events.asSharedFlow()

    /** 采集链路各点的 PCM 峰值探针，见 [logFrame]。 */
    private val frameListener = object : AoqClientListener.AoqAudioFrameListener {
        override fun onCapturedAudioFrame(frame: AoqClientEngine.AoqAudioFrameData) = logFrame("captured", frame)

        override fun onProcessCapturedAudioFrame(frame: AoqClientEngine.AoqAudioFrameData) = logFrame("3A", frame)

        override fun onPublishAudioFrame(
            trackType: AoqClientEngine.AoqTrackType,
            frame: AoqClientEngine.AoqAudioFrameData,
        ) = logFrame("publish", frame)

        override fun onPlaybackAudioFrame(frame: AoqClientEngine.AoqAudioFrameData) = logFrame("playback", frame)
    }

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
            // Data 轨每条消息的字节数：如果下行音频其实是从 Data 轨以 base64 下发的
            // （response.audio.delta），这里会看到周期性的几十 KB 大块，与字幕的小 JSON 明显可分。
            val size = msg.data?.size ?: -1
            if (size > DATA_CHUNK_LOG_THRESHOLD) Log.i(TAG, "dataMsg 大块 size=${size}B")
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
         *
         * bitrate 单位是 bps（实测稳态 32000，正是我们设的 [OPUS_BITRATE]），别当成 kbps。
         *
         * 上下行都打印**整个数组**而不是 firstOrNull：只有一条轨时看不出差别，
         * 但一旦 SDK 把订阅轨拆成多条（或我们订阅的 trackType 与它统计的对不上），
         * firstOrNull 会把有流量的那条漏掉，表现成"↓sub=0 但其实有音频"。
         */
        override fun onStats(stats: AoqClientEngine.AoqStats) {
            val net = stats.networkStats
            // 逐条打印数组而不是 firstOrNull：SDK 若把订阅轨拆成多条，firstOrNull 会漏掉
            // 真正有流量的那条，表现成"↓sub=0 但其实有音频"
            val pub = stats.audioPublishStats?.joinToString(", ") {
                "${it.trackType}=${it.bytes}B/${it.bitrate}bps/encVol=${it.encodeVolume}"
            } ?: "null"
            val sub = stats.audioSubscribeStats?.joinToString(", ") {
                "${it.trackType}=${it.bytes}B/${it.bitrate}bps/playVol=${it.playVolume}"
            } ?: "null"
            val dataPub = stats.dataMsgPublishStats?.joinToString(", ") {
                "${it.trackType}=${it.bytes}B"
            } ?: "null"
            val dataSub = stats.dataMsgSubscribeStats?.joinToString(", ") {
                "${it.trackType}=${it.bytes}B"
            } ?: "null"
            Log.i(
                TAG,
                "stats ↑audio=[$pub] ↓audio=[$sub] ↑data=[$dataPub] ↓data=[$dataSub] " +
                    "net recv=${net?.recvBytes ?: -1} send=${net?.sendBytes ?: -1} " +
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

        // 上行 Opus + 16k（Stream 模式要求，见 SAMPLE_RATE 说明）；
        // 下行解码按模型原生 24k 声明，见 DECODE_SAMPLE_RATE 说明。
        logRet("setAudioEncoderConfig", eng.setAudioEncoderConfig(opusCodec(SAMPLE_RATE)))
        logRet("setAudioDecoderConfig", eng.setAudioDecoderConfig(opusCodec(DECODE_SAMPLE_RATE)))

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
            // 订阅侧 Audio 轨用 Segment，与 decoder 的 24kHz 配套（见 DECODE_SAMPLE_RATE：
            // SDK 校验明确「24000 is not supported in Stream mode; use Segment mode」）。
            // 此前是 Stream + 16kHz：上行正常，但下行包到了本机却被丢弃（↓audio 恒 0B）。
            // Data 轨保持 Stream —— 它一直是好的（字幕/事件都正常）。
            subscribeTracks = listOf(
                trackParam(
                    AoqClientEngine.AoqTrackType.AoqTrackTypeAudio,
                    AoqClientEngine.AoqTrackMode.AoqTrackModeSegment,
                ),
                streamTrack(AoqClientEngine.AoqTrackType.AoqTrackTypeData),
            )
        }
        logNativeLibs("connect")
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
        // 探针要在这里摘掉：帧回调在 native 线程上，destroy 之后再写文件会踩已释放的引擎。
        // enableAudioFrameObserver 的 config 参数 SDK 声明为非空，关闭时同样要传一个。
        runCatching {
            engine?.enableAudioFrameObserver(
                false,
                AoqClientEngine.AoqAudioSource.AoqAudioSourcePlayback,
                AoqClientEngine.AoqAudioObserverConfig(),
            )
        }
        runCatching { engine?.setAudioFrameObserver(null) }
        finishPcmDump()
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
            // 服务端建连后的首个事件，携带它**真正生效**的默认配置。
            // 打出来才能确认我们的 session.update 到底被采纳了什么（voice / turn_detection / model）。
            "session.created" ->
                Log.i(TAG, "session.created session=${obj.optJSONObject("session")}")

            "session.updated" -> {
                val eng = engine ?: return
                // 回显服务端**最终生效**的配置：voice / format / turn_detection 若与提交值不符即被忽略或拒绝
                Log.i(TAG, "session.updated session=${obj.optJSONObject("session")}")
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
                // 采集起来后再接探针，否则拿不到任何帧
                enableMicProbe(eng)
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

            // 一轮回复由哪些内容块组成，全看这两个事件：part.type 若是 "text" 就说明服务端
            // 压根没打算发音频（与"有字幕、无下行音频字节"完全自洽）；是 "audio" 则问题在传输/解码侧。
            "response.output_item.added" ->
                Log.i(TAG, "response.output_item.added item=${obj.optJSONObject("item")}")

            "response.content_part.added" ->
                Log.i(TAG, "response.content_part.added part=${obj.optJSONObject("part")}")

            // 若下行音频以 base64 走 Data 轨（而不是 Audio 轨），delta 事件会带 audio 字段；
            // 这条日志用来确认它到底有没有出现、以及音频的承载方式。
            "response.audio.delta" ->
                Log.i(TAG, "response.audio.delta 音频块 base64Len=${obj.optString("delta").length}")

            "response.audio.done" -> {
                Log.i(TAG, "response.audio.done 服务端声明本轮音频已生成")
                logNativeLibs("audio.done")
            }

            "response.done" -> emit(AoqEvent.ResponseDone)

            "error" -> {
                val err = obj.optJSONObject("error")
                // 客户端错误（invalid_request_error）不中断连接，只吞掉本轮，所以必须显式暴露出来
                Log.e(TAG, "模型错误 type=${err?.optString("type")} " +
                    "code=${err?.optString("code")} param=${err?.optString("param")} " +
                    "message=${err?.optString("message")}")
                emit(AoqEvent.Failure(err?.optString("message").takeUnless { it.isNullOrEmpty() } ?: "模型返回错误"))
            }

            // 未识别的事件一律打出来（带原文）：事件名与 OpenAI 有差异时，静默丢弃会表现成"一切正常但界面空白"
            else -> Log.i(
                TAG,
                "未处理的事件 $type raw=${obj.toString().take(400)}",
            )
        }
    }

    private fun logFrame(stage: String, frame: AoqClientEngine.AoqAudioFrameData) {
        // dataPtr 是平台类型：声明为非空 ByteArray，靠 elvis 提前返回保证安全
        val pcm: ByteArray = frame.dataPtr ?: return
        val bytes = minOf(frame.dataSize, pcm.size)
        // 落盘必须在下面的限流 return **之前**：限流是 1 秒只放一帧进日志，
        // 若放在后面就只录到 1/100 的音频，WAV 会变成断续 noise，反而误导音质判断。
        if (stage == "playback") {
            frame.samplesPerSec.takeIf { it > 0 }?.let { dumpSampleRate = it }
            frame.numOfChannels.takeIf { it > 0 }?.let { dumpChannels = it }
            appendPcmDump(pcm, bytes, dumpSampleRate, dumpChannels)
        }
        var peak = 0
        var i = 0
        while (i + 1 < bytes) {
            val sample = ((pcm[i + 1].toInt() shl 8) or (pcm[i].toInt() and 0xFF)).toShort()
            val abs = if (sample < 0) -sample.toInt() else sample.toInt()
            if (abs > peak) peak = abs
            i += 2
        }
        // 逐 stage 限流：共用一个时间戳会让后面的 stage 全被第一个吞掉，就失去对照意义了
        val now = System.currentTimeMillis()
        val last = frameLogMs[stage] ?: 0L
        if (now - last < 1000) {
            frameCount[stage] = (frameCount[stage] ?: 0) + 1
            framePeak[stage] = maxOf(framePeak[stage] ?: 0, peak)
            return
        }
        val count = frameCount[stage] ?: 0
        val maxPeak = maxOf(framePeak[stage] ?: 0, peak)
        frameLogMs[stage] = now
        frameCount[stage] = 1
        framePeak[stage] = 0
        // frames 与 peakMax 都不可省：只报峰值会分不清「完全没有帧」(frames=0)
        // 和「有帧但全是静音」(frames≈100, peakMax≈0)，这两者的根因完全不同。
        // autoGenMute=true 表示这一帧是 SDK 自己补的静音占位，不是远端真实音频。
        Log.i(
            TAG,
            "mic[$stage] frames=$count peakMax=$maxPeak autoGenMute=${frame.autoGenMute} " +
                "sr=${frame.samplesPerSec} ch=${frame.numOfChannels} bytes=$bytes",
        )
    }

    /**
     * 打印进程里**真正加载**的 AOQ 相关 native 库，以及 Opus 插件文件在不在磁盘上。
     *
     * 为什么这是本轮最关键的一条：下行音频是 Opus，而 AOQ 的 Opus 编解码走的是**外挂插件**
     * `libPluginOpus.so`。此前查 `/proc/<pid>/maps` 只见 `libaoq_client_sdk.so` 与
     * `libaoqffmpeg_rd.so`，插件没被 dlopen。若确实如此，表现恰好就是现在这样：
     * 包到了网卡（`net recv` 在 AI 说话时暴涨）却解不出音频（`↓audio=0`、playback 全静音），
     * 而字幕走 Data 轨的 JSON，完全不受影响——正好对上「有字幕、没声音」。
     *
     * 磁盘上存在但 maps 里没有 = 加载路径/初始化问题；磁盘上根本没有 = 打包或 ABI 问题。
     */
    private fun logNativeLibs(stage: String) {
        if (!BuildConfig.DEBUG) return
        val pattern = Regex("aoq|opus|plugin|webrtc|ffmpeg", RegexOption.IGNORE_CASE)
        // 必须 readText 一次性读完：用 bufferedReader/useLines 时惰性序列在流关闭后才求值，
        // 上一版就是因此打出 "Stream closed"，整条证据白拿。
        val loaded = runCatching {
            File("/proc/self/maps").readText().lineSequence()
                .mapNotNull { it.trim().substringAfterLast(' ').takeIf { p -> p.endsWith(".so") } }
                .map { it.substringAfterLast('/') }
                .filter { pattern.containsMatchIn(it) }
                .distinct()
                .sorted()
        }.getOrElse { listOf("<maps 读取失败: ${it.message}>") }
        val dir = runCatching { context.applicationInfo.nativeLibraryDir }.getOrNull()
        val plugin = dir?.let { File(it, "libPluginOpus.so") }
        Log.i(
            TAG,
            "native[$stage] loaded=$loaded | pluginPath=${plugin?.absolutePath} " +
                "exists=${plugin?.exists()} size=${plugin?.length()}",
        )
    }

    /**
     * 打开下行播放 PCM 探针（ReadOnly，不改动音频），并落盘成 WAV，**仅 debug 构建**。
     *
     * 用途是切开「AI 的语音本身糊」与「模拟器输出糊」这两种音质问题：探针拿到的字节是
     * **解码之后、送 AudioTrack 之前**的数据，把它拿到 PC 上听——
     * 清楚 = 流和解码都没问题，糊是模拟器/播放配置造成的；糊 = 问题在更上游。
     *
     * 只注册 Playback 一个数据源：帧回调运行在实时音频线程上，之前四个源一起开
     * （captured/3A/publish/playback）本身就可能挤占线程造成断续，先把它降到最小。
     * 把 [DUMP_PLAYBACK_PCM] 置 false 可彻底关掉探针，用来做「有没有探针」的 A/B 对比。
     */
    private fun enableMicProbe(eng: AoqClientEngine) {
        if (!BuildConfig.DEBUG || !DUMP_PLAYBACK_PCM) return
        logRet("setAudioFrameObserver", eng.setAudioFrameObserver(frameListener))
        val config = AoqClientEngine.AoqAudioObserverConfig().apply {
            sampleRate = DECODE_SAMPLE_RATE
            channels = 1
            mode = AoqClientEngine.AoqAudioObserverMode.AoqAudioObserverModeReadOnly
        }
        val source = AoqClientEngine.AoqAudioSource.AoqAudioSourcePlayback
        logRet("enableAudioFrameObserver($source)", eng.enableAudioFrameObserver(true, source, config))
    }

    /** 第一帧到达时才建文件：采样率/声道数以帧里的真实值为准，不能拿我们声明的去猜。 */
    private fun appendPcmDump(pcm: ByteArray, bytes: Int, sampleRate: Int, channels: Int) {
        if (!DUMP_PLAYBACK_PCM) return
        val raf = pcmDump ?: runCatching {
            val dir = context.externalCacheDir ?: context.cacheDir
            RandomAccessFile(File(dir, "aoq-playback.wav"), "rw").apply {
                setLength(0)
                write(ByteArray(WAV_HEADER_BYTES))
            }
        }.getOrElse {
            Log.w(TAG, "PCM dump 打不开：${it.message}")
            return
        }
        if (dumpBytes > DUMP_MAX_BYTES) return
        runCatching {
            raf.write(pcm, 0, bytes)
            dumpBytes += bytes
        }.onFailure {
            Log.w(TAG, "PCM dump 写入失败，已停止：${it.message}")
            finishPcmDump()
        }
    }

    /** 回填 RIFF 头，并打出 pull 路径。 */
    private fun finishPcmDump() {
        val raf = pcmDump ?: return
        pcmDump = null
        val dataBytes = dumpBytes
        runCatching {
            raf.seek(0)
            raf.write(wavHeader(dataBytes, dumpSampleRate, dumpChannels))
        }
        runCatching { raf.close() }
        val dir = context.externalCacheDir ?: context.cacheDir
        Log.i(TAG, "PCM dump 已保存 ${dataBytes}B → ${File(dir, "aoq-playback.wav").absolutePath}")
    }

    private fun wavHeader(dataBytes: Long, sampleRate: Int, channels: Int): ByteArray {
        val byteRate = sampleRate * channels * 2
        val out = java.nio.ByteBuffer.allocate(WAV_HEADER_BYTES).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        out.put("RIFF".toByteArray()).putInt((dataBytes + 36).toInt())
        out.put("WAVE".toByteArray())
        out.put("fmt ".toByteArray()).putInt(16).putShort(1) // PCM
        out.putShort(channels.toShort()).putInt(sampleRate)
        out.putInt(byteRate).putShort((channels * 2).toShort()).putShort(16)
        out.put("data".toByteArray()).putInt(dataBytes.toInt())
        return out.array()
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
        trackParam(type, AoqClientEngine.AoqTrackMode.AoqTrackModeStream)

    private fun trackParam(
        type: AoqClientEngine.AoqTrackType,
        mode: AoqClientEngine.AoqTrackMode,
    ) = AoqClientEngine.AoqTrackParam().apply {
        trackType = type
        trackMode = mode
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

        /**
         * 下行解码采样率 = **24000**，与上行不同，依据两条：
         *
         * 1. 模型侧 `output_audio_format=pcm` 文档写明是 **24kHz** 16bit 单声道，
         *    网关大概率按原生 24k 下发音频。
         * 2. SDK 的 connect 校验报错原文给了合法组合：
         *    `subscribe audio sampleRate 24000 is not supported in Stream mode;
         *     use Segment mode or change decoder sampleRate`
         *    —— 即 **24000 必须配 Segment 订阅轨**，两者是绑定的，见 [subscribeTracks]。
         *
         * 此前按后半句把 decoder 改成 16000 并保留 Stream：上行因此正常（ASR/VAD 都对），
         * 但下行 `↓audio` 恒为 0B，而抓包量显示音频包确实到了本机 → 声明与实发不符被丢弃。
         * 注意上行 [SAMPLE_RATE] 仍是 16000，**不要一起改**，那是 Stream 上行的硬性要求。
         */
        private const val DECODE_SAMPLE_RATE = 24000

        private const val OPUS_BITRATE = 32000

        /**
         * Data 轨单条消息超过此字节数就单独记一行：字幕 JSON 只有几百 B，
         * 而音频块会是几十 KB，两者据此可分。
         */
        private const val DATA_CHUNK_LOG_THRESHOLD = 4096

        /**
         * 是否把下行播放 PCM 落盘成 WAV（诊断音质，见 [enableMicProbe]）。
         * 置 false 会连帧探针一起关掉，可直接做「开/关探针」的 A/B 对比。
         */
        private const val DUMP_PLAYBACK_PCM = true

        private const val WAV_HEADER_BYTES = 44

        /** 上限 60s @24kHz 单声道 16bit ≈ 2.7MB，防止忘了关就一直写。 */
        private const val DUMP_MAX_BYTES = 2_880_000L
    }
}
