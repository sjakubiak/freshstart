package com.freshstart.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * All app data, kept in one JSON file on the phone. When signed in, [CloudSync]
 * also backs it up to the user's account. Health Connect sharing is opt-in.
 */
class AppStore(context: Context) {
    private val file = File(context.filesDir, "freshstart.json")
    val health = HealthSync(context.applicationContext)
    private val scope = MainScope()

    /** Whether weigh-ins and meals are shared with Health Connect. */
    var healthOn by mutableStateOf(false)
        private set

    var profile by mutableStateOf<Profile?>(null)
        private set
    val foods = mutableStateListOf<FoodEntry>()
    val weights = mutableStateListOf<WeightEntry>()
    val customFoods = mutableStateListOf<FoodItem>()
    val lessonsRead = mutableStateListOf<Int>()

    /** Foods you usually have at home, used to tailor meal suggestions. */
    val pantry = mutableStateListOf<String>()

    /** Meal ideas Gemini suggested from your kitchen. */
    val aiIdeas = mutableStateListOf<MealIdea>()

    /** Your own free Gemini API key, for photo scanning. Blank means the feature is off. */
    var geminiKey by mutableStateOf("")
        private set

    /** Called after every change, so the account backup can upload it. */
    var onChanged: (() -> Unit)? = null

    /** When the data last changed, in milliseconds. Used to decide which copy is newer. */
    var updatedAt = 0L
        private set

