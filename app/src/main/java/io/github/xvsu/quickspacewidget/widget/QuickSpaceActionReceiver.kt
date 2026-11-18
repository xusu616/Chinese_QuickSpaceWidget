package io.github.xvsu.quickspacewidget.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import io.github.xvsu.quickspacewidget.R
import io.github.xvsu.quickspacewidget.data.QuickSpacePreferences
import io.github.xvsu.quickspacewidget.util.LocaleHelper

class QuickSpaceActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_NEXT_QUOTE,
            ACTION_REFRESH_ALARM -> {
                QuickSpaceWidgetUpdater.updateAllWidgets(context, forceNewQuote = true)
            }
            ACTION_WIDGET_PINNED -> {
                QuickSpaceWidgetUpdater.updateAllWidgets(context, forceNewQuote = false)
            }
            ACTION_NO_CALENDAR -> {
                val settings = QuickSpacePreferences(context).loadSettings()
                val localized = LocaleHelper.localizedContext(context, settings.language)
                Toast.makeText(
                    localized,
                    localized.getString(R.string.no_calendar_found),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_ALARM = "io.github.xvsu.quickspacewidget.ACTION_REFRESH_ALARM"
        const val ACTION_NEXT_QUOTE = "io.github.xvsu.quickspacewidget.ACTION_NEXT_QUOTE"
        const val ACTION_WIDGET_PINNED = "io.github.xvsu.quickspacewidget.ACTION_WIDGET_PINNED"
        const val ACTION_NO_CALENDAR = "io.github.xvsu.quickspacewidget.ACTION_NO_CALENDAR"
    }
}
