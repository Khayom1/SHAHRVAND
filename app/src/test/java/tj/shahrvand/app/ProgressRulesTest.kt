package tj.shahrvand.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressRulesTest {
    @Test
    fun levelStartsAtOneAndAdvancesEveryHundredXp() {
        assertEquals(1, ProgressRules.levelFor(0))
        assertEquals(1, ProgressRules.levelFor(99))
        assertEquals(2, ProgressRules.levelFor(100))
        assertEquals(3, ProgressRules.levelFor(200))
    }

    @Test
    fun progressIsBoundedWithinCurrentLevel() {
        assertEquals(0f, ProgressRules.levelProgress(100), 0.0001f)
        assertEquals(0.5f, ProgressRules.levelProgress(250), 0.0001f)
        assertEquals(0.99f, ProgressRules.levelProgress(99), 0.0001f)
    }

    @Test
    fun duplicateMissionCannotBeCompletedTwiceInOneDay() {
        val completed = setOf("read_10")
        assertFalse(ProgressRules.canCompleteToday("read_10", completed))
        assertTrue(ProgressRules.canCompleteToday("help_home", completed))
        assertFalse(ProgressRules.canCompleteToday("", completed))
    }

    @Test
    fun dailyGoalProgressIsClamped() {
        assertEquals(0f, ProgressRules.dailyGoalProgress(-1), 0.0001f)
        assertEquals(0.5f, ProgressRules.dailyGoalProgress(1), 0.0001f)
        assertEquals(1f, ProgressRules.dailyGoalProgress(5), 0.0001f)
        assertEquals(0f, ProgressRules.dailyGoalProgress(3, 0), 0.0001f)
    }
}
