package com.shenjj.edu.model

/**
 * 英文水平。入库为 name 字符串（见 Converters），避免 ordinal 变化导致数据错乱。
 */
enum class StudyLevel(val label: String) {
    BEGINNER("初级"),
    INTERMEDIATE("中级"),
    ADVANCED("高级"),
    ;

    companion object {
        fun fromName(value: String?): StudyLevel =
            entries.firstOrNull { it.name == value } ?: INTERMEDIATE
    }
}

/**
 * 对话模式：温柔型 / 教学型 / 纠正型 / 闲聊型。
 * 既是老师的默认风格，也可在通话中单独切换。
 */
enum class ChatMode(val label: String) {
    GENTLE("温柔型"),
    TEACHING("教学型"),
    CORRECTIVE("纠正型"),
    CASUAL("闲聊型"),
    ;

    companion object {
        fun fromName(value: String?): ChatMode =
            entries.firstOrNull { it.name == value } ?: TEACHING

        fun fromLabel(label: String?): ChatMode =
            entries.firstOrNull { it.label == label } ?: TEACHING
    }
}

/** 词条类型：单词 / 习语 */
enum class WordKind(val label: String) {
    WORD("单词"),
    PHRASE("习语"),
    ;

    companion object {
        fun fromName(value: String?): WordKind =
            entries.firstOrNull { it.name == value } ?: WORD
    }
}

/** 字幕角色：AI 老师 / 学习者本人 */
enum class MessageRole {
    AI,
    USER,
    ;

    companion object {
        fun fromName(value: String?): MessageRole =
            entries.firstOrNull { it.name == value } ?: AI
    }
}

/** 学习目标可选项（多选），对应原型 onboarding 的 chips */
object LearningGoals {
    val ALL = listOf("日常交流", "考试提分", "职场英语", "出国旅行", "美剧听力")
}
