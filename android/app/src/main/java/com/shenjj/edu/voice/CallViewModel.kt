package com.shenjj.edu.voice

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shenjj.edu.TalkFlowApp
import com.shenjj.edu.data.AppRepository
import com.shenjj.edu.data.local.entity.SessionEntity
import com.shenjj.edu.model.ChatMode
import com.shenjj.edu.model.MessageRole
import com.shenjj.edu.model.Teacher
import com.shenjj.edu.model.TeacherCatalog
import com.shenjj.edu.model.WordKind
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** 通话页进入参数（都来自路由，可空/缺省表示「新建自由对话」） */
data class CallArgs(
    val sessionId: String = "",
    val teacherId: String = "",
    val wordId: Long = -1L,
)

enum class CallPhase { Idle, Connecting, Ready, Failed }

/** 一条字幕。streaming=true 表示还在增量输出（未定稿） */
data class Caption(
    val id: Long,
    val role: MessageRole,
    val en: String,
    val zh: String = "",
    val streaming: Boolean = false,
)

data class CallUiState(
    val phase: CallPhase = CallPhase.Idle,
    val error: String? = null,
    val seconds: Int = 0,
    val captions: List<Caption> = emptyList(),
    /** 已收进单词本的词条（小写英文），用于把内联词渲染成「已收录」态 */
    val savedTerms: Set<String> = emptySet(),
    val teacher: Teacher = TeacherCatalog.ALL.first(),
    val mode: ChatMode = ChatMode.TEACHING,
    val bilingual: Boolean = true,
    val immersive: Boolean = false,
    val reviewMode: Boolean = false,
    /** 复习模式下正在练的那个词 */
    val reviewWordEn: String? = null,
    val reviewWordDone: Boolean = false,
    val sessionId: String = "",
    val sessionTitle: String = "",
)

/**
 * 通话页的 ViewModel：持有 AOQ 引擎与字幕状态，隔离重组。
 *
 * 生命周期：[start] 建会话 → 取一次性凭证 → 起引擎；[hangup] 停引擎并结算（p5c）。
 * 引擎与凭证都不进 UI 层，UI 只消费 [state] 与调用动作方法。
 */
