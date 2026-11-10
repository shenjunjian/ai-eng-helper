package com.shenjj.edu.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shenjj.edu.TalkFlowApp
import com.shenjj.edu.ui.components.BottomTabBar
import com.shenjj.edu.ui.components.LocalToast
import com.shenjj.edu.ui.components.TalkFlowToastHost
import com.shenjj.edu.ui.components.rememberToastState
import com.shenjj.edu.ui.screens.AnalyticsScreen
import com.shenjj.edu.ui.screens.CallScreen
import com.shenjj.edu.ui.screens.ChatsScreen
import com.shenjj.edu.ui.screens.LoginScreen
import com.shenjj.edu.ui.screens.MeScreen
import com.shenjj.edu.ui.screens.ProfileScreen
import com.shenjj.edu.ui.screens.RechargeScreen
import com.shenjj.edu.ui.screens.OnboardingScreen
import com.shenjj.edu.ui.screens.SplashScreen
import com.shenjj.edu.ui.screens.WordsScreen
import com.shenjj.edu.voice.CallArgs
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay

/** 启动页最短停留时长，避免闪屏一闪而过 */
private const val SPLASH_MIN_DELAY_MS = 700L

/**
 * 应用导航骨架，对应原型的 10 个屏幕与底部三页签。
 *
 * 布局要点（对齐 AGENTS.md 与原型）：
 * - 页签栏**固定渲染在应用窗口底部**，用 Box 叠在页面内容之上，
 *   因此列表可以像原型那样从栏下滑过；只有 [Routes.TAB_ROUTES] 三个一级页显示它，
 *   进入实时对话页 [Routes.CALL] 时完全不渲染（全屏无 Tab）；
 * - 页面内容作为毛玻璃采样源（hazeSource），页签栏用 hazeEffect 取其背景做模糊；
 * - 各页自行处理状态栏/底部留白（见 [com.shenjj.edu.ui.screens.TabBarSpace]）；
 * - Toast 经 [LocalToast] 下发，替代原型的全局 toast()。
 */
@Composable
fun TalkFlowNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as TalkFlowApp }
    // 在 Activity 作用域创建，保证各页面共享同一实例
    val appViewModel: AppViewModel = viewModel(factory = AppViewModel.factory(app))

    val hazeState = remember { HazeState() }
    val toastState = rememberToastState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showTabs = currentRoute in Routes.TAB_ROUTES
    val pendingWordCount by appViewModel.pendingWordCount.collectAsStateWithLifecycle()

    CompositionLocalProvider(
        LocalAppViewModel provides appViewModel,
        LocalToast provides toastState,
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Routes.SPLASH,
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState),
            ) {
                splashGraph(navController)
                composable(Routes.LOGIN) {
                    LoginScreen(
                        // 已登录且完善过资料 → 会话首页；否则 → 学习档案
                        onLoggedIn = {
                            navController.navigate(Routes.CHATS) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        },
                        onNeedProfile = {
                            navController.navigate(Routes.ONBOARDING) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        },
                    )
                }
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(
                        onDone = {
                            navController.navigate(Routes.CHATS) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        },
                        onBack = { navController.back() },
                    )
                }
                composable(Routes.CHATS) {
                    ChatsScreen(
                        onOpenSession = { id -> navController.navigate(Routes.call(sessionId = id)) },
                        onOpenWords = { navController.switchTab(Routes.WORDS) },
                    )
                }
                composable(Routes.WORDS) {
                    WordsScreen(
                        onOpenSession = { id -> navController.navigate(Routes.call(sessionId = id)) },
                        // 和 AI 老师复习这个词：进入它来源会话的通话，并带上 wordId
                        onReviewWord = { wordId ->
                            navController.navigate(Routes.call(wordId = wordId))
                        },
                    )
                }
                composable(Routes.ME) {
                    MeScreen(
                        onProfile = { navController.navigate(Routes.PROFILE) },
                        onAnalytics = { navController.navigate(Routes.ANALYTICS) },
                        onWords = { navController.switchTab(Routes.WORDS) },
                        onRecharge = { navController.navigate(Routes.RECHARGE) },
                        onLogout = {
                            // 清掉本地登录态，回登录页并清栈（返回键不会回到「我的」）
                            appViewModel.logout()
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        },
                    )
                }
                composable(Routes.ANALYTICS) {
                    AnalyticsScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.PROFILE) {
                    ProfileScreen(
                        onBack = { navController.popBackStack() },
                        onRecharge = { navController.navigate(Routes.RECHARGE) },
                    )
                }
                composable(Routes.RECHARGE) {
                    RechargeScreen(onBack = { navController.popBackStack() })
                }
                callGraph(navController)
            }

            // 底部页签：固定在窗口底部，实时对话页不渲染
            if (showTabs) {
                BottomTabBar(
                    currentRoute = currentRoute,
                    hazeState = hazeState,
                    badgeCount = pendingWordCount,
                    onTabSelected = { route -> navController.switchTab(route) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                )
            }

            // Toast 浮层（原型 z-index:240）
            TalkFlowToastHost(state = toastState)
        }
    }
}

