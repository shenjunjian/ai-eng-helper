package com.shenjj.edu.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shenjj.edu.R
import com.shenjj.edu.ui.theme.Radii
import com.shenjj.edu.ui.theme.ToastBg
import kotlinx.coroutines.delay

/* ============================================================
   Toast —— 对应原型 toast(msg, icon)：底部 150dp 处的深色
   胶囊，带图标，2.2s 自动消失
   ============================================================ */

/** 原型 toast 的图标色（成功用 #7DE2C0） */
private val ToastIconTint = Color(0xFF7DE2C0)

/** 一次 Toast 的内容。[iconRes] 为 null 时不显示图标 */
data class ToastData(
    val message: String,
    val iconRes: Int? = R.drawable.ic_check,
)

/**
 * Toast 状态持有者。页面根布局里调 [TalkFlowToastHost] 渲染，
 * 通过 [show] 触发，复刻原型 2.2s 后自动隐藏。
 */
class ToastState {
    var current by mutableStateOf<ToastData?>(null)
        private set

    /** 递增序号：连续弹同一条文案也能重新触发显示 */
    var seq by mutableStateOf(0)
        private set

    fun show(message: String, iconRes: Int? = R.drawable.ic_check) {
        current = ToastData(message, iconRes)
        seq++
    }
}

@Composable
fun rememberToastState(): ToastState = remember { ToastState() }

/**
 * 跨页面共享的 Toast 通道。根 Composable 里用 [CompositionLocalProvider] 提供，
 * 任意页面通过 `LocalToast.current.show("…")` 弹出，替代原型的 toast()。
 */
val LocalToast: ProvidableCompositionLocal<ToastState> =
    staticCompositionLocalOf { error("ToastState 未提供：请在根布局用 CompositionLocalProvider 注入") }

/**
 * 覆盖在页面之上的 Toast 宿主。放在根 Box 的最后一个子项，
 * 使其浮于内容之上（原型 z-index:240）。
 */
@Composable
fun TalkFlowToastHost(
    state: ToastState,
    modifier: Modifier = Modifier,
    durationMillis: Long = 2200L,
) {
    var visible by remember { mutableStateOf(false) }
    val data = state.current
    val seq = state.seq

    LaunchedEffect(seq) {
        if (data == null) return@LaunchedEffect
        visible = true
        delay(durationMillis)
        visible = false
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = visible && data != null,
            enter = fadeIn() + slideInVertically { it / 6 },
            exit = fadeOut() + slideOutVertically { it / 6 },
        ) {
            data?.let {
                Row(
                    modifier = Modifier
                        .padding(bottom = 150.dp)
                        .widthIn(max = 300.dp)
                        .background(ToastBg, RoundedCornerShape(Radii.Toast))
                        .padding(horizontal = 18.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    it.iconRes?.let { res ->
                        Icon(
                            painter = painterResource(res),
                            contentDescription = null,
                            tint = ToastIconTint,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        text = it.message,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = Color.White,
                    )
                }
            }
        }
    }
}
