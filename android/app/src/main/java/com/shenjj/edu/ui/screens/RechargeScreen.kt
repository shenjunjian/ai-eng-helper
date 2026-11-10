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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shenjj.edu.data.RechargeResult
import com.shenjj.edu.data.RechargeTier
import com.shenjj.edu.ui.Fmt
import com.shenjj.edu.ui.components.DarkButton
import com.shenjj.edu.ui.components.LocalToast
import com.shenjj.edu.ui.components.NavBar
import com.shenjj.edu.ui.nav.AuthState
import com.shenjj.edu.ui.nav.appViewModel
import com.shenjj.edu.ui.theme.Ink
import com.shenjj.edu.ui.theme.Ink2
import com.shenjj.edu.ui.theme.Line
import com.shenjj.edu.ui.theme.Muted
import com.shenjj.edu.ui.theme.Spark
import kotlinx.coroutines.launch

/**
 * 充值（原型 recharge）。
 *
 * 走本地假充值：[com.shenjj.edu.data.gateway.LocalPaymentGateway] 直接返回成功，
 * 成功后写 recharge_orders 流水并给 users 余额加钱。
 * 上线后只需替换 PaymentGateway 实现，本页与流水表结构都不动。
 */
@Composable
fun RechargeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = appViewModel()
    val scope = rememberCoroutineScope()
    val toast = LocalToast.current
    val auth by vm.auth.collectAsStateWithLifecycle()
    val balanceCents = (auth as? AuthState.SignedIn)?.user?.balanceCents ?: 0

    val tiers = vm.repository.rechargeTiers
    // 原型默认选中「¥68 热销」那一档
    var selected by remember { mutableStateOf(tiers.indexOfFirst { it.tag == "热销" }.coerceAtLeast(0)) }
    var busy by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        NavBar(title = "充值", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 26.dp),
        ) {
            Text(
                text = "按对话时长计费，1 元 ≈ 2 分钟实时语音。",
                style = MaterialTheme.typography.bodyMedium,
                color = Ink2,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // 档位：两列网格
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                tiers.chunked(2).forEach { rowTiers ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowTiers.forEach { tier ->
                            val index = tiers.indexOf(tier)
                            TierCard(
                                tier = tier,
                                selected = index == selected,
                                onClick = { selected = index },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        // 奇数个档位时补位，保持两列等宽
                        if (rowTiers.size == 1) Box(Modifier.weight(1f))
                    }
                }
            }

            // 当前余额
            Column(
                modifier = Modifier
                    .padding(top = 18.dp, bottom = 18.dp)
                    .fillMaxWidth()
                    .clip(GroupShape)
                    .background(Color.White)
                    .border(1.dp, Line, GroupShape),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "当前余额",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Ink,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = Fmt.yuan(balanceCents),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Ink,
                    )
                }
            }

            DarkButton(
                text = "微信支付",
                enabled = !busy,
                onClick = {
                    busy = true
                    scope.launch {
                        val tier = tiers[selected]
                        when (val r = vm.repository.recharge(tier)) {
                            is RechargeResult.Success -> {
                                toast.show("充值成功 · 余额 ${Fmt.yuan(r.newBalanceCents)}")
                                onBack()
                            }

                            is RechargeResult.Failure -> {
                                busy = false
                                toast.show(r.reason)
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private val GroupShape = RoundedCornerShape(20.dp)
private val TierShape = RoundedCornerShape(22.dp)

/** 档位卡：选中态用强调色 1.5dp 描边，「热销」角标浮在卡右上角外 */
@Composable
private fun TierCard(
    tier: RechargeTier,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(modifier = modifier.padding(top = 9.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(TierShape)
                .background(Color.White)
                .border(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) Spark else Line,
                    shape = TierShape,
                )
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                )
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Text(
                text = "¥${tier.amountYuan}",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                ),
                color = Ink,
            )
            Text(
                text = "约 ${tier.minutes} 分钟",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = Muted,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        if (tier.tag != null) {
            Text(
                text = tier.tag,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                ),
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 12.dp)
                    .offset(y = (-9).dp)
                    .background(Spark, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}
