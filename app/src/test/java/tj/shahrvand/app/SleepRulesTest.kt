package tj.shahrvand.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepRulesTest {
    @Test
    fun schoolAgeTargetIsNineHours() {
        assertEquals(540L, SleepRules.minimumTargetMinutes(7))
        assertEquals(540L, SleepRules.minimumTargetMinutes(12))
    }

    @Test
    fun teenTargetIsEightHours() {
        assertEquals(480L, SleepRules.minimumTargetMinutes(13))
        assertEquals(480L, SleepRules.minimumTargetMinutes(17))
    }

    @Test
    fun adultTargetIsAtLeastSevenHours() {
        assertEquals(420L, SleepRules.minimumTargetMinutes(18))
        assertEquals(420L, SleepRules.minimumTargetMinutes(60))
    }

    @Test
    fun oneShortLatestNightIsNotClassifiedAsRecurring() {
        val insight = SleepRules.analyzeRecentNights(
            minutesNewestFirst = listOf(300L, 450L, 440L, 430L),
            targetMinutes = 420L
        )

        assertEquals(SleepPattern.LAST_NIGHT_SHORT, insight?.pattern)
        assertEquals(1, insight?.belowTargetCount)
        assertEquals(1, insight?.consecutiveBelowTarget)
    }

    @Test
    fun threeShortNightsInRecentSevenRecordsAreFlaggedAsRecurring() {
        val insight = SleepRules.analyzeRecentNights(
            minutesNewestFirst = listOf(360L, 430L, 400L, 450L, 420L, 410L, 430L),
            targetMinutes = 420L
        )

        assertEquals(SleepPattern.REPEATED_SHORT_SLEEP, insight?.pattern)
        assertEquals(3, insight?.belowTargetCount)
        assertEquals(1, insight?.consecutiveBelowTarget)
        assertEquals(7, insight?.recentNightCount)
    }

    @Test
    fun adequateLatestNightCanStillShowAnIrregularRecentPattern() {
        val insight = SleepRules.analyzeRecentNights(
            minutesNewestFirst = listOf(450L, 390L, 430L, 400L),
            targetMinutes = 420L
        )

        assertEquals(SleepPattern.MIXED_RECENT_SLEEP, insight?.pattern)
        assertEquals(2, insight?.belowTargetCount)
    }

    @Test
    fun exactlyMeetingTargetCountsAsAdequate() {
        val insight = SleepRules.analyzeRecentNights(
            minutesNewestFirst = listOf(420L, 420L, 450L),
            targetMinutes = 420L
        )

        assertEquals(SleepPattern.ADEQUATE, insight?.pattern)
        assertEquals(0, insight?.belowTargetCount)
        assertTrue((insight?.averageMinutes ?: 0L) >= 420L)
    }

    @Test
    fun emptyOrInvalidInputHasNoInsight() {
        assertNull(SleepRules.analyzeRecentNights(emptyList(), 420L))
        assertNull(SleepRules.analyzeRecentNights(listOf(420L), 0L))
        assertNull(SleepRules.analyzeRecentNights(listOf(0L, -1L), 420L))
    }

    @Test
    fun onlySevenNewestRecordsAreAnalysed() {
        val insight = SleepRules.analyzeRecentNights(
            minutesNewestFirst = listOf(430L, 430L, 430L, 430L, 430L, 430L, 430L, 100L),
            targetMinutes = 420L
        )

        assertEquals(SleepPattern.ADEQUATE, insight?.pattern)
        assertEquals(7, insight?.recentNightCount)
    }
}
