package com.weightcoach.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weightcoach.app.data.Activity
import com.weightcoach.app.data.Calc
import com.weightcoach.app.data.Profile
import com.weightcoach.app.data.Sex
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

private val paceLb = listOf(0.5, 1.0, 1.5, 2.0)
private val paceKg = listOf(0.25, 0.5, 0.75, 1.0)

/**
 * Collects the details needed to work out a calorie budget.
 * Used for first-time setup ([existing] == null) and for editing later.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileForm(
    existing: Profile?,
    submitLabel: String,
    onSubmit: (Profile) -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {}
) {
    val startImperial = existing?.useImperial ?: true
    var imperial by rememberSaveable { mutableStateOf(startImperial) }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var sex by rememberSaveable { mutableStateOf(existing?.sex ?: Sex.MALE) }
    var age by rememberSaveable { mutableStateOf(existing?.age?.toString() ?: "") }

    val totalIn = existing?.let { it.heightCm / Calc.CM_PER_IN }
    var heightFt by rememberSaveable { mutableStateOf(totalIn?.let { (it.roundToInt() / 12).toString() } ?: "") }
    var heightIn by rememberSaveable { mutableStateOf(totalIn?.let { (it.roundToInt() % 12).toString() } ?: "") }
    var heightCm by rememberSaveable { mutableStateOf(existing?.heightCm?.roundToInt()?.toString() ?: "") }

    fun shown(kg: Double?) = kg?.let { Calc.weightValue(it, startImperial).trimmed() } ?: ""
    var weight by rememberSaveable { mutableStateOf(shown(existing?.startWeightKg)) }
    var goal by rememberSaveable { mutableStateOf(shown(existing?.goalWeightKg)) }
    var activity by rememberSaveable { mutableStateOf(existing?.activity ?: Activity.LIGHT) }
    var paceIndex by rememberSaveable {
        mutableStateOf(existing?.let { p -> paceKg.indices.minBy { abs(paceKg[it] - p.weeklyLossKg) } } ?: 1)
    }

    fun toKg(v: String) = v.toDoubleOrNull()?.let { if (imperial) Calc.lbToKg(it) else it }

    val heightValue: Double? = if (imperial) {
        val ft = heightFt.toDoubleOrNull()
        val inch = heightIn.toDoubleOrNull() ?: 0.0
        ft?.let { (it * 12 + inch) * Calc.CM_PER_IN }
    } else heightCm.toDoubleOrNull()

    val weightKg = toKg(weight)
    val goalKg = toKg(goal)
    val ageValue = age.toIntOrNull()
    val valid = heightValue != null && heightValue in 100.0..250.0 &&
        weightKg != null && weightKg in 30.0..350.0 &&
        goalKg != null && goalKg in 30.0..350.0 &&
        ageValue != null && ageValue in 13..100

    val draft = if (valid) Profile(
        name = name.trim(),
        sex = sex,
        age = ageValue!!,
        heightCm = heightValue!!,
        startWeightKg = weightKg!!,
        goalWeightKg = goalKg!!,
        activity = activity,
        weeklyLossKg = paceKg[paceIndex],
        useImperial = imperial,
        startDate = existing?.startDate ?: LocalDate.now().toString(),
        remindersOn = existing?.remindersOn ?: true,
        calorieOverride = existing?.calorieOverride
    ) else null

    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        header()

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = imperial, onClick = {
                if (!imperial) {
                    weight = toKg(weight)?.let { Calc.kgToLb(it).trimmed() } ?: weight
                    goal = toKg(goal)?.let { Calc.kgToLb(it).trimmed() } ?: goal
                    imperial = true
                }
            }, label = { Text("lb / ft") })
            FilterChip(selected = !imperial, onClick = {
                if (imperial) {
                    weight = toKg(weight)?.trimmed() ?: weight
                    goal = toKg(goal)?.trimmed() ?: goal
                    imperial = false
                }
            }, label = { Text("kg / cm") })
        }

        OutlinedTextField(
            value = name, onValueChange = { name = it.take(30) },
            label = { Text("First name (optional)") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Sex (for the calorie formula)", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Sex.entries.forEach { s ->
                FilterChip(selected = sex == s, onClick = { sex = s }, label = { Text(s.label) })
            }
        }

        NumberField(age, { age = it }, "Age", Modifier.fillMaxWidth(), decimal = false)

        if (imperial) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(heightFt, { heightFt = it }, "Height", Modifier.weight(1f), decimal = false, suffix = "ft")
                NumberField(heightIn, { heightIn = it }, "", Modifier.weight(1f), decimal = false, suffix = "in")
            }
        } else {
            NumberField(heightCm, { heightCm = it }, "Height", Modifier.fillMaxWidth(), suffix = "cm")
        }

        val unit = Calc.weightUnit(imperial)
        NumberField(
            weight, { weight = it },
            if (existing == null) "Current weight" else "Starting weight",
            Modifier.fillMaxWidth(), suffix = unit
        )
        NumberField(goal, { goal = it }, "Goal weight", Modifier.fillMaxWidth(), suffix = unit)

        Text("How active are you day to day?", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Activity.entries.forEach { a ->
                FilterChip(selected = activity == a, onClick = { activity = a }, label = { Text(a.label) })
            }
        }

        Text("How fast do you want to lose?", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val labels = if (imperial) paceLb.map { "${it.trimmed()} lb/week" } else paceKg.map { "${it.trimmed()} kg/week" }
            labels.forEachIndexed { i, label ->
                FilterChip(selected = paceIndex == i, onClick = { paceIndex = i }, label = { Text(label) })
            }
        }
        Text(
            "1 lb (0.5 kg) a week is a steady, sustainable pace for most people.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (draft != null) {
            val budget = Calc.dailyBudget(draft.copy(calorieOverride = null), draft.startWeightKg)
            val goalDate = Calc.projectedGoalDate(draft, draft.startWeightKg, LocalDate.now())
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("Your daily calorie budget", style = MaterialTheme.typography.labelLarge)
                    Text("$budget calories", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    if (goalDate != null) {
                        Text("At this pace you could reach your goal around ${formatLongDate(goalDate)}.")
                    }
                    if (budget == Calc.minimumCalories(sex)) {
                        Text(
                            "This is the safe minimum. Pick a slower pace or add activity to lose faster.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = { draft?.let(onSubmit) },
            enabled = draft != null,
            modifier = Modifier.fillMaxWidth()
        ) { Text(submitLabel) }
        if (draft == null) {
            Text(
                "Fill in your age, height, weight and goal to continue.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun formatLongDate(d: LocalDate): String =
    d.format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy"))
