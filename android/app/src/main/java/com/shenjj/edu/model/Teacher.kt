package com.shenjj.edu.model

import androidx.compose.ui.graphics.Color

/**
 * AI 老师。老师是固定的静态目录（对应原型 TEACHERS），不入数据库；
 * 数据库里只保存 teacherId 字符串。
 */
data class Teacher(
    val id: String,
    val name: String,
    /** 头像用的 emoji 字形，叠在渐变圆底上 */
    val glyph: String,
    /** 该老师的默认对话风格 */
    val style: ChatMode,
    val desc: String,
    val colorStart: Long,
    val colorEnd: Long,
) {
    val startColor: Color get() = Color(colorStart)
    val endColor: Color get() = Color(colorEnd)
}

object TeacherCatalog {

    val ALL: List<Teacher> = listOf(
        Teacher(
            id = "emma",
            name = "Emma",
            glyph = "\uD83D\uDC69\uD83C\uDFFB\u200D\uD83C\uDFEB",
            style = ChatMode.TEACHING,
            desc = "耐心细致 · 美音",
            colorStart = 0xFFFF9A68,
            colorEnd = 0xFFFF6B4A,
        ),
        Teacher(
            id = "leo",
            name = "Leo",
            glyph = "\uD83E\uDDD1\uD83C\uDFFD\u200D\uD83C\uDF93",
            style = ChatMode.CORRECTIVE,
            desc = "逐句纠错 · 英音",
            colorStart = 0xFF8AA7FF,
            colorEnd = 0xFF4F63F2,
        ),
        Teacher(
            id = "mia",
            name = "Mia",
            glyph = "\uD83D\uDC69\uD83C\uDFFC\u200D\uD83E\uDDB0",
            style = ChatMode.GENTLE,
            desc = "鼓励为主 · 慢速",
            colorStart = 0xFFFFB3D1,
            colorEnd = 0xFFF2649B,
        ),
        Teacher(
            id = "kai",
            name = "Kai",
            glyph = "\uD83E\uDDD4\uD83C\uDFFB",
            style = ChatMode.CASUAL,
            desc = "话题广 · 语速快",
            colorStart = 0xFF7DE2C0,
            colorEnd = 0xFF18A97F,
        ),
    )

    val DEFAULT_ID: String = ALL.first().id

    fun byId(id: String?): Teacher = ALL.firstOrNull { it.id == id } ?: ALL.first()
}
