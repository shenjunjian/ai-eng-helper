---
name: aoq-android-integration
description: 在 Android App 中对接阿里云 AOQ（qwen-audio 实时语音）SDK，实现实时语音对话通话。用于：(1)新建/调试 AOQ 语音通话、接入 AoqClientEngine、配置上下行音频参数，(2)实时语音"连接成功但没声音""字幕有语音不出""VAD 不触发"等故障排查，(3)session.update 事件协议字段取值（voice / pcm / turn_detection / input_audio_transcription），(4)Android 模拟器上麦克风、音频输出相关的环境问题。触发词：AOQ、AoqClientEngine、qwen-audio-realtime、实时语音对话、语音通话、没声音、Opus 插件、模拟器麦克风。
---

# Android 对接 AOQ 实时语音 SDK

TalkFlow（`com.shenjj.edu`）的实时语音对话走 AOQ SDK + `qwen-audio-3.1-realtime-flash`。
实现集中在 `android/app/src/main/java/com/shenjj/edu/voice/AoqVoiceEngine.kt`。

官方文档：<https://help.aliyun.com/zh/model-studio/audio-realtime-aoq-access>
事件协议：<https://help.aliyun.com/zh/model-studio/fun-audiochat-client-events>

## 第一原则：不要照 OpenAI Realtime 的写法套

AOQ 的事件协议是 **fun-audiochat**，与 OpenAI Realtime **字段名和取值都不相同**。
本次对接耗时的最大单一原因，就是把 OpenAI 的写法当成 AOQ 的写法。具体差异见下表。

## 关键参数（实测有效值，别再改）

| 项 | 值 | 依据 / 踩坑 |
|---|---|---|
| 上行 encoder | Opus, **16000 Hz**, mono, Stream 轨 | Stream 模式只接受 16000 |
| 下行 decoder | Opus, **24000 Hz**, mono, **Segment 轨** | 见下文「下行无声」 |
| `OPUS_BITRATE` | 32000 | 单位是 **bps，不是 kbps** |
| `voice` | `longanqian_v3.1` | 3.1 Plus/Flash 的默认值；`longanqian` **只属于 3.0 系列** |
| `input/output_audio_format` | `"pcm"` | `"pcm16"` 是 OpenAI 语法，这里是**非法值** |
| `turn_detection` | `server_vad`, threshold `0.5`, silence `800` | 只在 IDLE 态可改 |
| `SAMPLE_RATE`(探针/采集) | 16000 | 麦克风链路 |

上下行采样率**不相等**是正常且必需的。早期版本这里写成"上下行都必须 16000"，是错的。

## 接入流程

1. **AAR**：直接引用仓库内产物，不走 maven
   `files("${projectDir}/../../libs/AOQ SDK/AoqClientSdk-release.aar")`
2. **Opus 插件要手动放**：AAR **不含** `libPluginOpus.so`，需从 `libs/AOQ SDK/libPluginOpus.zip`
   解压到 `app/src/main/jniLibs/{arm64-v8a,armeabi-v7a}/`，且 `abiFilters` 必须覆盖对应 ABI。
   漏掉的表现：连接、字幕、VAD 全正常，**只有音频是坏的**。
3. **凭证**（一次性，不可复用）：
   `POST https://{AOQ_WORKSPACE_ID}.{AOQ_REGION}.maas.aliyuncs.com/api/v1/webrtc/realtime?model={AOQ_MODEL}`
   → 返回 `token / sid / certFingerprint / workspaceIdHash / relayEndpoints`。
   密钥只存 `android/local.properties`（已 gitignore），经 `BuildConfig` 注入，**绝不入库**。
4. **引擎生命周期**：
   `createEngine` → `setAudioEncoderConfig` → `setAudioDecoderConfig` → `startAudioPlayer`
   → `enableSendMediaStream(Audio, false)` → `connect`
   销毁顺序：`stopAudioCapture` → `enableSendMediaStream(false)` → `disconnect` → `stopAudioPlayer` → `destroy`
5. **事件时序**（顺序错就白干）：
   `AoqConnectionStatusConnected` → 发 `session.update` → 收 `session.created` → 收 `session.updated`
   → `enableSendMediaStream(Audio, true)` → `startAudioCapture`

   常规版本**不**注册音频帧探针；只在专项排障时临时启用，定位后移除（见「诊断方法」）。

