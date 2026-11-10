package com.shenjj.edu.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 单词表。唯一来源是会话：字幕中的内联生词由用户点击确认后收录。
 * mastered = true 即"已掌握"，属软删除——仍保留记录，可从筛选切回查看。
 */
@Entity(
    tableName = "words",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["user_id", "created_at"]),
        Index(value = ["session_id"]),
        Index(value = ["user_id", "mastered"]),
        // 同一用户下同一词条只收录一次
        Index(value = ["user_id", "en"], unique = true),
    ],
)
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    /** 英文词条 */
    val en: String,
    /** 中文释义 */
    val zh: String,
    /** WordKind.name：单词 / 习语 */
    val kind: String,
    /** 来源会话；会话被删除后置空，UI 显示"已删除的会话" */
    @ColumnInfo(name = "session_id") val sessionId: String? = null,
    /** 收录时间，列表按此倒序（最新收录在前） */
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    /** 已掌握（软删除出待复习列表） */
    val mastered: Boolean = false,
    @ColumnInfo(name = "mastered_at") val masteredAt: Long? = null,
    /** 累计复习次数 */
    @ColumnInfo(name = "review_count") val reviewCount: Int = 0,
)

/**
 * 充值订单表。当前为假充值（本地直接到账），
 * 上线后对接线上支付，订单号与状态由服务端回填。
 */
@Entity(
    tableName = "recharge_orders",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["user_id", "created_at"])],
)
data class RechargeOrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    /** 充值金额，单位：分 */
    @ColumnInfo(name = "amount_cents") val amountCents: Int,
    /** 折算可用对话分钟数 */
    val minutes: Int,
    /** 支付渠道，如 wechat */
    val channel: String = "wechat",
    /** paid / pending / failed */
    val status: String = "paid",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)

/**
 * 学习记录表。按天聚合学习（新对话）与复习时长，
 * 供"学习分析"页的柱状图与学习 vs 复习占比使用。
 */
@Entity(
    tableName = "study_logs",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["user_id", "day"], unique = true)],
)
data class StudyLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    /** yyyy-MM-dd */
    val day: String,
    /** 学习（非复习）时长，秒 */
    @ColumnInfo(name = "study_seconds") val studySeconds: Int = 0,
    /** 复习时长，秒 */
    @ColumnInfo(name = "review_seconds") val reviewSeconds: Int = 0,
    /** 当天新收录词数 */
    @ColumnInfo(name = "new_words") val newWords: Int = 0,
    /** 当天标记已掌握词数 */
    @ColumnInfo(name = "mastered_words") val masteredWords: Int = 0,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)
