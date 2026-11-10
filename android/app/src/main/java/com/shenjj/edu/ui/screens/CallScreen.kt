package com.shenjj.edu.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.LinkAnnotation
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shenjj.edu.R
import com.shenjj.edu.TalkFlowApp
import com.shenjj.edu.model.ChatMode
import com.shenjj.edu.model.MessageRole
import com.shenjj.edu.model.TeacherCatalog
import com.shenjj.edu.model.WordKind
import com.shenjj.edu.ui.Fmt
import com.shenjj.edu.ui.components.Chip
import com.shenjj.edu.ui.components.LocalToast
import com.shenjj.edu.ui.components.SheetContent
import com.shenjj.edu.ui.components.SheetOption
import com.shenjj.edu.ui.components.TalkFlowSheet
import com.shenjj.edu.ui.components.TeacherAvatar
import com.shenjj.edu.ui.theme.AppBg
import com.shenjj.edu.ui.theme.AppSurface
import com.shenjj.edu.ui.theme.Hot
import com.shenjj.edu.ui.theme.Ink
import com.shenjj.edu.ui.theme.Ink2
import com.shenjj.edu.ui.theme.Line
import com.shenjj.edu.ui.theme.Muted
import com.shenjj.edu.ui.theme.Ok
import com.shenjj.edu.ui.theme.Spark
import com.shenjj.edu.voice.CallArgs
import com.shenjj.edu.voice.CallPhase
import com.shenjj.edu.voice.CallUiState
import com.shenjj.edu.voice.CallViewModel
import com.shenjj.edu.voice.Caption

/**
 * 实时对话页（原型 call）。
 *
 * 顶部控制条（关闭 / 老师名与计时 / 切换角色）→ 功能 Chip 条（模式 / 中英字幕 / 沉浸式 /
 * 复习确认掌握）→ 流式字幕列表（AI 白气泡在左、用户墨色气泡在右，内联生词可点收录）→
 * 底部挂断。进入即请求录音权限，授权后才建连；建连成功启动前台服务。
 *
 * 本页不在 [com.shenjj.edu.ui.nav.Routes.TAB_ROUTES] 中，底部页签自动隐藏。
 */
@Composable
fun CallScreen(
    args: CallArgs,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as TalkFlowApp }
    val toast = LocalToast.current
    val vm: CallViewModel = viewModel(factory = CallViewModel.factory(app, args))
    val state by vm.state.collectAsStateWithLifecycle()

    var showTeacherSheet by remember { mutableStateOf(false) }
    var showModeSheet by remember { mutableStateOf(false) }

    // 录音权限：授权后才建连（AOQ 上行轨需要麦克风）
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionGranted = granted
        if (granted) vm.start() else vm.reportPermissionDenied()
    }

    LaunchedEffect(Unit) {
        if (permissionGranted) vm.start() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // 系统返回键等同挂断：结算后返回首页，而不是把通话留在后台
    BackHandler { vm.hangup(onExit) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBg)
            .statusBarsPadding(),
    ) {
        CallHeader(
            state = state,
            onClose = { vm.hangup(onExit) },
            onSwitchTeacher = { showTeacherSheet = true },
        )

        CtrlStrip(
            state = state,
            onMode = { showModeSheet = true },
            onBilingual = vm::toggleBilingual,
            onImmersive = vm::toggleImmersive,
            onConfirmMastered = {
                vm.confirmMastered()
                toast.show("已标记掌握，移出待复习")
            },
        )

        // 沉浸式提示（原型在字幕区中央浮一条）
        if (state.immersive) {
            Text(
                text = "沉浸式 · 中文字幕已隐藏",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            )
        }

        CaptionList(
            state = state,
            onWordClick = { en, zh, kind ->
                vm.saveInlineWord(en, zh, kind)
                toast.show("已收进单词本：$en")
            },
            modifier = Modifier.weight(1f),
        )

        CallFoot(onHangup = { vm.hangup(onExit) })
    }

    // 建连失败/无权限的覆盖层
    if (state.phase == CallPhase.Failed) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBg.copy(alpha = 0.92f))
                .padding(30.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "暂时连不上语音服务",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
                Text(
                    text = state.error ?: "请检查网络后重试",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 19.sp),
                    color = Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
                )
                Chip(text = "返回会话", onClick = onExit)
            }
        }
    }

    // 切换 AI 角色
    TalkFlowSheet(visible = showTeacherSheet, onDismiss = { showTeacherSheet = false }) {
        SheetContent(title = "切换 AI 老师", subtitle = "本次对话立即生效，会话头像同步更新") {
            TeacherCatalog.ALL.forEachIndexed { index, teacher ->
                if (index > 0) SheetDivider()
                SheetOption(
                    title = "${teacher.name} · ${teacher.style.label}",
                    subtitle = teacher.desc,
                    selected = teacher.id == state.teacher.id,
                    leading = { TeacherAvatar(teacher = teacher, size = 38.dp) },
                    onClick = {
                        vm.switchTeacher(teacher)
                        showTeacherSheet = false
                        toast.show("已切换到 ${teacher.name}")
                    },
                )
            }
        }
    }

    // 对话模式
    TalkFlowSheet(visible = showModeSheet, onDismiss = { showModeSheet = false }) {
        SheetContent(title = "对话模式", subtitle = "老师的说话风格") {
            ChatMode.entries.forEachIndexed { index, mode ->
                if (index > 0) SheetDivider()
                SheetOption(
                    title = mode.label,
                    selected = mode == state.mode,
                    onClick = {
                        vm.setMode(mode)
                        showModeSheet = false
                    },
                )
            }
        }
    }
}

