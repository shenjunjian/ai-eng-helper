package com.shenjj.edu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shenjj.edu.ui.theme.AppSurface
import com.shenjj.edu.ui.theme.Ink
import com.shenjj.edu.ui.theme.Line
import com.shenjj.edu.ui.theme.Muted
import com.shenjj.edu.ui.theme.Radii

/* ============================================================
   输入框 —— 对应原型 .field：白底 + 1px 描边 + 16px 圆角，
   :focus 时描边转墨色并带 4px 柔光环
   ============================================================ */

/** 聚焦时的柔光环颜色：原型 box-shadow rgba(17,19,24,.05) */
private val FocusHalo = Color(0x0D111318)

/**
 * 文本输入框。
 * @param width 固定宽度（原型设置页里昵称 150dp、年龄 90dp），null 时占满
 * @param align 文字对齐（设置页行内输入用 End）
 * @param numeric 数字键盘（手机号、验证码、年龄）
 */
@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    width: Dp? = null,
    align: TextAlign = TextAlign.Start,
    numeric: Boolean = false,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(Radii.Field)

    val style: TextStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = if (compact) 15.sp else 16.sp,
        fontWeight = FontWeight.Normal,
        textAlign = align,
        color = Ink,
    )

    val padding = if (compact) {
        Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
    } else {
        Modifier.padding(horizontal = 16.dp, vertical = 15.dp)
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .background(AppSurface, shape)
            .border(
                width = if (focused) 1.5.dp else 1.dp,
                color = if (focused) Ink else Line,
                shape = shape,
            )
            .then(padding),
        enabled = enabled,
        singleLine = singleLine,
        textStyle = style,
        cursorBrush = SolidColor(Ink),
        interactionSource = interaction,
        keyboardOptions = if (numeric) {
            KeyboardOptions(keyboardType = KeyboardType.Number)
        } else {
            KeyboardOptions.Default
        },
        decorationBox = { inner ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            style = style.copy(color = Muted),
                        )
                    }
                    inner()
                }
            }
        },
    )
}

/**
 * 行内输入：左侧标签 + 右侧右对齐输入框。
 * 对应原型 .item 里嵌 .field 的写法（昵称/年龄）。
 */
@Composable
fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 150.dp,
    numeric: Boolean = false,
    placeholder: String = "",
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Ink,
            modifier = Modifier.weight(1f),
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            width = width,
            align = TextAlign.End,
            numeric = numeric,
            placeholder = placeholder,
            compact = true,
        )
    }
}
