import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// 本地私密配置（AOQ 凭证等），不进入版本库
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun localProp(key: String, default: String = ""): String =
    localProps.getProperty(key) ?: default

android {
    namespace = "com.shenjj.edu"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.shenjj.edu"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // AOQ SDK 仅提供这两个 ABI 的 .so
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }

        buildConfigField("String", "AOQ_API_KEY", "\"${localProp("AOQ_API_KEY")}\"")
        buildConfigField("String", "AOQ_WORKSPACE_ID", "\"${localProp("AOQ_WORKSPACE_ID")}\"")
        buildConfigField("String", "AOQ_REGION", "\"${localProp("AOQ_REGION", "cn-beijing")}\"")
        buildConfigField("String", "AOQ_MODEL", "\"${localProp("AOQ_MODEL", "qwen-audio-3.1-realtime-flash")}\"")
        // 仅用于音频路由/AEC 的 A/B 回归；默认保持当前已验证行为。
        buildConfigField("boolean", "AOQ_USE_VOIP_MODE", localProp("AOQ_USE_VOIP_MODE", "true").toBoolean().toString())
    }

    buildTypes {
        release {
            optimization {
                enable = true
                // 不要设置 packageScope：部分优化（gradual shrinking）只会重命名被圈定的包，
                // 但它把 kotlin.** 重命名成了无包名的扁平类（如 f91），而 f91 继承的父类
                // kotlin.sequences.d 未被重命名、是包内可见的 → 运行期 IllegalAccessError，
                // 应用启动即闪退。必须让 R8 一次性处理全部包，可见性才自洽。
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)

    // 底部页签的毛玻璃背景模糊（对应原型 backdrop-filter: blur(20px)）
    implementation(libs.haze)

    // 本地数据库
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // AOQ 实时语音 SDK（aar 复用仓库根 libs/ 下已入库的那一份，不再复制副本）
    // 相对路径以模块目录 android/app 为基准，故需回退两级
    implementation(libs.okhttp)
    implementation(files("${projectDir}/../../libs/AOQ SDK/AoqClientSdk-release.aar"))

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