/** 顶部：关闭 / 老师名 + 状态与计时 / 切换角色 */
@Composable
private fun CallHeader(
    state: CallUiState,
    onClose: () -> Unit,
    onSwitchTeacher: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose, iconRes = R.drawable.ic_x)
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "${state.teacher.name} · ${if (state.reviewMode) "复习模式" else "对话中"}",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                ),
                color = Ink,
                maxLines = 1,
            )
            Row(
                modifier = Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                val live = state.phase == CallPhase.Ready
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (live) Ok else Muted),
                )
                Text(
                    text = "${state.mode.label} · ${Fmt.duration(state.seconds)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.sp,
                    ),
                    color = if (live) Ok else Muted,
                )
            }
        }
        IconButton(onClick = onSwitchTeacher, iconRes = R.drawable.ic_mask)
    }
}

/** 36dp 圆形描边图标按钮（原型 .icon-btn） */
@Composable
private fun IconButton(onClick: () -> Unit, iconRes: Int) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(AppSurface)
            .border(1.dp, Line, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Ink2,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** 功能 Chip 条：模式 / 中英字幕 / 沉浸式 /（复习模式）确认掌握 */
@Composable
private fun CtrlStrip(
    state: CallUiState,
    onMode: () -> Unit,
    onBilingual: () -> Unit,
    onImmersive: () -> Unit,
    onConfirmMastered: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Chip(text = state.mode.label, leadingRes = R.drawable.ic_spark, onClick = onMode)
        Chip(
            text = "中英字幕",
            selected = state.bilingual,
            leadingRes = R.drawable.ic_captions,
            onClick = onBilingual,
        )
        Chip(
            text = "沉浸式",
            selected = state.immersive,
            sparkOn = state.immersive,
            leadingRes = R.drawable.ic_moon,
            onClick = onImmersive,
        )
        if (state.reviewMode && state.reviewWordEn != null) {
            Chip(
                text = if (state.reviewWordDone) "已掌握" else "确认掌握 ${state.reviewWordEn}",
                selected = state.reviewWordDone,
                sparkOn = state.reviewWordDone,
                leadingRes = R.drawable.ic_check,
                onClick = onConfirmMastered,
            )
        }
    }
}

/** 字幕列表：逐条显示并自动滚动到底部 */
@Composable
private fun CaptionList(
    state: CallUiState,
    onWordClick: (String, String, WordKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val captions = state.captions
    // 字幕增量或条数变化时滚到底（原型 chat-scroll 自动跟随）
    val tail = captions.lastOrNull()?.en?.length ?: 0
    LaunchedEffect(captions.size, tail) {
        if (captions.isNotEmpty()) listState.animateScrollToItem(captions.lastIndex)
    }

    if (captions.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = if (state.phase == CallPhase.Connecting) "正在接通 ${state.teacher.name}…"
                else "开口说点什么，字幕会出现在这里",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = Muted,
            )
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(captions, key = { it.id }) { caption ->
            CaptionBubble(
                caption = caption,
                // 沉浸模式或非双语时隐藏中文（AGENTS.md：沉浸模式下隐藏中文）
                showZh = !state.immersive && state.bilingual,
                savedTerms = state.savedTerms,
                onWordClick = onWordClick,
            )
        }
    }
}

