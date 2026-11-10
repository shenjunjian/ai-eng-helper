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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.shenjj.edu.data.local.SessionWithWordCount
import com.shenjj.edu.model.Teacher
import com.shenjj.edu.model.TeacherCatalog
import com.shenjj.edu.ui.components.CellDivider
import com.shenjj.edu.ui.components.CircleIconButton
import com.shenjj.edu.ui.components.LocalToast
import com.shenjj.edu.ui.components.SessionCell
import com.shenjj.edu.ui.components.TeacherAvatar
import com.shenjj.edu.ui.components.tabBarBottomInset
import com.shenjj.edu.ui.nav.AuthState
import com.shenjj.edu.ui.nav.appViewModel
import com.shenjj.edu.ui.theme.Ink
import com.shenjj.edu.ui.theme.Muted
import com.shenjj.edu.ui.theme.OnDarkMuted
import com.shenjj.edu.ui.theme.Spark
import kotlinx.coroutines.launch

/**
 * 会话首页（原型 chats）。
 *
 * 按 AGENTS.md：本页**不提供任何"开始学习"类功能入口**，唯一的开始方式是
 * 顶部的「自由对话」，或在其中点头像直接指定本次对话的老师；下方是按最近活跃
 * 倒序的历史会话列表（SQL 已 ORDER BY last_message_at DESC），
 * 点击任一会话会 touch 置顶后进入实时对话，挂断只更新它、不新建。
 */
@Composable
fun ChatsScreen(
    onOpenSession: (String) -> Unit,
    onOpenWords: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = appViewModel()
    val scope = rememberCoroutineScope()
    val toast = LocalToast.current

    val auth by vm.auth.collectAsStateWithLifecycle()
    val user = (auth as? AuthState.SignedIn)?.user
    val sessions by vm.repository.observeSessions().collectAsStateWithLifecycle(emptyList())
    val pendingWords by vm.pendingWordCount.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .tabBarBottomInset(),
    ) {
        // 标题行：会话 + 搜索 + 单词本（带待复习角标）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "会话",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            CircleIconButton(
                iconRes = R.drawable.ic_search,
                contentDescription = "搜索",
                onClick = { toast.show("按标题与词条检索即将上线") },
            )
            Box(modifier = Modifier.padding(start = 9.dp)) {
                CircleIconButton(
                    iconRes = R.drawable.ic_book,
                    contentDescription = "单词本",
                    onClick = onOpenWords,
                )
                if (pendingWords > 0) {
                    Badge(
                        count = pendingWords,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset3dp(),
                    )
                }
            }
        }

        Text(
            text = "Hi ${user?.nickname ?: "同学"}，${sessions.size} 段对话在这里",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = Muted,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
        )

        FreeChatEntry(
            defaultTeacherId = user?.defaultTeacherId ?: TeacherCatalog.ALL.first().id,
            onStart = { teacherId ->
                scope.launch {
                    val teacher = TeacherCatalog.byId(teacherId)
                    val session = vm.repository.startFreeChat(teacherId, teacher.style)
                    onOpenSession(session.id)
                }
            },
        )

        if (sessions.isEmpty()) {
            EmptyHint()
        } else {
            SectionLabel(text = "最近对话")
            SessionList(
                sessions = sessions,
                onOpen = { id ->
                    scope.launch {
                        // openSession 内部 touch 置顶，因此回到首页时它已在最新位置
                        vm.repository.openSession(id)
                        onOpenSession(id)
                    }
                },
            )
        }
    }
}

/**
 * 自由对话入口（原型 .big-ic 卡片）：墨底大圆角卡 + 右上强调色径向光晕，
 * 主体点击用默认老师开始；下方一排老师头像可直接指定本次对话的老师。
 */
@Composable
private fun FreeChatEntry(
    defaultTeacherId: String,
    onStart: (teacherId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(26.dp)

    Box(
        modifier = modifier
            .padding(horizontal = 20.dp, vertical = 3.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(Ink)
            .clickable(
                interactionSource = interaction,
                indication = null,
            ) { onStart(defaultTeacherId) }
            .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 16.dp),
    ) {
        // 光晕：radial-gradient(circle, var(--spark), transparent 68%)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 24.dp, y = (-30).dp)
                .size(130.dp)
                .alpha(0.5f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Spark, Color.Transparent),
                    ),
                    CircleShape,
                ),
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(Color.White.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_mic),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp),
                ) {
                    Text(
                        text = "自由对话",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            letterSpacing = (-0.3).sp,
                        ),
                        color = Color.White,
                    )
                    Text(
                        text = "随时开口，AI 老师陪你聊任何话题",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                        color = OnDarkMuted,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.ic_chev),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp),
                )
            }

            // 点头像指定老师：默认老师全亮并带强调色描边环
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TeacherCatalog.ALL.forEach { t ->
                    TeacherPick(
                        teacher = t,
                        isDefault = t.id == defaultTeacherId,
                        onClick = { onStart(t.id) },
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                }
                Text(
                    text = "点头像指定老师",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun TeacherPick(
    teacher: Teacher,
    isDefault: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(36.dp)
            .alpha(if (isDefault) 1f else 0.62f)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        TeacherAvatar(teacher = teacher, size = 36.dp, glyphSize = 18.dp)
        if (isDefault) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .border(2.dp, Spark, CircleShape),
            )
        }
    }
}

/** 历史会话列表：白底圆角容器 + cell 之间的左缩进分隔线（原型 .cell+.cell::before） */
@Composable
private fun SessionList(
    sessions: List<SessionWithWordCount>,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp)
            .clip(shape)
            .background(Color.White),
    ) {
        sessions.forEachIndexed { index, s ->
            if (index > 0) CellDivider()
            SessionCell(
                session = s,
                onClick = { onOpen(s.session.id) },
            )
        }
    }
}

@Composable
private fun EmptyHint(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "还没有对话",
            style = MaterialTheme.typography.titleSmall,
            color = Ink,
        )
        Text(
            text = "点上面的「自由对话」，开口就能开始",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
            color = Muted,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** sec-label：11px 700 + .8px 字距 + 大写 + muted */
@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
        ),
        color = Muted,
        modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp),
    )
}

/** 角标：min-width 17dp 胶囊，超过 9 显示「9+」 */
@Composable
private fun Badge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(17.dp)
            .background(Spark, RoundedCornerShape(9.dp))
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 9) "9+" else count.toString(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
            ),
            color = Color.White,
        )
    }
}

/** 角标外扩 3dp（对应原型 top:-3px;right:-3px），用 offset 而非负 padding */
private fun Modifier.offset3dp(): Modifier = this.offset(x = 3.dp, y = (-3).dp)
