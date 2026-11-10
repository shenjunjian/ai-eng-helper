package com.shenjj.edu.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.shenjj.edu.data.local.dao.MessageDao
import com.shenjj.edu.data.local.dao.RechargeOrderDao
import com.shenjj.edu.data.local.dao.SessionDao
import com.shenjj.edu.data.local.dao.StudyLogDao
import com.shenjj.edu.data.local.dao.UserDao
import com.shenjj.edu.data.local.dao.WordDao
import com.shenjj.edu.data.local.entity.MessageEntity
import com.shenjj.edu.data.local.entity.RechargeOrderEntity
import com.shenjj.edu.data.local.entity.SessionEntity
import com.shenjj.edu.data.local.entity.StudyLogEntity
import com.shenjj.edu.data.local.entity.UserEntity
import com.shenjj.edu.data.local.entity.WordEntity

/**
 * 本地数据库。所有数据（用户、会话、消息、单词、充值流水、学习记录）
 * 都保存在设备本地；上线后登录与充值改为对接线上服务，本地表保留为缓存与业务数据。
 *
 * 枚举一律以 name 字符串落库，因此不需要 TypeConverter。
 */
@Database(
    entities = [
        UserEntity::class,
        SessionEntity::class,
        MessageEntity::class,
        WordEntity::class,
        RechargeOrderEntity::class,
        StudyLogEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class TalkFlowDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao
    abstract fun wordDao(): WordDao
    abstract fun rechargeOrderDao(): RechargeOrderDao
    abstract fun studyLogDao(): StudyLogDao

    companion object {
        private const val DB_NAME = "talkflow.db"

        @Volatile
        private var instance: TalkFlowDatabase? = null

        fun getInstance(context: Context): TalkFlowDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        private fun build(context: Context): TalkFlowDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                TalkFlowDatabase::class.java,
                DB_NAME,
            )
                // 开发期允许破坏性迁移；上线前替换为正式 Migration
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
