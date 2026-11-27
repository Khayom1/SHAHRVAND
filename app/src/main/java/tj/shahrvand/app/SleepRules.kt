package tj.shahrvand.app

/** Evidence-based minimum sleep-duration guidance by age group. */
object SleepRules {
    fun minimumTargetMinutes(age: Int): Long = when {
        age in 7..12 -> 9L * 60L
        age in 13..17 -> 8L * 60L
        else -> 7L * 60L
    }

    /**
     * Analyses the newest sleep records (newest first). This is a screening aid,
     * not a diagnosis and not a measure of sleep quality.
     */
    fun analyzeRecentNights(
        minutesNewestFirst: List<Long>,
        targetMinutes: Long
    ): SleepInsight? {
        if (targetMinutes <= 0L || minutesNewestFirst.isEmpty()) return null

        val recent = minutesNewestFirst.take(7).filter { it > 0L }
        if (recent.isEmpty()) return null

        val belowTargetCount = recent.count { it < targetMinutes }
        val latestBelowTarget = recent.first() < targetMinutes
        val consecutiveBelowTarget = recent.takeWhile { it < targetMinutes }.size
        val averageMinutes = recent.average().toLong()

        val pattern = when {
            belowTargetCount >= 3 -> SleepPattern.REPEATED_SHORT_SLEEP
            latestBelowTarget -> SleepPattern.LAST_NIGHT_SHORT
            belowTargetCount > 0 -> SleepPattern.MIXED_RECENT_SLEEP
            else -> SleepPattern.ADEQUATE
        }

        return SleepInsight(
            recentNightCount = recent.size,
            belowTargetCount = belowTargetCount,
            consecutiveBelowTarget = consecutiveBelowTarget,
            averageMinutes = averageMinutes,
            targetMinutes = targetMinutes,
            pattern = pattern
        )
    }
}

internal enum class SleepPattern {
    REPEATED_SHORT_SLEEP,
    LAST_NIGHT_SHORT,
    MIXED_RECENT_SLEEP,
    ADEQUATE
}

internal data class SleepInsight(
    val recentNightCount: Int,
    val belowTargetCount: Int,
    val consecutiveBelowTarget: Int,
    val averageMinutes: Long,
    val targetMinutes: Long,
    val pattern: SleepPattern
)
