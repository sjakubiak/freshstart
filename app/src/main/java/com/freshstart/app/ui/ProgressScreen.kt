package com.freshstart.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freshstart.app.data.AppStore
import com.freshstart.app.data.Calc
import com.freshstart.app.data.Profile
import com.freshstart.app.data.WeightEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun ProgressScreen(store: AppStore) {
    val profile = store.profile ?: return
    val imperial = profile.useImperial
    var logging by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val current = store.currentWeightKg
    val lost = profile.startWeightKg - current
    val toGo = current - profile.goalWeightKg
    val totalToLose = profile.startWeightKg - profile.goalWeightKg

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your progress", style = MaterialTheme.typography.titleMedium)
                    Row {
                        Stat("Current", Calc.formatWeight(current, imperial), Modifier.weight(1f))
                        Stat(
                            if (lost >= 0) "Lost" else "Gained",
                            Calc.formatWeight(kotlin.math.abs(lost), imperial),
                            Modifier.weight(1f)
                        )
                        Stat("To go", Calc.formatWeight(toGo.coerceAtLeast(0.0), imperial), Modifier.weight(1f))
                    }
                    if (totalToLose > 0) {
                        val pct = (lost / totalToLose).coerceIn(0.0, 1.0)
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { pct.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(10.dp),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                            gapSize = 0.dp,
                            drawStopIndicator = {}
                        )
                        Text("${(pct * 100).toInt()}% of the way to your goal", style = MaterialTheme.typography.bodySmall)
                    }
                    val bmi = Calc.bmi(profile.heightCm, current)
                    Text("BMI %.1f".format(bmi), style = MaterialTheme.typography.bodyMedium)
                    val goalDate = Calc.projectedGoalDate(profile, current, today)
                    Text(
                        if (goalDate != null) "On track to reach ${Calc.formatWeight(profile.goalWeightKg, imperial)} around ${formatLongDate(goalDate)}"
                        else "You've reached your goal weight. Amazing work! 🎉",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Weight trend", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    WeightChart(store.weights.toList(), profile)
                }
            }
        }

        item {
            Button(onClick = { logging = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Log today's weight")
            }
        }

        item { Text("Weigh-ins", style = MaterialTheme.typography.titleMedium) }
        items(store.weights.sortedByDescending { it.date }, key = { it.date }) { w ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    LocalDate.parse(w.date).format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")),
                    Modifier.weight(1f)
                )
                Text(Calc.formatWeight(w.kg, imperial), fontWeight = FontWeight.SemiBold)
                IconButton(onClick = { store.removeWeight(w.date) }, enabled = store.weights.size > 1) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete weigh-in")
                }
            }
            HorizontalDivider()
        }
        item {
            Text(
                "Tip: weigh yourself each morning after the bathroom and before breakfast. Daily ups and downs are normal; watch the trend.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (logging) {
        LogWeightDialog(
            imperial = imperial,
            initialKg = current,
            onDismiss = { logging = false },
            onSave = { date, kg ->
                store.logWeight(date, kg)
                logging = false
            }
        )
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WeightChart(weights: List<WeightEntry>, profile: Profile) {
    val imperial = profile.useImperial
    if (weights.size < 2) {
        Text(
            "Log your weight on a couple of different days to see your trend line here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    val sorted = weights.sortedBy { it.date }
    val first = LocalDate.parse(sorted.first().date)
    val last = LocalDate.parse(sorted.last().date)
    val span = ChronoUnit.DAYS.between(first, last).coerceAtLeast(1).toFloat()
    val values = sorted.map { Calc.weightValue(it.kg, imperial) }
    val goal = Calc.weightValue(profile.goalWeightKg, imperial)
    val minV = minOf(values.min(), goal) - 1
    val maxV = values.max() + 1
    val range = (maxV - minV).coerceAtLeast(1.0)

    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val goalColor = FoodGreen
    val fmt = DateTimeFormatter.ofPattern("MMM d")

    Row {
        Column(Modifier.height(180.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(maxV.trimmed(), style = MaterialTheme.typography.labelSmall)
            Text(minV.trimmed(), style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Canvas(Modifier.fillMaxWidth().height(180.dp)) {
                fun x(date: String) = ChronoUnit.DAYS.between(first, LocalDate.parse(date)) / span * size.width
                fun y(v: Double) = ((1 - (v - minV) / range) * size.height).toFloat()

                for (i in 0..4) {
                    val gy = size.height * i / 4f
                    drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
                }
                val gy = y(goal)
                drawLine(
                    goalColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                )

                val path = Path()
                sorted.forEachIndexed { i, w ->
                    val px = x(w.date)
                    val py = y(values[i])
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                drawPath(path, lineColor, style = Stroke(width = 5f))
                sorted.forEachIndexed { i, w ->
                    drawCircle(lineColor, radius = 7f, center = Offset(x(w.date), y(values[i])))
                }
            }
            Row(Modifier.fillMaxWidth()) {
                Text(first.format(fmt), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                Text(last.format(fmt), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    Spacer(Modifier.height(4.dp))
    Text(
        "Dashed green line: your goal (${Calc.formatWeight(profile.goalWeightKg, imperial)})",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun LogWeightDialog(
    imperial: Boolean,
    initialKg: Double,
    onDismiss: () -> Unit,
    onSave: (String, Double) -> Unit
) {
    val today = LocalDate.now()
    var day by remember { mutableStateOf(today) }
    var text by remember { mutableStateOf(Calc.weightValue(initialKg, imperial).trimmed()) }
    val value = text.toDoubleOrNull()
    val kg = value?.let { if (imperial) Calc.lbToKg(it) else it }
    val valid = kg != null && kg in 30.0..350.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log weight") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = day == today, onClick = { day = today }, label = { Text("Today") })
                    FilterChip(
                        selected = day == today.minusDays(1),
                        onClick = { day = today.minusDays(1) },
                        label = { Text("Yesterday") }
                    )
                }
                NumberField(text, { text = it }, "Weight", Modifier.fillMaxWidth(), suffix = Calc.weightUnit(imperial))
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onSave(day.toString(), kg!!) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