> `createEngine` 在 native 库加载失败时返回 **null**（不抛异常），声明成可空并显式判空。

## session.update 的坑

- **无效字段会被服务端静默忽略**，不报错。所以"没报错"≠"参数生效"。
  判断依据只有一个：看 `session.created` / `session.updated` 的**回显**。本次 `voice`、
  `threshold`、`silence_duration_ms` 我们改了半天，回显显示服务端默认值本来就等于我们的值——纯属空转。
- **`input_audio_transcription` 没有 `model` 字段**，只有 `language`。转写模型（`fun-asr`）由服务端
  下发；客户端设 `gpt-4o-transcribe` 之类只会得到 `invalid_value`。
- **`voice` 只能在第一次 `session.update` 里设置**，之后改无效。
- **`turn_detection` 和 `input_audio_format` 只在首次发送音频前（IDLE 态）允许修改**，
  所以 `session.update` 必须赶在 `enableSendMediaStream(true)` 之前发出。
- **ASR 增量字段是 `text` + `stash`，不是 OpenAI 的 `delta`。**
  但注意 `response.audio_transcript.delta` 用的**确实**是 `delta`——两类事件不要混。
- 未识别的事件一律打日志兜底。事件名与 OpenAI 有差异时，`when` 里静默丢弃会表现成
  "一切正常但界面空白"，极难反推。

## 下行无声：最难查的一个坑

症状：连接成功、VAD 触发、字幕逐字出、`response.audio.done` 也有，**就是耳机里没声音，且不报任何错**。

根因：**下行 decoder 必须 24000 Hz 且订阅轨必须是 Segment 模式，两者绑定。**
模型输出原生是 24kHz（`output_audio_format=pcm` 文档写明 24kHz）。

SDK 的 connect 校验报错原文给了**两条**出路：

```
subscribe audio sampleRate 24000 is not supported in Stream mode;
use Segment mode or change decoder sampleRate
```

只读后半句、把 decoder 改成 16000 并保留 Stream，就会掉进静默失败：
音频包**确实到了本机网卡**，但因声明与实发不符被**在解码前丢弃**，且不计入订阅字节、不报错。

正确配置：

```kotlin
eng.setAudioEncoderConfig(opusCodec(16000))   // 上行：16k + Stream
eng.setAudioDecoderConfig(opusCodec(24000))   // 下行：24k
subscribeTracks = listOf(
    trackParam(TrackTypeAudio, TrackModeSegment),  // ← 24k 必须配 Segment
    trackParam(TrackTypeData,  TrackModeStream),   // Data 轨保持 Stream
)
```

上行保持 16k/Stream 不要动（改了会同时毁掉 ASR/VAD）；Data 轨保持 Stream（字幕走这里）。

## 模拟器环境坑（先排除这些，再怀疑代码）

- **麦克风默认不进 guest**：必须 **Extended Controls → microphone → 勾选**，
  否则宿主麦克风根本不进系统。表现是 `mic[captured] peak` 只有 3~9（≈ −71dBFS 死噪，需临时开帧探针才看得到）、VAD 永不触发。
  也可用 `-audio winin` 启动模拟器。**这是"没反应"的第一嫌疑，成本最低，先查它。**
- Windows 侧可查注册表 `HKCU\...\CapabilityAccessManager\ConsentStore\microphone\NonPackaged`
  看有没有 emulator/qemu 的条目。
- 模拟器默认音频后端是 `dsound`，多一层混音与重采样，**音质会打折**；音质结论必须真机复核。
- **`adb pull` 在 Git-Bash 下会把 `/storage/...` 转成 Windows 路径**，要加 `MSYS_NO_PATHCONV=1`，
  且目标路径用 `cygpath -w` 转换。

## 诊断方法（本次真正定论的手段）

**以下 3~5 依赖帧探针 / WAV dump / `net recv`，常规版本都不包含，排障时需临时加回。**

按"最快定论"排序：

1. **`onStats` 里 `net recv` 增量 vs `↓data` 增量对比**（排障版才含这两个字段）—— 一刀切开
   "服务端没发音频" 和 "发了但本机没处理"。差值就是 Audio 轨收到的字节。
   本次：AI 说话 2s 内 `net recv` +52KB 而 `↓data` 只 +9.4KB → 43KB 是音频包，已到本机。
