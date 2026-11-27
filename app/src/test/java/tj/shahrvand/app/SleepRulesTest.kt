package tj.shahrvand.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepRulesTest {
    @Test
    fun schoolAgeTargetIsNineHours() {
        assertEquals(540L, SleepRules.minimumTargetMinutes(7))
        assertEquals(540L, SleepRules.minimumTargetMinutes(12))
        assertFalse(SleepRules.qualifies(12, 539L))
        assertTrue(SleepRules.qualifies(12, 540L))
    }

    @Test
    fun teenTargetIsEightHours() {
        assertEquals(480L, SleepRules.minimumTargetMinutes(13))
        assertEquals(480L, SleepRules.minimumTargetMinutes(17))
        assertFalse(SleepRules.qualifies(16, 479L))
        assertTrue(SleepRules.qualifies(16, 480L))
    }

    @Test
    fun sleepEligibilityIsProviderAgnosticButRejectsManualOrMissingSource() {
        assertTrue(SleepRules.isEligibleRecord(false, "com.sec.android.app.shealth"))
        assertTrue(SleepRules.isEligibleRecord(false, "com.google.android.apps.fitness"))
        assertTrue(SleepRules.isEligibleRecord(false, "com.xiaomi.wearable"))
        assertFalse(SleepRules.isEligibleRecord(true, "com.sec.android.app.shealth"))
        assertFalse(SleepRules.isEligibleRecord(false, ""))
        assertFalse(SleepRules.isEligibleRecord(false, "   "))
    }

    @Test
    fun adultTargetIsAtLeastSevenHours() {
        assertEquals(420L, SleepRules.minimumTargetMinutes(18))
        assertEquals(420L, SleepRules.minimumTargetMinutes(60))
        assertTrue(SleepRules.qualifies(26, 420L))
    }
}
