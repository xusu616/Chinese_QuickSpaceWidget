package io.github.xvsu.quickspacewidget.widget

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import io.github.xvsu.quickspacewidget.model.AccentColorMode
import io.github.xvsu.quickspacewidget.model.QuickSpaceSettings
import io.github.xvsu.quickspacewidget.model.TextColorMode

data class ResolvedWidgetColors(
    val textColor: Int,
    val accentColor: Int,
    val isLightText: Boolean,
)

object ColorResolver {

    private const val COLOR_WHITE = 0xFFFFFFFF.toInt()
    private const val COLOR_DARK = 0xFF1C1B1F.toInt()
    private const val SKY_LIGHT = 0xFFA8D8FF.toInt()
    private const val SKY_DARK = 0xFF006494.toInt()
    private const val FALLBACK_DYNAMIC_LIGHT = 0xFFB8E2FF.toInt()
    private const val FALLBACK_DYNAMIC_DARK = 0xFF006399.toInt()

    fun resolve(context: Context, settings: QuickSpaceSettings): ResolvedWidgetColors {
        val isSystemNight = (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        val textColor = when (settings.textColorMode) {
            TextColorMode.WHITE -> COLOR_WHITE
            TextColorMode.DARK -> COLOR_DARK
            TextColorMode.AUTO -> if (isSystemNight) COLOR_WHITE else COLOR_DARK
        }

        val isLightText = ColorUtils.calculateLuminance(textColor) > 0.35

        val accentColor = when (settings.accentColorMode) {
            AccentColorMode.MATCH_TEXT -> textColor
            AccentColorMode.CRDROID_SKY -> if (isLightText) SKY_LIGHT else SKY_DARK
            AccentColorMode.DYNAMIC -> resolveDynamicAccent(context, isLightText)
        }

        return ResolvedWidgetColors(
            textColor = textColor,
            accentColor = accentColor,
            isLightText = isLightText,
        )
    }

    private fun resolveDynamicAccent(context: Context, isLightText: Boolean): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val resId = if (isLightText) {
                android.R.color.system_accent1_200
            } else {
                android.R.color.system_accent1_600
            }
            return runCatching { ContextCompat.getColor(context, resId) }
                .getOrElse { if (isLightText) FALLBACK_DYNAMIC_LIGHT else FALLBACK_DYNAMIC_DARK }
        }
        return if (isLightText) FALLBACK_DYNAMIC_LIGHT else FALLBACK_DYNAMIC_DARK
    }
}
