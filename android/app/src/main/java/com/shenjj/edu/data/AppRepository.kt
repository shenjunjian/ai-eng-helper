package com.shenjj.edu.data

import com.shenjj.edu.data.local.SessionWithWordCount
import com.shenjj.edu.data.local.StudyTotals
import com.shenjj.edu.data.local.WordCounts
import com.shenjj.edu.data.local.WordWithSession
import com.shenjj.edu.data.local.entity.MessageEntity
import com.shenjj.edu.data.local.entity.RechargeOrderEntity
import com.shenjj.edu.data.local.entity.SessionEntity
import com.shenjj.edu.data.local.entity.StudyLogEntity
import com.shenjj.edu.data.local.entity.UserEntity
import com.shenjj.edu.data.local.entity.WordEntity
import com.shenjj.edu.data.local.TalkFlowDatabase
import com.shenjj.edu.model.ChatMode
import com.shenjj.edu.model.MessageRole
import com.shenjj.edu.model.StudyLevel
import com.shenjj.edu.model.TeacherCatalog
import com.shenjj.edu.model.WordKind
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * 应用唯一的业务数据入口。UI 只依赖本类，不直接接触 DAO 与实体，
 * 以便未来把登录/充值换成线上服务时改动集中在这一层。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppRepository(
    private val db: TalkFlowDatabase,
    private val auth: com.shenjj.edu.data.gateway.PhoneAuthGateway,
    private val payment: com.shenjj.edu.data.gateway.PaymentGateway,
    private val seeder: DemoSeeder,
) {
    private val userDao get() = db.userDao()
    private val sessionDao get() = db.sessionDao()
    private val messageDao get() = db.messageDao()
    private val wordDao get() = db.wordDao()
    private val orderDao get() = db.rechargeOrderDao()
    private val logDao get() = db.studyLogDao()

    // ---------- 用户 / 登录（本地假登录） ----------

    fun observeCurrentUser(): Flow<User?> =
        userDao.observeCurrentUser().map { it?.toUser() }

    suspend fun currentUser(): User? = userDao.currentUser()?.toUser()

    /** 发送验证码：假网关直接成功 */
    suspend fun sendCode(phone: String): Result<Unit> = auth.sendCode(phone)

    /** 验证码登录：手机号不存在则注册，存在则登录 */
    suspend fun login(phone: String, code: String): LoginResult {
        val verified = auth.verifyCode(phone, code).getOrElse {
            return LoginResult.Failure(it.message ?: "登录失败")
        }
        val now = System.currentTimeMillis()
        val existing = userDao.findByPhone(verified)
        val isNew = existing == null
        val user = if (existing == null) {
            val id = userDao.insert(
                UserEntity(
                    phone = verified,
                    nickname = "学习者",
                    age = 0,
                    level = StudyLevel.INTERMEDIATE.name,
                    defaultTeacherId = TeacherCatalog.DEFAULT_ID,
                    defaultChatMode = TeacherCatalog.byId(null).style.name,
                    // 演示余额：与原型一致的 ¥25.00（上线后由充值服务决定初始额度）
                    balanceCents = 2500,
                    createdAt = now,
                    lastActiveAt = now,
                )
            )
            userDao.findById(id)!!
        } else {
            existing
        }
        userDao.clearLogin()
        userDao.markLoggedIn(user.id, now)
        // 首次注册写入与原型一致的示例数据，便于界面直接可评审
        if (isNew) seedIfNeeded()
        return LoginResult.Success(userDao.findById(user.id)!!.toUser())
    }

    suspend fun logout() {
        userDao.clearLogin()
    }

    /** 保存学习档案（onboarding 或个人设置） */
    suspend fun saveProfile(
        nickname: String,
        age: Int,
        level: StudyLevel,
        goals: List<String>,
        teacherId: String,
        chatMode: ChatMode? = null,
        immersive: Boolean? = null,
    ) {
        val user = userDao.currentUser() ?: return
        val now = System.currentTimeMillis()
        userDao.update(
            user.copy(
                nickname = nickname.ifBlank { user.nickname },
                age = age,
                level = level.name,
                goals = goals.joinToString(","),
                defaultTeacherId = teacherId,
                defaultChatMode = (chatMode ?: ChatMode.fromName(user.defaultChatMode)).name,
                defaultImmersive = immersive ?: user.defaultImmersive,
                onboarded = true,
                lastActiveAt = now,
            )
        )
    }

    suspend fun updateSettingDefaults(
        teacherId: String?,
        chatMode: ChatMode?,
        immersive: Boolean?,
    ) {
        val user = userDao.currentUser() ?: return
        userDao.update(
            user.copy(
                defaultTeacherId = teacherId ?: user.defaultTeacherId,
                defaultChatMode = (chatMode ?: ChatMode.fromName(user.defaultChatMode)).name,
                defaultImmersive = immersive ?: user.defaultImmersive,
            )
        )
    }

    // ---------- 充值（本地假充值） ----------

    val rechargeTiers = listOf(
        RechargeTier(amountYuan = 30, minutes = 60),
        RechargeTier(amountYuan = 68, minutes = 140, tag = "热销"),
        RechargeTier(amountYuan = 128, minutes = 270),
        RechargeTier(amountYuan = 198, minutes = 420, tag = "超值"),
    )

    /** 假充值：网关返回成功后写流水并给余额加钱 */
    suspend fun recharge(tier: RechargeTier, channel: String = "wechat"): RechargeResult {
        val user = userDao.currentUser() ?: return RechargeResult.Failure("请先登录")
        val amountCents = tier.amountYuan * 100
        payment.pay(amountCents, tier.minutes).getOrElse {
            return RechargeResult.Failure(it.message ?: "支付失败")
        }
        val rowId = orderDao.insert(
            RechargeOrderEntity(
                userId = user.id,
                amountCents = amountCents,
                minutes = tier.minutes,
                channel = channel,
                status = "paid",
            )
        )
        userDao.addBalance(user.id, amountCents)
        return RechargeResult.Success(rowId, user.balanceCents + amountCents)
    }

    fun observeOrders(): Flow<List<RechargeOrderEntity>> =
        observeCurrentUserId().flatMapLatest { id ->
            if (id == null) kotlinx.coroutines.flow.flowOf(emptyList<RechargeOrderEntity>())
            else orderDao.observeOrders(id)
        }

    /** 扣费：按通话时长扣减余额（1 元 ≈ 2 分钟 → 每分钟 50 分） */
    suspend fun consumeForCall(seconds: Int): Int {
        val user = userDao.currentUser() ?: return 0
        val minutes = if (seconds <= 0) 0 else ((seconds + 59) / 60)
        val cost = minutes * CENTS_PER_MINUTE
        if (cost <= 0) return 0
        userDao.addBalance(user.id, -cost)
        return cost
    }

    /** 余额是否足够开始 N 分钟对话 */
    fun affordableMinutes(balanceCents: Int): Int =
        if (balanceCents <= 0) 0 else balanceCents / CENTS_PER_MINUTE

    // ---------- 会话 ----------

    fun observeSessions(): Flow<List<SessionWithWordCount>> =
        observeCurrentUserId().flatMapLatest { id ->
            if (id == null) kotlinx.coroutines.flow.flowOf(emptyList<SessionWithWordCount>())
            else sessionDao.observeSessionsWithWordCount(id)
        }

    fun observeSession(id: String): Flow<SessionEntity?> = sessionDao.observeSession(id)

    suspend fun sessionById(id: String): SessionEntity? = sessionDao.findById(id)

    /** 点击历史会话：置顶并返回会话 */
    suspend fun openSession(id: String): SessionEntity? {
        sessionDao.touch(id)
        return sessionDao.findById(id)
    }

    /** 新建自由对话会话 */
    suspend fun startFreeChat(teacherId: String, mode: ChatMode): SessionEntity {
        val user = userDao.currentUser()
            ?: error("startFreeChat 需要已登录用户")
        val now = System.currentTimeMillis()
        val session = SessionEntity(
            id = newSessionId(),
            userId = user.id,
            title = "新的自由对话",
            teacherId = teacherId,
            mode = mode.name,
            preview = "随时开口 · 话题随你",
            fresh = true,
            review = false,
            createdAt = now,
            lastMessageAt = now,
        )
        sessionDao.upsert(session)
        return session
    }

    /** 新建复习会话（从单词本发起，标题固定，review = true） */
    suspend fun startReviewSession(teacherId: String, mode: ChatMode, wordCount: Int): SessionEntity {
        val user = userDao.currentUser()
            ?: error("startReviewSession 需要已登录用户")
        val now = System.currentTimeMillis()
        val session = SessionEntity(
            id = newSessionId(),
            userId = user.id,
            title = "单词复习 · $wordCount 个词",
            teacherId = teacherId,
            mode = mode.name,
            preview = "围绕已收录的词汇展开练习",
            fresh = true,
            review = true,
            createdAt = now,
            lastMessageAt = now,
        )
        sessionDao.upsert(session)
        return session
    }

    /**
     * 挂断结算：新生成的自由对话用 AI 总结标题命名；
     * 历史会话只更新（时间、时长、摘要），不新建。
     */
    suspend fun settleSession(
        sessionId: String,
        aiTitle: String?,
        preview: String?,
        teacherId: String,
        mode: ChatMode,
        addedSeconds: Int,
        review: Boolean,
    ) {
        val session = sessionDao.findById(sessionId) ?: return
        val now = System.currentTimeMillis()
        // 历史会话（非 fresh）保持原标题，只更新其余字段
        val title = if (session.fresh && !aiTitle.isNullOrBlank()) aiTitle.trim() else session.title
        sessionDao.settle(
            id = sessionId,
            title = title,
            preview = (preview ?: session.preview).ifBlank { "随时开口 · 话题随你" },
            teacherId = teacherId,
            mode = mode.name,
            fresh = false,
            addedSeconds = addedSeconds,
            at = now,
        )
        recordUsage(
            studySeconds = if (review) 0 else addedSeconds,
            reviewSeconds = if (review) addedSeconds else 0,
            newWords = 0,
            masteredWords = 0,
        )
        consumeForCall(addedSeconds)
    }

    suspend fun updateSessionTeacher(sessionId: String, teacherId: String) =
        sessionDao.updateTeacher(sessionId, teacherId)

    suspend fun deleteSession(sessionId: String) = sessionDao.deleteById(sessionId)

    fun observeSessionCount(): Flow<Int> =
        observeCurrentUserId().flatMapLatest { id ->
            if (id == null) kotlinx.coroutines.flow.flowOf(0) else sessionDao.observeSessionCount(id)
        }

    // ---------- 消息（字幕） ----------

    fun observeMessages(sessionId: String): Flow<List<MessageEntity>> =
        messageDao.observeBySession(sessionId)

    suspend fun latestMessages(sessionId: String, limit: Int = 6): List<MessageEntity> =
        messageDao.latestBySession(sessionId, limit)

    /** 保存一整轮字幕：AI 与用户各一条 */
    suspend fun saveTurn(sessionId: String, userText: String?, aiEn: String, aiZh: String) {
        val now = System.currentTimeMillis()
        val list = buildList {
            if (!userText.isNullOrBlank()) {
                add(
                    MessageEntity(
                        sessionId = sessionId,
                        role = MessageRole.USER.name,
                        textEn = userText,
                        createdAt = now - 1,
                    )
                )
            }
            add(
                MessageEntity(
                    sessionId = sessionId,
                    role = MessageRole.AI.name,
                    textEn = aiEn,
                    textZh = aiZh,
                    createdAt = now,
                )
            )
        }
        messageDao.insertAll(list)
    }

    // ---------- 单词本 ----------

    fun observeWords(filter: WordFilter): Flow<List<WordWithSession>> =
        observeCurrentUserId().flatMapLatest { id ->
            if (id == null) kotlinx.coroutines.flow.flowOf(emptyList<WordWithSession>())
            else wordDao.observeWords(id, filter.queryValue)
        }

    fun observeWord(id: Long): Flow<WordWithSession?> = wordDao.observeWord(id)

    fun observeWordCounts(): Flow<WordCounts> =
        observeCurrentUserId().flatMapLatest { id ->
            if (id == null) kotlinx.coroutines.flow.flowOf(WordCounts())
            else wordDao.observeCounts(id)
        }

    fun observePendingWordCount(): Flow<Int> =
        observeCurrentUserId().flatMapLatest { id ->
            if (id == null) kotlinx.coroutines.flow.flowOf(0) else wordDao.observePendingCount(id)
        }

    suspend fun existingTerms(): List<String> =
        userDao.currentUser()?.let { wordDao.existingTerms(it.id) } ?: emptyList()

    /** 字幕内联生词被点击确认后收录；同词条已存在则忽略 */
    suspend fun saveWord(en: String, zh: String, kind: WordKind, sessionId: String?): WordEntity? {
        val user = userDao.currentUser() ?: return null
        val term = en.trim()
        if (term.isEmpty()) return null
        val rowId = wordDao.insert(
            WordEntity(
                userId = user.id,
                en = term,
                zh = zh.trim(),
                kind = kind.name,
                sessionId = sessionId,
            )
        )
        if (rowId == -1L) return null // 已收录过
        recordUsage(studySeconds = 0, reviewSeconds = 0, newWords = 1, masteredWords = 0)
        return wordDao.findById(rowId)
    }

    /** 标记已掌握（软删除出待复习列表）或放回 */
    suspend fun setMastered(wordId: Long, mastered: Boolean) {
        wordDao.setMastered(wordId, mastered)
        if (mastered) recordUsage(0, 0, 0, 1)
    }

    suspend fun markReviewed(wordId: Long) = wordDao.incrementReviewCount(wordId)

    suspend fun deleteWord(wordId: Long) = wordDao.deleteById(wordId)

    // ---------- 学习分析 ----------

    fun observeAnalytics(rangeDays: Int): Flow<Analytics> {
        val days = (rangeDays - 1 downTo 0).map { daysAgo(it) }
        val dayKeys = days.map { dayString(it) }
        val fromDay = dayKeys.first()
        return observeCurrentUserId().flatMapLatest { id ->
            if (id == null) {
                kotlinx.coroutines.flow.flowOf(
                    Analytics(
                        daily = dayKeys.map { DayPoint(it, 0, 0, 0) },
                        totals = StudyTotals(),
                        counts = WordCounts(),
                        streak = 0,
                        mastered = 0,
                        newWordsInRange = 0,
                    )
                )
            } else {
                kotlinx.coroutines.flow.combine(
                    logDao.observeSince(id, fromDay),
                    wordDao.observeCounts(id),
                ) { logs, counts ->
                    val byDay = logs.associateBy { it.day }
                    val daily = dayKeys.map { key ->
                        val log = byDay[key]
                        DayPoint(
                            day = key,
                            studySeconds = log?.studySeconds ?: 0,
                            reviewSeconds = log?.reviewSeconds ?: 0,
                            newWords = log?.newWords ?: 0,
                        )
                    }
                    Analytics(
                        daily = daily,
                        totals = StudyTotals(
                            studySeconds = logs.sumOf { it.studySeconds.toLong() },
                            reviewSeconds = logs.sumOf { it.reviewSeconds.toLong() },
                        ),
                        counts = counts,
                        streak = computeStreak(logs.map { it.day }),
                        mastered = counts.mastered,
                        newWordsInRange = counts.total,
                    )
                }
            }
        }
    }

    // ---------- 种子数据 ----------

    /** 首次启动写入原型示例数据，便于直接看到界面效果 */
    suspend fun seedIfNeeded(force: Boolean = false) = seeder.seedIfNeeded(force)

    // ---------- 内部 ----------

    private fun observeCurrentUserId(): Flow<Long?> =
        userDao.observeCurrentUser().map { it?.id }

    /** 写入当天学习记录（按天累加） */
    private suspend fun recordUsage(
        studySeconds: Int,
        reviewSeconds: Int,
        newWords: Int,
        masteredWords: Int,
    ) {
        val user = userDao.currentUser() ?: return
        if (studySeconds <= 0 && reviewSeconds <= 0 && newWords <= 0 && masteredWords <= 0) return
        val day = dayString(Date())
        if (logDao.find(user.id, day) == null) {
            logDao.insert(StudyLogEntity(userId = user.id, day = day))
        }
        logDao.accumulate(user.id, day, studySeconds, reviewSeconds, newWords, masteredWords)
    }

    private fun computeStreak(activeDays: List<String>): Int {
        if (activeDays.isEmpty()) return 0
        val set = activeDays.toHashSet()
        var streak = 0
        var cursor = Calendar.getInstance()
        // 今天没有记录不算断连，从昨天起算
        if (!set.contains(dayString(cursor.time))) {
            cursor = daysBefore(1)
        }
        while (set.contains(dayString(cursor.time))) {
            streak++
            cursor = daysBefore(1, cursor.time)
        }
        return streak
    }

    private fun daysBefore(n: Int, from: Date = Date()): Calendar =
        Calendar.getInstance().apply {
            time = from
            add(Calendar.DAY_OF_YEAR, -n)
        }

    private fun daysAgo(n: Int): Date = daysBefore(n).time

    private fun dayString(date: Date): String = DAY_FORMAT.format(date)

    private fun UserEntity.toUser() = User(
        id = id,
        phone = phone,
        nickname = nickname,
        age = age,
        level = StudyLevel.fromName(level),
        goals = if (goals.isBlank()) emptyList() else goals.split(","),
        balanceCents = balanceCents,
        defaultTeacherId = defaultTeacherId,
        defaultChatMode = ChatMode.fromName(defaultChatMode),
        defaultImmersive = defaultImmersive,
        onboarded = onboarded,
    )

    private fun newSessionId(): String = UUID.randomUUID().toString()

    companion object {
        /** 1 元 ≈ 2 分钟 → 每分钟 50 分 */
        const val CENTS_PER_MINUTE = 50
        private val DAY_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    }
}

/** 单词本筛选 */
enum class WordFilter(val label: String, val queryValue: String) {
    ALL("全部", "all"),
    PENDING("待复习", "pending"),
    WORD("单词", "WORD"),
    PHRASE("习语", "PHRASE"),
    MASTERED("已掌握", "done"),
}

/** 分析页单日数据点 */
data class DayPoint(
    val day: String,
    val studySeconds: Int,
    val reviewSeconds: Int,
    val newWords: Int,
) {
    val totalSeconds: Int get() = studySeconds + reviewSeconds
    val label: String get() = day.takeLast(2)
}

/** 分析页聚合结果 */
data class Analytics(
    val daily: List<DayPoint>,
    val totals: StudyTotals,
    val counts: WordCounts,
    val streak: Int,
    val mastered: Int,
    val newWordsInRange: Int,
) {
    val totalMinutes: Int get() = ((totals.studySeconds + totals.reviewSeconds) / 60).toInt()
    val studyMinutes: Int get() = (totals.studySeconds / 60).toInt()
    val reviewMinutes: Int get() = (totals.reviewSeconds / 60).toInt()
}
