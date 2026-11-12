# TalkFlow Android

英语对话记单词 App 的 Android 实现（Kotlin + Jetpack Compose + Room），UI 对齐仓库根 `ui/` 下的 HTML 原型。
所有数据存在**本地 Room 数据库**；登录与充值目前是**本地假实现**，上线时替换网关即可（见文末）。

## 环境要求

| 项 | 值 |
| --- | --- |
| compileSdk | 37 |
| minSdk | 30（Android 11） |
| targetSdk | 37 |
| AGP | 9.5.0（使用内建 Kotlin，需 `android.disallowKotlinSourceSets=false`） |
| JDK | 17+，实测用 Android Studio 自带 JBR（JDK 25） |
| ABI | `armeabi-v7a`、`arm64-v8a`（AOQ 的 native 库只打了这两个） |

## 构建

```bash
cd android
JAVA_HOME="d:/AndroidStudio2026/jbr" ./gradlew.bat :app:assembleDebug --console=plain
```

产物：`app/build/outputs/apk/debug/app-debug.apk`。
安装到真机或支持 ARM 应用的模拟器：

```bash
JAVA_HOME="d:/AndroidStudio2026/jbr" ./gradlew.bat :app:installDebug
```

## 配置 AOQ 凭证（必须）

实时语音走阿里云百炼 Qwen-Audio-Realtime 的 AOQ 分轨传输。凭证通过 `BuildConfig` 注入，
**只读 `local.properties`，该文件已被 gitignore，绝不入库**。

在 `android/local.properties` 里补上（`sdk.dir` 之外的四项）：

```properties
sdk.dir=D\:\\androidSDK
AOQ_API_KEY=你的百炼APIKey
AOQ_WORKSPACE_ID=llm-4xy851f7r9core2a
AOQ_REGION=cn-beijing
AOQ_MODEL=qwen-audio-3.1-realtime-flash
# 可选：采集与播放统一做 A/B，默认 true；不要只改其中一处。
AOQ_USE_VOIP_MODE=true
```

`AOQ_REGION` 与 `AOQ_MODEL` 有默认值，可省略。取凭证的端点由 WorkspaceId 拼出：

```
POST https://{AOQ_WORKSPACE_ID}.{AOQ_REGION}.maas.aliyuncs.com/api/v1/webrtc/realtime?model={AOQ_MODEL}
```

标题总结用的是文本模型 `qwen-flash`，复用同一个 `AOQ_API_KEY`（走 OpenAI 兼容接口）。

> 未配置 Key 时应用仍可运行：语音建连会失败并显示原因与「返回会话」，挂断时的标题总结会降级为本地规则标题。

## 首次启动与体验路径

1. 闪屏 → 按登录态分流（未登录进登录页，已登录但没填档案进 Onboarding，否则进会话首页）。
2. 登录页已预填 `13888886666` / `2481`，点「进入」即可（见下方假登录规则）。
3. 首次登录会自动注册并灌入 **5 条示例会话 + 若干词条 + 学习记录**（`DemoSeeder`），方便直接看列表、单词本、学习分析的样子。
4. 会话首页顶部「自由对话」→ 授权麦克风 → 进入实时对话页。

### 实时对话页

- 顶部：关闭 / 老师名与状态（对话中 · 复习模式）/ `模式 · mm:ss` 计时 / 切换 AI 老师。
- Chip 条：对话模式（温柔·教学·纠正·闲聊）、中英字幕、沉浸式、复习模式下的「确认掌握 X」。
- 字幕：AI 回复增量流式追加，用户话语随 ASR 结果刷新；沉浸式或非双语时隐藏中文。
- 生词：AI 回复里的 `[[english|中文释义|word]]` / `[[...|phrase]]` 会渲染成可点的强调色词，
  **点击后才收进单词本**并记下来源会话；已收录的词变绿且不可重复点。
- 挂断：AI 总结标题 → 保存/更新会话 → 扣费 + 写 study_log → 直接回会话首页（无结算页）。
  从历史会话进入的**只更新那一条**（时间、时长、摘要），不新建。

## 假实现说明（上线时要换的部分）

两处网关集中在 `data/gateway/Gateways.kt`，通过 `TalkFlowApp` 手动注入，换实现不影响调用方：

- `LocalPhoneAuthGateway`：手机号 11 位且以 `1` 开头、验证码非空即通过（不真正发短信）。
  上线后新增 `RemotePhoneAuthGateway` 对接真实验证码服务。
- `LocalPaymentGateway`：任何金额直接返回订单号 `local-<时间戳>`（不调起支付）。
  上线后换成微信/支付宝下单与回调校验。充值档位：30 元/60 分钟、68 元/140 分钟（热销）、128 元/270 分钟。

另外两项待线上化：

