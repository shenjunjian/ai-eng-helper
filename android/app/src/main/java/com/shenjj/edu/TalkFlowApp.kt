package com.shenjj.edu

import android.app.Application
import com.shenjj.edu.data.AppRepository
import com.shenjj.edu.data.DemoSeeder
import com.shenjj.edu.data.gateway.LocalPaymentGateway
import com.shenjj.edu.data.gateway.LocalPhoneAuthGateway
import com.shenjj.edu.data.local.TalkFlowDatabase

/**
 * 应用入口，兼作手动依赖注入容器：持有数据库与 Repository 单例。
 *
 * 上线时把 [LocalPhoneAuthGateway] / [LocalPaymentGateway] 换成线上实现即可，
 * 其余代码无需改动。
 */
class TalkFlowApp : Application() {

    val database: TalkFlowDatabase by lazy { TalkFlowDatabase.getInstance(this) }

    val repository: AppRepository by lazy {
        AppRepository(
            db = database,
            auth = LocalPhoneAuthGateway(),
            payment = LocalPaymentGateway(),
            seeder = DemoSeeder(this, database),
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: TalkFlowApp
            private set
    }
}
