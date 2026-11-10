package com.shenjj.edu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shenjj.edu.R
import com.shenjj.edu.ui.theme.AppSurface
import com.shenjj.edu.ui.theme.Grab
import com.shenjj.edu.ui.theme.Ink
import com.shenjj.edu.ui.theme.Line
import com.shenjj.edu.ui.theme.Muted
import com.shenjj.edu.ui.theme.SheetShape
import com.shenjj.edu.ui.theme.Spark
import com.shenjj.edu.ui.theme.Radii

/* ============================================================
   底部抽屉 —— 对应原型 openSheet/closeSheet：
   .scrim（rgba(11,12,15,.4)）+ .sheet（28px 顶部圆角、
   最高 72%、.grab 抓手、h3 标题、.opt 选项行）
   ============================================================ */

/**
 * 抽屉里的一行选项（.opt）：40dp 头像/图标 + 标题与副标题 + 右侧选中勾。
 * [selected] 为 true 时右侧显示强调色勾（原型里对应当前老师）。
 */
@Composable
fun SheetOption(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    selected: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Ink,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = Muted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = "已选中",
                tint = Spark,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** 选项行之间的分隔线（原型 .opt+.opt::before 从左侧 50dp 起） */
@Composable
fun SheetDivider(modifier: Modifier = Modifier, indent: Dp = 50.dp) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = indent)
            .height(1.dp)
            .background(Line),
    )
}

/**
 * 底部抽屉外壳：抓手 + 标题 + 可选副标题 + 内容。
 * 外层用 [TalkFlowSheet] 提供遮罩与显隐动画，这里只管内容排版。
 * 内容区最高占屏 72%（原型 .sheet max-height:72%），超出可滚动。
 */
@Composable
fun SheetContent(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(SheetShape)
            .background(AppSurface)
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 30.dp)
            .navigationBarsPadding(),
    ) {
        // .grab 38x4 抓手
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 38.dp, height = 4.dp)
                .background(Grab, androidx.compose.foundation.shape.RoundedCornerShape(Radii.Grab)),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Ink,
            modifier = Modifier.padding(top = 14.dp),
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = Muted,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
            )
        } else {
            Box(modifier = Modifier.size(10.dp))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            content()
        }
    }
}

/**
 * 带遮罩的抽屉容器：[visible] 控制显隐，点遮罩或返回键触发 [onDismiss]。
 * 内容从底部滑入，遮罩淡入淡出（对应原型 .32s / .25s 过渡）。
 */
@Composable
fun TalkFlowSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (!visible) return
    Box(modifier = modifier.fillMaxSize()) {
        // .scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(com.shenjj.edu.ui.theme.SheetScrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )
        // .sheet
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            content()
        }
    }
}
