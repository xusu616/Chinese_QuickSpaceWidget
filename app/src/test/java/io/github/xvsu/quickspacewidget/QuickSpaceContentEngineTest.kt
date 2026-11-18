package io.github.xvsu.quickspacewidget

import io.github.xvsu.quickspacewidget.engine.QuickSpaceContentEngine
import io.github.xvsu.quickspacewidget.model.QuickSpaceSettings
import io.github.xvsu.quickspacewidget.model.QuoteCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class QuickSpaceContentEngineTest {

    private val fakeSizes: (QuoteCategory) -> Int = { cat ->
        when (cat) {
            QuoteCategory.MORNING -> 22
            QuoteCategory.NOON -> 20
            QuoteCategory.EARLY_EVENING -> 18
            QuoteCategory.EVENING -> 17
            QuoteCategory.MIDNIGHT -> 17
            QuoteCategory.RANDOM -> 42
            QuoteCategory.NONE -> 0
        }
    }

    private fun millisAt(hour: Int, day: Int = 18): Long =
        Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, day, hour, 15, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test
    fun strictBucketsMatchCrDroidHours() {
        assertEquals(QuoteCategory.MIDNIGHT, QuickSpaceContentEngine.categoryForHour(0, true))
        assertEquals(QuoteCategory.MIDNIGHT, QuickSpaceContentEngine.categoryForHour(3, true))
        assertEquals(QuoteCategory.NONE, QuickSpaceContentEngine.categoryForHour(4, true))
        assertEquals(QuoteCategory.MORNING, QuickSpaceContentEngine.categoryForHour(5, true))
        assertEquals(QuoteCategory.MORNING, QuickSpaceContentEngine.categoryForHour(9, true))
        assertEquals(QuoteCategory.NONE, QuickSpaceContentEngine.categoryForHour(10, true))
        assertEquals(QuoteCategory.NONE, QuickSpaceContentEngine.categoryForHour(11, true))
        assertEquals(QuoteCategory.NOON, QuickSpaceContentEngine.categoryForHour(12, true))
        assertEquals(QuoteCategory.NOON, QuickSpaceContentEngine.categoryForHour(15, true))
        assertEquals(QuoteCategory.EARLY_EVENING, QuickSpaceContentEngine.categoryForHour(16, true))
        assertEquals(QuoteCategory.EARLY_EVENING, QuickSpaceContentEngine.categoryForHour(18, true))
        assertEquals(QuoteCategory.EVENING, QuickSpaceContentEngine.categoryForHour(19, true))
        assertEquals(QuoteCategory.EVENING, QuickSpaceContentEngine.categoryForHour(21, true))
        assertEquals(QuoteCategory.NONE, QuickSpaceContentEngine.categoryForHour(22, true))
        assertEquals(QuoteCategory.NONE, QuickSpaceContentEngine.categoryForHour(23, true))
    }

    @Test
    fun continuousBucketsCoverEveryHour() {
        for (hour in 0..23) {
            val cat = QuickSpaceContentEngine.categoryForHour(hour, strictCrDroidBuckets = false)
            assertTrue("Hour $hour should map to a visible category", cat != QuoteCategory.NONE)
        }
    }

    @Test
    fun probabilityHideBehaviorMatchesCrDroid() {
        val settings = QuickSpaceSettings(crDroidProbabilityHide = true, enableEasterEggs = true)
        val morningMs = millisAt(8)

        val hiddenRoll = QuickSpaceContentEngine.selectQuoteState(
            arraySize = fakeSizes,
            settings = settings,
            previous = null,
            forceNew = true,
            nowMillis = morningMs,
            randomInt = { 3 }, // luckNumber < 7 -> hide
        )
        assertTrue(hiddenRoll.isHidden)
        assertFalse(hiddenRoll.disabledByUser)
        assertEquals(QuoteCategory.NONE, hiddenRoll.category)

        val easterRoll = QuickSpaceContentEngine.selectQuoteState(
            arraySize = fakeSizes,
            settings = settings,
            previous = null,
            forceNew = true,
            nowMillis = morningMs,
            randomInt = { 7 }, // luckNumber == 7 -> random PSA + crDroid icon
        )
        assertFalse(easterRoll.isHidden)
        assertTrue(easterRoll.showEasterIcon)
        assertEquals(QuoteCategory.RANDOM, easterRoll.category)

        val normalRoll = QuickSpaceContentEngine.selectQuoteState(
            arraySize = fakeSizes,
            settings = settings,
            previous = null,
            forceNew = true,
            nowMillis = morningMs,
            randomInt = { bound -> if (bound == 14) 10 else 5 },
        )
        assertFalse(normalRoll.isHidden)
        assertFalse(normalRoll.showEasterIcon)
        assertEquals(QuoteCategory.MORNING, normalRoll.category)
        assertEquals(5, normalRoll.index)
    }

    @Test
    fun togglingShowQuotesOffThenOnWithProbabilityHideRollsFreshQuote() {
        val now = millisAt(8)
        val disabledState = QuickSpaceContentEngine.selectQuoteState(
            arraySize = fakeSizes,
            settings = QuickSpaceSettings(showQuotes = false, crDroidProbabilityHide = true),
            previous = null,
            forceNew = false,
            nowMillis = now,
        )
        assertTrue(disabledState.disabledByUser)
        assertTrue(disabledState.isHidden)

        val reEnabledState = QuickSpaceContentEngine.selectQuoteState(
            arraySize = fakeSizes,
            settings = QuickSpaceSettings(showQuotes = true, crDroidProbabilityHide = true),
            previous = disabledState,
            forceNew = false,
            nowMillis = now + 5_000L,
            randomInt = { bound -> if (bound == 14) 10 else 2 },
        )
        assertFalse(reEnabledState.disabledByUser)
        assertFalse(reEnabledState.isHidden)
        assertEquals(QuoteCategory.MORNING, reEnabledState.category)
        assertEquals(2, reEnabledState.index)
    }

    @Test
    fun quoteIsReusedWithin30MinutesAndAvoidsRepeatOnForceNew() {
        val settings = QuickSpaceSettings(crDroidProbabilityHide = false, enableEasterEggs = false)
        val startMs = millisAt(8)

        val first = QuickSpaceContentEngine.selectQuoteState(
            arraySize = fakeSizes,
            settings = settings,
            previous = null,
            forceNew = true,
            nowMillis = startMs,
            randomInt = { 4 },
        )
        assertEquals(4, first.index)

        val reused = QuickSpaceContentEngine.selectQuoteState(
            arraySize = fakeSizes,
            settings = settings,
            previous = first,
            forceNew = false,
            nowMillis = startMs + 10 * 60 * 1000L,
            randomInt = { 9 },
        )
        assertEquals(first, reused)

        val forcedNext = QuickSpaceContentEngine.selectQuoteState(
            arraySize = fakeSizes,
            settings = settings,
            previous = first,
            forceNew = true,
            nowMillis = startMs + 10 * 60 * 1000L,
            randomInt = { 4 },
        )
        assertNotEquals(first.index, forcedNext.index)

        val expired = QuickSpaceContentEngine.selectQuoteState(
            arraySize = fakeSizes,
            settings = settings,
            previous = first,
            forceNew = false,
            nowMillis = startMs + QuickSpaceContentEngine.AUTO_REFRESH_INTERVAL_MS + 1000L,
            randomInt = { 11 },
        )
        assertEquals(11, expired.index)
    }

    @Test
    fun dateKeyDiffersAcrossDays() {
        val c1 = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 18, 23, 59) }
        val c2 = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 19, 0, 1) }
        assertFalse(QuickSpaceContentEngine.dateKey(c1) == QuickSpaceContentEngine.dateKey(c2))
    }
}
