package com.shenjj.edu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.shenjj.edu.data.local.StudyTotals
import com.shenjj.edu.data.local.entity.RechargeOrderEntity
import com.shenjj.edu.data.local.entity.StudyLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * 充值订单 DAO。当前为假充值：本地写一条 paid 流水并直接给余额加钱。
 * 上线后改为向线上支付服务下单，成功后回填 status。
 */
@Dao
interface RechargeOrderDao {

    @Insert
    suspend fun insert(order: RechargeOrderEntity): Long

    @Query(
        """
        SELECT * FROM recharge_orders
        WHERE user_id = :userId
        ORDER BY created_at DESC
        """
    )
    fun observeOrders(userId: Long): Flow<List<RechargeOrderEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(amount_cents), 0) FROM recharge_orders
        WHERE user_id = :userId AND status = 'paid'
        """
    )
    suspend fun totalPaidCents(userId: Long): Int

    @Query("UPDATE recharge_orders SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
}

/**
 * 学习记录 DAO。按 (userId, day) 唯一，累加当天时长与词数。
 */
@Dao
interface StudyLogDao {

    @Query("SELECT * FROM study_logs WHERE user_id = :userId AND day = :day LIMIT 1")
    suspend fun find(userId: Long, day: String): StudyLogEntity?

    @Insert
    suspend fun insert(log: StudyLogEntity): Long

    @Query(
        """
        UPDATE study_logs
        SET study_seconds = study_seconds + :studySeconds,
            review_seconds = review_seconds + :reviewSeconds,
            new_words = new_words + :newWords,
            mastered_words = mastered_words + :masteredWords,
            updated_at = :at
        WHERE user_id = :userId AND day = :day
        """
    )
    suspend fun accumulate(
        userId: Long,
        day: String,
        studySeconds: Int,
        reviewSeconds: Int,
        newWords: Int,
        masteredWords: Int,
        at: Long = System.currentTimeMillis(),
    )

    /** 最近 N 天的记录，用于柱状图 */
    @Query(
        """
        SELECT * FROM study_logs
        WHERE user_id = :userId AND day >= :fromDay
        ORDER BY day ASC
        """
    )
    fun observeSince(userId: Long, fromDay: String): Flow<List<StudyLogEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(study_seconds), 0) AS study_seconds,
               COALESCE(SUM(review_seconds), 0) AS review_seconds
        FROM study_logs
        WHERE user_id = :userId AND day >= :fromDay
        """
    )
    fun observeTotalsSince(userId: Long, fromDay: String): Flow<StudyTotals>

    /** 连续学习天数：取出有学习记录的日子，在业务层按日历倒推 */
    @Query(
        """
        SELECT day FROM study_logs
        WHERE user_id = :userId AND (study_seconds > 0 OR review_seconds > 0)
        ORDER BY day DESC
        """
    )
    suspend fun activeDays(userId: Long): List<String>

    @Query(
        """
        SELECT COALESCE(SUM(study_seconds + review_seconds), 0) FROM study_logs
        WHERE user_id = :userId AND day >= :fromDay
        """
    )
    fun observeTotalSecondsSince(userId: Long, fromDay: String): Flow<Long>
}
