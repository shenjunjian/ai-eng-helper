package com.shenjj.edu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.shenjj.edu.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * 消息（字幕）表 DAO。保存每轮对话字幕，
 * 使单词的"来源会话"可以回跳并重现上下文。
 */
@Dao
interface MessageDao {

    @Query(
        """
        SELECT * FROM messages
        WHERE session_id = :sessionId
        ORDER BY created_at ASC, id ASC
        """
    )
    fun observeBySession(sessionId: String): Flow<List<MessageEntity>>

    @Query(
        """
        SELECT * FROM messages
        WHERE session_id = :sessionId
        ORDER BY created_at DESC, id DESC
        LIMIT :limit
        """
    )
    suspend fun latestBySession(sessionId: String, limit: Int): List<MessageEntity>

    @Insert
    suspend fun insert(message: MessageEntity): Long

    @Insert
    suspend fun insertAll(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE session_id = :sessionId")
    suspend fun deleteBySession(sessionId: String)
}
