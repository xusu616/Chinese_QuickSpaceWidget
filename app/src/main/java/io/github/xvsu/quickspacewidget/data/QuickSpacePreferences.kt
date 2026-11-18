package io.github.xvsu.quickspacewidget.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.xvsu.quickspacewidget.model.AccentColorMode
import io.github.xvsu.quickspacewidget.model.AppLanguage
import io.github.xvsu.quickspacewidget.model.PreviewWallpaper
import io.github.xvsu.quickspacewidget.model.QuickSpaceSettings
import io.github.xvsu.quickspacewidget.model.QuoteCategory
import io.github.xvsu.quickspacewidget.model.QuoteClickAction
import io.github.xvsu.quickspacewidget.model.QuoteSelectionState
import io.github.xvsu.quickspacewidget.model.RenderMode
import io.github.xvsu.quickspacewidget.model.TextAlignment
import io.github.xvsu.quickspacewidget.model.TextColorMode
import io.github.xvsu.quickspacewidget.model.WidgetStyle

class QuickSpacePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadSettings(): QuickSpaceSettings = QuickSpaceSettings(
        language = AppLanguage.fromCode(prefs.getString(KEY_LANGUAGE, null)),
        style = WidgetStyle.fromKey(prefs.getString(KEY_STYLE, null)),
        renderMode = RenderMode.fromKey(prefs.getString(KEY_RENDER_MODE, null)),
        alignment = TextAlignment.fromKey(prefs.getString(KEY_ALIGNMENT, null)),
        textColorMode = TextColorMode.fromKey(prefs.getString(KEY_TEXT_COLOR, null)),
        accentColorMode = AccentColorMode.fromKey(prefs.getString(KEY_ACCENT_COLOR, null)),
        titleSizeSp = prefs.getInt(KEY_TITLE_SIZE, 20).coerceIn(16, 28),
        subtitleSizeSp = prefs.getInt(KEY_SUBTITLE_SIZE, 16).coerceIn(12, 22),
        shadowStrengthPercent = prefs.getInt(KEY_SHADOW_STRENGTH, 100).coerceIn(0, 200),
        horizontalPaddingDp = prefs.getInt(KEY_HORIZONTAL_PADDING, 16).coerceIn(0, 32),
        lineSpacingDp = prefs.getInt(KEY_LINE_SPACING, 6).coerceIn(2, 14),
        showQuotes = prefs.getBoolean(KEY_SHOW_QUOTES, true),
        crDroidProbabilityHide = prefs.getBoolean(KEY_CRDROID_PROB_HIDE, false),
        enableEasterEggs = prefs.getBoolean(KEY_ENABLE_EASTER_EGGS, true),
        quoteClickAction = QuoteClickAction.fromKey(prefs.getString(KEY_QUOTE_CLICK_ACTION, null)),
        previewWallpaper = PreviewWallpaper.fromKey(prefs.getString(KEY_PREVIEW_WALLPAPER, null)),
    )

    fun saveSettings(settings: QuickSpaceSettings) {
        prefs.edit {
            putString(KEY_LANGUAGE, settings.language.code)
            putString(KEY_STYLE, settings.style.key)
            putString(KEY_RENDER_MODE, settings.renderMode.key)
            putString(KEY_ALIGNMENT, settings.alignment.key)
            putString(KEY_TEXT_COLOR, settings.textColorMode.key)
            putString(KEY_ACCENT_COLOR, settings.accentColorMode.key)
            putInt(KEY_TITLE_SIZE, settings.titleSizeSp)
            putInt(KEY_SUBTITLE_SIZE, settings.subtitleSizeSp)
            putInt(KEY_SHADOW_STRENGTH, settings.shadowStrengthPercent)
            putInt(KEY_HORIZONTAL_PADDING, settings.horizontalPaddingDp)
            putInt(KEY_LINE_SPACING, settings.lineSpacingDp)
            putBoolean(KEY_SHOW_QUOTES, settings.showQuotes)
            putBoolean(KEY_CRDROID_PROB_HIDE, settings.crDroidProbabilityHide)
            putBoolean(KEY_ENABLE_EASTER_EGGS, settings.enableEasterEggs)
            putString(KEY_QUOTE_CLICK_ACTION, settings.quoteClickAction.key)
            putString(KEY_PREVIEW_WALLPAPER, settings.previewWallpaper.key)
        }
    }

    fun loadQuoteState(): QuoteSelectionState? {
        if (!prefs.contains(KEY_STATE_LAST_UPDATED)) return null
        return QuoteSelectionState(
            category = QuoteCategory.fromKey(prefs.getString(KEY_STATE_CATEGORY, null)),
            index = prefs.getInt(KEY_STATE_INDEX, 0),
            showEasterIcon = prefs.getBoolean(KEY_STATE_EASTER_ICON, false),
            isHidden = prefs.getBoolean(KEY_STATE_IS_HIDDEN, false),
            disabledByUser = prefs.getBoolean(KEY_STATE_DISABLED_BY_USER, false),
            lastUpdatedMillis = prefs.getLong(KEY_STATE_LAST_UPDATED, 0L),
            lastDateKey = prefs.getString(KEY_STATE_LAST_DATE, "") ?: "",
            lastHourBucket = prefs.getInt(KEY_STATE_HOUR_BUCKET, -1),
        )
    }

    fun saveQuoteState(state: QuoteSelectionState) {
        prefs.edit {
            putString(KEY_STATE_CATEGORY, state.category.key)
            putInt(KEY_STATE_INDEX, state.index)
            putBoolean(KEY_STATE_EASTER_ICON, state.showEasterIcon)
            putBoolean(KEY_STATE_IS_HIDDEN, state.isHidden)
            putBoolean(KEY_STATE_DISABLED_BY_USER, state.disabledByUser)
            putLong(KEY_STATE_LAST_UPDATED, state.lastUpdatedMillis)
            putString(KEY_STATE_LAST_DATE, state.lastDateKey)
            putInt(KEY_STATE_HOUR_BUCKET, state.lastHourBucket)
        }
    }

    companion object {
        private const val PREFS_NAME = "quickspace_prefs"

        private const val KEY_LANGUAGE = "language"
        private const val KEY_STYLE = "style"
        private const val KEY_RENDER_MODE = "render_mode"
        private const val KEY_ALIGNMENT = "alignment"
        private const val KEY_TEXT_COLOR = "text_color"
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val KEY_TITLE_SIZE = "title_size_sp"
        private const val KEY_SUBTITLE_SIZE = "subtitle_size_sp"
        private const val KEY_SHADOW_STRENGTH = "shadow_strength_percent"
        private const val KEY_HORIZONTAL_PADDING = "horizontal_padding_dp"
        private const val KEY_LINE_SPACING = "line_spacing_dp"
        private const val KEY_SHOW_QUOTES = "show_quotes"
        private const val KEY_CRDROID_PROB_HIDE = "crdroid_prob_hide"
        private const val KEY_ENABLE_EASTER_EGGS = "enable_easter_eggs"
        private const val KEY_QUOTE_CLICK_ACTION = "quote_click_action"
        private const val KEY_PREVIEW_WALLPAPER = "preview_wallpaper"

        private const val KEY_STATE_CATEGORY = "state_category"
        private const val KEY_STATE_INDEX = "state_index"
        private const val KEY_STATE_EASTER_ICON = "state_easter_icon"
        private const val KEY_STATE_IS_HIDDEN = "state_is_hidden"
        private const val KEY_STATE_DISABLED_BY_USER = "state_disabled_by_user"
        private const val KEY_STATE_LAST_UPDATED = "state_last_updated"
        private const val KEY_STATE_LAST_DATE = "state_last_date"
        private const val KEY_STATE_HOUR_BUCKET = "state_hour_bucket"
    }
}
