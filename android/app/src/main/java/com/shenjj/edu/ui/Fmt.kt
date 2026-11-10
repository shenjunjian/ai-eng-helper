package com.shenjj.edu.ui

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 时间与格式化工具 —— 复刻原型 ui/app.js 的 ago() 等展示逻辑。
 */
object Fmt {

    private const val MINUTE = 60_000L
    private const val DAY = 24 * 60 * 60_000L

    /**
     * 相对时间：刚刚 / HH:MM / 昨天 / N天前 / M月D日（对应原型 ago）。
     * [now] 便于测试注入。
     */
    fun ago(ts: Long, now: Long = System.currentTimeMillis()): String {
        val diff = now - ts
        if (diff < 0) return justNow()
        if (diff < MINUTE) return "刚刚"

        val d = Calendar.getInstance().apply { timeInMillis = ts }
        val n = Calendar.getInstance().apply { timeInMillis = now }
        if (sameDay(d, n)) return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))

        val y = Calendar.getInstance().apply { timeInMillis = now - DAY }
        if (sameDay(d, y)) return "昨天"

        if (diff < 7 * DAY) return "${diff / DAY}天前"
        return "${d.get(Calendar.MONTH) + 1}月${d.get(Calendar.DAY_OF_MONTH)}日"
    }

    private fun justNow() = "刚刚"

    private fun sameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    /** 秒 → mm:ss（通话计时 .timer） */
    fun duration(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%02d:%02d".format(m, s)
    }

    /** 分钟 → 中文分钟数（会话摘要里的「N分钟」） */
    fun minutes(seconds: Int): Int = (seconds + 59) / 60

    /** 分 → ¥ 金额字符串，两位小数 */
    fun yuan(cents: Int): String = "¥ %.2f".format(cents / 100.0)

    /** 手机号打码：138****8888 */
    fun maskPhone(phone: String): String =
        if (phone.length == 11) phone.take(3) + "****" + phone.takeLast(4) else phone

    /** 日期 → yyyy-MM-dd（study_logs 按天聚合用） */
    fun dayKey(ts: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(ts))
}
