package com.freshstart.app.data

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Talks to Google's Gemini API with the user's own free API key.
 * Used only for scanning kitchen photos, meal ideas, and the AI coach when the user asks for it.
 */
class GeminiClient(private val apiKey: String) {
    class GeminiException(message: String) : Exception(message)

    /** Food items visible in a photo of a fridge, freezer or cupboard. */
    suspend fun foodsInPhoto(photo: Bitmap): List<String> {
        val jpeg = ByteArrayOutputStream().also { photo.compress(Bitmap.CompressFormat.JPEG, 80, it) }.toByteArray()
        val prompt = """
            This is a photo of someone's fridge, freezer, pantry or cupboard.
            List every food or drink item you can identify. Use short, generic, lowercase names
            (for example "eggs", "greek yogurt", "spinach", "chicken breast", "pasta", "peanut butter").
            Skip non-food items. Reply with JSON only: {"items": ["eggs", "milk"]}
        """.trimIndent()
        val parts = JSONArray()
            .put(JSONObject().put("text", prompt))
            .put(
                JSONObject().put(
                    "inline_data",
                    JSONObject().put("mime_type", "image/jpeg").put("data", Base64.encodeToString(jpeg, Base64.NO_WRAP))
                )
            )
        val json = generate(parts)
        val items = json.optJSONArray("items") ?: return emptyList()
        return (0 until items.length()).map { items.getString(it).trim().lowercase() }.filter { it.isNotBlank() }.distinct()
    }

    /** Healthy meal ideas built mostly from what's in the kitchen, sized to the calorie budget. */
    suspend fun mealIdeas(pantry: List<String>, dailyBudget: Int): List<MealIdea> {
        val targets = Meal.entries.joinToString(", ") { "${it.name}: about ${(dailyBudget * MealIdeas.mealShare(it)).toInt()} kcal" }
        val prompt = """
            Suggest healthy, simple weight-loss meals for one person, using mostly these foods they have at home:
            ${pantry.joinToString(", ")}.
            They can assume basic staples (salt, pepper, spices, oil, water). Calorie targets per meal: $targets.
            Give 2 ideas for each of BREAKFAST, LUNCH, DINNER and SNACK.
            For each, "color" is the Noom-style calorie density of the meal overall: GREEN (low), YELLOW (medium) or RED (high).
            Reply with JSON only:
            {"meals": [{"name": "...", "meal": "BREAKFAST", "calories": 350, "color": "GREEN",
              "ingredients": ["eggs", "spinach"], "howTo": "One or two short sentences."}]}
        """.trimIndent()
        val json = generate(JSONArray().put(JSONObject().put("text", prompt)))
        val meals = json.optJSONArray("meals") ?: return emptyList()
        return (0 until meals.length()).mapNotNull { i ->
            val o = meals.getJSONObject(i)
            runCatching {
                val ingredients = o.optJSONArray("ingredients")
                MealIdea(
                    name = o.getString("name"),
                    meal = Meal.valueOf(o.getString("meal").uppercase()),
                    calories = o.getInt("calories"),
                    color = runCatching { FoodColor.valueOf(o.getString("color").uppercase()) }.getOrDefault(FoodColor.YELLOW),
                    ingredients = if (ingredients == null) emptyList() else (0 until ingredients.length()).map { ingredients.getString(it) },
                    howTo = o.optString("howTo", ""),
                    fromAi = true
                )
            }.getOrNull()
        }
    }

    /** Personal weight-loss coaching from a summary of the user's own log (see [Coach.summary]). */
    suspend fun coachAdvice(summary: String, today: String): CoachAdvice {
        val prompt = """
            You are a warm, practical weight-loss coach in a Noom-style app that sorts food into
            green (low calorie density), yellow (medium) and red (high) foods.
            Below is a summary of one person's own log. Study it closely: weight trend vs. their planned pace,
            calories vs. budget, the color split, which meals and days run high, foods that add the most
            calories, logging gaps, and steps.
            Give 5 specific, encouraging suggestions that will help them lose weight. Each must point to
            something concrete in their data (name real foods, meals, days or numbers) and say exactly what to try,
            such as a swap for a food they eat often. Start with what is going well if anything is.
            If there is too little data, say so and suggest how to log so the advice can improve.
            Don't suggest lowering their calorie budget or eating under 1200 kcal a day, and don't give medical advice.
            Use plain, friendly language and the same weight units as the summary.
            Reply with JSON only:
            {"headline": "One-sentence overview of how they're doing.",
             "tips": [{"title": "Short tip title", "detail": "Two or three sentences."}]}
        """.trimIndent() + "\n\n" + summary
        val json = generate(JSONArray().put(JSONObject().put("text", prompt)), FAST_MODELS + MODELS)
        val tips = json.optJSONArray("tips") ?: JSONArray()
        return CoachAdvice(
            date = today,
            headline = json.optString("headline", ""),
            tips = (0 until tips.length()).mapNotNull { i ->
                val o = tips.optJSONObject(i) ?: return@mapNotNull null
                CoachTip(o.optString("title", ""), o.optString("detail", "")).takeIf { it.title.isNotBlank() || it.detail.isNotBlank() }
            }
        )
    }

    private suspend fun generate(parts: JSONArray, models: List<String> = MODELS): JSONObject = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", parts)))
            .put("generationConfig", JSONObject().put("responseMimeType", "application/json"))
            .toString()

        var lastError = "Couldn't reach Gemini."
        for (model in models) {
            val conn = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent").openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.connectTimeout = 20_000
                conn.readTimeout = 90_000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("x-goog-api-key", apiKey)
                conn.outputStream.use { it.write(body.toByteArray()) }
                val code = conn.responseCode
                val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
                when {
                    code in 200..299 -> {
                        val reply = JSONObject(text).getJSONArray("candidates").getJSONObject(0)
                            .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                        return@withContext JSONObject(reply.trim().removePrefix("```json").removePrefix("```").removeSuffix("```"))
                    }
                    code == 404 -> lastError = "Gemini model not found." // try the next model name
                    code == 400 || code == 401 || code == 403 ->
                        throw GeminiException("Gemini didn't accept the API key. Check it in Settings.")
                    // Busy or out of free quota for this model: try the next one.
                    code == 429 -> lastError = "Gemini's free limit was reached. Try again in a minute."
                    code >= 500 -> lastError = "Gemini is busy right now (error $code). Try again in a minute."
                    else -> throw GeminiException("Gemini had a problem (error $code). Try again later.")
                }
            } finally {
                conn.disconnect()
            }
        }
        throw GeminiException(lastError)
    }

    companion object {
        private val MODELS = listOf("gemini-flash-latest", "gemini-2.5-flash", "gemini-2.0-flash")

        /** Quick models that answer in seconds, for text-only requests like the coach. */
        private val FAST_MODELS = listOf("gemini-flash-lite-latest", "gemini-2.5-flash-lite")
        const val KEY_URL = "https://aistudio.google.com/apikey"
    }
}
