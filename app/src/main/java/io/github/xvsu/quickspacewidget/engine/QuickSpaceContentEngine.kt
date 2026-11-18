package io.github.xvsu.quickspacewidget.engine

import android.content.Context
import android.content.res.Resources
import android.icu.text.DateFormat
import android.icu.text.DisplayContext
import io.github.xvsu.quickspacewidget.R
import io.github.xvsu.quickspacewidget.model.QuickSpaceSettings
import io.github.xvsu.quickspacewidget.model.QuoteCategory
import io.github.xvsu.quickspacewidget.model.QuoteSelectionState
import io.github.xvsu.quickspacewidget.model.ResolvedWidgetContent
import io.github.xvsu.quickspacewidget.model.WidgetStyle
import io.github.xvsu.quickspacewidget.util.LocaleHelper
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ThreadLocalRandom

object QuickSpaceContentEngine {

    const val AUTO_REFRESH_INTERVAL_MS: Long = 30L * 60L * 1000L

    fun formatDate(
        localizedContext: Context,
        locale: Locale,
        extendedStyle: Boolean,
        timeMillis: Long = System.currentTimeMillis(),
    ): String {
        val skeleton = localizedContext.getString(
            if (extendedStyle) {
                R.string.quickspace_date_format_minimalistic
            } else {
                R.string.quickspace_date_format
            }
        )
        val dateFormat = DateFormat.getInstanceForSkeleton(skeleton, locale)
        dateFormat.setContext(DisplayContext.CAPITALIZATION_FOR_STANDALONE)
        return dateFormat.format(timeMillis)
    }

    fun resolveGreetingAndExt(resources: Resources, hourOfDay: Int): Pair<String, String> =
        when (hourOfDay) {
            in 5..9 -> resources.getString(R.string.quickspace_grt_morning) to
                resources.getString(R.string.quickspace_ext_one)
            in 12..15 -> resources.getString(R.string.quickspace_grt_afternoon) to
                resources.getString(R.string.quickspace_ext_two)
            in 16..20 -> resources.getString(R.string.quickspace_grt_evening) to
                resources.getString(R.string.quickspace_ext_two)
            in 21..23, in 0..3 -> resources.getString(R.string.quickspace_grt_night) to
                resources.getString(R.string.quickspace_ext_two)
            else -> resources.getString(R.string.quickspace_grt_general) to
                resources.getString(R.string.quickspace_ext_two)
        }

    fun categoryForHour(hourOfDay: Int, strictCrDroidBuckets: Boolean): QuoteCategory {
        if (strictCrDroidBuckets) {
            return when (hourOfDay) {
                in 0..3 -> QuoteCategory.MIDNIGHT
                in 5..9 -> QuoteCategory.MORNING
                in 12..15 -> QuoteCategory.NOON
                in 16..18 -> QuoteCategory.EARLY_EVENING
                in 19..21 -> QuoteCategory.EVENING
                else -> QuoteCategory.NONE
            }
        }
        return when (hourOfDay) {
            in 0..4 -> QuoteCategory.MIDNIGHT
            in 5..11 -> QuoteCategory.MORNING
            in 12..15 -> QuoteCategory.NOON
            in 16..18 -> QuoteCategory.EARLY_EVENING
            in 19..21 -> QuoteCategory.EVENING
            else -> QuoteCategory.MIDNIGHT
        }
    }

    fun hourBucketId(hourOfDay: Int, strictCrDroidBuckets: Boolean): Int =
        categoryForHour(hourOfDay, strictCrDroidBuckets).ordinal

    fun arrayResForCategory(category: QuoteCategory): Int? = when (category) {
        QuoteCategory.MORNING -> R.array.quickspace_psa_morning
        QuoteCategory.NOON -> R.array.quickspace_psa_noon
        QuoteCategory.EARLY_EVENING -> R.array.quickspace_psa_early_evening
        QuoteCategory.EVENING -> R.array.quickspace_psa_evening
        QuoteCategory.MIDNIGHT -> R.array.quickspace_psa_midnight
        QuoteCategory.RANDOM -> R.array.quickspace_psa_random
        QuoteCategory.NONE -> null
    }

    fun dateKey(calendar: Calendar): String =
        "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.DAY_OF_YEAR)}"

    fun nextOrCurrentQuoteState(
        resources: Resources,
        settings: QuickSpaceSettings,
        previous: QuoteSelectionState?,
        forceNew: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
        randomInt: (bound: Int) -> Int = { bound -> ThreadLocalRandom.current().nextInt(bound) },
    ): QuoteSelectionState = selectQuoteState(
        arraySize = { category ->
            arrayResForCategory(category)?.let { resources.getStringArray(it).size } ?: 0
        },
        settings = settings,
        previous = previous,
        forceNew = forceNew,
        nowMillis = nowMillis,
        randomInt = randomInt,
    )

