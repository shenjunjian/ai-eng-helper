package com.shenjj.edu.ui.theme

import androidx.compose.ui.graphics.Color

/* ============================================================
   颜色 token —— 一一对应 ui/index.html 的 :root 变量
   ============================================================ */

/** --bg 页面底色 */
val AppBg = Color(0xFFF5F5F3)

/** --surface 卡片/列表/输入框底 */
val AppSurface = Color(0xFFFFFFFF)

/** --ink 主文字、深色按钮底 */
val Ink = Color(0xFF111318)

/** --ink-2 次级文字 */
val Ink2 = Color(0xFF575D66)

/** --muted 弱化文字/时间戳 */
val Muted = Color(0xFF98A0AA)

/** --line 描边与分隔线 */
val Line = Color(0xFFECECE7)

/** --spark 唯一强调色 */
val Spark = Color(0xFFFF7A50)

/** --spark-soft 强调色浅底（生词标记、tag） */
val SparkSoft = Color(0xFFFFEDE4)

/** --ok 成功/已掌握/通话中状态 */
val Ok = Color(0xFF12B76A)

/** --hot 挂断/错误 */
val Hot = Color(0xFFE5484D)

/* ---------- 派生色（原型中散落的固定值） ---------- */

/** 生词标记的文字色（tag 用），来自 .tag 的 color */
val SparkDeep = Color(0xFFC7502A)

/** 习语 tag：#EDE9FE 底 + #6D28D9 字 */
val TagPhraseBg = Color(0xFFEDE9FE)
val TagPhraseInk = Color(0xFF6D28D9)

/** 已掌握 tag：#E8F7EF 底 + --ok 字 */
val TagDoneBg = Color(0xFFE8F7EF)

/** 已收录生词的下划线色（.wlink.saved 的 border-color） */
val OkSoftLine = Color(0xFFD6F0E2)

/** 列表项按压态 */
val Pressed = Color(0xFFF6F6F3)
val PressedStrong = Color(0xFFF0F0EC)

/** 右箭头图标色（.chev） */
val Chev = Color(0xFFC6CBD2)

/** 抽屉抓手（.grab） */
val Grab = Color(0xFFDCDDE2)

/** 毛玻璃页签栏底色 rgba(255,255,255,.82) */
val TabBarScrim = Color(0xD1FFFFFF)

/** 遮罩 rgba(11,12,15,.40) */
val SheetScrim = Color(0x660B0C0F)

/** 挂断总结遮罩 rgba(11,12,15,.42) */
val VeilScrim = Color(0x6B0B0C0F)

/** Toast 底 rgba(17,19,24,.92) */
val ToastBg = Color(0xEB111318)

/** 深色底上的次级文字 rgba(255,255,255,.6) */
val OnDarkMuted = Color(0x99FFFFFF)

/** 分析页柱状图：普通柱 = --line，高亮柱 = --ink，当天柱 = --spark（已定义） */
val BarIdle = Line

/** 通话页渐变遮罩的透明端 */
val BgTransparent = Color(0x00F5F5F3)