- **API Key 目前在客户端**。正式上线应把「换取 AOQ 凭证」和「标题总结」都挪到自有后端代理，客户端不再持有 Key。
- `TalkFlowDatabase` 用了 `fallbackToDestructiveMigration(dropAllTables = true)`（开发期方便改表）。
  **发版前必须换成正式 Migration**，否则升级会清空本地数据。

## 数据模型

Room 六张表（schema 导出在 `app/schemas/`，version 1）：

| 表 | 用途 |
| --- | --- |
| `users` | 昵称、年龄、水平、学习目标、默认老师与模式、余额分钟、onboarded |
| `sessions` | 会话：标题、摘要、老师、模式、时长、fresh（是否还没被总结命名） |
| `messages` | 会话消息（role + 英文 + 中文），历史会话回看用 |
| `words` | 单词本：英文、中文、类型（word/phrase）、mastered、reviewCount、来源会话 |
| `recharge_orders` | 充值流水 |
| `study_logs` | 按天聚合的学习/复习时长、新词数、掌握数 |

枚举一律以 `name` 字符串落库（`StudyLevel` / `ChatMode` / `WordKind` / `MessageRole`），无需 TypeConverter。

## 目录

```
app/src/main/java/com/shenjj/edu/
├── TalkFlowApp.kt        应用入口 + 手动 DI 容器
├── MainActivity.kt       enableEdgeToEdge + TalkFlowTheme
├── data/
│   ├── AppRepository.kt  唯一数据出入口
│   ├── DemoSeeder.kt     首次启动示例数据
│   ├── Models.kt         领域模型（User/Session/Word/Tier…）
│   ├── gateway/          假登录/假充值网关（上线替换点）
│   └── local/            Room 实体、DAO、Database、Relations 投影
├── model/                枚举 + Teacher 目录（四位老师及渐变色）
├── ui/
│   ├── Fmt.kt            时间/金额/手机号格式化
│   ├── components/       头像、Chip、卡片、列表 cell、Sheet、Toast、TabBar…
│   ├── nav/              Routes、TabItem、AppViewModel、TalkFlowNavHost
│   ├── screens/          10 个页面（Splash/Login/Onboarding/Chats/Words/Me/Analytics/Profile/Recharge/Call）
│   └── theme/            Color/Type/Shape/Theme（对齐原型 token，已关 dynamicColor）
└── voice/                AOQ：TokenClient、VoiceEngine、CallViewModel、标题总结、前台服务
```

## 语音回归与 `isVoipMode` A/B

常规版本不注册 PCM 帧探针，也不写 `aoq-playback.wav`。保留 `AoqVoice` 下的
session 关键配置回显、上下行音频字节/音量、VAD、设备错误与路由日志。

1. 先在模拟器 **Extended Controls → Microphone** 启用宿主麦克风，或启动时加 `-audio winin`。
   同时授予 App 录音权限，并确认安装包已更新：
   `adb shell dumpsys package com.shenjj.edu | grep lastUpdateTime`。
2. 保持 `AOQ_USE_VOIP_MODE=true`，重新构建、安装并彻底重启 App。先验证“仅清理诊断代码”：
   能连接并收到 `session.updated`、讲话有 VAD 与字幕、AI 有语音回复、挂断后可再次通话。
3. 需要对照时改为 `AOQ_USE_VOIP_MODE=false`，重新构建安装。该值同时作用于采集和播放，
   通过日志 `audio config voip=...` 确认测试的是新配置；不要只改其中一处。
4. 观察 `audio stats`：讲话时 `↑` 字节持续增长，AI 回复时 `↓` 字节和 `playVol` 增长。
   `encVol`/`playVol` 是辅助指标，不是 PCM 峰值。`RecordFail`/`PlayFail` 或启动返回非 0
   应显示错误，不能只看界面“对话中”。

`isVoipMode` 在 SDK 中默认是 `false`，会传入 native 音频实现。当前保留 `true` 是为了
通话音频处理与回声抑制；关闭可能改变音量通道、音频路由以及 AEC/降噪效果。AAR 没有
公开完整映射，不能把模拟器听感当作这些系统行为的证明。模拟器仅回归连接、VAD、字幕
与链路日志；外放回声、音质、延迟和蓝牙耳机路由必须在真机上复核。

## 排错

- **进通话页显示「暂时连不上语音服务」**：检查 `local.properties` 四项是否齐全、Key 是否有效、网络是否可达百炼。
- **提示需要麦克风权限**：系统设置里给 TalkFlow 授予录音权限后重进（Android 13+ 还需通知权限才看得到通话中的前台通知）。
- **改了表结构后崩溃**：开发期靠破坏性迁移，卸载重装即可；正式版本请写 Migration。
- **构建报 source set 冲突**：确认 `gradle.properties` 里 `android.disallowKotlinSourceSets=false` 还在（AGP 内建 Kotlin 与 KSP 共存所需）。
