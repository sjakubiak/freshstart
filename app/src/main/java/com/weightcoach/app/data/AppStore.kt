package com.weightcoach.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** All app data, kept in one JSON file on the phone. Nothing leaves the device. */
class AppStore(context: Context) {
    private val file = File(context.filesDir, "weightcoach.json")

    var profile by mutableStateOf<Profile?>(null)
        private set
    val foods = mutableStateListOf<FoodEntry>()
    val weights = mutableStateListOf<WeightEntry>()
    val customFoods = mutableStateListOf<FoodItem>()
    val lessonsRead = mutableStateListOf<Int>()

    init {
        load()
    }

    val currentWeightKg: Double
        get() = weights.maxByOrNull { it.date }?.kg ?: profile?.startWeightKg ?: 0.0

    fun foodsOn(date: String) = foods.filter { it.date == date }

    fun saveProfile(p: Profile) {
        val isNew = profile == null
        profile = p
        if (isNew && weights.none { it.date == p.startDate }) {
            weights.add(WeightEntry(p.startDate, p.startWeightKg))
        }
        save()
    }

    fun addFood(entry: FoodEntry) {
        foods.add(entry)
        save()
    }

    fun removeFood(id: Long) {
        foods.removeAll { it.id == id }
        save()
    }

    fun addCustomFood(item: FoodItem) {
        customFoods.removeAll { it.name.equals(item.name, ignoreCase = true) }
        customFoods.add(item.copy(custom = true))
        save()
    }

    fun removeCustomFood(item: FoodItem) {
        customFoods.remove(item)
        save()
    }

    /** One weigh-in per day; logging again replaces that day's value. */
    fun logWeight(date: String, kg: Double) {
        weights.removeAll { it.date == date }
        weights.add(WeightEntry(date, kg))
        weights.sortBy { it.date }
        save()
    }

    fun removeWeight(date: String) {
        weights.removeAll { it.date == date }
        save()
    }

    fun markLessonRead(index: Int, read: Boolean) {
        lessonsRead.remove(index)
        if (read) lessonsRead.add(index)
        save()
    }

    /** Consecutive days, ending today or yesterday, with at least one food logged. */
    fun loggingStreak(today: java.time.LocalDate): Int {
        val days = foods.map { it.date }.toSet()
        var day = if (days.contains(today.toString())) today else today.minusDays(1)
        var streak = 0
        while (days.contains(day.toString())) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }

    private fun load() {
        if (!file.exists()) return
        try {
            val root = JSONObject(file.readText())
            root.optJSONObject("profile")?.let { profile = profileFrom(it) }
            root.optJSONArray("foods")?.forEachObject { o ->
                foods.add(
                    FoodEntry(
                        id = o.getLong("id"),
                        date = o.getString("date"),
                        meal = Meal.valueOf(o.getString("meal")),
                        name = o.getString("name"),
                        calories = o.getInt("calories"),
                        color = FoodColor.valueOf(o.getString("color")),
                        servings = o.optDouble("servings", 1.0)
                    )
                )
            }
            root.optJSONArray("weights")?.forEachObject { o ->
                weights.add(WeightEntry(o.getString("date"), o.getDouble("kg")))
            }
            weights.sortBy { it.date }
            root.optJSONArray("customFoods")?.forEachObject { o ->
                customFoods.add(
                    FoodItem(
                        name = o.getString("name"),
                        serving = o.optString("serving", "1 serving"),
                        calories = o.getInt("calories"),
                        color = FoodColor.valueOf(o.getString("color")),
                        custom = true
                    )
                )
            }
            root.optJSONArray("lessonsRead")?.let { arr ->
                for (i in 0 until arr.length()) lessonsRead.add(arr.getInt(i))
            }
        } catch (e: Exception) {
            // Keep a copy of an unreadable file rather than silently losing it.
            runCatching {
                file.copyTo(File(file.parentFile, "weightcoach-corrupt-${System.currentTimeMillis()}.json"), overwrite = true)
            }
        }
    }

    private fun save() {
        val root = JSONObject()
        profile?.let { root.put("profile", profileTo(it)) }
        root.put("foods", JSONArray().apply {
            foods.forEach { e ->
                put(JSONObject().apply {
                    put("id", e.id)
                    put("date", e.date)
                    put("meal", e.meal.name)
                    put("name", e.name)
                    put("calories", e.calories)
                    put("color", e.color.name)
                    put("servings", e.servings)
                })
            }
        })
        root.put("weights", JSONArray().apply {
            weights.forEach { w -> put(JSONObject().put("date", w.date).put("kg", w.kg)) }
        })
        root.put("customFoods", JSONArray().apply {
            customFoods.forEach { c ->
                put(JSONObject().apply {
                    put("name", c.name)
                    put("serving", c.serving)
                    put("calories", c.calories)
                    put("color", c.color.name)
                })
            }
        })
        root.put("lessonsRead", JSONArray().apply { lessonsRead.forEach { put(it) } })

        // Write to a temp file first so a crash mid-write can't corrupt the data.
        val tmp = File(file.parentFile, "weightcoach.json.tmp")
        tmp.writeText(root.toString())
        if (!tmp.renameTo(file)) {
            file.writeText(root.toString())
            tmp.delete()
        }
    }

    private fun profileTo(p: Profile) = JSONObject().apply {
        put("name", p.name)
        put("sex", p.sex.name)
        put("age", p.age)
        put("heightCm", p.heightCm)
        put("startWeightKg", p.startWeightKg)
        put("goalWeightKg", p.goalWeightKg)
        put("activity", p.activity.name)
        put("weeklyLossKg", p.weeklyLossKg)
        put("useImperial", p.useImperial)
        put("startDate", p.startDate)
        put("remindersOn", p.remindersOn)
        p.calorieOverride?.let { put("calorieOverride", it) }
    }

    private fun profileFrom(o: JSONObject) = Profile(
        name = o.optString("name", ""),
        sex = Sex.valueOf(o.getString("sex")),
        age = o.getInt("age"),
        heightCm = o.getDouble("heightCm"),
        startWeightKg = o.getDouble("startWeightKg"),
        goalWeightKg = o.getDouble("goalWeightKg"),
        activity = Activity.valueOf(o.getString("activity")),
        weeklyLossKg = o.getDouble("weeklyLossKg"),
        useImperial = o.optBoolean("useImperial", true),
        startDate = o.getString("startDate"),
        remindersOn = o.optBoolean("remindersOn", true),
        calorieOverride = if (o.has("calorieOverride")) o.getInt("calorieOverride") else null
    )

    private inline fun JSONArray.forEachObject(block: (JSONObject) -> Unit) {
        for (i in 0 until length()) block(getJSONObject(i))
    }
}
