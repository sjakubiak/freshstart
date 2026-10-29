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

/**
 * Heroes are head-and-shoulders emoji so gear can be placed on them. [headTop] is where the
 * top of the head sits, as a share of the emoji's height, and [hatScale] enlarges headgear
 * for heroes that already wear a hat, so it covers it.
 */
enum class HeroClass(
    val label: String,
    val emoji: String,
    val blurb: String,
    val boost: XpSource,
    val headTop: Float = 0.03f,
    val hatScale: Float = 1f
) {
    WIZARD("Green Wizard", "🧙", "Draws power from vegetables and fruit.", XpSource.GREEN, headTop = 0f, hatScale = 1.25f),
    NINJA("Step Ninja", "🥷", "Grows stronger with every step.", XpSource.STEPS),
    HERO("Budget Hero", "🧑", "Rewarded for days spent inside the calorie budget.", XpSource.BUDGET),
    RANGER("Streak Ranger", "🧔", "Never misses a day on the trail.", XpSource.STREAK),
    FAIRY("Logging Elf", "🧝", "Loves a complete food diary.", XpSource.LOGGING, headTop = 0.04f),
    ROBOT("Scale Bot", "🤖", "Runs on regular weigh-ins.", XpSource.WEIGH_IN, headTop = 0.16f)
}

enum class GearSlot(val label: String) {
    HEAD("Headgear"),
    OUTFIT("Outfit"),
    HAND("Held item"),
    PET("Pet"),
    AURA("Aura")
}

/**
 * Something the hero can wear. Auras have no emoji: they tint the circle behind the hero
 * with [colors], given as ARGB values from the center outwards. [lift] raises headgear whose
 * emoji fills its whole square, as a share of its size, so it sits on the head, not the face.
 */
data class Gear(
    val id: String,
    val name: String,
    val slot: GearSlot,
    val level: Int,
    val emoji: String = "",
    val colors: List<Long> = emptyList(),
    val lift: Float = 0f
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

    val gear: List<Gear> = listOf(
        Gear("cap", "Sporty cap", GearSlot.HEAD, 2, "🧢"),
        Gear("chick", "Chick", GearSlot.PET, 3, "🐣"),
        Gear("tee", "Training tee", GearSlot.OUTFIT, 4, "👕"),
        Gear("carrot", "Carrot wand", GearSlot.HAND, 5, "🥕"),
        Gear("leafy", "Leafy aura", GearSlot.AURA, 6, colors = listOf(0xFFB9F6CA, 0xFF43A047)),
        Gear("vest", "Running vest", GearSlot.OUTFIT, 7, "🎽"),
        Gear("cat", "Cat", GearSlot.PET, 8, "🐱"),
        Gear("helm", "Builder's helmet", GearSlot.HEAD, 9, "⛑️"),
        Gear("dagger", "Dagger", GearSlot.HAND, 10, "🗡️"),
        Gear("sunrise", "Sunrise aura", GearSlot.AURA, 11, colors = listOf(0xFFFFE082, 0xFFFF7043)),
        Gear("gi", "Dojo gi", GearSlot.OUTFIT, 12, "🥋"),
        Gear("dog", "Dog", GearSlot.PET, 13, "🐶"),
        Gear("tophat", "Top hat", GearSlot.HEAD, 14, "🎩"),
        Gear("bow", "Bow", GearSlot.HAND, 15, "🏹"),
        Gear("ocean", "Ocean aura", GearSlot.AURA, 16, colors = listOf(0xFFB3E5FC, 0xFF1E88E5)),
        Gear("coat", "Adventurer's coat", GearSlot.OUTFIT, 17, "🧥"),
        Gear("fox", "Fox", GearSlot.PET, 18, "🦊"),
        Gear("wand", "Magic wand", GearSlot.HAND, 19, "🪄"),
        Gear("crown", "Crown", GearSlot.HEAD, 20, "👑"),
        Gear("owl", "Owl", GearSlot.PET, 22, "🦉"),
        Gear("ember", "Ember aura", GearSlot.AURA, 23, colors = listOf(0xFFFFCC80, 0xFFD84315)),
        Gear("shield", "Shield", GearSlot.HAND, 24, "🛡️"),
        Gear("robe", "Royal robe", GearSlot.OUTFIT, 25, "👘"),
        Gear("unicorn", "Unicorn", GearSlot.PET, 27, "🦄"),
        Gear("swords", "Twin swords", GearSlot.HAND, 28, "⚔️"),
        Gear("golden", "Golden aura", GearSlot.AURA, 30, colors = listOf(0xFFFFF59D, 0xFFFFB300)),
        Gear("dragon", "Dragon", GearSlot.PET, 30, "🐉"),
        Gear("trident", "Trident", GearSlot.HAND, 35, "🔱"),
        Gear("cosmic", "Cosmic aura", GearSlot.AURA, 40, colors = listOf(0xFFE1BEE7, 0xFF4527A0)),
        Gear("star", "Star crown", GearSlot.HEAD, 40, "🌟", lift = 0.3f)
    )

    fun gearById(id: String?): Gear? = gear.firstOrNull { it.id == id }

    /** Gear that becomes available when going from level [from] up to level [to]. */
    fun unlockedBetween(from: Int, to: Int) = gear.filter { it.level in (from + 1)..to }

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
                    eaten <= budget -> lines += XpLine(XpSource.BUDGET, "Stayed within your budget", 25)
                    eaten <= budget * 1.1 -> lines += XpLine(XpSource.BUDGET, "Just over budget", 10)
                    eaten > budget * 1.2 -> lines += XpLine(XpSource.BUDGET, "Well over budget", -10)
                }
            }

            if (streak >= 2) lines += XpLine(XpSource.STREAK, "$streak-day logging streak", min(streak, 10))
        }

        if (steps != null && steps > 0) {
            val stepXp = min(30, (steps / 500).toInt())
            if (stepXp > 0) lines += XpLine(XpSource.STEPS, "${"%,d".format(steps)} steps", stepXp)
            if (steps >= STEP_GOAL) lines += XpLine(XpSource.STEPS, "Hit 10,000 steps", 15)
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