/**
 * 启动页：等登录态确定后按「未登录 → 登录页 / 已登录未完善资料 → onboarding /
 * 已就绪 → 会话首页」分流，并把自身移出返回栈（返回键不会退回闪屏）。
 */
private fun NavGraphBuilder.splashGraph(navController: NavHostController) {
    composable(Routes.SPLASH) {
        val auth by appViewModel().auth.collectAsStateWithLifecycle()

        LaunchedEffect(auth) {
            val state = auth
            if (state is AuthState.Unknown) return@LaunchedEffect
            delay(SPLASH_MIN_DELAY_MS)
            val target = when (state) {
                is AuthState.SignedIn ->
                    if (state.user.onboarded) Routes.CHATS else Routes.ONBOARDING

                AuthState.SignedOut, AuthState.Unknown -> Routes.LOGIN
            }
            navController.navigate(target) {
                popUpTo(Routes.SPLASH) { inclusive = true }
                launchSingleTop = true
            }
        }

        SplashScreen()
    }
}

/**
 * 实时对话页。三个参数都可选：全空 = 新建自由对话；
 * [Routes.ARG_SESSION_ID] = 复习历史会话（挂断只更新它，不新建）；
 * [Routes.ARG_WORD_ID] = 和 AI 老师复习某个单词。
 *
 * 本页不渲染底部页签（不在 [Routes.TAB_ROUTES] 中），实现见 p5b。
 */
private fun NavGraphBuilder.callGraph(navController: NavHostController) {
    composable(
        route = Routes.CALL,
        arguments = listOf(
            navArgument(Routes.ARG_SESSION_ID) {
                type = NavType.StringType
                defaultValue = ""
            },
            navArgument(Routes.ARG_TEACHER_ID) {
                type = NavType.StringType
                defaultValue = ""
            },
            navArgument(Routes.ARG_WORD_ID) {
                type = NavType.LongType
                defaultValue = -1L
            },
        ),
    ) { entry ->
        val args = CallArgs(
            sessionId = entry.arguments?.getString(Routes.ARG_SESSION_ID).orEmpty(),
            teacherId = entry.arguments?.getString(Routes.ARG_TEACHER_ID).orEmpty(),
            wordId = entry.arguments?.getLong(Routes.ARG_WORD_ID) ?: -1L,
        )
        CallScreen(
            args = args,
            // 挂断后直接回会话首页（AGENTS.md：不展示任何结算页）
            onExit = {
                navController.navigate(Routes.CHATS) {
                    popUpTo(navController.graph.id) { inclusive = false }
                    launchSingleTop = true
                }
            },
        )
    }
}

/* ============================================================
   导航动作
   ============================================================ */

/**
 * 页签切换：三个一级页之间切换不堆栈，回到该页签的单一实例
 * 并保留其滚动状态（对应原型 go() 的直接切屏）。
 */
fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** 统一返回：栈空时兜底回会话首页，避免退出到空白 */
fun NavHostController.back() {
    if (!popBackStack()) {
        navigate(Routes.CHATS) {
            popUpTo(graph.id) { inclusive = true }
        }
    }
}
