package com.shenjj.edu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shenjj.edu.model.Teacher

/* ============================================================
   老师头像 —— 对应原型 ava()：
   渐变圆底 linear-gradient(140deg, c0, c1) + emoji glyph，
   投影 0 4px 14px {c1}40
   ============================================================ */

/**
 * @param size 头像直径，原型默认 46dp（会话列表）、通话页 40dp、我的页 62dp 等
 * @param glyphSize emoji 字号，null 时按原型的 size*0.58 计算
 */
@Composable
fun TeacherAvatar(
    teacher: Teacher,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    glyphSize: Dp? = null,
) {
    val glyphSp = glyphSize ?: (size.value * 0.58f).dp
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 4.dp,
                shape = CircleShape,
                spotColor = teacher.endColor.copy(alpha = 0.25f),
                ambientColor = teacher.endColor.copy(alpha = 0.25f),
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(teacher.startColor, teacher.endColor),
                ),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = teacher.glyph,
            fontSize = glyphSp.value.sp,
            lineHeight = glyphSp.value.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * 纯色小圆点（.dot），用于「在线/进行中」状态指示。
 */
@Composable
fun StatusDot(
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(color, CircleShape),
    )
}
