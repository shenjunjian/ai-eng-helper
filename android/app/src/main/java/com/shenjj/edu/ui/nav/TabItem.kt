package com.shenjj.edu.ui.nav

import com.shenjj.edu.R

/**
 * 底部页签项 —— 对应原型 TABS（会话/单词本/我的）。
 */
enum class TabItem(
    val route: String,
    val label: String,
    val iconRes: Int,
    /** 是否在图标上显示角标（单词本 = 待复习数） */
    val showBadge: Boolean = false,
) {
    CHATS(Routes.CHATS, "会话", R.drawable.ic_chats),
    WORDS(Routes.WORDS, "单词本", R.drawable.ic_book, showBadge = true),
    ME(Routes.ME, "我的", R.drawable.ic_user),
    ;

    companion object {
        val ALL: List<TabItem> = entries
    }
}