    /** The user chose to skip signing in and keep data only on this phone. */
    var useWithoutAccount by mutableStateOf(false)
        private set

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
        sync { writeFood(entry) }
    }

    fun removeFood(id: Long) {
        foods.removeAll { it.id == id }
        save()
        sync { deleteFood(id) }
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
        putWeight(date, kg)
        save()
        sync { writeWeight(WeightEntry(date, kg)) }
    }

    private fun putWeight(date: String, kg: Double) {
        weights.removeAll { it.date == date }
        weights.add(WeightEntry(date, kg))
        weights.sortBy { it.date }
    }

    fun removeWeight(date: String) {
        weights.removeAll { it.date == date }
        save()
        sync { deleteWeight(date) }
    }

    /** Turns sharing on or off. Turning it on sends everything logged so far and pulls in weigh-ins. */
    fun setHealthEnabled(on: Boolean) {
        healthOn = on
        save()
        if (on) {
            val allWeights = weights.toList()
            val allFoods = foods.toList()
            sync {
                allWeights.forEach { writeWeight(it) }
                allFoods.forEach { writeFood(it) }
            }
            importFromHealth()
        }
    }

    /** Adds weigh-ins from scales and other apps for days FreshStart has no entry for. */
    fun importFromHealth(onDone: (Int) -> Unit = {}) {
        if (!healthOn) return
        scope.launch {
            val incoming = runCatching { health.readWeights() }.getOrDefault(emptyMap())
            val known = weights.map { it.date }.toSet()
            val added = incoming.filterKeys { it !in known }
            added.forEach { (date, kg) -> putWeight(date, kg) }
            if (added.isNotEmpty()) save()
            onDone(added.size)
        }
    }

    private fun sync(block: suspend HealthSync.() -> Unit) {
        if (!healthOn) return
        scope.launch { runCatching { health.block() } }
    }

    fun addPantryItems(items: Collection<String>): Int {
        val existing = pantry.map { it.lowercase() }.toSet()
        val fresh = items.map { it.trim().lowercase() }.filter { it.isNotBlank() && it !in existing }.distinct()
        pantry.addAll(fresh)
        pantry.sort()
        if (fresh.isNotEmpty()) save()
        return fresh.size
    }

    fun removePantryItem(item: String) {
        pantry.remove(item)
        save()
    }

    fun clearPantry() {
        pantry.clear()
        save()
    }

    fun setAiIdeas(ideas: List<MealIdea>) {
        aiIdeas.clear()
        aiIdeas.addAll(ideas)
        save()
    }

    fun updateGeminiKey(key: String) {
        geminiKey = key.trim()
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
            applyJson(JSONObject(file.readText()))
        } catch (e: Exception) {
            // Keep a copy of an unreadable file rather than silently losing it.
            runCatching {
                file.copyTo(File(file.parentFile, "freshstart-corrupt-${System.currentTimeMillis()}.json"), overwrite = true)
            }
        }
    }

    private fun applyJson(root: JSONObject) {
        profile = null
        foods.clear()
        weights.clear()
        customFoods.clear()
        pantry.clear()
        aiIdeas.clear()
        lessonsRead.clear()
        updatedAt = root.optLong("updatedAt", 0L)
        useWithoutAccount = root.optBoolean("useWithoutAccount", false)
        run {
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
            healthOn = root.optBoolean("healthConnect", false)
            geminiKey = root.optString("geminiKey", "")
            root.optJSONArray("pantry")?.let { arr ->
                for (i in 0 until arr.length()) pantry.add(arr.getString(i))
            }
            root.optJSONArray("aiIdeas")?.forEachObject { o ->
                val ing = o.optJSONArray("ingredients")
                aiIdeas.add(
                    MealIdea(
                        name = o.getString("name"),
                        meal = Meal.valueOf(o.getString("meal")),
                        calories = o.getInt("calories"),
                        color = FoodColor.valueOf(o.getString("color")),
                        ingredients = if (ing == null) emptyList() else (0 until ing.length()).map { ing.getString(it) },
                        howTo = o.optString("howTo", ""),
                        fromAi = true
                    )
                )
            }
            root.optJSONArray("lessonsRead")?.let { arr ->
                for (i in 0 until arr.length()) lessonsRead.add(arr.getInt(i))
            }
        }
    }

    fun chooseWithoutAccount(value: Boolean) {
        useWithoutAccount = value
        writeFile(toJson())
    }

    /** Everything worth backing up. Phone-specific settings and the Gemini key stay on the phone. */
    fun toCloudJson(): JSONObject = toJson().apply {
        remove("geminiKey")
        remove("healthConnect")
        remove("useWithoutAccount")
    }

    /** Replaces local data with the account's copy, keeping phone-specific settings. */
    fun replaceFromCloud(root: JSONObject) {
        val key = geminiKey
        val health = healthOn
        applyJson(root)
        geminiKey = key
        healthOn = health
        useWithoutAccount = false
        writeFile(toJson())
    }

    /** Wipes everything on this phone, for signing out or after deleting the account. */
    fun clearAll() {
        applyJson(JSONObject())
        geminiKey = ""
        healthOn = false
        file.delete()
    }

    private fun save() {
        updatedAt = System.currentTimeMillis()
        writeFile(toJson())
        onChanged?.invoke()
    }

    private fun writeFile(root: JSONObject) {
        // Write to a temp file first so a crash mid-write can't corrupt the data.
        val tmp = File(file.parentFile, "freshstart.json.tmp")
        tmp.writeText(root.toString())
        if (!tmp.renameTo(file)) {
            file.writeText(root.toString())
            tmp.delete()
        }
    }

    private fun toJson(): JSONObject {
        val root = JSONObject()
        root.put("updatedAt", updatedAt)
        root.put("useWithoutAccount", useWithoutAccount)
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
        root.put("healthConnect", healthOn)
        root.put("geminiKey", geminiKey)
        root.put("pantry", JSONArray().apply { pantry.forEach { put(it) } })
        root.put("aiIdeas", JSONArray().apply {
            aiIdeas.forEach { idea ->
                put(JSONObject().apply {
                    put("name", idea.name)
                    put("meal", idea.meal.name)
                    put("calories", idea.calories)
                    put("color", idea.color.name)
                    put("ingredients", JSONArray().apply { idea.ingredients.forEach { put(it) } })
                    put("howTo", idea.howTo)
                })
            }
        })
        return root
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
