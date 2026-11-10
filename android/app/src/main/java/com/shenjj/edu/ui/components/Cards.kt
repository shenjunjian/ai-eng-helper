package com.shenjj.edu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shenjj.edu.data.local.SessionWithWordCount
import com.shenjj.edu.data.local.WordWithSession
import com.shenjj.edu.model.Teacher
import com.shenjj.edu.model.TeacherCatalog
import com.shenjj.edu.model.WordKind
import com.shenjj.edu.ui.Fmt
import com.shenjj.edu.ui.theme.AppSurface
import com.shenjj.edu.ui.theme.Ink
import com.shenjj.edu.ui.theme.Ink2
import com.shenjj.edu.ui.theme.Line
import com.shenjj.edu.ui.theme.Muted
import com.shenjj.edu.ui.theme.Pressed
import com.shenjj.edu.ui.theme.Radii
import com.shenjj.edu.ui.theme.Spark

/* ============================================================
   卡片与列表 cell —— 对应原型 .card / .list / .cell /
   .word-cell / .sec-label
   ============================================================ */

/** 白色圆角卡片（.card：22px 圆角 + 1px 描边 + 18dp 内边距） */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    radius: Dp = Radii.Card,
    padding: Dp = 18.dp,
    border: Color? = Line,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Box(
        modifier = modifier
            .background(if (pressed && onClick != null) Pressed else AppSurface, shape)
            .then(if (border == null) Modifier else Modifier.border(1.dp, border, shape))
            .then(
                if (onClick == null) Modifier
                else Modifier.clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                )
            )
            .padding(padding),
    ) {
        content()
    }
}

/** 分组列表容器（.list / .group：白底大圆角，内部 cell 自带分隔线） */
@Composable
fun ListContainer(
    modifier: Modifier = Modifier,
    radius: Dp = Radii.List,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppSurface, RoundedCornerShape(radius))
            .border(1.dp, Line, RoundedCornerShape(radius)),
    ) {
        content()
    }
}

/** 分节标题（.sec-label：11px/700/字距 .8px/大写/弱化色） */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = Muted,
        modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp),
    )
}

/** 列表行分隔线：原型 .cell+.cell::before 从左侧 62dp 起画 1px 线 */
@Composable
fun CellDivider(modifier: Modifier = Modifier, indent: Dp = 62.dp) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = indent)
            .height(1.dp)
            .background(Line),
    )
}

/**
 * 会话列表行（.cell）：老师头像 46dp + 标题 + 摘要 + 右上时间。
 * 摘要按原型拼成「preview · N分钟 · M 词」。
 */
@Composable
fun SessionCell(
    session: SessionWithWordCount,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    now: Long = System.currentTimeMillis(),
) {
    val s = session.session
    val teacher: Teacher = TeacherCatalog.byId(s.teacherId)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val sub = buildString {
        if (s.preview.isNotEmpty()) {
            append(s.preview)
            append(" · ")
        }
        append(Fmt.minutes(s.durationSeconds))
        append("分钟")
        if (session.wordCount > 0) {
            append(" · ")
            append(session.wordCount)
            append(" 词")
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (pressed) Pressed else Color.Transparent)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        TeacherAvatar(teacher = teacher, size = 46.dp, glyphSize = 22.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = s.title,
                style = MaterialTheme.typography.titleSmall,
                color = Ink,
                maxLines = 1,
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = Muted,
                maxLines = 1,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Text(
            text = Fmt.ago(s.lastMessageAt, now),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp),
            color = Muted,
            modifier = Modifier.align(Alignment.Top).padding(top = 3.dp),
        )
    }
}

/**
 * 单词本行（.word-cell）：英文 + 类型徽标 + 中文释义 + 来源会话。
 * [onSourceClick] 非空时来源可点（跳回该会话）。
 */
@Composable
fun WordCell(
    item: WordWithSession,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSourceClick: (() -> Unit)? = null,
    now: Long = System.currentTimeMillis(),
) {
    val w = item.word
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val kind = WordKind.fromName(w.kind)
    val sourceTitle = item.session?.title

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(if (pressed) Pressed else Color.Transparent)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 15.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = w.en,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    fontSize = 16.sp,
                ),
                color = Ink,
            )
            WordTag(kind = kind, done = w.mastered)
        }
        Text(
            text = w.zh,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = Ink2,
            modifier = Modifier.padding(top = 2.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .padding(top = 4.dp)
                .then(
                    if (onSourceClick == null || sourceTitle == null) Modifier
                    else Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSourceClick,
                    )
                ),
        ) {
            Text(
                text = buildString {
                    append("来自 ")
                    append(sourceTitle ?: "已删除的会话")
                    append(" · ")
                    append(Fmt.ago(w.createdAt, now))
                    append("收录")
                },
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp),
                color = if (onSourceClick != null && sourceTitle != null) Spark else Muted,
            )
        }
    }
}
