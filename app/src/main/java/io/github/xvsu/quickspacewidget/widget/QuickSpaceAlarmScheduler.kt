package io.github.xvsu.quickspacewidget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.github.xvsu.quickspacewidget.engine.QuickSpaceContentEngine
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min

object QuickSpaceAlarmScheduler {

    private const val REQUEST_CODE_ALARM = 4001
    private const val MIN_DELAY_MS = 60_000L

    fun scheduleNextRefresh(
        context: Context,
        lastUpdatedMillis: Long = System.currentTimeMillis(),
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val baseMillis = if (lastUpdatedMillis > 0L) lastUpdatedMillis else nowMillis
        val nextHalfHour = max(
            nowMillis + MIN_DELAY_MS,
            baseMillis + QuickSpaceContentEngine.AUTO_REFRESH_INTERVAL_MS
        )

        val nextMidnight = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 5)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val triggerAt = min(nextHalfHour, nextMidnight)
        val pi = alarmPendingIntent(context)
        alarmManager.cancel(pi)
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, triggerAt, pi)
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(alarmPendingIntent(context))
    }

    private fun alarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, QuickSpaceActionReceiver::class.java).apply {
            action = QuickSpaceActionReceiver.ACTION_REFRESH_ALARM
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
