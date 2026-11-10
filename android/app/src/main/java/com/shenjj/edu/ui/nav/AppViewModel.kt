package com.shenjj.edu.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shenjj.edu.TalkFlowApp
import com.shenjj.edu.data.AppRepository
import com.shenjj.edu.data.User
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * 登录态。[Unknown] 是「数据库还没读出结果」，与 [SignedOut]「确认未登录」区分开，
 * 启动页必须等到不再是 Unknown 才能决定去向，否则会把已登录用户误送到登录页。
 */
sealed interface AuthState {
    data object Unknown : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val user: User) : AuthState
}

/**
 * 根级 ViewModel：跨页面共享的登录态、单词本角标（待复习数）、会话数。
 *
 * 手动 DI：由 [TalkFlowApp] 提供 [AppRepository]（见 [factory]）。
 * 实例作用域为 Activity，通过 [LocalAppViewModel] 下发给各页面，
 * 避免在 NavHost destination 里被解析成 per-destination 的实例。
 */
class AppViewModel(val repository: AppRepository) : ViewModel() {

    /** 用 Eagerly 让查询在 VM 创建时就开始，启动页无需额外等待 */
    val auth: StateFlow<AuthState> = repository.observeCurrentUser()
        .map<User?, AuthState> { user ->
            if (user == null) AuthState.SignedOut else AuthState.SignedIn(user)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AuthState.Unknown)

    /** 单词本角标 */
    val pendingWordCount: StateFlow<Int> = repository.observePendingWordCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** 会话总数（首页空态判断） */
    val sessionCount: StateFlow<Int> = repository.observeSessionCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** 退出登录：清掉本地登录态（users.is_current 置空），auth 流自动转为 SignedOut */
    fun logout() {
        viewModelScope.launch { repository.logout() }
    }

    companion object {
        fun factory(app: TalkFlowApp): ViewModelProvider.Factory = viewModelFactory {
            initializer { AppViewModel(app.repository) }
        }
    }
}

/** 共享的根 ViewModel；由 TalkFlowNavHost 在 Activity 作用域创建并下发 */
val LocalAppViewModel: ProvidableCompositionLocal<AppViewModel> =
    staticCompositionLocalOf { error("AppViewModel 未提供：请经由 TalkFlowNavHost 进入页面") }

/** 页面内取得共享的 [AppViewModel]（含 [AppViewModel.repository]） */
@Composable
fun appViewModel(): AppViewModel = LocalAppViewModel.current
