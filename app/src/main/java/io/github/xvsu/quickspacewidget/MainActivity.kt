package io.github.xvsu.quickspacewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.github.xvsu.quickspacewidget.data.QuickSpacePreferences
import io.github.xvsu.quickspacewidget.engine.QuickSpaceContentEngine
import io.github.xvsu.quickspacewidget.model.QuickSpaceSettings
import io.github.xvsu.quickspacewidget.model.QuoteCategory
import io.github.xvsu.quickspacewidget.model.QuoteSelectionState
import io.github.xvsu.quickspacewidget.ui.MainScreen
import io.github.xvsu.quickspacewidget.ui.theme.QuickSpaceTheme
import io.github.xvsu.quickspacewidget.util.LocaleHelper
import io.github.xvsu.quickspacewidget.widget.QuickSpaceActionReceiver
import io.github.xvsu.quickspacewidget.widget.QuickSpaceWidgetProvider
import io.github.xvsu.quickspacewidget.widget.QuickSpaceWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class MainActivity : ComponentActivity() {

    private lateinit var prefs: QuickSpacePreferences
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    private var settingsState by mutableStateOf(QuickSpaceSettings())
    private var quoteState by mutableStateOf(QuoteSelectionState())
    private var placedCountState by mutableIntStateOf(0)
    private var syncJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        prefs = QuickSpacePreferences(this)
        handleWidgetConfigureResult(intent)
        reloadState(forceNewQuote = false)

        setContent {
            val localizedCtx = remember(settingsState.language) {
                LocaleHelper.localizedContext(this@MainActivity, settingsState.language)
            }
            val resolvedContent = remember(localizedCtx, settingsState, quoteState) {
                QuickSpaceContentEngine.resolveContent(
                    baseContext = this@MainActivity,
                    settings = settingsState,
                    state = quoteState,
                )
            }
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()

            QuickSpaceTheme {
                MainScreen(
                    localizedContext = localizedCtx,
                    settings = settingsState,
                    content = resolvedContent,
                    placedWidgetCount = placedCountState,
                    snackbarHostState = snackbarHostState,
                    onUpdatePreviewSettings = { preview ->
                        settingsState = preview
                    },
                    onCommitSettings = { updated ->
                        settingsState = updated
                        prefs.saveSettings(updated)
                        val newLocalized = LocaleHelper.localizedContext(this@MainActivity, updated.language)
                        val nextQuote = QuickSpaceContentEngine.nextOrCurrentQuoteState(
                            resources = newLocalized.resources,
                            settings = updated,
                            previous = quoteState,
                            forceNew = false,
                        )
                        quoteState = nextQuote
                        prefs.saveQuoteState(nextQuote)
                        launchWidgetSync(scope, forceNewQuote = false)
                    },
                    onNextQuote = {
                        val nextQuote = QuickSpaceContentEngine.nextOrCurrentQuoteState(
                            resources = localizedCtx.resources,
                            settings = settingsState,
                            previous = quoteState,
                            forceNew = true,
                        )
                        quoteState = nextQuote
                        prefs.saveQuoteState(nextQuote)
                        launchWidgetSync(scope, forceNewQuote = false)
                    },
                    onSelectSpecificQuote = { category, index ->
                        val now = System.currentTimeMillis()
                        val cal = Calendar.getInstance().apply { timeInMillis = now }
                        val updatedSettings = if (!settingsState.showQuotes) {
                            settingsState.copy(showQuotes = true).also {
                                settingsState = it
                                prefs.saveSettings(it)
                            }
                        } else {
                            settingsState
                        }
                        val picked = QuoteSelectionState(
                            category = category,
                            index = index,
                            showEasterIcon = category == QuoteCategory.RANDOM && updatedSettings.enableEasterEggs,
                            isHidden = false,
                            disabledByUser = false,
                            lastUpdatedMillis = now,
                            lastDateKey = QuickSpaceContentEngine.dateKey(cal),
                            lastHourBucket = QuickSpaceContentEngine.hourBucketId(
                                cal.get(Calendar.HOUR_OF_DAY),
                                updatedSettings.crDroidProbabilityHide,
                            ),
                        )
                        quoteState = picked
                        prefs.saveQuoteState(picked)
                        launchWidgetSync(scope, forceNewQuote = false)
                        scope.launch {
                            snackbarHostState.showSnackbar(localizedCtx.getString(R.string.quote_applied_toast))
                        }
                    },
                    onAddWidget = {
                        requestPinWidget(localizedCtx) { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    },
                    onSyncWidgets = {
                        scope.launch {
                            val synced = withContext(Dispatchers.Default) {
                                QuickSpaceWidgetUpdater.updateAllWidgets(
                                    this@MainActivity,
                                    forceNewQuote = false,
                                )
                            }
                            quoteState = synced
                            refreshPlacedCount()
                            snackbarHostState.showSnackbar(localizedCtx.getString(R.string.widgets_synced))
                        }
                    },
                    onResetDefaults = {
                        val defaults = QuickSpaceSettings()
                        settingsState = defaults
                        prefs.saveSettings(defaults)
                        val defLocalized = LocaleHelper.localizedContext(this@MainActivity, defaults.language)
                        val nextQuote = QuickSpaceContentEngine.nextOrCurrentQuoteState(
                            resources = defLocalized.resources,
                            settings = defaults,
                            previous = null,
                            forceNew = true,
                        )
                        quoteState = nextQuote
                        prefs.saveQuoteState(nextQuote)
                        launchWidgetSync(scope, forceNewQuote = false)
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPlacedCount()
        val oldQuote = quoteState
        reloadState(forceNewQuote = false)
        if (quoteState != oldQuote) {
            QuickSpaceWidgetUpdater.refreshIfNeeded(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleWidgetConfigureResult(intent)
    }

    private fun launchWidgetSync(
        scope: kotlinx.coroutines.CoroutineScope,
        forceNewQuote: Boolean,
    ) {
        syncJob?.cancel()
        syncJob = scope.launch {
            withContext(Dispatchers.Default) {
                QuickSpaceWidgetUpdater.updateAllWidgets(this@MainActivity, forceNewQuote = forceNewQuote)
            }
            refreshPlacedCount()
        }
    }

    private fun handleWidgetConfigureResult(intent: Intent?) {
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(
                RESULT_OK,
                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            )
        }
    }

    private fun reloadState(forceNewQuote: Boolean) {
        val loaded = prefs.loadSettings()
        settingsState = loaded
        val localized = LocaleHelper.localizedContext(this, loaded.language)
        val previous = prefs.loadQuoteState()
        val next = QuickSpaceContentEngine.nextOrCurrentQuoteState(
            resources = localized.resources,
            settings = loaded,
            previous = previous,
            forceNew = forceNewQuote,
        )
        quoteState = next
        prefs.saveQuoteState(next)
        refreshPlacedCount()
    }

    private fun refreshPlacedCount() {
        placedCountState = QuickSpaceWidgetUpdater.getPlacedWidgetIds(this).size
    }

    private fun requestPinWidget(
        localizedCtx: Context,
        onMessage: (String) -> Unit,
    ) {
        val manager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, QuickSpaceWidgetProvider::class.java)
        if (manager.isRequestPinAppWidgetSupported) {
            val callbackIntent = Intent(this, QuickSpaceActionReceiver::class.java).apply {
                action = QuickSpaceActionReceiver.ACTION_WIDGET_PINNED
            }
            val successCallback = PendingIntent.getBroadcast(
                this,
                4201,
                callbackIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val accepted = manager.requestPinAppWidget(provider, null, successCallback)
            onMessage(
                localizedCtx.getString(
                    if (accepted) R.string.pin_requested else R.string.pin_not_supported
                )
            )
        } else {
            onMessage(localizedCtx.getString(R.string.pin_not_supported))
        }
    }
}
