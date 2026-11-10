package com.shenjj.edu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.shenjj.edu.data.local.WordCounts
import com.shenjj.edu.data.local.WordWithSession
import com.shenjj.edu.data.local.entity.WordEntity
import kotlinx.coroutines.flow.Flow

/**
 * 单词表 DAO。唯一来源是会话中用户确认收录的生词；
 * 按收录顺序倒序展示（最新在前）。
 *
 * filter 取值：
 *  - pending：待复习（mastered = 0）
 *  - word / phrase：按类型（WordKind.name 的小写形式）
 *  - done：已掌握（mastered = 1）
 *  - all：全部
 */
@Dao
interface WordDao {

    @Transaction
    @Query(
        """
        SELECT * FROM words
        WHERE user_id = :userId
          AND (:filter = 'all'
               OR (:filter = 'pending' AND mastered = 0)
               OR (:filter = 'done' AND mastered = 1)
               OR (:filter NOT IN ('all', 'pending', 'done') AND kind = :filter))
        ORDER BY created_at DESC, id DESC
        """
    )
    fun observeWords(userId: Long, filter: String): Flow<List<WordWithSession>>

    @Transaction
    @Query(
        """
        SELECT * FROM words
        WHERE user_id = :userId
        ORDER BY created_at DESC, id DESC
        """
    )
    fun observeAllWords(userId: Long): Flow<List<WordWithSession>>

    @Transaction
    @Query("SELECT * FROM words WHERE id = :id LIMIT 1")
    fun observeWord(id: Long): Flow<WordWithSession?>

    @Query("SELECT * FROM words WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): WordEntity?

    /** 通话中判断某个词条是否已收录（用于内联标记的已收录态） */
    @Query("SELECT en FROM words WHERE user_id = :userId")
    suspend fun existingTerms(userId: Long): List<String>

    /** 首页单词本角标：待复习数量 */
    @Query("SELECT COUNT(*) FROM words WHERE user_id = :userId AND mastered = 0")
    fun observePendingCount(userId: Long): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) AS total,
               SUM(CASE WHEN mastered = 0 THEN 1 ELSE 0 END) AS pending,
               SUM(CASE WHEN mastered = 1 THEN 1 ELSE 0 END) AS mastered
        FROM words WHERE user_id = :userId
        """
    )
    fun observeCounts(userId: Long): Flow<WordCounts>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(word: WordEntity): Long

    /** 标记已掌握 / 放回待复习（软删除，记录始终保留） */
    @Query(
        """
        UPDATE words
        SET mastered = :mastered,
            mastered_at = CASE WHEN :mastered = 1 THEN :at ELSE NULL END
        WHERE id = :id
        """
    )
    suspend fun setMastered(id: Long, mastered: Boolean, at: Long = System.currentTimeMillis())

    @Query("UPDATE words SET review_count = review_count + 1 WHERE id = :id")
    suspend fun incrementReviewCount(id: Long)

    @Query("SELECT COUNT(*) FROM words WHERE user_id = :userId AND mastered = 1")
    suspend fun masteredCount(userId: Long): Int

    @Query(
        """
        SELECT COUNT(*) FROM words
        WHERE user_id = :userId AND created_at >= :since
        """
    )
    suspend fun newWordCountSince(userId: Long, since: Long): Int

    @Query("DELETE FROM words WHERE id = :id")
    suspend fun deleteById(id: Long)
}
