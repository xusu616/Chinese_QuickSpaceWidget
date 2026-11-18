package io.github.xvsu.quickspacewidget.model

enum class AppLanguage(val code: String) {
    SYSTEM("system"),
    ZH_CN("zh-CN"),
    EN("en");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: SYSTEM
    }
}

enum class WidgetStyle(val key: String) {
    DOUBLE_LINE("double_line"),
    EXTENDED("extended");

    companion object {
        fun fromKey(key: String?): WidgetStyle =
            entries.firstOrNull { it.key == key } ?: DOUBLE_LINE
    }
}

enum class RenderMode(val key: String) {
    NATIVE_TEXT("native"),
    CANVAS_DOUBLE_SHADOW("canvas");

    companion object {
        fun fromKey(key: String?): RenderMode =
            entries.firstOrNull { it.key == key } ?: CANVAS_DOUBLE_SHADOW
    }
}

enum class TextAlignment(val key: String) {
    START("start"),
    CENTER("center"),
    END("end");

    companion object {
        fun fromKey(key: String?): TextAlignment =
            entries.firstOrNull { it.key == key } ?: START
    }
}

enum class TextColorMode(val key: String) {
    WHITE("white"),
    DARK("dark"),
    AUTO("auto");

    companion object {
        fun fromKey(key: String?): TextColorMode =
            entries.firstOrNull { it.key == key } ?: WHITE
    }
}

enum class AccentColorMode(val key: String) {
    DYNAMIC("dynamic"),
    CRDROID_SKY("sky"),
    MATCH_TEXT("match");

    companion object {
        fun fromKey(key: String?): AccentColorMode =
            entries.firstOrNull { it.key == key } ?: DYNAMIC
    }
}

enum class QuoteClickAction(val key: String) {
    NEXT_QUOTE("next_quote"),
    OPEN_CALENDAR("open_calendar");

    companion object {
        fun fromKey(key: String?): QuoteClickAction =
            entries.firstOrNull { it.key == key } ?: NEXT_QUOTE
    }
}

enum class QuoteCategory(val key: String) {
    MORNING("morning"),
    NOON("noon"),
    EARLY_EVENING("early_evening"),
    EVENING("evening"),
    MIDNIGHT("midnight"),
    RANDOM("random"),
    NONE("none");

    companion object {
        fun fromKey(key: String?): QuoteCategory =
            entries.firstOrNull { it.key == key } ?: NONE
    }
}

data class QuickSpaceSettings(
    val language: AppLanguage = AppLanguage.SYSTEM,
    val style: WidgetStyle = WidgetStyle.DOUBLE_LINE,
    val renderMode: RenderMode = RenderMode.CANVAS_DOUBLE_SHADOW,
    val alignment: TextAlignment = TextAlignment.START,
    val textColorMode: TextColorMode = TextColorMode.WHITE,
    val accentColorMode: AccentColorMode = AccentColorMode.DYNAMIC,
    val titleSizeSp: Int = 20,
    val subtitleSizeSp: Int = 16,
    val shadowStrengthPercent: Int = 100,
    val horizontalPaddingDp: Int = 16,
    val verticalOffsetDp: Int = 0,
    val lineSpacingDp: Int = 6,
    val showQuotes: Boolean = true,
    val crDroidProbabilityHide: Boolean = false,
    val enableEasterEggs: Boolean = true,
    val quoteClickAction: QuoteClickAction = QuoteClickAction.NEXT_QUOTE,
)

data class QuoteSelectionState(
    val category: QuoteCategory = QuoteCategory.MORNING,
    val index: Int = 0,
    val showEasterIcon: Boolean = false,
    val isHidden: Boolean = false,
    val disabledByUser: Boolean = false,
    val lastUpdatedMillis: Long = 0L,
    val lastDateKey: String = "",
    val lastHourBucket: Int = -1,
)

data class ResolvedWidgetContent(
    val greeting: String,
    val clockPrefix: String,
    val dateText: String,
    val quoteText: String?,
    val showEasterIcon: Boolean,
    val hiddenByProbability: Boolean,
)
