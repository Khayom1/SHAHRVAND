package tj.shahrvand.app

import org.junit.Assert.assertEquals
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
}
