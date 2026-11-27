package tj.shahrvand.app

/** Age-based sleep targets used by the sleep-habit challenge. */
object SleepRules {
    const val REWARD_XP = 15

    fun minimumTargetMinutes(age: Int): Long = when {
        age in 7..12 -> 9L * 60L
        age in 13..17 -> 8L * 60L
        else -> 7L * 60L
    }

    fun qualifies(age: Int, sleepMinutes: Long): Boolean =
        sleepMinutes >= minimumTargetMinutes(age)

    /** Accept non-manual sleep sessions from any connected Health Connect provider. */
    fun isEligibleRecord(isManual: Boolean, sourcePackage: String): Boolean =
        !isManual && sourcePackage.isNotBlank()
}
