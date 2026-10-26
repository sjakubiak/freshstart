package com.freshstart.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.freshstart.app.data.AppStore
import com.freshstart.app.data.Calc
import com.freshstart.app.data.FoodColor
import com.freshstart.app.data.Lessons
import com.freshstart.app.data.Meal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(store: AppStore, onOpenLesson: (Int) -> Unit) {
    val profile = store.profile ?: return
    val today = LocalDate.now()
    var dateText by rememberSaveable { mutableStateOf(today.toString()) }
    val date = LocalDate.parse(dateText)
    var addingTo by remember { mutableStateOf<Meal?>(null) }

    val entries = store.foodsOn(dateText)
    val budget = Calc.dailyBudget(profile, store.currentWeightKg)
    val eaten = entries.sumOf { it.calories }
    val remaining = budget - eaten

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { dateText = date.minusDays(1).toString() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    val greeting = if (profile.name.isNotBlank()) "Hi ${profile.name}" else "Hi there"
                    Text(
                        if (date == today) "$greeting, here's today" else dayLabel(date, today),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        date.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { dateText = date.plusDays(1).toString() }, enabled = date < today) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
                }
            }
        }

        item { BudgetCard(budget, eaten, remaining, store.loggingStreak(today)) }

        item { ColorCard(budget, entries.groupBy { it.color }.mapValues { (_, v) -> v.sumOf { it.calories } }) }

        if (date == today) {
            item {
                val dayNumber = Calc.daysSince(profile.startDate, today)
                val index = Lessons.forDay(dayNumber)
                val lesson = Lessons.all[index]
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenLesson(index) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Today's lesson", style = MaterialTheme.typography.labelLarge)
                        Text(lesson.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(lesson.body, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (store.lessonsRead.contains(index)) "Read ✓" else "Tap to read",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Meal.entries.forEach { meal ->
            item(key = meal.name) {
                val mealEntries = entries.filter { it.meal == meal }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(meal.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text(
                                "${mealEntries.sumOf { it.calories }} cal",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = { addingTo = meal }) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Text("Add")
                            }
                        }
                        mealEntries.forEach { e ->
                            HorizontalDivider()
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ColorDot(e.color)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(e.name, style = MaterialTheme.typography.bodyLarge)
                                    if (e.servings != 1.0) {
                                        Text(
                                            "${e.servings.trimmed()} servings",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text("${e.calories}")
                                IconButton(onClick = { store.removeFood(e.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Remove ${e.name}")
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    addingTo?.let { meal ->
        AddFoodDialog(store = store, date = dateText, initialMeal = meal, onDismiss = { addingTo = null })
    }
}

private fun dayLabel(date: LocalDate, today: LocalDate) =
    if (date == today.minusDays(1)) "Yesterday" else date.format(DateTimeFormatter.ofPattern("MMM d"))

@Composable
private fun BudgetCard(budget: Int, eaten: Int, remaining: Int, streak: Int) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(if (remaining >= 0) "Calories left" else "Over budget by", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "${kotlin.math.abs(remaining)}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (remaining >= 0) MaterialTheme.colorScheme.onPrimaryContainer else FoodRed
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Budget $budget", style = MaterialTheme.typography.bodyMedium)
                    Text("Eaten $eaten", style = MaterialTheme.typography.bodyMedium)
                    if (streak > 0) Text("🔥 $streak-day streak", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (eaten.toFloat() / budget).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = if (remaining >= 0) Brand else FoodRed,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
        }
    }
}

@Composable
private fun ColorCard(budget: Int, byColor: Map<FoodColor, Int>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Food colors", style = MaterialTheme.typography.titleMedium)
            FoodColor.entries.forEach { c ->
                val target = (budget * Calc.colorShare(c)).toInt()
                val used = byColor[c] ?: 0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ColorDot(c)
                    Spacer(Modifier.width(8.dp))
                    Text(c.label, Modifier.width(56.dp), style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(
                        progress = { if (target == 0) 0f else (used.toFloat() / target).coerceIn(0f, 1f) },
                        modifier = Modifier.weight(1f).height(8.dp),
                        color = c.ui(),
                        strokeCap = StrokeCap.Round,
                        gapSize = 0.dp,
                        drawStopIndicator = {}
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "$used / $target",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (c == FoodColor.RED && used > target) FoodRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                "Aim for about 30% green, 45% yellow and 25% red. Fill up on green!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
