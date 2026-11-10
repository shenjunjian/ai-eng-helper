package com.shenjj.edu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.shenjj.edu.data.WordFilter
import com.shenjj.edu.data.local.WordCounts
import com.shenjj.edu.model.TeacherCatalog
import com.shenjj.edu.model.WordKind
import com.shenjj.edu.ui.Fmt
import com.shenjj.edu.ui.components.Chip
import com.shenjj.edu.ui.components.DarkButton
import com.shenjj.edu.ui.components.DoneChip
import com.shenjj.edu.ui.components.GhostButton
import com.shenjj.edu.ui.components.InfoChip
import com.shenjj.edu.ui.components.LocalToast
import com.shenjj.edu.ui.components.SheetContent
import com.shenjj.edu.ui.components.SheetOption
import com.shenjj.edu.ui.components.TalkFlowSheet
import com.shenjj.edu.ui.components.WordCell
import com.shenjj.edu.ui.components.tabBarBottomInset
import com.shenjj.edu.ui.nav.AuthState
import com.shenjj.edu.ui.nav.appViewModel
import com.shenjj.edu.ui.theme.AppBg
import com.shenjj.edu.ui.theme.Ink
import com.shenjj.edu.ui.theme.Ink2
import com.shenjj.edu.ui.theme.Line
import com.shenjj.edu.ui.theme.Muted
import com.shenjj.edu.ui.theme.Ok
import com.shenjj.edu.ui.theme.Spark
import kotlinx.coroutines.launch

/** sheet 未选中词条时传给 observeWord 的哨兵 id（自增主键从 1 起，-1 永不命中） */
private const val WORD_ID_NONE = -1L

/** 筛选页签：与原型一致的四项（待复习 / 单词 / 习语 / 已掌握） */
private val FILTERS = listOf(
    WordFilter.PENDING to "待复习",
    WordFilter.WORD to "单词",
    WordFilter.PHRASE to "习语",
    WordFilter.MASTERED to "已掌握",
)

/**
 * 单词本（原型 words）。
 *
 * 按 AGENTS.md：词条**只有一个来源——会话**，本页不提供快速录入或"稍后学习"；
 * 列表按收录顺序（最新在前，SQL ORDER BY created_at DESC）。
 * 标记已掌握是软删除：从待复习列表移出，仍可切到「已掌握」筛选查看。
 */