2. **`onStats` 用 `joinToString` 打整个数组**，不要 `firstOrNull()`：SDK 可能拆成多条订阅轨，
   只取第一条会漏掉真正有流量的那条。
3. **音频帧探针分段定位**：`setAudioFrameObserver` 注册一次，
   `enableAudioFrameObserver(true, AoqAudioSource, config)` 逐点开
   （`Captured` / `ProcessCaptured` / `Publish` / `Playback`），比较各点 PCM 峰值。
4. **每帧要同时报 `frames` + `peakMax` + `autoGenMute`**。只报峰值分不清三种完全不同的情况：
   `frames=0`（根本没帧）／`frames≈100, peakMax=0`（有帧全静音）／`autoGenMute=true`（SDK 补的静音占位）。
5. **把 `Playback` 点的 PCM 落成 WAV、拿到 PC 上听** —— 一次排除网络传输、Opus 解码、
   音频内容本身、上下行参数配置，把问题域缩小到 `AudioTrack` 之后（即设备/虚拟机输出链）。
   本次就是靠它定论"流没问题、是模拟器的锅"。

### 探针自己会劣化音质（务必知道）

帧回调跑在 **native 实时音频线程**上。若在回调里同步写文件，或一次开 4 个数据源，
探针本身就会造成卡顿和发糊——**然后你会把自己的诊断工具当成 bug**。
正确做法：回调线程只做 `copyOf` 入队，落盘交给 `MIN_PRIORITY` 后台线程；定位清楚后只留需要的数据源。

## 别踩的分析陷阱

- **不要用没有依据的绝对阈值判音质。** 本次曾以"3kHz 以上应占 5~15%"断言音频"闷"，
  而该阈值纯属凭空设定：正常语音天然有 60%+ 能量在 1kHz 以下。
  **没有"已知良好"的参照样本时，不要靠绝对占比下结论**，直接听原始 PCM 才是判据。
- **动手分析日志前，先核对设备上装的是不是新包**：`dumpsys package <pkg> | grep lastUpdateTime`。
  本次有一次拿旧 APK 的日志验证新代码，白跑一轮。
- `/proc/self/maps` 要用 `readText()` 一次读完。用 `bufferedReader().useLines{}` 返回惰性序列，
  流关闭后才求值，会得到 "Stream closed"。
- `.so` **在磁盘上 ≠ 已加载**，但**上行能工作 = Opus 插件确实已加载**（编解码共用同一插件）。

## 验证清单（按顺序，每步都能否证一个假设）

常规版本按以下顺序检查：

1. `↑audio` 字节持续增长、`encVol` > 0 → 上行推流正常（否则先查模拟器麦克风和权限）
2. `input_audio_buffer.speech_started` / `speech_stopped` → 服务端 VAD 在收
3. `session.updated` 回显与预期一致 → 参数真的被接受
4. `response content type=audio` → 服务端声明了音频（不是只回文本）
5. `↓audio` 字节 > 0 且 `playVol` > 0 → SDK 收下并解码了

若仍不能定位，再按「诊断方法」临时加入探针，补充检查：

1. `mic[captured] peak` > 1000 → 模拟器麦克风通了（否则先查 Extended Controls）
2. `net recv` 增量 ≫ `↓data` 增量 → 音频包到了本机
3. `mic[playback] peakMax` 跳起来 → PCM 进了播放管线
4. WAV dump 在 PC 上清晰 → 流与解码都好，剩下的问题在设备/虚拟机输出链

## 收尾待办

- **音频帧探针与 WAV 落盘已从常规版本移除**（曾会写外部缓存 + 在音频线程挂回调，影响音质与存储）。
  排障时按需临时加回，用完删掉。
- 音频焦点/路由尚未处理：`isDefaultSpeaker = true` 是硬编码，没有 `AudioManager` / `AudioFocus` 管理，
  耳机与听筒切换未验证。`isVoipMode` 现由 `BuildConfig.AOQ_USE_VOIP_MODE`（`local.properties`，默认 `true`）
  控制，**同时作用于采集与播放**，方便 A/B；保留 `true` 是为了 AEC，否则 AI 的声音会被麦克风拾回形成循环。
- 音质、延迟、蓝牙路由结论都必须真机复核，模拟器不作数。
