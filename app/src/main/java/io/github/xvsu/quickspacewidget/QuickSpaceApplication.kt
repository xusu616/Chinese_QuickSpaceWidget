package io.github.xvsu.quickspacewidget

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import io.github.xvsu.quickspacewidget.widget.QuickSpaceWidgetUpdater

class QuickSpaceApplication : Application() {

    private val userPresentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_USER_PRESENT) {
                QuickSpaceWidgetUpdater.refreshIfNeeded(context)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        ContextCompat.registerReceiver(
            this,
            userPresentReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }
}
