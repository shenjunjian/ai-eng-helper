package com.shenjj.edu.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/* ============================================================
   圆角 token —— 对应原型各处 border-radius
   ============================================================ */
object Radii {
    /** --radius:22px，.card */
    val Card = 22.dp

    /** .list 24px */
    val List = 24.dp

    /** .group 20px */
    val Group = 20.dp

    /** .sheet 28px（仅上方两角） */
    val SheetTop = 28.dp

    /** .field 16px */
    val Field = 16.dp

    /** .msg 20px，带 7px 的尾角 */
    val Bubble = 20.dp
    val BubbleTail = 7.dp

    /** .toast 14px */
    val Toast = 14.dp

    /** .mark 20px（启动页 logo 底） */
    val Mark = 20.dp

    /** .item .ic 9px */
    val ItemIcon = 9.dp

    /** .grab 2px */
    val Grab = 2.dp

    /** chip / tag / btn：999px 全圆角 */
    val Pill = 999.dp

    /** 页签栏高度：--tab-h-safe 98px（含 24px 安全区留白） */
    val TabBarHeight = 98.dp
    val TabBarInset = 24.dp

    /** 状态栏占位高度（原型 .pt-safe 54px） */
    val StatusInset = 54.dp
}

/** 只圆上方两角（底部抽屉用） */
val SheetShape = RoundedCornerShape(
    topStart = Radii.SheetTop,
    topEnd = Radii.SheetTop,
)

/** AI 气泡：左下 7dp 尾角 */
val AiBubbleShape = RoundedCornerShape(
    topStart = Radii.Bubble,
    topEnd = Radii.Bubble,
    bottomEnd = Radii.Bubble,
    bottomStart = Radii.BubbleTail,
)

/** 用户气泡：右下 7dp 尾角 */
val MeBubbleShape = RoundedCornerShape(
    topStart = Radii.Bubble,
    topEnd = Radii.Bubble,
    bottomEnd = Radii.BubbleTail,
    bottomStart = Radii.Bubble,
)

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(Radii.Field),
    large = RoundedCornerShape(Radii.Card),
    extraLarge = RoundedCornerShape(Radii.SheetTop),
)