/**
 * 单条字幕气泡。AI 在左（白底描边，右下圆角），用户在右（墨底白字，左下圆角）。
 * 文本里的 `[[en|zh|word]]` 内联标记渲染成可点链接：未收录为强调色，已收录为绿色。
 */
@Composable
private fun CaptionBubble(
    caption: Caption,
    showZh: Boolean,
    savedTerms: Set<String>,
    onWordClick: (String, String, WordKind) -> Unit,
) {
    val isMe = caption.role == MessageRole.USER
    val shape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = if (isMe) 20.dp else 7.dp,
        bottomEnd = if (isMe) 7.dp else 20.dp,
    )
    // 保留 [[en|zh|kind]] 原始标记，交给 parseSegments 渲染成可点链接
    val text = caption.en

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .clip(shape)
                .background(if (isMe) Ink else AppSurface)
                .then(
                    if (isMe) Modifier else Modifier.border(1.dp, Line, shape),
                )
                .padding(horizontal = 15.dp, vertical = 12.dp),
        ) {
            val annotated = remember(text, savedTerms, isMe) {
                buildAnnotatedString {
                    parseSegments(text).forEach { seg ->
                        when (seg) {
                            is Segment.Plain -> append(seg.text)
                            is Segment.Word -> {
                                val saved = seg.en.lowercase() in savedTerms
                                withLink(
                                    LinkAnnotation.Clickable(
                                        tag = seg.en,
                                        styles = TextLinkStyles(
                                            SpanStyle(
                                                color = if (isMe) Color.White else if (saved) Ok else Spark,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                        ),
                                        linkInteractionListener = {
                                            if (!saved) onWordClick(seg.en, seg.zh, seg.kind)
                                        },
                                    ),
                                ) { append(seg.en) }
                            }
                        }
                    }
                }
            }
            Text(
                text = annotated,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, lineHeight = 21.75.sp),
                color = if (isMe) Color.White else Ink,
            )
            if (showZh && caption.zh.isNotBlank()) {
                Text(
                    text = caption.zh,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 18.sp),
                    color = if (isMe) Color.White.copy(alpha = 0.6f) else Muted,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
            if (caption.streaming) {
                Text(
                    text = "…",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = if (isMe) Color.White.copy(alpha = 0.6f) else Muted,
                )
            }
        }
    }
}

/** 底部：提示文案 + 挂断按钮（渐变底衬，原型 .call-foot） */
@Composable
private fun CallFoot(onHangup: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "点对话里带颜色的词，收进单词本并记下来源会话",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = Muted,
            textAlign = TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Hot)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onHangup,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_hangup),
                contentDescription = "挂断",
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

/** sheet 内条目分隔线（与 Sheets.kt 的 SheetDivider 缩进一致） */
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

/** 字幕文本切片：普通文本 或 内联生词 */
private sealed interface Segment {
    data class Plain(val text: String) : Segment
    data class Word(val en: String, val zh: String, val kind: WordKind) : Segment
}

/** 解析 `[[en|zh|word|phrase]]` 内联标记 */
private fun parseSegments(text: String): List<Segment> {
    val out = mutableListOf<Segment>()
    var last = 0
    for (match in CallViewModel.INLINE_WORD_REGEX.findAll(text)) {
        if (match.range.first > last) out += Segment.Plain(text.substring(last, match.range.first))
        val kind = if (match.groupValues[3] == "phrase") WordKind.PHRASE else WordKind.WORD
        out += Segment.Word(
            en = match.groupValues[1].trim(),
            zh = match.groupValues[2].trim(),
            kind = kind,
        )
        last = match.range.last + 1
    }
    if (last < text.length) out += Segment.Plain(text.substring(last))
    return out
}
