package com.shenjj.edu.data.gateway

/**
 * 登录网关。当前为本地假实现：任意手机号 + 任意验证码即通过。
 * 上线后新增 RemotePhoneAuthGateway 替换即可，调用方无需改动。
 */
interface PhoneAuthGateway {
    /** 发送验证码（假实现不真正发送，直接返回成功） */
    suspend fun sendCode(phone: String): Result<Unit>

    /** 校验验证码并返回手机号（假实现原样返回手机号） */
    suspend fun verifyCode(phone: String, code: String): Result<String>
}

class LocalPhoneAuthGateway : PhoneAuthGateway {

    override suspend fun sendCode(phone: String): Result<Unit> =
        if (isValidPhone(phone)) Result.success(Unit)
        else Result.failure(IllegalArgumentException("手机号格式不正确"))

    override suspend fun verifyCode(phone: String, code: String): Result<String> =
        if (isValidPhone(phone) && code.isNotBlank()) Result.success(phone)
        else Result.failure(IllegalArgumentException("验证码不能为空"))

    private fun isValidPhone(phone: String) =
        phone.length == 11 && phone.startsWith("1") && phone.all { it.isDigit() }
}

/**
 * 支付网关。当前为本地假实现：直接返回支付成功。
 * 上线后替换为微信/支付宝下单与回调校验。
 */
interface PaymentGateway {
    /** 返回订单号 */
    suspend fun pay(amountCents: Int, minutes: Int): Result<String>
}

class LocalPaymentGateway : PaymentGateway {
    override suspend fun pay(amountCents: Int, minutes: Int): Result<String> =
        Result.success("local-${System.currentTimeMillis()}")
}
