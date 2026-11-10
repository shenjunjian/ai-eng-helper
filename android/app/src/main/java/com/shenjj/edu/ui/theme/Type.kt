package com.shenjj.edu.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/* ============================================================
   字号 token —— 对应 ui/index.html 的排版规则
   ============================================================ */
object TypeScale {
    /** h1 28px/700/-0.6px */
    val Title = 28.sp

    /** h2 / .call-name 15–16px/600–700 */
    val Section = 15.sp

    /** .cell-title 15.5px/600 */
    val CellTitle = 15.5.sp

    /** p 14px/400/1.55 */
    val Body = 14.sp

    /** .msg 14.5px */
    val Bubble = 14.5.sp

    /** .item .lbl / .btn 15px */
    val ListItem = 15.sp

    /** .word-en 16px/700 */
    val WordEn = 16.sp

    /** .word-zh / .cell-sub 13px */
    val WordZh = 13.sp

    /** .tiny / .item .val 12–13.5px */
    val Caption = 12.sp

    /** .cell-time / .word-src / .timer 11.5px */
    val Meta = 11.5.sp

    /** .sec-label 11px/700/+0.8px */
    val Label = 11.sp

    /** .tab 10.5px/600 */
    val Tab = 10.5.sp

    /** .tag 10.5px/700 */
    val Tag = 10.5.sp

    /** .chip 12.5px/600 */
    val Chip = 12.5.sp

    /** .sheet h3 18px/700 */
    val SheetTitle = 18.sp

    /** .big-num 34px/700 */
    val BigNum = 34.sp

    /** .stat-mini .k 22px/700 */
    val StatNum = 22.sp

    /** .toast / .veil-txt 13–13.5px */
    val Toast = 13.sp
}

/**
 * 原型字体栈以 PingFang SC / Microsoft YaHei 为主，
 * Android 侧交由系统默认（思源黑体/Noto）处理中英文混排。
 */
private val AppFont = FontFamily.Default

val Typography = Typography(
    // h1：页面大标题
    headlineLarge = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Bold,
        fontSize = TypeScale.Title,
        lineHeight = 34.sp,
        letterSpacing = (-0.6).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.5).sp,
    ),
    // .sheet h3
    titleLarge = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Bold,
        fontSize = TypeScale.SheetTitle,
        lineHeight = 24.sp,
        letterSpacing = (-0.3).sp,
    ),
    // h2 / .call-name / .cell-title
    titleMedium = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = TypeScale.Section,
        lineHeight = 20.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleSmall = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = TypeScale.CellTitle,
        lineHeight = 20.sp,
        letterSpacing = (-0.2).sp,
    ),
    // .item .lbl / .btn
    bodyLarge = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Medium,
        fontSize = TypeScale.ListItem,
        lineHeight = 22.sp,
    ),
    // p
    bodyMedium = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Normal,
        fontSize = TypeScale.Body,
        lineHeight = 22.sp,
    ),
    // .msg 字幕
    bodySmall = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Normal,
        fontSize = TypeScale.Bubble,
        lineHeight = 22.sp,
    ),
    // .btn 主按钮文字
    labelLarge = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = TypeScale.ListItem,
        lineHeight = 20.sp,
    ),
    // .chip / .word-zh
    labelMedium = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = TypeScale.Chip,
        lineHeight = 18.sp,
    ),
    // .sec-label
    labelSmall = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Bold,
        fontSize = TypeScale.Label,
        lineHeight = 14.sp,
        letterSpacing = 0.8.sp,
    ),
)
