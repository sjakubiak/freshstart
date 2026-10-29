package com.freshstart.app.data

import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Where a day's XP came from. Each hero earns a bonus on one of these. */
enum class XpSource(val label: String) {
    LOGGING("logging food"),
    GREEN("green foods"),
    BUDGET("staying on budget"),
    STEPS("steps"),
    WEIGH_IN("weigh-ins"),
    STREAK("streaks"),
    RED("red foods")
}

enum class HeroClass(val label: String, val emoji: String, val blurb: String, val boost: XpSource) {
    WIZARD("Green Wizard", "🧙", "Draws power from vegetables and fruit.", XpSource.GREEN),
    NINJA("Step Ninja", "🥷", "Grows stronger with every step.", XpSource.STEPS),
    HERO("Budget Hero", "🧑", "Rewarded for days spent inside the calorie budget.", XpSource.BUDGET),
    RANGER("Streak Ranger", "🧔", "Never misses a day on the trail.", XpSource.STREAK),
    FAIRY("Logging Elf", "🧝", "Loves a complete food diary.", XpSource.LOGGING),
    ROBOT("Scale Bot", "🤖", "Runs on regular weigh-ins.", XpSource.WEIGH_IN)
}

/** An achievement earned from what's logged. [hint] says how to earn it. */
data class Badge(val id: String, val emoji: String, val name: String, val hint: String)

/** Everything badges are judged on. */
data class BadgeFacts(
    val profile: Profile,
    val foods: List<FoodEntry>,
    val weights: List<WeightEntry>,
    val progress: GameProgress,
    val lessonsRead: Int,
    val lessonCount: Int
)

/** One line of a day's XP breakdown, such as "+12 · 6,000 steps". */
data class XpLine(val source: XpSource?, val label: String, val xp: Int)

data class DayXp(val date: String, val lines: List<XpLine>) {
    val total: Int get() = max(Game.WORST_DAY, lines.sumOf { it.xp })
}

data class GameProgress(
    val totalXp: Int,
    val level: Int,
    val xpIntoLevel: Int,
    val xpForNextLevel: Int,
    val days: Map<String, DayXp>
) {
    val title: String get() = Game.titleFor(level)
}

/**
 * Turns what's already logged (food colors, calories against the budget, weigh-ins and
 * Health Connect steps) into XP. Nothing extra to enter: XP is recalculated from the log,
 * so editing or deleting a food fixes that day's XP too.
 */
object Game {
    const val HERO_BONUS = 0.25
    const val WORST_DAY = -40
    const val STEP_GOAL = 10_000L
    private const val BUDGET_XP = 25
    private const val STEP_GOAL_LABEL = "Hit 10,000 steps"

    val badges: List<Badge> = listOf(
        Badge("first_log", "🍽️", "First bite", "Log your first food"),
        Badge("streak_3", "🔥", "On a roll", "Log food 3 days in a row"),
        Badge("streak_7", "📅", "Week warrior", "Log food 7 days in a row"),
        Badge("streak_30", "🏆", "Habit hero", "Log food 30 days in a row"),
        Badge("green_5", "🥦", "Green machine", "Eat 5 green foods in one day"),
        Badge("red_free", "🛡️", "Red-free day", "Finish a day with 3 or more foods and no red"),
        Badge("budget_1", "🎯", "On target", "Finish a day within your budget"),
        Badge("budget_7", "🏹", "Sharpshooter", "Finish 7 days within your budget"),
        Badge("budget_30", "💎", "Budget master", "Finish 30 days within your budget"),
        Badge("steps_10k", "👟", "10K club", "Walk 10,000 steps in a day"),
        Badge("steps_10k_7", "🏃", "Road runner", "Walk 10,000 steps on 7 days"),
        Badge("weigh_1", "⚖️", "Step on up", "Log a weigh-in after your first day"),
        Badge("weigh_10", "📈", "Data driven", "Log 10 weigh-ins"),
        Badge("lost_1", "🌱", "First steps", "Lose 1% of your starting weight"),
        Badge("lost_5", "🌿", "Gaining ground", "Lose 5% of your starting weight"),
        Badge("lost_10", "🌳", "Big change", "Lose 10% of your starting weight"),
        Badge("goal", "🥇", "Goal reached", "Reach your goal weight"),
        Badge("lessons_5", "📚", "Bookworm", "Read 5 lessons"),
        Badge("lessons_all", "🎓", "Graduate", "Read every lesson"),
        Badge("level_5", "⭐", "Adventurer", "Reach level 5"),
        Badge("level_10", "🌟", "Veteran", "Reach level 10"),
        Badge("level_20", "💫", "Champion", "Reach level 20"),
        Badge("level_30", "👑", "Legend", "Reach level 30")
    )

