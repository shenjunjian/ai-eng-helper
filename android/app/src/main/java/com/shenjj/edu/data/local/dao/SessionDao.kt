package com.shenjj.edu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.shenjj.edu.data.local.SessionWithWordCount
import com.shenjj.edu.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * 会话表 DAO。首页列表按 lastMessageAt 倒序（进入即置顶），
 * 每次进入历史会话都刷新该时间戳。
 */
@Dao
interface SessionDao {

    /** 首页历史对话列表：按最近活跃倒序，并带上该会话收录的词数 */
    @Query(
        """
        SELECT s.*, (SELECT COUNT(*) FROM words w
                     WHERE w.session_id = s.id) AS word_count
        FROM sessions s
        WHERE s.user_id = :userId
        ORDER BY s.last_message_at DESC
        """
    )
    fun observeSessionsWithWordCount(userId: Long): Flow<List<SessionWithWordCount>>

    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    fun observeSession(id: String): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: SessionEntity)

    /** 进入历史会话即置顶 */
    @Query("UPDATE sessions SET last_message_at = :at WHERE id = :id")
    suspend fun touch(id: String, at: Long = System.currentTimeMillis())

    /** 挂断后由 AI 总结标题并结算：新自由对话命名，历史会话只更新 */
    @Query(
        """
        UPDATE sessions
        SET title = :title,
            preview = :preview,
            teacher_id = :teacherId,
            mode = :mode,
            fresh = :fresh,
            duration_seconds = duration_seconds + :addedSeconds,
            last_message_at = :at
        WHERE id = :id
        """
    )
    suspend fun settle(
        id: String,
        title: String,
        preview: String,
        teacherId: String,
        mode: String,
        fresh: Boolean,
        addedSeconds: Int,
        at: Long = System.currentTimeMillis(),
    )

    /** 对话中切换老师：会话头像同步更新 */
    @Query("UPDATE sessions SET teacher_id = :teacherId WHERE id = :id")
    suspend fun updateTeacher(id: String, teacherId: String)

    @Query("SELECT COUNT(*) FROM sessions WHERE user_id = :userId")
    fun observeSessionCount(userId: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(duration_seconds), 0) FROM sessions WHERE user_id = :userId")
    fun observeTotalDurationSeconds(userId: Long): Flow<Int>

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Transaction
    suspend fun upsertAndTouch(session: SessionEntity) {
        upsert(session)
        touch(session.id, session.lastMessageAt)
    }
}
