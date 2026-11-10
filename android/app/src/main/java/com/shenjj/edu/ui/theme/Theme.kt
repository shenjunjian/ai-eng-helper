package com.shenjj.edu.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * TalkFlow 配色：原型只有浅色一套（--bg 米白 + 单一 --spark 强调色），
 * 因此关闭 dynamicColor，深色模式下也沿用浅色方案，保证与 UI 原型一致。
 */
private val TalkFlowColorScheme = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    primaryContainer = SparkSoft,
    onPrimaryContainer = SparkDeep,

    secondary = Ink2,
    onSecondary = Color.White,
    secondaryContainer = Line,
    onSecondaryContainer = Ink,

    tertiary = Spark,
    onTertiary = Color.White,
    tertiaryContainer = SparkSoft,
    onTertiaryContainer = SparkDeep,

    error = Hot,
    onError = Color.White,
    errorContainer = Color(0xFFFFE5E6),
    onErrorContainer = Hot,

    background = AppBg,
    onBackground = Ink,

    surface = AppSurface,
    onSurface = Ink,
    surfaceVariant = AppBg,
    onSurfaceVariant = Ink2,
    surfaceTint = Spark,
    inverseSurface = Ink,
    inverseOnSurface = Color.White,

    outline = Line,
    outlineVariant = Line,
    scrim = SheetScrim,
)

@Composable
fun TalkFlowTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 浅色底 → 状态栏与导航栏图标用深色
            // 栏位透明由 MainActivity 的 enableEdgeToEdge() 负责（API 35 起
            // statusBarColor/navigationBarColor 已弃用，此处不再重复设置）
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = TalkFlowColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