    fun badgeById(id: String): Badge? = badges.firstOrNull { it.id == id }

    /** Ids of the badges the log currently qualifies for. */
    fun qualifiedBadges(f: BadgeFacts): Set<String> {
        val days = f.progress.days.values
        val onBudget = days.count { d -> d.lines.any { it.source == XpSource.BUDGET && it.xp == BUDGET_XP } }
        val bigStepDays = days.count { d -> d.lines.any { it.source == XpSource.STEPS && it.label == STEP_GOAL_LABEL } }
        val foodsByDay = f.foods.groupBy { it.date }
        val today = f.progress.days.keys.maxOrNull()
        val lostShare = if (f.profile.startWeightKg > 0) {
            (f.profile.startWeightKg - (f.weights.maxByOrNull { it.date }?.kg ?: f.profile.startWeightKg)) / f.profile.startWeightKg
        } else 0.0
        val latestKg = f.weights.maxByOrNull { it.date }?.kg
        val streak = longestStreak(foodsByDay.keys)

        return buildSet {
            if (f.foods.isNotEmpty()) add("first_log")
            if (streak >= 3) add("streak_3")
            if (streak >= 7) add("streak_7")
            if (streak >= 30) add("streak_30")
            if (foodsByDay.values.any { list -> list.count { it.color == FoodColor.GREEN } >= 5 }) add("green_5")
            if (foodsByDay.any { (date, list) -> date != today && list.size >= 3 && list.none { it.color == FoodColor.RED } }) add("red_free")
            if (onBudget >= 1) add("budget_1")
            if (onBudget >= 7) add("budget_7")
            if (onBudget >= 30) add("budget_30")
            if (bigStepDays >= 1) add("steps_10k")
            if (bigStepDays >= 7) add("steps_10k_7")
            if (f.weights.any { it.date != f.profile.startDate }) add("weigh_1")
            if (f.weights.size >= 10) add("weigh_10")
            if (lostShare >= 0.01) add("lost_1")
            if (lostShare >= 0.05) add("lost_5")
            if (lostShare >= 0.10) add("lost_10")
            if (latestKg != null && f.profile.goalWeightKg < f.profile.startWeightKg && latestKg <= f.profile.goalWeightKg) add("goal")
            if (f.lessonsRead >= 5) add("lessons_5")
            if (f.lessonCount > 0 && f.lessonsRead >= f.lessonCount) add("lessons_all")
            if (f.progress.level >= 5) add("level_5")
            if (f.progress.level >= 10) add("level_10")
            if (f.progress.level >= 20) add("level_20")
            if (f.progress.level >= 30) add("level_30")
        }
    }

    private fun longestStreak(loggedDays: Set<String>): Int {
        var best = 0
        for (day in loggedDays) {
            val date = LocalDate.parse(day)
            // Only count from the first day of each run.
            if (date.minusDays(1).toString() in loggedDays) continue
            best = max(best, streakForward(loggedDays, date))
        }
        return best
    }

    private fun streakForward(loggedDays: Set<String>, start: LocalDate): Int {
        var d = start
        var n = 0
        while (d.toString() in loggedDays) {
            n++
            d = d.plusDays(1)
        }
        return n
    }

    /** XP needed to go from [level] to the next one. Grows slowly so early levels come quickly. */
    fun xpForNext(level: Int) = 100 + 25 * (level - 1)

    fun titleFor(level: Int) = when {
        level >= 30 -> "Legend"
        level >= 20 -> "Hero"
        level >= 15 -> "Champion"
        level >= 10 -> "Veteran"
        level >= 5 -> "Adventurer"
        else -> "Rookie"
    }

