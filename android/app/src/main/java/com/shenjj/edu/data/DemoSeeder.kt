package com.shenjj.edu.data

import android.content.Context
import android.content.SharedPreferences
import com.shenjj.edu.data.local.TalkFlowDatabase
import com.shenjj.edu.data.local.entity.MessageEntity
import com.shenjj.edu.data.local.entity.SessionEntity
import com.shenjj.edu.data.local.entity.StudyLogEntity
import com.shenjj.edu.data.local.entity.WordEntity
import com.shenjj.edu.model.ChatMode
import com.shenjj.edu.model.MessageRole
import com.shenjj.edu.model.WordKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 首次启动种子数据：写入与 UI 原型（ui/app.js）一致的示例会话与词汇，
 * 让各界面在未产生真实对话前也有可评审的内容。
 *
 * 上线前把 [ENABLED] 置 false 即可停止写入（已写入的数据不会被自动清除）。
 * 只执行一次，由 SharedPreferences 标记控制；调试可用 force = true 重写。
 */
class DemoSeeder(
    private val context: Context,
    private val db: TalkFlowDatabase,
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("talkflow_seed", Context.MODE_PRIVATE)
    }

    suspend fun seedIfNeeded(force: Boolean = false) {
        if (!ENABLED && !force) return
        if (!force && prefs.getBoolean(KEY_SEEDED, false)) return

        withContext(Dispatchers.IO) {
            val userId = db.userDao().currentUser()?.id ?: return@withContext
            if (force) {
                // 示例数据使用固定主键（s1..s5），重写前整体清空即可
                db.clearAllTables()
            }
            seed(userId)
            prefs.edit().putBoolean(KEY_SEEDED, true).apply()
        }
    }

    private suspend fun seed(userId: Long) {
        val now = System.currentTimeMillis()
        val min = 60_000L
        val hour = 3_600_000L
        val day = 86_400_000L

        data class Seed(
            val id: String,
            val title: String,
            val teacherId: String,
            val mode: ChatMode,
            val preview: String,
            val ts: Long,
            val minutes: Int,
            val turns: List<Triple<String, String, String>>, // user, aiEn, aiZh
        )

        val seeds = listOf(
            Seed(
                id = "s1",
                title = "餐厅点餐：汉堡与饮品",
                teacherId = "emma",
                mode = ChatMode.TEACHING,
                preview = "Would you like anything to drink?",
                ts = now - 2 * hour,
                minutes = 12,
                turns = listOf(
                    Triple("I'd like a cheeseburger, please.", "Sure! Would you like anything to drink with that?", "没问题！需要搭配什么饮品吗？"),
                    Triple("A Coke, no ice.", "Great choice. Anything else? Our appetizers are quite popular today.", "很好的选择。还要别的吗？今天的开胃菜很受欢迎。"),
                    Triple("That's all, thank you.", "Your total is twelve dollars. The fries are on the house today.", "一共十二美元。今天的薯条是店家免费赠送的。"),
                ),
            ),
            Seed(
                id = "s2",
                title = "周末出行计划",
                teacherId = "kai",
                mode = ChatMode.CASUAL,
                preview = "So where are you headed this weekend?",
                ts = now - 1 * day - 3 * hour,
                minutes = 18,
                turns = listOf(
                    Triple("I'm going to Hangzhou.", "Nice! So where are you headed this weekend exactly?", "不错！周末具体打算去哪儿？"),
                    Triple("West Lake and some tea gardens.", "That sounds like pure serendipity waiting to happen.", "听起来会是一场美丽的意外之喜。"),
                    Triple("I haven't made a plan yet.", "You should draft a loose itinerary, it saves a lot of time.", "你可以先拟一份宽松的行程表，能省不少时间。"),
                ),
            ),
            Seed(
                id = "s3",
                title = "机场值机与安检",
                teacherId = "leo",
                mode = ChatMode.CORRECTIVE,
                preview = "Please place your bag on the scale.",
                ts = now - 2 * day,
                minutes = 22,
                turns = listOf(
                    Triple("I want to check in.", "May I see your passport? Please place your bag on the scale.", "能看下您的护照吗？请把行李放到秤上。"),
                    Triple("Is my bag too heavy?", "It's slightly overweight, so you'll need to remove a few items.", "稍微超重了一点，需要取出几件物品。"),
                    Triple("Can I take this bag with me?", "Yes, that's your carry-on, it fits the size limit.", "可以的，那是您的随身行李，符合尺寸限制。"),
                ),
            ),
            Seed(
                id = "s4",
                title = "电影偏好闲聊",
                teacherId = "mia",
                mode = ChatMode.GENTLE,
                preview = "I am totally into sci-fi lately.",
                ts = now - 4 * day,
                minutes = 9,
                turns = listOf(
                    Triple("I like watching movies.", "That's lovely! What kind of movies are you into these days?", "真好！最近你喜欢哪类电影？"),
                    Triple("Maybe science fiction.", "I am totally into sci-fi lately — the visuals are stunning.", "我最近也超迷科幻片，画面太惊艳了。"),
                ),
            ),
            Seed(
                id = "s5",
                title = "英文自我介绍 · 面试",
                teacherId = "emma",
                mode = ChatMode.TEACHING,
                preview = "Tell me a little about yourself.",
                ts = now - 9 * day,
                minutes = 26,
                turns = listOf(
                    Triple("Hello, I'm a student.", "Nice to meet you. Tell me a little about yourself.", "很高兴认识你。请简单介绍一下自己。"),
                    Triple("I study computer science.", "Great. Why do you think you're a good fit for this role?", "很好。你觉得为什么自己适合这个岗位？"),
                ),
            ),
        )

        seeds.forEach { s ->
            db.sessionDao().upsert(
                SessionEntity(
                    id = s.id,
                    userId = userId,
                    title = s.title,
                    teacherId = s.teacherId,
                    mode = s.mode.name,
                    preview = s.preview,
                    durationSeconds = s.minutes * 60,
                    fresh = false,
                    review = false,
                    createdAt = s.ts - s.minutes * 60_000L,
                    lastMessageAt = s.ts,
                )
            )
            var t = s.ts - s.minutes * 60_000L
            s.turns.forEach { (userText, aiEn, aiZh) ->
                t += 20_000L
                db.messageDao().insertAll(
                    listOf(
                        MessageEntity(sessionId = s.id, role = MessageRole.USER.name, textEn = userText, createdAt = t),
                        MessageEntity(sessionId = s.id, role = MessageRole.AI.name, textEn = aiEn, textZh = aiZh, createdAt = t + 4_000L),
                    )
                )
            }
        }

        data class SeedWord(val en: String, val zh: String, val kind: WordKind, val src: String, val ts: Long, val done: Boolean)

        val words = listOf(
            SeedWord("board something", "登机", WordKind.PHRASE, "s3", now - 2 * day, false),
            SeedWord("overweight", "超重的", WordKind.WORD, "s3", now - 2 * day + 8 * min, false),
            SeedWord("carry-on", "随身行李", WordKind.WORD, "s3", now - 2 * day + 15 * min, false),
            SeedWord("sci-fi", "科幻（题材）", WordKind.WORD, "s4", now - 4 * day, false),
            SeedWord("into something", "迷上某事", WordKind.PHRASE, "s4", now - 4 * day + 6 * min, false),
            SeedWord("serendipity", "意外发现美好事物的运气", WordKind.WORD, "s2", now - 1 * day - 3 * hour, false),
            SeedWord("itinerary", "行程表", WordKind.WORD, "s2", now - 1 * day - 2 * hour, false),
            SeedWord("classic", "经典的；老派的", WordKind.WORD, "s1", now - 2 * hour - 20 * min, false),
            SeedWord("appetizer", "开胃菜", WordKind.WORD, "s1", now - 2 * hour - 12 * min, true),
            SeedWord("on the house", "店家请客、免费赠送", WordKind.PHRASE, "s1", now - 2 * hour - 4 * min, true),
        )
        words.forEach { w ->
            db.wordDao().insert(
                WordEntity(
                    userId = userId,
                    en = w.en,
                    zh = w.zh,
                    kind = w.kind.name,
                    sessionId = w.src,
                    createdAt = w.ts,
                    mastered = w.done,
                    masteredAt = if (w.done) w.ts + min else null,
                    reviewCount = if (w.done) 1 else 0,
                )
            )
        }

        // 学习记录：把示例会话的时长落到对应日期，让分析页有内容
        val logByDay = linkedMapOf<String, Pair<Int, Int>>() // day -> (studySeconds, newWords)
        seeds.forEach { s ->
            val day = DAY_FORMAT.format(s.ts)
            val prev = logByDay[day] ?: (0 to 0)
            logByDay[day] = (prev.first + s.minutes * 60) to prev.second
        }
        words.groupBy { DAY_FORMAT.format(it.ts) }.forEach { (day, list) ->
            val prev = logByDay[day] ?: (0 to 0)
            logByDay[day] = prev.first to (prev.second + list.size)
        }
        logByDay.forEach { (day, pair) ->
            db.studyLogDao().insert(
                StudyLogEntity(
                    userId = userId,
                    day = day,
                    studySeconds = pair.first,
                    reviewSeconds = 0,
                    newWords = pair.second,
                    masteredWords = 0,
                    updatedAt = now,
                )
            )
        }
        // 最近三天的碎片复习时长，使占比环图不只有学习
        listOf(0, 1, 2).forEach { ago ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -ago) }
            val day = DAY_FORMAT.format(cal.time)
            val existing = db.studyLogDao().find(userId, day)
            val reviewSeconds = (4 + ago * 2) * 60
            if (existing == null) {
                db.studyLogDao().insert(
                    StudyLogEntity(userId = userId, day = day, reviewSeconds = reviewSeconds, updatedAt = now)
                )
            } else {
                db.studyLogDao().accumulate(userId, day, 0, reviewSeconds, 0, 2.takeIf { ago == 0 } ?: 0, now)
            }
        }
    }

    companion object {
        /** 上线前置 false 停止写入示例数据 */
        const val ENABLED = true
        private const val KEY_SEEDED = "seeded_v1"
        private val DAY_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    }
}
