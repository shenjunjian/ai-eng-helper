package com.shenjj.edu.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shenjj.edu.data.LoginResult
import com.shenjj.edu.ui.components.Chip
import com.shenjj.edu.ui.components.DarkButton
import com.shenjj.edu.ui.components.GhostButton
import com.shenjj.edu.ui.components.LocalToast
import com.shenjj.edu.ui.components.TextField
import com.shenjj.edu.ui.nav.AuthState
import com.shenjj.edu.ui.nav.appViewModel
import com.shenjj.edu.ui.theme.Ink2
import com.shenjj.edu.ui.theme.Muted
import kotlinx.coroutines.launch

/**
 * 登录页（原型 login）：中国手机号 + 验证码，无密码。
 *
 * 当前走本地假登录（LocalPhoneAuthGateway：手机号 11 位且以 1 开头、
 * 验证码非空即通过，并自动注册/登录）。上线后只需替换 Gateway 实现，本页不改。
 */
@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onNeedProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = appViewModel()
    val scope = rememberCoroutineScope()
    val toast = LocalToast.current
    val auth by vm.auth.collectAsStateWithLifecycle()

    var phone by remember { mutableStateOf("13888886666") }
    var code by remember { mutableStateOf("2481") }
    var busy by remember { mutableStateOf(false) }
    var codeSent by remember { mutableStateOf(false) }

    // 登录成功后由 auth 流驱动跳转，避免与 StateFlow 竞争
    LaunchedEffect(auth) {
        val state = auth as? AuthState.SignedIn ?: return@LaunchedEffect
        busy = false
        if (state.user.onboarded) onLoggedIn() else onNeedProfile()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 26.dp),
    ) {
        Text(
            text = "登录 TalkFlow",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 44.dp, bottom = 8.dp),
        )
        Text(
            text = "中国手机号即可注册，无需密码",
            style = MaterialTheme.typography.bodyMedium,
            color = Ink2,
            modifier = Modifier.padding(bottom = 30.dp),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(bottom = 14.dp),
        ) {
            Chip(text = "+86", selected = true)
            TextField(
                value = phone,
                onValueChange = { phone = it.filter(Char::isDigit).take(11) },
                placeholder = "手机号",
                numeric = true,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = code,
                onValueChange = { code = it.filter(Char::isDigit).take(6) },
                placeholder = "验证码",
                numeric = true,
                modifier = Modifier.weight(1f),
            )
            GhostButton(
                text = if (codeSent) "已发送" else "获取验证码",
                onClick = {
                    scope.launch {
                        vm.repository.sendCode(phone).fold(
                            onSuccess = {
                                codeSent = true
                                toast.show("验证码已发送")
                            },
                            onFailure = { toast.show(it.message ?: "手机号格式不正确") },
                        )
                    }
                },
            )
        }

        DarkButton(
            text = "进入",
            enabled = !busy && phone.length == 11 && code.isNotBlank(),
            onClick = {
                busy = true
                scope.launch {
                    when (val r = vm.repository.login(phone, code)) {
                        is LoginResult.Failure -> {
                            busy = false
                            toast.show(r.reason)
                        }

                        is LoginResult.Success -> Unit
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
        )

        Text(
            text = "登录即代表同意《用户协议》与《隐私政策》",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
            color = Muted,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 22.dp),
        )
    }
}
