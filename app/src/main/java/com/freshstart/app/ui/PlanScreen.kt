package com.freshstart.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freshstart.app.data.AppStore
import com.freshstart.app.data.Calc
import com.freshstart.app.data.FoodEntry
import com.freshstart.app.data.GeminiClient
import com.freshstart.app.data.Meal
import com.freshstart.app.data.MealIdea
import com.freshstart.app.data.MealIdeas
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Picks a meal plan for today that fits the calorie budget. One tap logs a meal. */
@Composable
fun PlanScreen(store: AppStore) {
    val profile = store.profile ?: return
    val today = LocalDate.now()
    val todayText = today.toString()
    val budget = Calc.dailyBudget(profile, store.currentWeightKg)
    val eaten = store.foodsOn(todayText).sumOf { it.calories }
    val swaps = remember { mutableStateMapOf<Meal, Int>() }
    var showKitchen by remember { mutableStateOf(false) }
    var loadingIdeas by remember { mutableStateOf(false) }
    var ideasMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("What to eat today", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "A plan that fits your $budget calorie budget. Tap \"I ate this\" to log a meal, or \"Swap\" for another idea. " +
                        "${(budget - eaten).coerceAtLeast(0)} calories left today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("My kitchen", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (store.pantry.isEmpty()) "Tell FreshStart what you usually have, or snap a photo of your fridge, and the plan will use those foods."
                        else "${store.pantry.size} foods. The plan favors meals you can make with them.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { showKitchen = true }) {
                            Text(if (store.pantry.isEmpty()) "Set up my kitchen" else "Update my kitchen")
                        }
                        if (store.geminiKey.isNotBlank() && store.pantry.isNotEmpty()) {
                            OutlinedButton(enabled = !loadingIdeas, onClick = {
                                loadingIdeas = true
                                ideasMessage = null
                                scope.launch {
                                    val result = runCatching {
                                        GeminiClient(store.geminiKey).mealIdeas(store.pantry.toList(), budget)
                                    }
                                    loadingIdeas = false
                                    result.onSuccess { ideas ->
                                        if (ideas.isEmpty()) {
                                            ideasMessage = "No ideas came back. Try again."
                                        } else {
                                            store.setAiIdeas(ideas)
                                            swaps.clear()
                                            ideasMessage = "Added ${ideas.size} new ideas made from your kitchen ✨"
                                        }
                                    }.onFailure { ideasMessage = friendlyError(it) }
                                }
                            }) {
                                if (loadingIdeas) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                else Text("New AI ideas")
                            }
                        }
                    }
                    ideasMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }

        Meal.entries.forEach { meal ->
            item(key = meal.name) {
                val target = (budget * MealIdeas.mealShare(meal)).toInt()
                val options = MealIdeas.ranked(meal, target, store.pantry, today, store.aiIdeas)
                val idea = options[(swaps[meal] ?: 0) % options.size]
                val logged = store.foodsOn(todayText).filter { it.meal == meal }
                MealCard(
                    meal = meal,
                    idea = idea,
                    target = target,
                    have = MealIdeas.pantryMatches(idea, store.pantry),
                    loggedCalories = logged.sumOf { it.calories },
                    loggedThis = logged.any { it.name == idea.name },
                    onSwap = { swaps[meal] = (swaps[meal] ?: 0) + 1 },
                    onAte = {
                        store.addFood(
                            FoodEntry(
                                id = System.currentTimeMillis(),
                                date = todayText,
                                meal = meal,
                                name = idea.name,
                                calories = idea.calories,
                                color = idea.color,
                                servings = 1.0
                            )
                        )
                    }
                )
            }
        }

        item {
            Text(
                "Calories are estimates for the portions described. Ate something different? Log it from the Today tab.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showKitchen) KitchenDialog(store, onDismiss = { showKitchen = false })
}

@Composable
private fun MealCard(
    meal: Meal,
    idea: MealIdea,
    target: Int,
    have: List<String>,
    loggedCalories: Int,
    loggedThis: Boolean,
    onSwap: () -> Unit,
    onAte: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(meal.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                Text("about $target cal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorDot(idea.color)
                Spacer(Modifier.width(8.dp))
                Text(idea.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${idea.calories} cal", style = MaterialTheme.typography.bodyMedium)
            }
            if (idea.fromAi) Text("✨ Made from your kitchen", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(idea.howTo, style = MaterialTheme.typography.bodyMedium)
            if (have.isNotEmpty()) {
                Text(
                    "You have: ${have.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (loggedCalories > 0) {
                Text(
                    "Logged for ${meal.label.lowercase()} so far: $loggedCalories cal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAte, enabled = !loggedThis) {
                    if (loggedThis) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Logged")
                    } else Text("I ate this")
                }
                OutlinedButton(onClick = onSwap) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Swap")
                }
            }
        }
    }
}

fun friendlyError(t: Throwable): String = when (t) {
    is GeminiClient.GeminiException -> t.message ?: "Something went wrong."
    is java.io.IOException -> "Couldn't connect. Check your internet connection and try again."
    else -> "Something went wrong: ${t.message ?: t.javaClass.simpleName}"
}
