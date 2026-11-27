package tj.shahrvand.app

/**
 * BMI is a screening measure, not a diagnosis.
 *
 * WHO BMI-for-age references are required for users under 20. This module
 * deliberately does not apply adult cut-offs to children or adolescents.
 */
enum class AdultBmiCategory {
    UNDERWEIGHT,
    HEALTHY_RANGE,
    OVERWEIGHT,
    OBESITY
}

data class BmiAssessment(
    val bmi: Double,
    val adultCategory: AdultBmiCategory?
) {
    val requiresPediatricReference: Boolean
        get() = adultCategory == null
}

object HealthAssessment {
    fun assessBmi(ageYears: Int, heightCm: Double, weightKg: Double): BmiAssessment? {
        if (ageYears !in 7..120) return null
        if (!heightCm.isFinite() || heightCm !in 80.0..250.0) return null
        if (!weightKg.isFinite() || weightKg !in 5.0..500.0) return null

        val heightMeters = heightCm / 100.0
        val rawBmi = weightKg / (heightMeters * heightMeters)
        if (!rawBmi.isFinite()) return null

        val category = if (ageYears < 20) {
            null
        } else {
            when {
                rawBmi < 18.5 -> AdultBmiCategory.UNDERWEIGHT
                rawBmi < 25.0 -> AdultBmiCategory.HEALTHY_RANGE
                rawBmi < 30.0 -> AdultBmiCategory.OVERWEIGHT
                else -> AdultBmiCategory.OBESITY
            }
        }

        return BmiAssessment(
            bmi = kotlin.math.round(rawBmi * 10.0) / 10.0,
            adultCategory = category
        )
    }
}