@Composable
fun WordsScreen(
    onOpenSession: (String) -> Unit,
    onReviewWord: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = appViewModel()
    val scope = rememberCoroutineScope()
    val toast = LocalToast.current

    val auth by vm.auth.collectAsStateWithLifecycle()
    val defaultTeacherId = (auth as? AuthState.SignedIn)?.user?.defaultTeacherId
        ?: TeacherCatalog.ALL.first().id

    var filter by remember { mutableStateOf(WordFilter.PENDING) }
    // remember 住 Flow 实例：否则每次重组新建，collectAsStateWithLifecycle 会反复重启收集
    val countsFlow = remember(vm.repository) { vm.repository.observeWordCounts() }
    val wordsFlow = remember(vm.repository, filter) { vm.repository.observeWords(filter) }
    val counts by countsFlow.collectAsStateWithLifecycle(WordCounts())
    val words by wordsFlow.collectAsStateWithLifecycle(emptyList())

    // 词条详情 sheet 的当前目标
    var sheetWordId by remember { mutableStateOf<Long?>(null) }
    val sheetWordFlow = remember(vm.repository, sheetWordId) {
        vm.repository.observeWord(sheetWordId ?: WORD_ID_NONE)
    }
    val sheetWord by sheetWordFlow.collectAsStateWithLifecycle(null)

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .tabBarBottomInset(),
        ) {
            Text(
                text = "单词本",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
            )

            StatBar(counts = counts)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 0.dp)
                    .padding(top = 10.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FILTERS.forEach { (f, label) ->
                    Chip(
                        text = label,
                        selected = f == filter,
                        onClick = { filter = f },
                    )
                }
            }

            SectionLabel(text = "按收录顺序")

            if (words.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(ListShape)
                        .background(Color.White)
                        .border(1.dp, Line, ListShape)
                        .padding(vertical = 34.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (filter == WordFilter.MASTERED) {
                            "还没有标记为已掌握的词"
                        } else {
                            "这里还空着\n在对话中点一下单词就会记进来"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 20.sp,
                        ),
                        color = Muted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 18.dp)
                        .clip(ListShape)
                        .background(Color.White)
                        .border(1.dp, Line, ListShape),
                ) {
                    words.forEachIndexed { index, item ->
                        if (index > 0) Box(Modifier.height(1.dp).fillMaxWidth().background(Line))
                        WordCell(
                            item = item,
                            onClick = { sheetWordId = item.word.id },
                            onSourceClick = item.word.sessionId?.let { sid ->
                                { onOpenSession(sid) }
                            },
                        )
                    }
                }
            }

            Text(
                text = "点词条可与老师单独复习它，\n确认掌握后自动移出待复习列表",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 20.sp),
                color = Muted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp, vertical = 6.dp),
            )
        }

        // 词条详情 sheet
        TalkFlowSheet(
            visible = sheetWordId != null && sheetWord != null,
            onDismiss = { sheetWordId = null },
        ) {
            sheetWord?.let { item ->
                val teacher = TeacherCatalog.byId(item.session?.teacherId ?: defaultTeacherId)
                SheetContent(title = item.word.en, subtitle = item.word.zh) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        InfoChip(text = WordKind.fromName(item.word.kind).label)
                        InfoChip(text = "${Fmt.ago(item.word.createdAt)}收录")
                        if (item.word.mastered) DoneChip()
                    }

                    SheetOption(
                        title = "来源会话",
                        subtitle = item.session?.let { s ->
                            "${s.title} · ${Fmt.ago(s.lastMessageAt)}"
                        } ?: "会话已删除",
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(AppBg, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_chats),
                                    contentDescription = null,
                                    tint = Ink2,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                        },
                        onClick = {
                            val sid = item.word.sessionId
                            if (sid == null || item.session == null) return@SheetOption
                            sheetWordId = null
                            onOpenSession(sid)
                        },
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        GhostButton(
                            text = if (item.word.mastered) "放回待复习" else "标记已掌握",
                            onClick = {
                                val id = item.word.id
                                val en = item.word.en
                                val toMastered = !item.word.mastered
                                scope.launch {
                                    vm.repository.setMastered(id, toMastered)
                                    sheetWordId = null
                                    toast.show(
                                        if (toMastered) "「$en」已掌握，移出待复习"
                                        else "「$en」已放回待复习",
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                        DarkButton(
                            text = "和 ${teacher.name} 复习",
                            onClick = {
                                // 复习计数不在这里累加：按原型语义，
                                // 要等复习对话中确认掌握后才算一次复习（p5c 结算处理）
                                val id = item.word.id
                                sheetWordId = null
                                onReviewWord(id)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** 统计条：收录总数 | 待复习（强调色）| 已掌握（绿色），中间 1dp 竖分隔 */
@Composable
private fun StatBar(counts: WordCounts, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .fillMaxWidth()
            .clip(StatShape)
            .background(Color.White)
            .border(1.dp, Line, StatShape)
            .padding(vertical = 16.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatMini(value = counts.total.toString(), label = "收录总数", valueColor = Ink, modifier = Modifier.weight(1f))
        StatDivider()
        StatMini(value = counts.pending.toString(), label = "待复习", valueColor = Spark, modifier = Modifier.weight(1f))
        StatDivider()
        StatMini(value = counts.mastered.toString(), label = "已掌握", valueColor = Ok, modifier = Modifier.weight(1f))
    }
}

/** 统计条中间的 1×30dp 竖分隔线（原型 width:1px;height:30px;background:var(--line)） */
@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .size(width = 1.dp, height = 30.dp)
            .background(Line),
    )
}

@Composable
private fun StatMini(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
            ),
            color = valueColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 0.sp),
            color = Muted,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

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
        modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 8.dp),
    )
}

/** 列表卡片形状（原型 .list border-radius:24px + border:1px solid var(--line)） */
private val ListShape = RoundedCornerShape(24.dp)

/** 统计条卡片形状（原型 border-radius:20px） */
private val StatShape = RoundedCornerShape(20.dp)
