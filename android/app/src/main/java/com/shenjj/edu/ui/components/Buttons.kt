package com.shenjj.edu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shenjj.edu.R
import com.shenjj.edu.ui.theme.AppSurface
import com.shenjj.edu.ui.theme.Chev
import com.shenjj.edu.ui.theme.Hot
import com.shenjj.edu.ui.theme.Ink
import com.shenjj.edu.ui.theme.Line
import com.shenjj.edu.ui.theme.Muted
import com.shenjj.edu.ui.theme.Pressed
import com.shenjj.edu.ui.theme.Radii

/* ============================================================
   按钮与列表行 —— 对应原型 .btn-dark / .btn-ghost / .btn-sm /
   .icon-btn / .hangup / .item，统一带 :active 的缩放反馈
   ============================================================ */

/** 按压缩放：.btn:active{scale(.97)}、.icon-btn:active{scale(.92)} */
private const val PRESS_SCALE = 0.97f
private const val ICON_PRESS_SCALE = 0.92f

/** 按压态缩放修饰符（pressed 由调用方从 interactionSource 收集） */
private fun Modifier.pressScale(pressed: Boolean, scale: Float): Modifier =
    graphicsLayer {
        val s = if (pressed) scale else 1f
        scaleX = s
        scaleY = s
    }

/**
 * 胶囊按钮。[dark]=true 对应 .btn-dark（墨底白字），false 对应 .btn-ghost（白底描边墨字）；
 * [small] 对应 .btn-sm。[accent] 用于强调色底（充值等场景）。
 */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = true,
    small: Boolean = false,
    accent: Boolean = false,
    enabled: Boolean = true,
    leadingRes: Int? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val container = when {
        accent -> com.shenjj.edu.ui.theme.Spark
        dark -> Ink
        else -> AppSurface
    }
    val content = if (dark || accent) Color.White else Ink
    val contentPadding = if (small) {
        PaddingValues(horizontal = 16.dp, vertical = 9.dp)
    } else {
        PaddingValues(horizontal = 22.dp, vertical = 15.dp)
    }
    val style: TextStyle = if (small) {
        MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp)
    } else {
        MaterialTheme.typography.labelLarge
    }
    val tint = if (enabled) content else Muted

    Row(
        modifier = modifier
            .then(if (small) Modifier else Modifier.fillMaxWidth())
            .pressScale(pressed, PRESS_SCALE)
            .background(if (enabled) container else container.copy(alpha = 0.4f), CircleShape)
            .then(
                if (dark || accent) Modifier
                else Modifier.border(1.dp, Line, CircleShape)
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingRes != null) {
            Icon(
                painter = painterResource(leadingRes),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(text = text, style = style, color = tint)
    }
}

/** 深色主按钮（.btn-dark），默认占满宽度 */
@Composable
fun DarkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingRes: Int? = null,
) {
    PillButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        dark = true,
        enabled = enabled,
        leadingRes = leadingRes,
    )
}

/** 白底描边次按钮（.btn-ghost），默认占满宽度 */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    small: Boolean = false,
) {
    PillButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        dark = false,
        small = small,
        enabled = enabled,
    )
}

/** 强调色按钮（充值主操作等） */
@Composable
fun AccentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    PillButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        accent = true,
        enabled = enabled,
    )
}

/** 圆形图标按钮（.icon-btn）：白底 + 1px 描边，默认 36dp、图标 19dp */
@Composable
fun CircleIconButton(
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    iconSize: Dp = 19.dp,
    tint: Color = Ink,
    container: Color = AppSurface,
    border: Color? = Line,
    contentDescription: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Box(
        modifier = modifier
            .size(size)
            .pressScale(pressed, ICON_PRESS_SCALE)
            .background(container, CircleShape)
            .then(if (border == null) Modifier else Modifier.border(1.dp, border, CircleShape))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** 挂断按钮（.hangup）：66dp 正圆、--hot 底 + 投影、图标 27dp */
@Composable
fun HangupButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 66.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Box(
        modifier = modifier
            .size(size)
            .pressScale(pressed, 0.93f)
            .shadow(
                elevation = 10.dp,
                shape = CircleShape,
                spotColor = Hot.copy(alpha = 0.36f),
                ambientColor = Hot.copy(alpha = 0.36f),
            )
            .background(Hot, CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_hangup),
            contentDescription = "挂断",
            tint = Color.White,
            modifier = Modifier.size(27.dp),
        )
    }
}

/**
 * 设置式列表行（.item）：左侧 30dp 圆角图标底 + 标题 + 右侧值 + 箭头。
 * [iconGlyph] 放 emoji（原型 .item .ic 直接放老师 glyph），[iconRes] 放矢量图标，二者取一。
 */
@Composable
fun SettingItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    iconGlyph: String? = null,
    iconRes: Int? = null,
    iconTint: Color = Ink,
    iconBg: Color = Line,
    showChevron: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (pressed) Pressed else Color.Transparent)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (iconGlyph != null || iconRes != null) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(iconBg, RoundedCornerShape(Radii.ItemIcon)),
                contentAlignment = Alignment.Center,
            ) {
                if (iconGlyph != null) {
                    Text(text = iconGlyph, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Icon(
                        painter = painterResource(iconRes!!),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Ink,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                color = Muted,
            )
        }
        trailing?.invoke()
        if (showChevron) {
            Icon(
                painter = painterResource(R.drawable.ic_chev),
                contentDescription = null,
                tint = Chev,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
