package com.shenjj.edu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shenjj.edu.model.WordKind
import com.shenjj.edu.ui.theme.AppSurface
import com.shenjj.edu.ui.theme.Ink
import com.shenjj.edu.ui.theme.Ink2
import com.shenjj.edu.ui.theme.Line
import com.shenjj.edu.ui.theme.Ok
import com.shenjj.edu.ui.theme.OkSoftLine
import com.shenjj.edu.ui.theme.Spark
import com.shenjj.edu.ui.theme.SparkDeep
import com.shenjj.edu.ui.theme.SparkSoft
import com.shenjj.edu.ui.theme.TagDoneBg
import com.shenjj.edu.ui.theme.TagPhraseBg
import com.shenjj.edu.ui.theme.TagPhraseInk

/* ============================================================
   Chip 与 Tag —— 对应原型 .chip（可选中/可点击）与
   .tag / .tag.phrase / .tag.done（纯展示的类型徽标）
   ============================================================ */

/**
 * 筛选/开关 chip（.chip）。
 * @param selected 选中态：[sparkOn]=true 时用 .chip.spark.on（强调色底），否则 .chip.on（墨底）
 * @param leadingRes 前置矢量图标（原型 chip 里带 15dp 图标）
 * @param onClick 为 null 时不可点击（如登录页的 +86）
 */
@Composable
fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    sparkOn: Boolean = false,
    leadingRes: Int? = null,
    onClick: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }

    val container = when {
        !selected -> AppSurface
        sparkOn -> Spark
        else -> Ink
    }
    val border = if (!selected) Line else container
    val content = if (!selected) Ink2 else Color.White

    Row(
        modifier = modifier
            .background(container, CircleShape)
            .border(1.dp, border, CircleShape)
            .then(
                if (onClick == null) Modifier
                else Modifier.clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                )
            )
            .padding(horizontal = 13.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (leadingRes != null) {
            Icon(
                painter = painterResource(leadingRes),
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(15.dp),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            maxLines = 1,
        )
    }
}

/** 横向可滚动的 chip 组（.ctrl-strip / 单词本筛选行），隐藏滚动条 */
@Composable
fun ChipRow(
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
    spacing: androidx.compose.ui.unit.Dp = 8.dp,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
        content()
    }
}

/**
 * 词条类型徽标（.tag）：单词=强调浅底、习语=紫底、已掌握=绿底。
 * [done] 优先于 [kind]。
 */
@Composable
fun WordTag(
    kind: WordKind,
    done: Boolean,
    modifier: Modifier = Modifier,
) {
    val (bg, fg) = when {
        done -> TagDoneBg to Ok
        kind == WordKind.PHRASE -> TagPhraseBg to TagPhraseInk
        else -> SparkSoft to SparkDeep
    }
    val label = when {
        done -> "已掌握"
        kind == WordKind.PHRASE -> "习语"
        else -> "单词"
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.5.sp,
            letterSpacing = 0.sp,
        ),
        color = fg,
        modifier = modifier
            .background(bg, CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/**
 * 信息型胶囊（原型单词本详情里那些不可点的 .chip，如「已掌握」绿色变体）。
 */
@Composable
fun InfoChip(
    text: String,
    modifier: Modifier = Modifier,
    bg: Color = AppSurface,
    fg: Color = Ink2,
    border: Color = Line,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        modifier = modifier
            .background(bg, CircleShape)
            .border(1.dp, border, CircleShape)
            .padding(horizontal = 13.dp, vertical = 7.dp),
    )
}

/** 已掌握态的绿色 info chip（原型里硬编码 #E8F7EF / --ok / #D6F0E2） */
@Composable
fun DoneChip(text: String = "已掌握", modifier: Modifier = Modifier) {
    InfoChip(
        text = text,
        modifier = modifier,
        bg = TagDoneBg,
        fg = Ok,
        border = OkSoftLine,
    )
}
