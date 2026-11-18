package io.github.xvsu.quickspacewidget.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.CalendarContract

object CalendarIntentHelper {

    fun buildCalendarPendingIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = resolveCalendarIntent(context)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return if (intent != null) {
            PendingIntent.getActivity(context, requestCode, intent, flags)
        } else {
            val fallback = Intent(context, QuickSpaceActionReceiver::class.java).apply {
                action = QuickSpaceActionReceiver.ACTION_NO_CALENDAR
            }
            PendingIntent.getBroadcast(context, requestCode, fallback, flags)
        }
    }

    private fun resolveCalendarIntent(context: Context): Intent? {
        val pm = context.packageManager

        val appCalendarIntent = Intent.makeMainSelectorActivity(
            Intent.ACTION_MAIN,
            Intent.CATEGORY_APP_CALENDAR
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        if (canResolve(pm, appCalendarIntent)) {
            return appCalendarIntent
        }

        val viewIntent = Intent(Intent.ACTION_VIEW)
            .setData(CalendarContract.CONTENT_URI)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        if (canResolve(pm, viewIntent)) {
            return viewIntent
        }

        return null
    }

    @Suppress("DEPRECATION")
    private fun canResolve(pm: PackageManager, intent: Intent): Boolean {
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return resolved != null
    }
}
