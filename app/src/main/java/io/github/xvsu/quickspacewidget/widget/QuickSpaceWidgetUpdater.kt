package io.github.xvsu.quickspacewidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import io.github.xvsu.quickspacewidget.R
import io.github.xvsu.quickspacewidget.data.QuickSpacePreferences
import io.github.xvsu.quickspacewidget.engine.QuickSpaceContentEngine
import io.github.xvsu.quickspacewidget.model.QuickSpaceSettings
import io.github.xvsu.quickspacewidget.model.QuoteClickAction
import io.github.xvsu.quickspacewidget.model.QuoteSelectionState
import io.github.xvsu.quickspacewidget.model.RenderMode
import io.github.xvsu.quickspacewidget.model.ResolvedWidgetContent
import io.github.xvsu.quickspacewidget.model.TextAlignment
import io.github.xvsu.quickspacewidget.model.WidgetStyle
import io.github.xvsu.quickspacewidget.util.LocaleHelper
import kotlin.math.roundToInt

object QuickSpaceWidgetUpdater {

    private const val REQUEST_CODE_CALENDAR = 4101
    private const val REQUEST_CODE_NEXT_QUOTE = 4102

    fun getPlacedWidgetIds(context: Context): IntArray {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, QuickSpaceWidgetProvider::class.java)
        return manager.getAppWidgetIds(component)
    }

    fun refreshIfNeeded(context: Context): QuoteSelectionState {
        val appContext = context.applicationContext
        val allIds = getPlacedWidgetIds(appContext)
        val prefs = QuickSpacePreferences(appContext)
        val settings = prefs.loadSettings()
        val localizedCtx = LocaleHelper.localizedContext(appContext, settings.language)
        val previousState = prefs.loadQuoteState()
        val nextState = QuickSpaceContentEngine.nextOrCurrentQuoteState(
            resources = localizedCtx.resources,
            settings = settings,
            previous = previousState,
            forceNew = false,
        )
        if (allIds.isEmpty()) {
            prefs.saveQuoteState(nextState)
            QuickSpaceAlarmScheduler.cancel(appContext)
            return nextState
        }
        if (nextState != previousState) {
            return updateAllWidgets(appContext, forceNewQuote = false)
        }
        return nextState
    }

    fun updateAllWidgets(
        context: Context,
        forceNewQuote: Boolean = false,
        targetIds: IntArray? = null,
    ): QuoteSelectionState {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val allIds = getPlacedWidgetIds(appContext)

        val prefs = QuickSpacePreferences(appContext)
        val settings = prefs.loadSettings()
        val localizedCtx = LocaleHelper.localizedContext(appContext, settings.language)
        val previousState = prefs.loadQuoteState()
        val nextState = QuickSpaceContentEngine.nextOrCurrentQuoteState(
            resources = localizedCtx.resources,
            settings = settings,
            previous = previousState,
            forceNew = forceNewQuote,
        )
        prefs.saveQuoteState(nextState)

        if (allIds.isEmpty()) {
            QuickSpaceAlarmScheduler.cancel(appContext)
            return nextState
        }

        // If quote state rolled during a single-widget update, sync all widgets so none desync
        val idsToUpdate = if (targetIds != null && nextState == previousState) {
            targetIds
        } else {
            allIds
        }

        val content = QuickSpaceContentEngine.resolveContent(
            baseContext = appContext,
            settings = settings,
            state = nextState,
        )
        val colors = ColorResolver.resolve(appContext, settings)

        for (widgetId in idsToUpdate) {
            val options = manager.getAppWidgetOptions(widgetId)
            val remoteViews = buildRemoteViews(
                context = appContext,
                settings = settings,
                content = content,
                colors = colors,
                options = options,
            )
            manager.updateAppWidget(widgetId, remoteViews)
        }

        QuickSpaceAlarmScheduler.scheduleNextRefresh(
            context = appContext,
            lastUpdatedMillis = nextState.lastUpdatedMillis,
        )
        return nextState
    }

    private fun buildRemoteViews(
        context: Context,
        settings: QuickSpaceSettings,
        content: ResolvedWidgetContent,
        colors: ResolvedWidgetColors,
        options: Bundle,
    ): RemoteViews {
        val calendarPendingIntent = CalendarIntentHelper.buildCalendarPendingIntent(
            context = context,
            requestCode = REQUEST_CODE_CALENDAR,
        )
        val subtitlePendingIntent = when (settings.quoteClickAction) {
            QuoteClickAction.NEXT_QUOTE -> nextQuotePendingIntent(context)
            QuoteClickAction.OPEN_CALENDAR -> calendarPendingIntent
        }

        if (settings.renderMode == RenderMode.CANVAS_DOUBLE_SHADOW) {
            val (widthPx, heightPx) = resolveWidgetSizePx(context, options)
            val bitmap = DoubleShadowBitmapRenderer.render(
                context = context,
                settings = settings,
                content = content,
                colors = colors,
                targetWidthPx = widthPx,
                targetHeightPx = heightPx,
            )
            val hasQuote = !content.quoteText.isNullOrBlank()
            val layoutRes = when {
                !hasQuote -> R.layout.widget_quickspace_canvas_no_quote
                settings.style == WidgetStyle.EXTENDED -> R.layout.widget_quickspace_canvas_extended
                else -> R.layout.widget_quickspace_canvas_double
            }
            return RemoteViews(context.packageName, layoutRes).apply {
                setImageViewBitmap(R.id.canvas_widget_image, bitmap)
                setOnClickPendingIntent(R.id.canvas_click_title, calendarPendingIntent)
                if (hasQuote) {
                    setOnClickPendingIntent(R.id.canvas_click_subtitle, subtitlePendingIntent)
                }
            }
        }

        return buildNativeRemoteViews(
            context = context,
            settings = settings,
            content = content,
            colors = colors,
            calendarPendingIntent = calendarPendingIntent,
            subtitlePendingIntent = subtitlePendingIntent,
        )
    }

    private fun buildNativeRemoteViews(
        context: Context,
        settings: QuickSpaceSettings,
        content: ResolvedWidgetContent,
        colors: ResolvedWidgetColors,
        calendarPendingIntent: PendingIntent,
        subtitlePendingIntent: PendingIntent,
    ): RemoteViews {
        val layoutId = selectNativeLayout(settings.style, settings.alignment)
        val views = RemoteViews(context.packageName, layoutId)
        val metrics = context.resources.displayMetrics
        val hPadPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, settings.horizontalPaddingDp.toFloat(), metrics
        ).roundToInt()
        val vPadPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, metrics).roundToInt()
        val spacingPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, settings.lineSpacingDp.toFloat(), metrics
        ).roundToInt()

        views.setViewPadding(R.id.native_root, hPadPx, vPadPx, hPadPx, vPadPx)
        views.setViewPadding(R.id.native_subtitle_row, 0, spacingPx, 0, 0)

        val useShadow = colors.isLightText && settings.shadowStrengthPercent > 0

        if (settings.style == WidgetStyle.EXTENDED) {
            val greetSize = (settings.titleSizeSp - 2).coerceAtLeast(13).toFloat()
            val dateLineSize = (settings.titleSizeSp - 4).coerceAtLeast(12).toFloat()
            bindShadowPair(
                views = views,
                shadowId = R.id.native_greeting_shadow,
                plainId = R.id.native_greeting_plain,
                text = content.greeting,
                color = colors.accentColor,
                sizeSp = greetSize,
                useShadow = useShadow,
            )
            bindShadowPair(
                views = views,
                shadowId = R.id.native_clock_ext_shadow,
                plainId = R.id.native_clock_ext_plain,
                text = content.clockPrefix,
                color = colors.accentColor,
                sizeSp = dateLineSize,
                useShadow = useShadow,
            )
            bindShadowPair(
                views = views,
                shadowId = R.id.native_title_shadow,
                plainId = R.id.native_title_plain,
                text = content.dateText,
                color = colors.textColor,
                sizeSp = dateLineSize,
                useShadow = useShadow,
            )
        } else {
            bindShadowPair(
                views = views,
                shadowId = R.id.native_title_shadow,
                plainId = R.id.native_title_plain,
                text = content.dateText,
                color = colors.textColor,
                sizeSp = settings.titleSizeSp.toFloat(),
                useShadow = useShadow,
            )
        }

        val quote = content.quoteText
        if (!quote.isNullOrBlank()) {
            views.setViewVisibility(R.id.native_subtitle_row, View.VISIBLE)
            bindShadowPair(
                views = views,
                shadowId = R.id.native_subtitle_shadow,
                plainId = R.id.native_subtitle_plain,
                text = quote,
                color = colors.textColor,
                sizeSp = settings.subtitleSizeSp.toFloat(),
                useShadow = useShadow,
            )
            if (content.showEasterIcon) {
                views.setViewVisibility(R.id.native_subtitle_icon, View.VISIBLE)
                views.setInt(R.id.native_subtitle_icon, "setColorFilter", colors.textColor)
            } else {
                views.setViewVisibility(R.id.native_subtitle_icon, View.GONE)
            }
            views.setOnClickPendingIntent(R.id.native_subtitle_row, subtitlePendingIntent)
        } else {
            views.setViewVisibility(R.id.native_subtitle_row, View.GONE)
        }

        views.setOnClickPendingIntent(R.id.native_title_row, calendarPendingIntent)
        return views
    }

    private fun bindShadowPair(
        views: RemoteViews,
        shadowId: Int,
        plainId: Int,
        text: String,
        color: Int,
        sizeSp: Float,
        useShadow: Boolean,
    ) {
        val activeId = if (useShadow) shadowId else plainId
        val hiddenId = if (useShadow) plainId else shadowId
        views.setViewVisibility(activeId, View.VISIBLE)
        views.setViewVisibility(hiddenId, View.GONE)
        views.setTextViewText(activeId, text)
        views.setTextColor(activeId, color)
        views.setTextViewTextSize(activeId, TypedValue.COMPLEX_UNIT_SP, sizeSp)
    }

    private fun selectNativeLayout(style: WidgetStyle, alignment: TextAlignment): Int =
        when (style) {
            WidgetStyle.DOUBLE_LINE -> when (alignment) {
                TextAlignment.START -> R.layout.widget_quickspace_native_double_start
                TextAlignment.CENTER -> R.layout.widget_quickspace_native_double_center
                TextAlignment.END -> R.layout.widget_quickspace_native_double_end
            }
            WidgetStyle.EXTENDED -> when (alignment) {
                TextAlignment.START -> R.layout.widget_quickspace_native_extended_start
                TextAlignment.CENTER -> R.layout.widget_quickspace_native_extended_center
                TextAlignment.END -> R.layout.widget_quickspace_native_extended_end
            }
        }

    private fun resolveWidgetSizePx(context: Context, options: Bundle): Pair<Int, Int> {
        val metrics = context.resources.displayMetrics
        val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
        val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)

        val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        val widthDp = if (isLandscape) {
            when {
                maxW > 0 -> maxW
                minW > 0 -> minW
                else -> 340
            }
        } else {
            when {
                minW > 0 -> minW
                maxW > 0 -> maxW
                else -> 320
            }
        }.coerceIn(140, 600)

        val heightDp = if (isLandscape) {
            when {
                minH > 0 -> minH
                maxH > 0 -> maxH
                else -> 88
            }
        } else {
            when {
                maxH > 0 -> maxH
                minH > 0 -> minH
                else -> 108
            }
        }.coerceIn(56, 300)

        val wPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, widthDp.toFloat(), metrics).roundToInt()
        val hPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, heightDp.toFloat(), metrics).roundToInt()
        return wPx to hPx
    }

    private fun nextQuotePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, QuickSpaceActionReceiver::class.java).apply {
            action = QuickSpaceActionReceiver.ACTION_NEXT_QUOTE
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_NEXT_QUOTE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
