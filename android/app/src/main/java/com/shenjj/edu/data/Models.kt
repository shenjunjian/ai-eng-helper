package com.shenjj.edu.data

import com.shenjj.edu.model.ChatMode

/**
 * 领域模型（与 Room 实体解耦，供 UI 消费）。
 */
data class User(
    val id: Long,
    val phone: String,
    val nickname: String,
    val age: Int,
    val level: com.shenjj.edu.model.StudyLevel,
    val goals: List<String>,
    /** 单位：分 */
    val balanceCents: Int,
    val defaultTeacherId: String,
    val defaultChatMode: ChatMode,
    val defaultImmersive: Boolean,
    val onboarded: Boolean,
) {
    val balanceYuan: Double get() = balanceCents / 100.0
    val goalsText: String get() = goals.joinToString(" · ")
}

/** 充值档位（对应原型 ¥30/¥68/¥128/¥198，1 元 ≈ 2 分钟） */
data class RechargeTier(
    val amountYuan: Int,
    val minutes: Int,
    val tag: String? = null,
)

/** 充值结果 */
sealed interface RechargeResult {
    data class Success(val orderRowId: Long, val newBalanceCents: Int) : RechargeResult
    data class Failure(val reason: String) : RechargeResult
}

/** 登录结果 */
sealed interface LoginResult {
    data class Success(val user: User) : LoginResult
    data class Failure(val reason: String) : LoginResult
}
