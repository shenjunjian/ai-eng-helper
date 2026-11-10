package com.shenjj.edu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shenjj.edu.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * 用户表 DAO。本地假登录：按手机号查号，查不到即注册。
 * 上线后替换为线上登录服务，接口形状保持不变。
 */
@Dao
interface UserDao {

    /** 当前登录账号（本地单账号） */
    @Query("SELECT * FROM users WHERE logged_in = 1 ORDER BY last_active_at DESC LIMIT 1")
    fun observeCurrentUser(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE logged_in = 1 ORDER BY last_active_at DESC LIMIT 1")
    suspend fun currentUser(): UserEntity?

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun findByPhone(phone: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): UserEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity)

    /** 切换登录态：先清空所有账号的登录标记，再置位目标账号 */
    @Query("UPDATE users SET logged_in = 0")
    suspend fun clearLogin()

    @Query("UPDATE users SET logged_in = 1, last_active_at = :at WHERE id = :userId")
    suspend fun markLoggedIn(userId: Long, at: Long = System.currentTimeMillis())

    /** 余额变动（充值为正、消费为负），单位：分 */
    @Query("UPDATE users SET balance_cents = balance_cents + :deltaCents WHERE id = :userId")
    suspend fun addBalance(userId: Long, deltaCents: Int)

    @Query("UPDATE users SET last_active_at = :at WHERE id = :userId")
    suspend fun touch(userId: Long, at: Long = System.currentTimeMillis())
}
