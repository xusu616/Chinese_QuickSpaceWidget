package io.github.xvsu.quickspacewidget.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

internal object ColorOsWidgetEntry {

    private const val COLOR_OS_PACKAGE = "com.android.launcher"
    private const val COLOR_OS_ACTIVITY = "com.android.launcher.Launcher"
    private const val WIDGET_ENTRY_FEATURE = "com.android.launcher.support.card_widget_entry_integration"
    private val launcher = ComponentName(COLOR_OS_PACKAGE, COLOR_OS_ACTIVITY)

    fun isColorOsLauncher(context: Context): Boolean {
        val pm = context.packageManager
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        @Suppress("DEPRECATION")
        val home = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.resolveActivity(homeIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        }?.activityInfo ?: return false
        return ComponentName(home.packageName, home.name) == launcher
    }

    @Suppress("DEPRECATION")
    fun intent(context: Context): Intent? {
        val pm = context.packageManager
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val flags = PackageManager.MATCH_DEFAULT_ONLY or PackageManager.GET_META_DATA
        val home = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.resolveActivity(homeIntent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
        } else {
            pm.resolveActivity(homeIntent, flags)
        }?.activityInfo ?: return null

        if (ComponentName(home.packageName, home.name) != launcher) return null
        val hasWidgetEntry = home.applicationInfo.metaData?.getBoolean(WIDGET_ENTRY_FEATURE) == true
        if (!hasWidgetEntry) return null

        return Intent(Intent.ACTION_VIEW)
            .setComponent(launcher)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra("state", "ToggleBar_widget")
    }
}
