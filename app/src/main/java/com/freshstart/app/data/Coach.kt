package com.freshstart.app.data

import java.time.LocalDate
import kotlin.math.roundToInt

data class CoachTip(val title: String, val detail: String)

/** Advice from the AI coach, kept until the user asks again. */
data class CoachAdvice(val date: String, val headline: String, val tips: List<CoachTip>)

/** Builds the plain-text summary of the user's log that the AI coach reads. */
object Coach {
    /** How many days of food and steps the coach looks at. */
    const val DAYS = 28

    fun summary(
        p: Profile,
        currentKg: Double,
        foods: List<FoodEntry>,
        weights: List<WeightEntry>,
        steps: Map<String, Long>,
        today: LocalDate
    ): String {
        val imperial = p.useImperial
        fun w(kg: Double) = Calc.formatWeight(kg, imperial)
        val budget = Calc.dailyBudget(p, currentKg)
        val from = today.minusDays(DAYS.toLong() - 1)
        val recent = foods.filter { LocalDate.parse(it.date) >= from }
        val days = (0 until DAYS).map { from.plusDays(it.toLong()).toString() }
        val loggedDays = days.count { d -> recent.any { it.date == d } }

        return buildString {
            appendLine("Today is $today.")
            appendLine("PROFILE: ${p.sex.label}, age ${p.age}, height ${(p.heightCm / Calc.CM_PER_IN).roundToInt()} in (${p.heightCm.roundToInt()} cm), activity: ${p.activity.label}.")
            appendLine("Started ${p.startDate} at ${w(p.startWeightKg)}. Goal ${w(p.goalWeightKg)}. Current ${w(currentKg)}. BMI %.1f.".format(Calc.bmi(p.heightCm, currentKg)))
            appendLine("Planned pace: ${w(p.weeklyLossKg)} per week. Daily calorie budget: $budget kcal${if (p.calorieOverride != null) " (set by the user)" else ""}.")
            appendLine("Suggested color split: green ${(Calc.colorShare(FoodColor.GREEN) * 100).toInt()}%, yellow ${(Calc.colorShare(FoodColor.YELLOW) * 100).toInt()}%, red ${(Calc.colorShare(FoodColor.RED) * 100).toInt()}% of calories.")
            appendLine()
            appendLine("WEIGH-INS (date: weight):")
            val ws = weights.sortedBy { it.date }.takeLast(60)
            if (ws.isEmpty()) appendLine("none") else ws.forEach { appendLine("${it.date}: ${w(it.kg)}") }
            appendLine()
            appendLine("DAILY LOG, last $DAYS days ($loggedDays days with food logged). date: total kcal | green/yellow/red kcal | breakfast/lunch/dinner/snacks kcal | steps")
            days.forEach { d ->
                val day = recent.filter { it.date == d }
                val s = steps[d]?.let { " | $it steps" } ?: ""
                if (day.isEmpty()) {
                    appendLine("$d: nothing logged$s")
                } else {
                    fun c(e: FoodEntry) = (e.calories * e.servings).roundToInt()
                    val byColor = FoodColor.entries.joinToString("/") { col -> day.filter { it.color == col }.sumOf(::c).toString() }
                    val byMeal = Meal.entries.joinToString("/") { m -> day.filter { it.meal == m }.sumOf(::c).toString() }
                    appendLine("$d: ${day.sumOf(::c)} | $byColor | $byMeal$s")
                }
            }
            appendLine()
            appendLine("FOODS EATEN in the last $DAYS days (name, color, times, total kcal), most calories first:")
            recent.groupBy { it.name.lowercase() to it.color }
                .map { (key, list) -> Triple(key, list.size, list.sumOf { (it.calories * it.servings).roundToInt() }) }
                .sortedByDescending { it.third }
                .take(40)
                .forEach { (key, n, kcal) -> appendLine("${key.first}, ${key.second.label.lowercase()}, ${n}x, $kcal kcal") }
            appendLine()
            appendLine("MEAL DETAIL, last 7 days:")
            recent.filter { LocalDate.parse(it.date) > today.minusDays(7) }
                .sortedWith(compareBy({ it.date }, { it.meal.ordinal }))
                .forEach { appendLine("${it.date} ${it.meal.label}: ${it.name} (${(it.calories * it.servings).roundToInt()} kcal, ${it.color.label.lowercase()})") }
        }
    }
}
