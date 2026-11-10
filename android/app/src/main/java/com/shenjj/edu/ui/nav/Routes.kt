package com.shenjj.edu.ui.nav

/**
 * 路由表 —— 对应原型 SCREENS 的 10 个屏幕。
 * 只有 [CHATS] / [WORDS] / [ME] 三个一级页显示底部 Tab（原型的 scr.tab），
 * 实时对话页 [CALL] 必须全屏无 Tab。
 */
object Routes {

    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val ONBOARDING = "onboarding"
    const val CHATS = "chats"
    const val WORDS = "words"
    const val ME = "me"
    const val ANALYTICS = "analytics"
    const val PROFILE = "profile"
    const val RECHARGE = "recharge"

    /** 显示底部 Tab 的路由（对应原型 SCREENS[x].tab 有值的三个屏） */
    val TAB_ROUTES: Set<String> = setOf(CHATS, WORDS, ME)

    /* ---------- 通话页参数（全部可选） ---------- */

    const val ARG_SESSION_ID = "sessionId"
    const val ARG_TEACHER_ID = "teacherId"
    const val ARG_WORD_ID = "wordId"

    /** 通话页 route pattern：三个参数都可选，缺省时走「自由对话」 */
    const val CALL =
        "call?$ARG_SESSION_ID={$ARG_SESSION_ID}&$ARG_TEACHER_ID={$ARG_TEACHER_ID}&$ARG_WORD_ID={$ARG_WORD_ID}"

    /**
     * 构造通话页导航地址。
     * @param sessionId 非空表示「复习历史会话」（挂断只更新该会话，不新建）
     * @param teacherId 指定本次对话的老师
     * @param wordId 非空表示「和 AI 老师复习这个词」
     */
    fun call(
        sessionId: String? = null,
        teacherId: String? = null,
        wordId: Long? = null,
    ): String = buildString {
        append("call?")
        append(ARG_SESSION_ID).append("=").append(sessionId?.let { encode(it) } ?: "")
        append("&").append(ARG_TEACHER_ID).append("=").append(teacherId?.let { encode(it) } ?: "")
        append("&").append(ARG_WORD_ID).append("=").append(wordId ?: "")
    }

    /** 会话 id 为 UUID，正常不含特殊字符；仍做一次保守转义 */
    private fun encode(value: String): String =
        value.replace("%", "%25").replace("&", "%26").replace("=", "%3D")
}
