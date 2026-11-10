package com.shenjj.edu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shenjj.edu.R
import com.shenjj.edu.data.local.WordCounts
import com.shenjj.edu.model.TeacherCatalog
import com.shenjj.edu.ui.Fmt
import com.shenjj.edu.ui.components.SettingItem
import com.shenjj.edu.ui.components.tabBarBottomInset
import com.shenjj.edu.ui.nav.AuthState
import com.shenjj.edu.ui.nav.appViewModel
import com.shenjj.edu.ui.theme.Chev
import com.shenjj.edu.ui.theme.Hot
import com.shenjj.edu.ui.theme.Ink
import com.shenjj.edu.ui.theme.Line
import com.shenjj.edu.ui.theme.Muted
import com.shenjj.edu.ui.theme.Ok
import com.shenjj.edu.ui.theme.Spark
import com.shenjj.edu.ui.theme.SparkSoft
import com.shenjj.edu.ui.theme.TagPhraseBg
import com.shenjj.edu.ui.theme.TagPhraseInk
import com.shenjj.edu.ui.theme.TagDoneBg

/**
 * 我的（原型 me）：用户卡片 + 余额卡 + 默认老师/学习分析/单词本/提醒/更多设置分组 + 退出登录。
 * 学习分析、个人设置、充值都是二级页，从本页进入（AGENTS.md：分析不作一级页签）。
 */
@Composable
fun MeScreen(
    onProfile: () -> Unit,
    onAnalytics: () -> Unit,
    onWords: () -> Unit,
    onRecharge: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = appViewModel()
    val auth by vm.auth.collectAsStateWithLifecycle()
    val user = (auth as? AuthState.SignedIn)?.user
    val countsFlow = remember(vm.repository) { vm.repository.observeWordCounts() }
    val counts by countsFlow.collectAsStateWithLifecycle(WordCounts())
    val totalSecFlow = remember(vm.repository) { vm.repository.observeTotalDurationSeconds() }
    val totalSeconds by totalSecFlow.collectAsStateWithLifecycle(0)

    val teacher = TeacherCatalog.byId(user?.defaultTeacherId ?: TeacherCatalog.ALL.first().id)
    val mins = totalSeconds / 60

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .tabBarBottomInset(),
    ) {
        Text(
            text = "我的",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
        )

        // 用户卡片：渐变圆头像 + 昵称 + 年龄·水平·目标
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onProfile,
                )
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserAvatar(name = user?.nickname ?: "同学")
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(
                    text = user?.nickname ?: "同学",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp, letterSpacing = (-0.3).sp),
                    color = Ink,
                )
                Text(
                    text = buildString {
                        append(user?.age ?: 0).append(" 岁 · ")
                        append(user?.level?.label ?: "").append(" · ")
                        append(user?.goals?.joinToString(" / ") ?: "")
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Muted,
                    modifier = Modifier.padding(top = 3.dp),
                    maxLines = 1,
                )
            }
            Icon(painterResource(R.drawable.ic_chev), null, tint = Chev, modifier = Modifier.size(18.dp))
        }

        BalanceCard(
            balanceCents = user?.balanceCents ?: 0,
            onRecharge = onRecharge,
        )

        GroupCard {
            SettingItem(
                label = "默认 AI 老师",
                value = "${teacher.name} · ${teacher.style.label}",
                iconGlyph = teacher.glyph,
                iconBg = teacher.endColor.copy(alpha = 0.10f),
                iconTint = teacher.endColor,
                onClick = onProfile,
            )
        }

        GroupCard {
            SettingItem(
                label = "学习分析",
                value = "$mins 分钟 · ${counts.mastered} 词",
                iconRes = R.drawable.ic_chart,
                iconBg = TagPhraseBg,
                iconTint = TagPhraseInk,
                onClick = onAnalytics,
            )
            ItemDivider()
            SettingItem(
                label = "单词本",
                value = "${counts.pending} 待复习",
                iconRes = R.drawable.ic_book,
                iconBg = SparkSoft,
                iconTint = Spark,
                onClick = onWords,
            )
        }

        GroupCard {
            SettingItem(
                label = "练习提醒",
                value = "每天 20:00",
                iconRes = R.drawable.ic_bell,
                iconBg = TagDoneBg,
                iconTint = Ok,
                onClick = {},
                showChevron = false,
            )
            ItemDivider()
            SettingItem(
                label = "更多设置",
                value = "昵称 / 年龄 / 目标",
                iconRes = R.drawable.ic_gear,
                iconBg = Color(0xFFEEF2FF),
                iconTint = Color(0xFF4F63F2),
                onClick = onProfile,
            )
        }

        // 退出登录
        GroupCard {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onLogout,
                    )
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "退出登录",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Hot,
                )
            }
        }
    }
}

/** 余额卡：墨底 + 右下光晕 + 账户余额 + 约可对话分钟数 + 充值按钮 */
@Composable
private fun BalanceCard(balanceCents: Int, onRecharge: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val minutes = balanceCents / 50 // CENTS_PER_MINUTE=50，1 元≈2 分钟
    Box(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(Ink)
            .padding(18.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 30.dp, y = 40.dp)
                .size(140.dp)
                .alpha(0.45f)
                .background(Brush.radialGradient(listOf(Spark, Color.Transparent)), CircleShape),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "账户余额",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp, letterSpacing = 0.4.sp),
                    color = Color.White.copy(alpha = 0.55f),
                )
                Text(
                    text = Fmt.yuan(balanceCents),
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp, letterSpacing = (-1).sp),
                    color = Color.White,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    text = "约可对话 $minutes 分钟",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Spark)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onRecharge,
                    )
                    .padding(horizontal = 18.dp, vertical = 9.dp),
            ) {
                Text("充值", style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
        }
    }
}

/** 渐变圆头像 + 昵称首字母（对应原型「我的」顶部头像，与老师头像区分） */
@Composable
private fun UserAvatar(name: String) {
    val initial = name.trim().firstOrNull()?.toString() ?: "T"
    Box(
        modifier = Modifier
            .size(60.dp)
            .background(
                Brush.linearGradient(listOf(Color(0xFFFFD9C4), Color(0xFFFF9A68))),
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
            color = Color(0xFFB4441E),
        )
    }
}

/** group 容器：白底 20dp 圆角 + 1dp 描边（原型 .group） */
@Composable
private fun GroupCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 14.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(20.dp)),
        content = content,
    )
}

/** item 之间的左缩进分隔线（原型 .item+.item::before，left:16px） */
@Composable
private fun ItemDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .height(1.dp)
            .background(Line),
    )
}
