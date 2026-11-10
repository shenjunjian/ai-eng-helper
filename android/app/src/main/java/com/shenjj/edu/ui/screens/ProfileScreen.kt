package com.shenjj.edu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shenjj.edu.R
import com.shenjj.edu.model.ChatMode
import com.shenjj.edu.model.LearningGoals
import com.shenjj.edu.model.StudyLevel
import com.shenjj.edu.model.TeacherCatalog
import com.shenjj.edu.ui.Fmt
import com.shenjj.edu.ui.components.Chip
import com.shenjj.edu.ui.components.GhostButton
import com.shenjj.edu.ui.components.LocalToast
import com.shenjj.edu.ui.components.NavBar
import com.shenjj.edu.ui.components.SheetContent
import com.shenjj.edu.ui.components.SheetOption
import com.shenjj.edu.ui.components.TalkFlowSheet
import com.shenjj.edu.ui.components.TeacherAvatar
import com.shenjj.edu.ui.components.TextField
import com.shenjj.edu.ui.nav.AuthState
import com.shenjj.edu.ui.nav.appViewModel
import com.shenjj.edu.ui.theme.Chev
import com.shenjj.edu.ui.theme.Ink
import com.shenjj.edu.ui.theme.Line
import com.shenjj.edu.ui.theme.Muted
import com.shenjj.edu.ui.theme.Spark
import kotlinx.coroutines.launch

private val GroupShape = RoundedCornerShape(20.dp)

/**
 * 个人设置（原型 profile）：昵称 / 年龄 / 英文水平 / 学习目标 / 默认 AI 老师 /
 * 对话模式 / 默认沉浸式，以及账号余额与充值入口（AGENTS.md：显示余额并让用户充值）。
 *
 * 与原型只读展示不同，这里各项都**可编辑**，保存时一次性写回 users 表。
 */
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onRecharge: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = appViewModel()
    val scope = rememberCoroutineScope()
    val toast = LocalToast.current
    val auth by vm.auth.collectAsStateWithLifecycle()
    val user = (auth as? AuthState.SignedIn)?.user

    var nickname by remember(user) { mutableStateOf(user?.nickname ?: "") }
    var ageText by remember(user) { mutableStateOf((user?.age ?: 0).toString()) }
    var level by remember(user) { mutableStateOf(user?.level ?: StudyLevel.INTERMEDIATE) }
    var goals by remember(user) { mutableStateOf(user?.goals ?: emptyList()) }
    var teacherId by remember(user) {
        mutableStateOf(user?.defaultTeacherId ?: TeacherCatalog.ALL.first().id)
    }
    var chatMode by remember(user) { mutableStateOf(user?.defaultChatMode ?: ChatMode.TEACHING) }
    var immersive by remember(user) { mutableStateOf(user?.defaultImmersive ?: false) }

    // 三个选择 sheet：老师 / 对话模式（沉浸式用行内开关，不需要 sheet）
    var showTeacherSheet by remember { mutableStateOf(false) }
    var showModeSheet by remember { mutableStateOf(false) }

    val teacher = TeacherCatalog.byId(teacherId)
    val ageValue = ageText.toIntOrNull()?.coerceIn(0, 120) ?: 0
    val canSave = nickname.isNotBlank() && ageValue in 3..100 && goals.isNotEmpty()

    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        NavBar(title = "个人设置", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 26.dp),
        ) {
            // 分组一：基本资料
            GroupCard {
                FieldRow(label = "昵称") {
                    TextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        placeholder = "怎么称呼你",
                        modifier = Modifier.weight(1f),
                    )
                }
                RowDivider()
                FieldRow(label = "年龄") {
                    TextField(
                        value = ageText,
                        onValueChange = { input -> ageText = input.filter(Char::isDigit).take(3) },
                        placeholder = "岁",
                        numeric = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                RowDivider()
                PickerRow(label = "英文水平", value = "", onClick = {}, showChevron = false)
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StudyLevel.entries.forEach { lv ->
                            Chip(
                                text = lv.label,
                                selected = lv == level,
                                onClick = { level = lv },
                            )
                        }
                    }
                }
                RowDivider()
                PickerRow(label = "学习目标", value = goals.joinToString(" · "), onClick = {})
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        LearningGoals.ALL.take(3).forEach { g ->
                            GoalChip(
                                text = g,
                                selected = g in goals,
                                onClick = { goals = goals.toggle(g) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        LearningGoals.ALL.drop(3).forEach { g ->
                            GoalChip(
                                text = g,
                                selected = g in goals,
                                onClick = { goals = goals.toggle(g) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        // 补位保持等宽
                        repeat(3 - LearningGoals.ALL.drop(3).size) {
                            Box(Modifier.weight(1f))
                        }
                    }
                }
            }

            // 分组二：AI 对话偏好
            GroupCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { showTeacherSheet = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TeacherAvatar(teacher = teacher, size = 34.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "默认 AI 老师",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Ink,
                        )
                        Text(
                            text = "${teacher.name} · ${teacher.style.label}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Muted,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Icon(
                        painter = painterResource(R.drawable.ic_chev),
                        contentDescription = null,
                        tint = Chev,
                        modifier = Modifier.size(16.dp),
                    )
                }
                RowDivider()
                PickerRow(
                    label = "对话模式",
                    value = chatMode.label,
                    onClick = { showModeSheet = true },
                )
                RowDivider()
                // 默认沉浸式：行内开关（通话时隐藏中文字幕）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "默认沉浸式",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Ink,
                        )
                        Text(
                            text = "开启后对话中隐藏中文字幕",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Muted,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Switch(
                        checked = immersive,
                        onCheckedChange = { immersive = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Spark,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Line,
                        ),
                    )
                }
            }

            // 分组三：余额与充值
            GroupCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onRecharge,
                        )
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "账号余额",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Ink,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = Fmt.yuan(user?.balanceCents ?: 0),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Ink,
                    )
                    Text(
                        text = "充值",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Spark,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.ic_chev),
                        contentDescription = null,
                        tint = Chev,
                        modifier = Modifier.size(16.dp).padding(start = 2.dp),
                    )
                }
            }

            GhostButton(
                text = "保存",
                enabled = canSave,
                onClick = {
                    scope.launch {
                        vm.repository.saveProfile(
                            nickname = nickname.trim(),
                            age = ageValue,
                            level = level,
                            goals = goals,
                            teacherId = teacherId,
                            chatMode = chatMode,
                            immersive = immersive,
                        )
                        toast.show("已保存")
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }

    // 选择默认 AI 老师
    TalkFlowSheet(visible = showTeacherSheet, onDismiss = { showTeacherSheet = false }) {
        SheetContent(title = "默认 AI 老师", subtitle = "开始新对话时会用它") {
            TeacherCatalog.ALL.forEachIndexed { index, t ->
                if (index > 0) SheetDivider()
                SheetOption(
                    title = "${t.name} · ${t.style.label}",
                    subtitle = t.desc,
                    selected = t.id == teacherId,
                    leading = { TeacherAvatar(teacher = t, size = 38.dp) },
                    onClick = {
                        teacherId = t.id
                        showTeacherSheet = false
                    },
                )
            }
        }
    }

    // 选择对话模式
    TalkFlowSheet(visible = showModeSheet, onDismiss = { showModeSheet = false }) {
        SheetContent(title = "对话模式", subtitle = "老师的说话风格") {
            ChatMode.entries.forEachIndexed { index, mode ->
                if (index > 0) SheetDivider()
                SheetOption(
                    title = mode.label,
                    subtitle = mode.hint,
                    selected = mode == chatMode,
                    onClick = {
                        chatMode = mode
                        showModeSheet = false
                    },
                )
            }
        }
    }
}

/** group 容器：白底 20dp 圆角 + 1dp 描边（原型 .group） */
@Composable
private fun GroupCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .clip(GroupShape)
            .background(Color.White)
            .border(1.dp, Line, GroupShape),
        content = content,
    )
}

