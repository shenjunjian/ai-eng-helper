package com.shenjj.edu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shenjj.edu.R
import com.shenjj.edu.model.LearningGoals
import com.shenjj.edu.model.StudyLevel
import com.shenjj.edu.model.TeacherCatalog
import com.shenjj.edu.ui.components.CellDivider
import com.shenjj.edu.ui.components.Chip
import com.shenjj.edu.ui.components.DarkButton
import com.shenjj.edu.ui.components.LabeledField
import com.shenjj.edu.ui.components.LocalToast
import com.shenjj.edu.ui.components.NavBar
import com.shenjj.edu.ui.components.TeacherAvatar
import com.shenjj.edu.ui.nav.AuthState
import com.shenjj.edu.ui.nav.appViewModel
import com.shenjj.edu.ui.theme.AppBg
import com.shenjj.edu.ui.theme.Ink
import com.shenjj.edu.ui.theme.Ink2
import com.shenjj.edu.ui.theme.Muted
import com.shenjj.edu.ui.theme.Radii
import com.shenjj.edu.ui.theme.Spark
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 学习档案（原型 onboarding）：昵称、年龄、英文水平、学习目标（多选）、默认 AI 老师。
 * 点「开始开口」写入 users 表并置 onboarded=true，然后进会话首页。
 *
 * 表单初值取当前登录用户（假登录会自动注册默认档案），已完善过资料的
 * 用户被带回本页时看到的是自己的数据而不是空表单。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = appViewModel()
    val scope = rememberCoroutineScope()
    val toast = LocalToast.current
    val auth by vm.auth.collectAsStateWithLifecycle()
    val current = (auth as? AuthState.SignedIn)?.user

    var nickname by remember { mutableStateOf(current?.nickname ?: "") }
    var age by remember { mutableStateOf(current?.age?.takeIf { it > 0 }?.toString() ?: "") }
    var level by remember { mutableStateOf(current?.level ?: StudyLevel.INTERMEDIATE) }
    var goals by remember { mutableStateOf(current?.goals ?: listOf(LearningGoals.ALL.first())) }
    var teacherId by remember { mutableStateOf(current?.defaultTeacherId ?: TeacherCatalog.ALL.first().id) }
    var busy by remember { mutableStateOf(false) }

    // auth 首次带上用户时同步一次表单初值（LaunchedEffect 版 remember）
    androidx.compose.runtime.LaunchedEffect(current?.id) {
        current?.let { u ->
            if (nickname.isBlank()) nickname = u.nickname
            if (age.isBlank() && u.age > 0) age = u.age.toString()
            if (u.goals.isNotEmpty()) goals = u.goals
            teacherId = u.defaultTeacherId
            level = u.level
        }
    }

    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        NavBar(title = "学习档案", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 6.dp, bottom = 26.dp),
        ) {
            Text(
                text = "这几步让 AI 老师知道该用什么词、什么语速跟你聊。",
                style = MaterialTheme.typography.bodyMedium,
                color = Ink2,
                modifier = Modifier.padding(bottom = 20.dp),
            )

            // 资料分组卡片（.group：白底大圆角）
            val groupShape = RoundedCornerShape(Radii.Group)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, groupShape)
                    .padding(vertical = 2.dp),
            ) {
                LabeledField(label = "昵称", value = nickname, onValueChange = { nickname = it.take(12) })
                CellDivider()
                LabeledField(
                    label = "年龄",
                    value = age,
                    onValueChange = { age = it.filter(Char::isDigit).take(3) },
                    numeric = true,
                    width = 90.dp,
                    placeholder = "如 22",
                )
                CellDivider()
                LevelRow(level = level, onPick = { level = it })
            }

            SectionLabel("学习目标（可多选）")
            FlowRow(
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                LearningGoals.ALL.forEach { g ->
                    Chip(
                        text = g,
                        selected = g in goals,
                        onClick = { goals = if (g in goals) goals - g else goals + g },
                    )
                }
            }

            SectionLabel("默认 AI 老师")
            Row(
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 26.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TeacherCatalog.ALL.forEach { t ->
                    TeacherPickItem(
                        teacher = t,
                        picked = teacherId == t.id,
                        onClick = { teacherId = t.id },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            DarkButton(
                text = "开始开口",
                enabled = !busy && nickname.isNotBlank() && age.isNotBlank(),
                onClick = {
                    busy = true
                    scope.launch {
                        vm.repository.saveProfile(
                            nickname = nickname.trim(),
                            age = age.toIntOrNull()?.coerceIn(6, 99) ?: 18,
                            level = level,
                            goals = goals,
                            teacherId = teacherId,
                        )
                        // 等 auth 流刷新出 onboarded=true 再跳转，避免闪回本页
                        val settled = snapshotFlow { vm.auth.value }
                            .first { (it as? AuthState.SignedIn)?.user?.onboarded == true }
                        onDone()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (nickname.isBlank()) {
                Text(
                    text = "请先填写昵称与年龄",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = Muted,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 12.dp),
                )
            }
        }
    }
}

/** 老师选择项：头像 + 右下角强调色勾选徽标 + 名字 + 风格 */
@Composable
private fun TeacherPickItem(
    teacher: com.shenjj.edu.model.Teacher,
    picked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier.clickable(
            interactionSource = interaction,
            indication = null,
            onClick = onClick,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            TeacherAvatar(teacher = teacher, size = 54.dp)
            if (picked) {
                // 徽标外描 2.5dp 米白边（原型 border:2.5px solid var(--bg)）；
                // 用 offset 外扩而不是负 padding（Compose 不允许负 padding）
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 3.dp, y = 3.dp)
                        .size(20.dp)
                        .border(2.5.dp, AppBg, CircleShape)
                        .background(Spark, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = "已选择 ${teacher.name}",
                        tint = Color.White,
                        modifier = Modifier.size(11.dp),
                    )
                }
            }
        }
        Text(
            text = teacher.name,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, letterSpacing = 0.sp),
            color = Ink,
            modifier = Modifier.padding(top = 7.dp),
        )
        Text(
            text = teacher.style.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.sp),
            color = Muted,
        )
    }
}

/** 英文水平：行内三选一（对应原型 select） */
@Composable
private fun LevelRow(level: StudyLevel, onPick: (StudyLevel) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "英文水平",
            style = MaterialTheme.typography.bodyMedium,
            color = Ink2,
            modifier = Modifier.weight(1f),
        )
        StudyLevel.entries.forEach { lv ->
            Chip(
                text = lv.label,
                selected = lv == level,
                onClick = { onPick(lv) },
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, letterSpacing = 0.2.sp),
        color = Muted,
        modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 10.dp),
    )
}