class CallViewModel(
    app: Application,
    private val args: CallArgs,
) : AndroidViewModel(app) {

    private val repository: AppRepository = (app as TalkFlowApp).repository
    private val tokenClient = AoqTokenClient()
    private val engine = AoqVoiceEngine(app)
    private val titleSummarizer = SessionTitleSummarizer()

    private val _state = MutableStateFlow(CallUiState())
    val state: StateFlow<CallUiState> = _state

    private var timerJob: Job? = null
    private var eventJob: Job? = null
    /** 字幕条目 id 发号器（LazyColumn 的 key 要稳定，不能用列表下标） */
    private var captionSeq = 0L
    /** 本轮用户说的话，response.done 时与 AI 回复一起落库 */
    private var pendingUserText: String? = null
    private var started = false

    /** 进入通话页后调用一次：解析会话 → 取凭证 → 建连 */
    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            _state.update { it.copy(phase = CallPhase.Connecting) }

            val user = repository.currentUser()
            if (user == null) {
                _state.update { it.copy(phase = CallPhase.Failed, error = "登录态已失效，请重新登录") }
                return@launch
            }
            val session = resolveSession()
            if (session == null) {
                _state.update { it.copy(phase = CallPhase.Failed, error = "无法建立会话") }
                return@launch
            }

            val teacher = TeacherCatalog.byId(
                args.teacherId.ifBlank { session.teacherId },
            )
            val reviewWord = if (args.wordId > 0) repository.wordById(args.wordId) else null
            val immersiveDefault = user.defaultImmersive

            _state.update {
                it.copy(
                    teacher = teacher,
                    mode = ChatMode.fromName(session.mode),
                    // 沉浸模式下只出英文字幕（AGENTS.md：沉浸式环境隐藏中文字幕）
                    bilingual = !immersiveDefault,
                    immersive = immersiveDefault,
                    reviewMode = args.wordId > 0,
                    reviewWordEn = reviewWord?.en,
                    reviewWordDone = reviewWord?.mastered == true,
                    sessionId = session.id,
                    sessionTitle = session.title,
                    savedTerms = repository.existingTerms().map { en -> en.lowercase() }.toSet(),
                )
            }

            collectEvents()

            when (val result = tokenClient.fetch()) {
                is TokenResult.Ok -> engine.start(
                    credential = result.credential,
                    instructions = buildInstructions(teacher, user?.nickname ?: "同学"),
                )

                is TokenResult.Failed -> _state.update {
                    it.copy(phase = CallPhase.Failed, error = result.reason)
                }
            }
        }
    }

    /**
     * 挂断：停引擎与前台服务，结算本次对话（p5c 实现标题总结/扣费/写 study_log）。
     * 返回本次通话秒数，供调用方决定是否需要在返回前提示。
     */
    fun hangup(onDone: () -> Unit) {
        viewModelScope.launch {
            timerJob?.cancel()
            eventJob?.cancel()
            val s = _state.value
            // 先把字幕抓出来：引擎一停，Data 轨就再也收不到事件了
            val transcript = buildTranscript(s.captions)
            engine.stop()
            CallForegroundService.stop(getApplication())
            if (s.seconds > 0 && s.sessionId.isNotBlank()) {
                // 标题总结：成功用 AI 标题，超时/失败降级为本地规则标题。
                // settleSession 内部只给 fresh 会话改名，历史会话保持原标题（AGENTS.md）。
                val title = withTimeoutOrNull(TITLE_TIMEOUT_MS) {
                    titleSummarizer.summarize(transcript)
                } ?: fallbackTitle(s)
                // settleSession 内部已完成：更新会话（标题/时长/摘要/置顶）+ 写 study_log + 扣费
                repository.settleSession(
                    sessionId = s.sessionId,
                    aiTitle = title,
                    preview = lastAiPreview(),
                    teacherId = s.teacher.id,
                    mode = s.mode,
                    addedSeconds = s.seconds,
                    review = s.reviewMode,
                )
            }
            onDone()
        }
    }

    /** 把字幕拼成「老师/学生」对话文本，剥掉内联生词标记，供标题模型阅读 */
    private fun buildTranscript(captions: List<Caption>): String =
        captions.joinToString("\n") { c ->
            val who = if (c.role == MessageRole.USER) "学生" else "老师"
            "$who：${INLINE_WORD_REGEX.replace(c.en, "$1").trim()}"
        }

    /**
     * 降级标题：AI 总结不可用时（无网络/超时/未配 Key）保证会话仍有个像样的名字。
     * 复习模式点名所练的词；自由对话用「与 X 的对话」，与老师名一致。
     */
    private fun fallbackTitle(s: CallUiState): String =
        if (s.reviewMode && !s.reviewWordEn.isNullOrBlank()) "复习 ${s.reviewWordEn}"
        else "与 ${s.teacher.name} 的对话"

    /** 会话摘要：最后一条 AI 回复（剥掉内联生词标记，只留纯文本） */
    private fun lastAiPreview(): String? =
        _state.value.captions
            .lastOrNull { it.role == MessageRole.AI && it.en.isNotBlank() }
            ?.let { INLINE_WORD_REGEX.replace(it.en, "$1").trim() }
            ?.takeIf { it.isNotBlank() }

    // ---------- 通话中的交互 ----------

    fun setMode(mode: ChatMode) {
        _state.update { it.copy(mode = mode) }
    }

    /** 用户拒绝了录音权限：没有上行音频就无法通话，直接进失败态 */
    fun reportPermissionDenied() {
        _state.update {
            it.copy(
                phase = CallPhase.Failed,
                error = "需要麦克风权限才能进行语音对话。\n请在系统设置里授予 TalkFlow 录音权限后重试。",
            )
        }
    }

    fun toggleBilingual() = _state.update { it.copy(bilingual = !it.bilingual) }

    fun toggleImmersive() = _state.update { it.copy(immersive = !it.immersive) }

    fun switchTeacher(teacher: Teacher) {
        _state.update { it.copy(teacher = teacher) }
        val sessionId = _state.value.sessionId
        if (sessionId.isNotBlank()) {
            // AGENTS.md：对话中切换老师时，会话头像同步更新
            viewModelScope.launch { repository.updateSessionTeacher(sessionId, teacher.id) }
        }
    }

    /** 点击字幕里的内联词 → 收进单词本，并记下来源会话 */
    fun saveInlineWord(en: String, zh: String, kind: WordKind) {
        val sessionId = _state.value.sessionId
        viewModelScope.launch {
            repository.saveWord(en = en, zh = zh, kind = kind, sessionId = sessionId.ifBlank { null })
            _state.update { it.copy(savedTerms = it.savedTerms + en.lowercase()) }
        }
    }

    /** 复习模式下确认掌握：标记已掌握 + 计一次复习 */
    fun confirmMastered() {
        if (args.wordId <= 0) return
        viewModelScope.launch {
            repository.setMastered(args.wordId, true)
            repository.markReviewed(args.wordId)
            _state.update { it.copy(reviewWordDone = true) }
        }
    }

    // ---------- 内部 ----------

    /**
     * 会话解析：
     * - 带 sessionId：复习历史会话（挂断只更新它，不新建）→ openSession 顺便置顶
     * - 带 wordId：进该词来源会话；来源已删则新建复习会话
     * - 都没有：新建自由对话
     */
    private suspend fun resolveSession(): SessionEntity? {
        val user = repository.currentUser() ?: return null
        if (args.sessionId.isNotBlank()) return repository.openSession(args.sessionId)
        if (args.wordId > 0) {
            val word = repository.wordById(args.wordId)
            word?.sessionId?.let { sid -> repository.openSession(sid)?.let { return it } }
            return repository.startReviewSession(
                teacherId = user.defaultTeacherId,
                mode = user.defaultChatMode,
                wordCount = 1,
            )
        }
        return repository.startFreeChat(
            teacherId = args.teacherId.ifBlank { user.defaultTeacherId },
            mode = user.defaultChatMode,
        )
    }

    private fun collectEvents() {
        eventJob = viewModelScope.launch {
            engine.events.collect { event -> onEngineEvent(event) }
        }
    }

    private suspend fun onEngineEvent(event: AoqEvent) {
        when (event) {
            AoqEvent.SessionReady -> {
                _state.update { it.copy(phase = CallPhase.Ready, error = null) }
                CallForegroundService.start(
                    getApplication(),
                    "${_state.value.teacher.name} · 对话中",
                )
                startTimer()
            }

            is AoqEvent.Status -> if (!event.connected && _state.value.phase == CallPhase.Ready) {
                _state.update { it.copy(phase = CallPhase.Failed, error = "连接已断开") }
            }

            is AoqEvent.UserTranscript -> upsertCaption(
                role = MessageRole.USER,
                text = event.text,
                final = event.isFinal,
            )

            is AoqEvent.AiDelta -> appendAi(event.text)

            is AoqEvent.AiDone -> finishAi(event.text)

            AoqEvent.ResponseDone -> persistTurn()

            is AoqEvent.Failure -> _state.update {
                it.copy(phase = CallPhase.Failed, error = event.reason)
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                _state.update { it.copy(seconds = it.seconds + 1) }
            }
        }
    }

    private fun upsertCaption(role: MessageRole, text: String, final: Boolean) {
        if (text.isBlank() && !final) return
        _state.update { state ->
            val captions = state.captions.toMutableList()
            val lastIndex = captions.indexOfLast { it.role == role && it.streaming }
            if (lastIndex >= 0) {
                captions[lastIndex] = captions[lastIndex].copy(en = text, streaming = !final)
            } else {
                captions += Caption(id = ++captionSeq, role = role, en = text, streaming = !final)
            }
            state.copy(captions = captions)
        }
        if (final && role == MessageRole.USER) pendingUserText = text
    }

    private fun appendAi(delta: String) {
        if (delta.isEmpty()) return
        _state.update { state ->
            val captions = state.captions.toMutableList()
            val lastIndex = captions.indexOfLast { it.role == MessageRole.AI && it.streaming }
            if (lastIndex >= 0) {
                val current = captions[lastIndex]
                captions[lastIndex] = current.copy(en = current.en + delta)
            } else {
                captions += Caption(
                    id = ++captionSeq,
                    role = MessageRole.AI,
                    en = delta,
                    streaming = true,
                )
            }
            state.copy(captions = captions)
        }
    }

    private fun finishAi(fullText: String) {
        _state.update { state ->
            val captions = state.captions.toMutableList()
            val lastIndex = captions.indexOfLast { it.role == MessageRole.AI && it.streaming }
            if (lastIndex >= 0) {
                captions[lastIndex] = captions[lastIndex].copy(
                    en = fullText.ifBlank { captions[lastIndex].en },
                    streaming = false,
                )
            }
            state.copy(captions = captions)
        }
    }

    /** 一轮结束：把用户话 + AI 回复写进 messages（历史会话回看用） */
    private suspend fun persistTurn() {
        val sessionId = _state.value.sessionId
        if (sessionId.isBlank()) return
        val ai = _state.value.captions.lastOrNull { it.role == MessageRole.AI } ?: return
        if (ai.en.isBlank()) return
        repository.saveTurn(
            sessionId = sessionId,
            userText = pendingUserText,
            aiEn = ai.en,
            aiZh = ai.zh,
        )
        pendingUserText = null
    }

    /**
     * 系统提示：老师人设 + 对话模式 + 生词内联标记规则。
     *
     * 生词用 `[[english|中文释义|word]]` / `[[english|中文释义|phrase]]` 内联标记，
     * 只出现在 Data 轨的字幕文本里（不会被 TTS 读出），用户点击后才收录。
     */
    private fun buildInstructions(teacher: Teacher, nickname: String): String {
        val s = _state.value
        val reviewPart = if (s.reviewMode && s.reviewWordEn != null) {
            "本次是复习模式，请围绕单词「${s.reviewWordEn}」展开练习，" +
                "引导学生在真实语境里用它造句，并在学生用对后给予肯定。"
        } else {
            "本次是自由对话，请选一个日常生活话题开始，逐步展开。"
        }
        return buildString {
            append("你是英语老师 ").append(teacher.name).append("（").append(teacher.desc).append("）。")
            append("正在与学生 ").append(nickname).append(" 进行实时英语口语对话。")
            append("当前对话模式：").append(s.mode.label).append("。")
            append(reviewPart)
            append("说话要求：每次回复控制在两三句以内，口语化、自然，语速适中；")
            append("先回应学生上一句，再抛出一个开放式问题推动对话。")
            append("生词标记要求：当回复里出现值得积累的新单词或习语时，")
            append("在英文文本中用 [[english|中文释义|word]] 或 [[english|中文释义|phrase]] 的形式内联标记，")
            append("每次回复最多标记 2 个，标记不要影响句子本身的通顺；")
            append("这些标记只用于字幕展示，不要在语音里读出方括号或中文释义。")
            append("除标记外，回复内容全部使用英语，不要输出整句中文翻译。")
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        eventJob?.cancel()
        engine.stop()
        super.onCleared()
    }

    companion object {
        fun factory(app: TalkFlowApp, args: CallArgs): ViewModelProvider.Factory = viewModelFactory {
            initializer { CallViewModel(app, args) }
        }

        /**
         * 内联生词标记：`[[english|中文释义|word|phrase]]`，只存在于 Data 轨字幕文本里。
         * 三个分组依次为英文词条、中文释义、类型（word/phrase）。
         */
        val INLINE_WORD_REGEX =
            Regex("""\[\[\s*([^|\[\]]+?)\s*\|\s*([^|\[\]]+?)\s*\|\s*(word|phrase)\s*\]\]""")

        /** 标题总结的等待上限：超时就降级成本地规则标题，不能让用户卡在通话页 */
        const val TITLE_TIMEOUT_MS = 8_000L
    }
}
