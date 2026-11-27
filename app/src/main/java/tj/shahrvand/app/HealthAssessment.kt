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

data class DailyHydrationReference(
    val totalWaterMl: Int,
    val beverageWaterMl: Int
)

object HealthAssessment {
    /**
     * Predicts resting energy expenditure (REE), not total daily energy needs.
     * Mifflin-St Jeor was developed in adults aged 19-78; SHAHRVAND applies it
     * conservatively only from age 20 through 78 and only when sex at birth is specified.
     * This is an estimate, not a measured metabolic rate or a calorie target.
     */
    fun estimateRestingEnergyKcalPerDay(
        ageYears: Int,
        heightCm: Double,
        weightKg: Double,
        sexAtBirth: String
    ): Double? {
        if (ageYears !in 20..78) return null
        if (!heightCm.isFinite() || heightCm !in 80.0..250.0) return null
        if (!weightKg.isFinite() || weightKg !in 5.0..500.0) return null

        val sexConstant = when (sexAtBirth) {
            "male" -> 5.0
            "female" -> -161.0
            else -> return null
        }
        val estimate = (10.0 * weightKg) +
            (6.25 * heightCm) -
            (5.0 * ageYears) +
            sexConstant
        return estimate.takeIf { it.isFinite() && it > 0.0 }
    }

    /**
     * Estimates total daily energy expenditure (TDEE) for adult maintenance.
     * Activity multipliers follow the conventional sedentary/light/moderate/high
     * categories. This is an estimate, not a prescribed intake or weight-loss plan.
     */
    fun estimateMaintenanceEnergyKcalPerDay(
        ageYears: Int,
        heightCm: Double,
        weightKg: Double,
        sexAtBirth: String,
        activityLevel: String
    ): Double? {
        val resting = estimateRestingEnergyKcalPerDay(
            ageYears = ageYears,
            heightCm = heightCm,
            weightKg = weightKg,
            sexAtBirth = sexAtBirth
        ) ?: return null
        val activityFactor = when (activityLevel) {
            "sedentary" -> 1.2
            "light" -> 1.375
            "moderate" -> 1.55
            "high" -> 1.725
            else -> return null
        }
        return (resting * activityFactor).takeIf { it.isFinite() && it > 0.0 }
    }

    /**
     * Age- and sex-specific adequate-intake reference values for healthy people.
     * Activity and heat can increase needs; this is not a fixed prescription.
     * Pregnancy, lactation and fluid-restricted conditions need clinical advice.
     */
    fun estimateDailyHydrationReference(
        ageYears: Int,
        sexAtBirth: String
    ): DailyHydrationReference? {
        val values = when {
            ageYears in 9..13 && sexAtBirth == "male" -> 2400 to 1800
            ageYears in 9..13 && sexAtBirth == "female" -> 2100 to 1600
            ageYears in 14..18 && sexAtBirth == "male" -> 3300 to 2600
            ageYears in 14..18 && sexAtBirth == "female" -> 2300 to 1800
            ageYears in 19..120 && sexAtBirth == "male" -> 3700 to 3000
            ageYears in 19..120 && sexAtBirth == "female" -> 2700 to 2200
            else -> return null
        }
        return DailyHydrationReference(values.first, values.second)
    }

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