    internal fun selectQuoteState(
        arraySize: (QuoteCategory) -> Int,
        settings: QuickSpaceSettings,
        previous: QuoteSelectionState?,
        forceNew: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
        randomInt: (bound: Int) -> Int = { bound -> ThreadLocalRandom.current().nextInt(bound) },
    ): QuoteSelectionState {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val currentDateKey = dateKey(cal)
        val bucket = hourBucketId(hour, settings.crDroidProbabilityHide)

        if (!forceNew &&
            previous != null &&
            !previous.disabledByUser &&
            previous.lastDateKey == currentDateKey &&
            previous.lastHourBucket == bucket &&
            (nowMillis - previous.lastUpdatedMillis) in 0 until AUTO_REFRESH_INTERVAL_MS &&
            settings.showQuotes &&
            (!previous.isHidden || settings.crDroidProbabilityHide) &&
            (!previous.showEasterIcon || settings.enableEasterEggs)
        ) {
            return previous
        }

        if (!settings.showQuotes) {
            return QuoteSelectionState(
                category = QuoteCategory.NONE,
                index = 0,
                showEasterIcon = false,
                isHidden = true,
                disabledByUser = true,
                lastUpdatedMillis = nowMillis,
                lastDateKey = currentDateKey,
                lastHourBucket = bucket,
            )
        }

        if (settings.crDroidProbabilityHide) {
            val luckNumber = randomInt(14) // 0..13 matching QuickEventsController
            if (luckNumber < 7) {
                return QuoteSelectionState(
                    category = QuoteCategory.NONE,
                    index = 0,
                    showEasterIcon = false,
                    isHidden = true,
                    lastUpdatedMillis = nowMillis,
                    lastDateKey = currentDateKey,
                    lastHourBucket = bucket,
                )
            }
            if (luckNumber == 7 && settings.enableEasterEggs) {
                val idx = pickIndex(arraySize(QuoteCategory.RANDOM), previous, QuoteCategory.RANDOM, randomInt)
                return QuoteSelectionState(
                    category = QuoteCategory.RANDOM,
                    index = idx,
                    showEasterIcon = true,
                    isHidden = false,
                    lastUpdatedMillis = nowMillis,
                    lastDateKey = currentDateKey,
                    lastHourBucket = bucket,
                )
            }
            val strictCat = categoryForHour(hour, strictCrDroidBuckets = true)
            val count = arraySize(strictCat)
            if (strictCat == QuoteCategory.NONE || count <= 0) {
                return QuoteSelectionState(
                    category = QuoteCategory.NONE,
                    index = 0,
                    showEasterIcon = false,
                    isHidden = true,
                    lastUpdatedMillis = nowMillis,
                    lastDateKey = currentDateKey,
                    lastHourBucket = bucket,
                )
            }
            val idx = pickIndex(count, previous, strictCat, randomInt)
            return QuoteSelectionState(
                category = strictCat,
                index = idx,
                showEasterIcon = false,
                isHidden = false,
                lastUpdatedMillis = nowMillis,
                lastDateKey = currentDateKey,
                lastHourBucket = bucket,
            )
        }

        // Always-visible quote mode: optionally mix in random easter eggs (~1/14 chance)
        if (settings.enableEasterEggs && randomInt(14) == 7) {
            val idx = pickIndex(arraySize(QuoteCategory.RANDOM), previous, QuoteCategory.RANDOM, randomInt)
            return QuoteSelectionState(
                category = QuoteCategory.RANDOM,
                index = idx,
                showEasterIcon = true,
                isHidden = false,
                lastUpdatedMillis = nowMillis,
                lastDateKey = currentDateKey,
                lastHourBucket = bucket,
            )
        }

        val cat = categoryForHour(hour, strictCrDroidBuckets = false)
        val idx = pickIndex(arraySize(cat), previous, cat, randomInt)
        return QuoteSelectionState(
            category = cat,
            index = idx,
            showEasterIcon = false,
            isHidden = false,
            lastUpdatedMillis = nowMillis,
            lastDateKey = currentDateKey,
            lastHourBucket = bucket,
        )
    }

    private fun pickIndex(
        size: Int,
        previous: QuoteSelectionState?,
        category: QuoteCategory,
        randomInt: (Int) -> Int,
    ): Int {
        if (size <= 1) return 0
        var candidate = randomInt(size).coerceIn(0, size - 1)
        if (previous != null && !previous.isHidden && previous.category == category && candidate == previous.index) {
            candidate = (candidate + 1) % size
        }
        return candidate
    }

    fun resolveContent(
        baseContext: Context,
        settings: QuickSpaceSettings,
        state: QuoteSelectionState,
        nowMillis: Long = System.currentTimeMillis(),
    ): ResolvedWidgetContent {
        val localizedCtx = LocaleHelper.localizedContext(baseContext, settings.language)
        val locale = LocaleHelper.resolveLocale(settings.language, baseContext)
        val res = localizedCtx.resources
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)

        val (greeting, clockExt) = resolveGreetingAndExt(res, hour)
        val dateText = formatDate(
            localizedContext = localizedCtx,
            locale = locale,
            extendedStyle = settings.style == WidgetStyle.EXTENDED,
            timeMillis = nowMillis,
        )

        val quoteText = if (!settings.showQuotes || state.isHidden || state.category == QuoteCategory.NONE) {
            null
        } else {
            val arrRes = arrayResForCategory(state.category)
            arrRes?.let {
                val arr = res.getStringArray(it)
                if (arr.isNotEmpty()) arr[state.index.coerceIn(0, arr.lastIndex)] else null
            }
        }

        return ResolvedWidgetContent(
            greeting = greeting,
            clockPrefix = clockExt,
            dateText = dateText,
            quoteText = quoteText,
            showEasterIcon = quoteText != null && state.showEasterIcon && settings.enableEasterEggs,
            hiddenByProbability = settings.showQuotes && state.isHidden && settings.crDroidProbabilityHide,
        )
    }
}
