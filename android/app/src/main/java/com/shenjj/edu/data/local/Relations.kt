package com.shenjj.edu.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Relation
import com.shenjj.edu.data.local.entity.SessionEntity
import com.shenjj.edu.data.local.entity.WordEntity

/**
 * 会话 + 该会话收录的词数。首页列表的"N 词"来自这里。
 */
data class SessionWithWordCount(
    @Embedded val session: SessionEntity,
    @ColumnInfo(name = "word_count") val wordCount: Int = 0,
)

/**
 * 词条 + 来源会话。来源会话被删除时为 null，UI 显示"已删除的会话"。
 */
data class WordWithSession(
    @Embedded val word: WordEntity,
    @Relation(parentColumn = "session_id", entityColumn = "id")
    val session: SessionEntity? = null,
)

/** 单词本顶部统计条 */
data class WordCounts(
    val total: Int = 0,
    val pending: Int = 0,
    val mastered: Int = 0,
)

/** 学习 vs 复习时长（秒），用于分析页占比环图 */
data class StudyTotals(
    @ColumnInfo(name = "study_seconds") val studySeconds: Long = 0,
    @ColumnInfo(name = "review_seconds") val reviewSeconds: Long = 0,
)