/** group 内行与行之间的左缩进分隔线（原型 .item+.item::before，left:16px） */
@Composable
private fun RowDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .height(1.dp)
            .background(Line),
    )
}

/** sheet 内条目之间的分隔线 */
@Composable
private fun SheetDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 50.dp)
            .height(1.dp)
            .background(Line),
    )
}

/** 左标签 + 右内容的编辑行 */
@Composable
private fun FieldRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Ink,
            modifier = Modifier.size(width = 68.dp, height = 20.dp),
        )
        content()
    }
}

/** 左标签 + 右取值 + 箭头的选择行 */
@Composable
private fun PickerRow(label: String, value: String, onClick: () -> Unit, showChevron: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Ink,
            modifier = Modifier.weight(1f),
        )
        if (value.isNotBlank()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                color = Muted,
                maxLines = 1,
            )
        }
        if (showChevron) {
            Icon(
                painter = painterResource(R.drawable.ic_chev),
                contentDescription = null,
                tint = Chev,
                modifier = Modifier.size(16.dp).padding(start = 4.dp),
            )
        }
    }
}

/** 学习目标 Chip：等宽铺满，选中态带 sparkOn 勾选色 */
@Composable
private fun GoalChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Chip(
        text = text,
        selected = selected,
        sparkOn = selected,
        onClick = onClick,
        modifier = modifier,
    )
}

private fun List<String>.toggle(item: String): List<String> =
    if (item in this) this - item else this + item

/** ChatMode 没有描述字段（模型层只有 label），这里给选择 sheet 补一句说明 */
private val ChatMode.hint: String
    get() = when (this) {
        ChatMode.GENTLE -> "多鼓励、放慢语速，适合刚开口"
        ChatMode.TEACHING -> "边聊边讲，遇到新词就展开"
        ChatMode.CORRECTIVE -> "及时纠正发音与语法"
        ChatMode.CASUAL -> "轻松闲聊，像朋友一样"
    }
