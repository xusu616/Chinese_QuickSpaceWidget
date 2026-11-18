package io.github.xvsu.quickspacewidget.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle

class QuickSpaceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        QuickSpaceWidgetUpdater.updateAllWidgets(
            context = context,
            forceNewQuote = false,
            targetIds = appWidgetIds,
        )
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        QuickSpaceWidgetUpdater.updateAllWidgets(
            context = context,
            forceNewQuote = false,
            targetIds = intArrayOf(appWidgetId),
        )
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        QuickSpaceAlarmScheduler.scheduleNextRefresh(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        QuickSpaceAlarmScheduler.cancel(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                QuickSpaceWidgetUpdater.updateAllWidgets(context, forceNewQuote = false)
            }
        }
    }
}
