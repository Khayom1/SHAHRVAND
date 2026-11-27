package tj.shahrvand.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthAssessmentTest {
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
        assertEquals(22.9, HealthAssessment.assessBmi(25, 170.0, 66.0)?.bmi ?: 0.0, 0.0001)
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