    fun progress(
        profile: Profile,
        foods: List<FoodEntry>,
        weights: List<WeightEntry>,
        steps: Map<String, Long>,
        hero: HeroClass?,
        today: LocalDate
    ): GameProgress {
        val foodsByDay = foods.groupBy { it.date }
        val weighDays = weights.map { it.date }.toSet()
        val sortedWeights = weights.sortedBy { it.date }
        val dates = (foodsByDay.keys + weighDays + steps.filterValues { it > 0 }.keys)
            .filter { it <= today.toString() }
            .sorted()

        val days = LinkedHashMap<String, DayXp>()
        for (date in dates) {
            val weightKg = sortedWeights.lastOrNull { it.date <= date }?.kg ?: profile.startWeightKg
            days[date] = dayXp(
                date = date,
                foods = foodsByDay[date].orEmpty(),
                budget = Calc.dailyBudget(profile, weightKg),
                steps = steps[date],
                weighedIn = date in weighDays,
                streak = streakOn(foodsByDay.keys, LocalDate.parse(date)),
                finished = date < today.toString(),
                hero = hero
            )
        }
        val total = max(0, days.values.sumOf { it.total })
        var level = 1
        var left = total
        while (left >= xpForNext(level)) {
            left -= xpForNext(level)
            level++
        }
        return GameProgress(total, level, left, xpForNext(level), days)
    }

    /** Consecutive days with food logged, ending on [date]. */
    private fun streakOn(loggedDays: Set<String>, date: LocalDate): Int {
        var d = date
        var n = 0
        while (d.toString() in loggedDays) {
            n++
            d = d.minusDays(1)
        }
        return n
    }

    /**
     * XP for one day. The budget bonus waits until the day is [finished], since a day
     * that's on budget at lunchtime might not be by bedtime.
     */
    fun dayXp(
        date: String,
        foods: List<FoodEntry>,
        budget: Int,
        steps: Long?,
        weighedIn: Boolean,
        streak: Int,
        finished: Boolean,
        hero: HeroClass?
    ): DayXp {
        val lines = mutableListOf<XpLine>()
        if (foods.isNotEmpty()) {
            lines += XpLine(XpSource.LOGGING, "Logged your food", 10)
            if (foods.map { it.meal }.distinct().size >= 3) lines += XpLine(XpSource.LOGGING, "Logged 3 or more meals", 5)

            val greens = foods.count { it.color == FoodColor.GREEN }
            if (greens > 0) lines += XpLine(XpSource.GREEN, plural(greens, "green food"), min(greens, 8) * 3)

            val reds = foods.filter { it.color == FoodColor.RED }
            if (reds.isNotEmpty()) {
                val redCalories = reds.sumOf { it.calories }
                val overShare = redCalories - (budget * Calc.colorShare(FoodColor.RED)).toInt()
                var penalty = reds.size * 3
                if (overShare > 0) penalty += overShare / 10
                val label = if (overShare > 0) {
                    "${plural(reds.size, "red food")}, $overShare cal over the red share"
                } else {
                    plural(reds.size, "red food")
                }
                lines += XpLine(XpSource.RED, label, -min(penalty, 40))
            }

            if (finished) {
                val eaten = foods.sumOf { it.calories }
                when {
                    eaten < budget * 0.6 -> Unit // Probably not everything was logged.
                    eaten <= budget -> lines += XpLine(XpSource.BUDGET, "Stayed within your budget", BUDGET_XP)
                    eaten <= budget * 1.1 -> lines += XpLine(XpSource.BUDGET, "Just over budget", 10)
                    eaten > budget * 1.2 -> lines += XpLine(XpSource.BUDGET, "Well over budget", -10)
                }
            }

            if (streak >= 2) lines += XpLine(XpSource.STREAK, "$streak-day logging streak", min(streak, 10))
        }

        if (steps != null && steps > 0) {
            val stepXp = min(30, (steps / 500).toInt())
            if (stepXp > 0) lines += XpLine(XpSource.STEPS, "${"%,d".format(steps)} steps", stepXp)
            if (steps >= STEP_GOAL) lines += XpLine(XpSource.STEPS, STEP_GOAL_LABEL, 15)
        }

        if (weighedIn) lines += XpLine(XpSource.WEIGH_IN, "Weighed in", 5)

        if (hero != null) {
            val earned = lines.filter { it.source == hero.boost && it.xp > 0 }.sumOf { it.xp }
            val bonus = (earned * HERO_BONUS).roundToInt()
            if (bonus > 0) lines += XpLine(null, "${hero.label} bonus for ${hero.boost.label}", bonus)
        }
        return DayXp(date, lines)
    }

    private fun plural(n: Int, word: String) = if (n == 1) "1 $word" else "$n ${word}s"
}
