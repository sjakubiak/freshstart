package com.weightcoach.app.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.roundToInt

enum class FoodColor(val label: String, val hint: String) {
    GREEN("Green", "Low calorie density: eat freely (veg, fruit, broth soups, non-fat dairy)"),
    YELLOW("Yellow", "Medium density: enjoy in moderation (lean meat, grains, beans, eggs)"),
    RED("Red", "High density: smaller portions (oils, nuts, sweets, fried food, cheese)")
}

enum class Sex(val label: String) { MALE("Male"), FEMALE("Female") }

enum class Activity(val factor: Double, val label: String) {
    SEDENTARY(1.2, "Mostly sitting"),
    LIGHT(1.375, "Lightly active"),
    MODERATE(1.55, "Moderately active"),
    VERY(1.725, "Very active")
}

enum class Meal(val label: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACK("Snacks")
}

data class Profile(
    val name: String,
    val sex: Sex,
    val age: Int,
    val heightCm: Double,
    val startWeightKg: Double,
    val goalWeightKg: Double,
    val activity: Activity,
    val weeklyLossKg: Double,
    val useImperial: Boolean,
    val startDate: String,
    val remindersOn: Boolean = true,
    val calorieOverride: Int? = null
)

data class FoodEntry(
    val id: Long,
    val date: String,
    val meal: Meal,
    val name: String,
    val calories: Int,
    val color: FoodColor,
    val servings: Double
)

data class WeightEntry(val date: String, val kg: Double)

/** A food you can pick from the list. [calories] is per [serving]. */
data class FoodItem(
    val name: String,
    val serving: String,
    val calories: Int,
    val color: FoodColor,
    val custom: Boolean = false
)

object Calc {
    const val KG_PER_LB = 0.45359237
    const val CM_PER_IN = 2.54
    private const val KCAL_PER_KG_FAT = 7700.0

    fun lbToKg(lb: Double) = lb * KG_PER_LB
    fun kgToLb(kg: Double) = kg / KG_PER_LB

    /** Mifflin-St Jeor resting metabolic rate. */
    fun bmr(p: Profile, weightKg: Double): Double {
        val base = 10 * weightKg + 6.25 * p.heightCm - 5 * p.age
        return if (p.sex == Sex.MALE) base + 5 else base - 161
    }

    fun minimumCalories(sex: Sex) = if (sex == Sex.MALE) 1500 else 1200

    fun dailyBudget(p: Profile, currentKg: Double): Int {
        p.calorieOverride?.let { return it }
        val maintenance = bmr(p, currentKg) * p.activity.factor
        val deficit = p.weeklyLossKg * KCAL_PER_KG_FAT / 7
        val target = max(minimumCalories(p.sex).toDouble(), maintenance - deficit)
        return ((target / 10).roundToInt() * 10)
    }

    /** Noom-style suggested share of the day's calories for each color. */
    fun colorShare(color: FoodColor) = when (color) {
        FoodColor.GREEN -> 0.30
        FoodColor.YELLOW -> 0.45
        FoodColor.RED -> 0.25
    }

    /** Calorie density in kcal per gram decides the color. */
    fun colorForDensity(kcal: Double, grams: Double): FoodColor {
        val d = kcal / grams
        return when {
            d < 1.0 -> FoodColor.GREEN
            d <= 2.4 -> FoodColor.YELLOW
            else -> FoodColor.RED
        }
    }

    fun bmi(heightCm: Double, kg: Double): Double {
        val m = heightCm / 100
        return kg / (m * m)
    }

    /** Estimated date the goal is reached at the chosen pace, or null if already there. */
    fun projectedGoalDate(p: Profile, currentKg: Double, today: LocalDate): LocalDate? {
        val toGo = currentKg - p.goalWeightKg
        if (toGo <= 0 || p.weeklyLossKg <= 0) return null
        val days = (toGo / p.weeklyLossKg * 7).roundToInt().toLong()
        return today.plusDays(days)
    }

    fun daysSince(start: String, today: LocalDate): Long =
        ChronoUnit.DAYS.between(LocalDate.parse(start), today)

    fun formatWeight(kg: Double, imperial: Boolean): String =
        if (imperial) "%.1f lb".format(kgToLb(kg)) else "%.1f kg".format(kg)

    fun weightValue(kg: Double, imperial: Boolean): Double = if (imperial) kgToLb(kg) else kg

    fun weightUnit(imperial: Boolean) = if (imperial) "lb" else "kg"
}
