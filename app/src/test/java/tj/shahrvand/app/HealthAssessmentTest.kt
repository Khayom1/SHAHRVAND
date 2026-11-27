package tj.shahrvand.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthAssessmentTest {
    @Test
    fun mifflinStJeorEstimatesRestingEnergyForAdults() {
        assertEquals(
            1648.75,
            HealthAssessment.estimateRestingEnergyKcalPerDay(30, 175.0, 70.0, "male") ?: 0.0,
            0.0001
        )
        assertEquals(
            1482.75,
            HealthAssessment.estimateRestingEnergyKcalPerDay(30, 175.0, 70.0, "female") ?: 0.0,
            0.0001
        )
    }

    @Test
    fun restingEnergyEstimateRejectsUnvalidatedAgeAndMissingSex() {
        assertNull(HealthAssessment.estimateRestingEnergyKcalPerDay(19, 170.0, 60.0, "male"))
        assertNull(HealthAssessment.estimateRestingEnergyKcalPerDay(79, 170.0, 60.0, "female"))
        assertNull(HealthAssessment.estimateRestingEnergyKcalPerDay(30, 170.0, 60.0, "not_specified"))
        assertNull(HealthAssessment.estimateRestingEnergyKcalPerDay(30, Double.NaN, 60.0, "male"))
        assertNull(HealthAssessment.estimateRestingEnergyKcalPerDay(30, 170.0, Double.POSITIVE_INFINITY, "female"))
    }


    @Test
    fun maintenanceEnergyUsesPersonalActivityLevel() {
        val sedentary = HealthAssessment.estimateMaintenanceEnergyKcalPerDay(
            30, 175.0, 70.0, "male", "sedentary"
        )
        val light = HealthAssessment.estimateMaintenanceEnergyKcalPerDay(
            30, 175.0, 70.0, "male", "light"
        )
        val moderate = HealthAssessment.estimateMaintenanceEnergyKcalPerDay(
            30, 175.0, 70.0, "male", "moderate"
        )
        val high = HealthAssessment.estimateMaintenanceEnergyKcalPerDay(
            30, 175.0, 70.0, "male", "high"
        )
        assertEquals(1978.5, sedentary ?: 0.0, 0.001)
        assertEquals(2267.03125, light ?: 0.0, 0.001)
        assertEquals(2555.5625, moderate ?: 0.0, 0.001)
        assertEquals(2844.09375, high ?: 0.0, 0.001)
        assertTrue((light ?: 0.0) > (sedentary ?: 0.0))
        assertTrue((moderate ?: 0.0) > (light ?: 0.0))
        assertTrue((high ?: 0.0) > (moderate ?: 0.0))
    }

    @Test
    fun maintenanceEnergyRejectsUnsupportedAgeAndActivity() {
        assertNull(HealthAssessment.estimateMaintenanceEnergyKcalPerDay(
            19, 170.0, 60.0, "male", "light"
        ))
        assertNull(HealthAssessment.estimateMaintenanceEnergyKcalPerDay(
            30, 170.0, 60.0, "male", "unknown"
        ))
    }

    @Test
    fun hydrationReferenceUsesAgeAndSexAndSeparatesBeveragesFromTotalWater() {
        assertEquals(DailyHydrationReference(3700, 3000),
            HealthAssessment.estimateDailyHydrationReference(20, "male"))
        assertEquals(DailyHydrationReference(2700, 2200),
            HealthAssessment.estimateDailyHydrationReference(35, "female"))
        assertEquals(DailyHydrationReference(3300, 2600),
            HealthAssessment.estimateDailyHydrationReference(17, "male"))
        assertEquals(DailyHydrationReference(2300, 1800),
            HealthAssessment.estimateDailyHydrationReference(17, "female"))
        assertEquals(DailyHydrationReference(2400, 1800),
            HealthAssessment.estimateDailyHydrationReference(13, "male"))
        assertEquals(DailyHydrationReference(2100, 1600),
            HealthAssessment.estimateDailyHydrationReference(13, "female"))
    }

    @Test
    fun hydrationReferenceRejectsUnknownSexAndUnsupportedAge() {
        assertNull(HealthAssessment.estimateDailyHydrationReference(20, "not_specified"))
        assertNull(HealthAssessment.estimateDailyHydrationReference(8, "male"))
        assertNull(HealthAssessment.estimateDailyHydrationReference(121, "female"))
    }

    @Test
    fun adultBmiUsesAdultScreeningRanges() {
        assertEquals(
            AdultBmiCategory.UNDERWEIGHT,
            HealthAssessment.assessBmi(20, 100.0, 18.4)?.adultCategory
        )
        assertEquals(
            AdultBmiCategory.HEALTHY_RANGE,
            HealthAssessment.assessBmi(20, 100.0, 18.5)?.adultCategory
        )
        assertEquals(
            AdultBmiCategory.HEALTHY_RANGE,
            HealthAssessment.assessBmi(35, 100.0, 24.9)?.adultCategory
        )
        assertEquals(
            AdultBmiCategory.OVERWEIGHT,
            HealthAssessment.assessBmi(35, 100.0, 25.0)?.adultCategory
        )
        assertEquals(
            AdultBmiCategory.OBESITY,
            HealthAssessment.assessBmi(35, 100.0, 30.0)?.adultCategory
        )
    }

    @Test
    fun usersUnderTwentyRequirePediatricReference() {
        val result = HealthAssessment.assessBmi(19, 170.0, 55.0)
        assertTrue(result?.requiresPediatricReference == true)
        assertNull(result?.adultCategory)
        assertFalse(HealthAssessment.assessBmi(20, 170.0, 55.0)?.requiresPediatricReference == true)
    }

    @Test
    fun bmiIsRoundedToOneDecimalForDisplay() {
        assertEquals(22.8, HealthAssessment.assessBmi(25, 170.0, 66.0)?.bmi ?: 0.0, 0.0001)
    }

    @Test
    fun invalidInputsAreRejected() {
        assertNull(HealthAssessment.assessBmi(6, 170.0, 65.0))
        assertNull(HealthAssessment.assessBmi(121, 170.0, 65.0))
        assertNull(HealthAssessment.assessBmi(30, 70.0, 65.0))
        assertNull(HealthAssessment.assessBmi(30, 170.0, 4.0))
        assertNull(HealthAssessment.assessBmi(30, Double.NaN, 65.0))
        assertNull(HealthAssessment.assessBmi(30, 170.0, Double.POSITIVE_INFINITY))
    }
}
