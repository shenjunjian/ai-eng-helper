package com.shenjj.edu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shenjj.edu.ui.nav.TabItem
import com.shenjj.edu.ui.theme.Ink
import com.shenjj.edu.ui.theme.Line
import com.shenjj.edu.ui.theme.Muted
import com.shenjj.edu.ui.theme.Spark
import com.shenjj.edu.ui.theme.TabBarScrim
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/* ============================================================
   底部页签栏 —— 对应原型 .tab-bar：
   固定于窗口底部、rgba(255,255,255,.82) + backdrop blur(20px)
   毛玻璃、仅 1px 顶边线、内容区 58dp + 系统安全区留白。
   进入实时对话页时整体不渲染（由 NavHost 判断路由后跳过）。
   ============================================================ */

/** 内容区高度：原型 --tab-h 82px 减去 24px 模拟安全区 */
val TAB_CONTENT_HEIGHT: Dp = 58.dp

/** 顶边线高度，对应原型 .tab-bar{border-top:1px solid var(--line)} */
private val TAB_BORDER_HEIGHT: Dp = 1.dp

/** 毛玻璃模糊半径，对应原型 blur(20px) */
private val TAB_BLUR = 20.dp

/**
 * 带底部页签栏的页面，内容需要的底部留白。
 *
 * 页签栏是用 Box **覆盖**在内容之上的（不是 Column 里占位），所以内容必须自己留出
 * 页签栏的高度：既能让最后一项完整露出，又能让内容滚到毛玻璃之下被采样到
 * （haze 需要有东西经过模糊层背后才有得可模糊）。
 *
 * 留白 = 系统导航栏安全区 + 1dp 顶边线 + 58dp 内容区，与 [BottomTabBar] 的实测高度一致。
 * 通话页不渲染页签栏，因此也不该套用本 Modifier。
 */
fun Modifier.tabBarBottomInset(): Modifier =
    navigationBarsPadding().padding(bottom = TAB_CONTENT_HEIGHT + TAB_BORDER_HEIGHT)

/**
 * @param hazeState 模糊采样源；调用方需给页面内容加 `Modifier.hazeSource(hazeState)`
 * @param badgeCount 单词本角标（待复习数），0 时不显示
 */
@Composable
fun BottomTabBar(
    currentRoute: String?,
    hazeState: HazeState,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .hazeEffect(state = hazeState) {
                blurRadius = TAB_BLUR
                // 原型的 rgba(255,255,255,.82)：白色打底 + .82 透明度的 tint 叠加；
                // saturate(180%) 由 haze 的色彩处理近似
                backgroundColor = Color.White
                tints = listOf(HazeTint(TabBarScrim))
            }
            // 安全区留白放在模糊层之内，使毛玻璃延伸到导航栏区域
            .navigationBarsPadding(),
    ) {
        // border-top:1px solid var(--line)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Line),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TAB_CONTENT_HEIGHT),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabItem.ALL.forEach { tab ->
                TabEntry(
                    tab = tab,
                    selected = currentRoute == tab.route,
                    badge = if (tab.showBadge) badgeCount else 0,
                    onClick = { onTabSelected(tab.route) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TabEntry(
    tab: TabItem,
    selected: Boolean,
    badge: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val tint = if (selected) Ink else Muted

    Column(
        modifier = modifier
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(bottom = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box {
            Icon(
                painter = painterResource(tab.iconRes),
                contentDescription = tab.label,
                tint = tint,
                modifier = Modifier
                    .size(24.dp)
                    // 原型 .tab.on svg{transform:translateY(-1px)}
                    .then(if (selected) Modifier.offset(y = (-1).dp) else Modifier),
            )
            if (badge > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-3).dp)
                        .background(Spark, CircleShape)
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (badge > 9) "9+" else badge.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            letterSpacing = 0.sp,
                        ),
                        color = Color.White,
                    )
                }
            }
        }
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 10.5.sp,
                letterSpacing = 0.2.sp,
            ),
            color = tint,
        )
    }
}
