package tj.shahrvand.app

/** Evidence-based minimum sleep-duration guidance by age group. */
object SleepRules {
    fun minimumTargetMinutes(age: Int): Long = when {
        age in 7..12 -> 9L * 60L
        age in 13..17 -> 8L * 60L
        else -> 7L * 60L
    }
}
