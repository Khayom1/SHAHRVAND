package tj.shahrvand.app

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object ProgressRules {
    private const val XP_PER_LEVEL = 100
    private val competitionZone: ZoneId = ZoneId.of("Asia/Dushanbe")

    fun competitionDate(now: Instant = Instant.now()): LocalDate =
        now.atZone(competitionZone).toLocalDate()

    fun levelFor(totalXp: Int): Int = (totalXp.coerceAtLeast(0) / XP_PER_LEVEL) + 1

    fun levelProgress(totalXp: Int): Float =
        (totalXp.coerceAtLeast(0) % XP_PER_LEVEL).toFloat() / XP_PER_LEVEL.toFloat()

    fun xpToNextLevel(totalXp: Int): Int =
        XP_PER_LEVEL - (totalXp.coerceAtLeast(0) % XP_PER_LEVEL)

    fun canCompleteToday(missionId: String, completedIds: Set<String>): Boolean =
        missionId.isNotBlank() && missionId !in completedIds

    fun dailyGoalProgress(completedToday: Int, target: Int = 3): Float {
        if (target <= 0) return 0f
        return (completedToday.coerceAtLeast(0).toFloat() / target.toFloat()).coerceIn(0f, 1f)
    }
}
