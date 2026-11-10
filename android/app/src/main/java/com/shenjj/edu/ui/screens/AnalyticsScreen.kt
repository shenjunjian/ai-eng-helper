package com.shenjj.edu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shenjj.edu.data.Analytics
import com.shenjj.edu.data.local.StudyTotals
import com.shenjj.edu.data.local.WordCounts
import com.shenjj.edu.model.TeacherCatalog
import com.shenjj.edu.ui.components.NavBar
import com.shenjj.edu.ui.nav.appViewModel
import com.shenjj.edu.ui.theme.Ink
import com.shenjj.edu.ui.theme.Line
import com.shenjj.edu.ui.theme.Muted
import com.shenjj.edu.ui.theme.Spark
import kotlin.math.roundToInt

private val CardShape = RoundedCornerShape(22.dp)

/** 数据未就绪时的占位（全部为零值，图表自动退化为空态） */
private val EmptyAnalytics = Analytics(
    daily = emptyList(),
    totals = StudyTotals(),
    counts = WordCounts(),
    streak = 0,
    mastered = 0,
    newWordsInRange = 0,
)

/**
 * 学习分析（原型 analytics），从「我的 → 学习分析」进入（AGENTS.md：分析不作一级页签）。
 *
 * 数据全部来自本地 Room 聚合 [Analytics]：累计掌握词数、区间内学习/复习时长、
 * 连续打卡天数、逐日开口时长柱状图、词汇来源会话排行、学习 vs 复习占比环。
 * 原型里的假数据（146、62%、6 天）一律换成真实统计。
 */
@Composable
fun AnalyticsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = appViewModel()
    // 近 7 天：与原型「本周开口时长」一致
    val analyticsFlow = remember(vm.repository) { vm.repository.observeAnalytics(7) }
    val analytics by analyticsFlow.collectAsStateWithLifecycle(EmptyAnalytics)
    val sessionsFlow = remember(vm.repository) { vm.repository.observeSessions() }
    val sessions by sessionsFlow.collectAsStateWithLifecycle(emptyList())

    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        NavBar(title = "学习分析", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 26.dp),
        ) {
            // 卡片一：三项统计 + 本周开口时长柱状图
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .clip(CardShape)
                    .background(Color.White)
                    .border(1.dp, Line, CardShape)
                    .padding(20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatMini(
                        value = analytics.mastered.toString(),
                        label = "累计掌握",
                        modifier = Modifier.weight(1f),
                    )
                    VerticalRule()
                    StatMini(
                        value = "${fmtHours(analytics.totalMinutes)}h",
                        label = "近 7 天时长",
                        modifier = Modifier.weight(1f),
                    )
                    VerticalRule()
                    StatMini(
                        value = analytics.streak.toString(),
                        label = "连续天数",
                        valueColor = Spark,
                        modifier = Modifier.weight(1f),
                    )
                }

                Text(
                    text = "本周开口时长",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Ink,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                BarChart(analytics = analytics)
            }

            // 卡片二：词汇来源（收录词最多的会话）
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .clip(CardShape)
                    .background(Color.White)
                    .border(1.dp, Line, CardShape)
                    .padding(18.dp),
            ) {
                Text(
                    text = "词汇来源",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Ink,
                    modifier = Modifier.padding(bottom = 14.dp),
                )
                val sources = sessions
                    .filter { it.wordCount > 0 }
                    .sortedByDescending { it.wordCount }
                    .take(4)
                if (sources.isEmpty()) {
                    Text(
                        text = "还没有从会话中收录词汇",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Muted,
                    )
                }
                val maxWords = sources.maxOfOrNull { it.wordCount } ?: 1
                sources.forEachIndexed { index, item ->
                    val teacher = TeacherCatalog.byId(item.session.teacherId)
                    SourceRow(
                        title = item.session.title,
                        words = item.wordCount,
                        fraction = item.wordCount.toFloat() / maxWords.toFloat(),
                        gradient = Brush.linearGradient(
                            listOf(teacher.startColor, teacher.endColor),
                        ),
                        showDividerSpace = index < sources.lastIndex,
                    )
                }
            }

            // 卡片三：学习 vs 复习占比环
            val study = analytics.studyMinutes
            val review = analytics.reviewMinutes
            val total = study + review
            val studyPercent = if (total <= 0) 0 else (study * 100f / total).roundToInt()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(Color.White)
                    .border(1.dp, Line, CardShape)
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "学习 vs 复习",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Ink,
                    )
                    Text(
                        text = if (total <= 0) {
                            "开始第一次对话后这里会有数据"
                        } else {
                            "新学 $studyPercent%，复习巩固 ${100 - studyPercent}%"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Muted,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Donut(percent = studyPercent, modifier = Modifier.padding(start = 13.dp))
            }
        }
    }
}

/** 累计掌握 / 时长 / 连续天数（原型 .stat-mini：k 22sp bold + v 11sp muted） */
@Composable
private fun StatMini(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Ink,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            ),
            color = valueColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
            ),
            color = Muted,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

/** 统计项之间的竖分隔线（原型 width:1px;height:32px;background:var(--line)） */
@Composable
private fun VerticalRule() {
    Box(
        Modifier
            .size(width = 1.dp, height = 32.dp)
            .background(Line),
    )
}

/** 本周开口时长柱状图：高度按当日时长占周内最大值的比例，今日用强调色 */
@Composable
private fun BarChart(analytics: Analytics, modifier: Modifier = Modifier) {
    val days = analytics.daily
    val maxSeconds = (days.maxOfOrNull { it.totalSeconds } ?: 0).coerceAtLeast(1)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(104.dp).padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            days.forEachIndexed { index, point ->
                val fraction = (point.totalSeconds.toFloat() / maxSeconds.toFloat()).coerceIn(0.02f, 1f)
                val isToday = index == days.lastIndex
                val color = when {
                    isToday -> Spark
                    // 原型 v>70 用深色，这里等价于超过周内最大值 70%
                    fraction > 0.7f -> Ink
                    else -> Line
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(fraction)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            days.forEach { point ->
                Text(
                    text = point.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 0.sp),
                    color = Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** 词汇来源单行：标题 + 词数 + 老师渐变进度条 */
@Composable
private fun SourceRow(
    title: String,
    words: Int,
    fraction: Float,
    gradient: Brush,
    showDividerSpace: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(bottom = if (showDividerSpace) 13.dp else 0.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                color = Ink,
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false),
            )
            Text(
                text = "$words 词",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = Muted,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Line),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(gradient),
            )
        }
    }
}

/** 学习 vs 复习占比环（原型 conic-gradient：强调色占 percent，其余用 Line） */
@Composable
private fun Donut(percent: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(62.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 9.dp.toPx()
            val inset = stroke / 2f
            val arcSize = size.minDimension - stroke
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            // 背景环
            drawArc(
                color = Line,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                style = Stroke(width = stroke, cap = StrokeCap.Butt),
            )
            if (percent > 0) {
                drawArc(
                    color = Spark,
                    startAngle = -90f,
                    sweepAngle = 360f * percent / 100f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                )
            }
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                color = Ink,
            )
        }
    }
}

private fun fmtHours(minutes: Int): String {
    val hours = minutes / 60f
    return ((hours * 10).roundToInt() / 10f).toString()
}
