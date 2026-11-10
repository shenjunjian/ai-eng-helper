package com.shenjj.edu.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 用户表。本地假登录：手机号 + 任意验证码即注册/登录，
 * 上线后此表由线上账号体系替代，字段保持可映射。
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["phone"], unique = true)],
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phone: String,
    val nickname: String,
    val age: Int,
    /** StudyLevel.name */
    val level: String,
    /** 学习目标，多个用 "," 连接 */
    val goals: String = "",
    /** 账户余额，单位：分。避免浮点误差 */
    @ColumnInfo(name = "balance_cents") val balanceCents: Int = 0,
    /** 默认 AI 老师 id */
    @ColumnInfo(name = "default_teacher_id") val defaultTeacherId: String = "",
    /** 默认对话模式 ChatMode.name */
    @ColumnInfo(name = "default_chat_mode") val defaultChatMode: String = "",
    /** 默认是否沉浸式 */
    @ColumnInfo(name = "default_immersive") val defaultImmersive: Boolean = false,
    /** 是否已完成学习档案（onboarding） */
    val onboarded: Boolean = false,
    /** 当前是否为已登录账号（本地单账号会话） */
    @ColumnInfo(name = "logged_in") val loggedIn: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_active_at") val lastActiveAt: Long = System.currentTimeMillis(),
)

/**
 * 会话表。首页即历史对话列表；每次进入某会话都刷新 lastMessageAt 以置顶。
 */
@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["last_message_at"]),
    ],
)
data class SessionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: Long,
    val title: String,
    /** 会话当前 AI 老师 id；对话中切换老师时同步更新，头像跟随 */
    @ColumnInfo(name = "teacher_id") val teacherId: String,
    /** ChatMode.name */
    val mode: String,
    /** 最后一条消息摘要 */
    val preview: String = "",
    /** 累计对话时长（秒），挂断时累加 */
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Int = 0,
    /** 是否为刚创建、尚未由 AI 总结命名的自由对话 */
    val fresh: Boolean = true,
    /** 是否为复习模式的会话 */
    val review: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_message_at") val lastMessageAt: Long = System.currentTimeMillis(),
)

/**
 * 消息（字幕）表。保存每轮对话的英文与中文字幕，
 * 使单词的"来源会话"可以回跳并重现上下文。
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["session_id"])],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: String,
    /** MessageRole.name */
    val role: String,
    @ColumnInfo(name = "text_en") val textEn: String,
    @ColumnInfo(name = "text_zh") val textZh: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)
