package com.weightcoach.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weightcoach.app.data.AppStore
import com.weightcoach.app.data.Calc

@Composable
fun SettingsScreen(store: AppStore, onRemindersChanged: (Boolean) -> Unit) {
    val profile = store.profile ?: return
    var editing by rememberSaveable { mutableStateOf(false) }

    if (editing) {
        Column(Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                IconButton(onClick = { editing = false }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Edit your plan", style = MaterialTheme.typography.titleLarge)
            }
            ProfileForm(
                existing = profile,
                submitLabel = "Save",
                onSubmit = {
                    store.saveProfile(it)
                    editing = false
                }
            )
        }
        return
    }

    val autoBudget = Calc.dailyBudget(profile.copy(calorieOverride = null), store.currentWeightKg)
    var overrideText by rememberSaveable { mutableStateOf(profile.calorieOverride?.toString() ?: "") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Your plan", style = MaterialTheme.typography.titleMedium)
                Text("Started at ${Calc.formatWeight(profile.startWeightKg, profile.useImperial)} on ${formatLongDate(java.time.LocalDate.parse(profile.startDate))}")
                Text("Goal ${Calc.formatWeight(profile.goalWeightKg, profile.useImperial)}")
                Text("Activity: ${profile.activity.label}")
                Button(onClick = { editing = true }, modifier = Modifier.padding(top = 8.dp)) { Text("Edit plan") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Daily calorie budget", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Worked out for you: $autoBudget calories. It updates as your weight goes down. " +
                        "If you'd like a different number, set it here.",
                    style = MaterialTheme.typography.bodyMedium
                )
                NumberField(overrideText, { overrideText = it }, "Custom budget (optional)", Modifier.fillMaxWidth(), decimal = false, suffix = "cal")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val v = overrideText.toIntOrNull()
                    Button(
                        enabled = v != null && v in 800..6000,
                        onClick = { store.saveProfile(profile.copy(calorieOverride = v)) }
                    ) { Text("Use custom") }
                    OutlinedButton(
                        enabled = profile.calorieOverride != null,
                        onClick = {
                            overrideText = ""
                            store.saveProfile(profile.copy(calorieOverride = null))
                        }
                    ) { Text("Use automatic") }
                }
                if (profile.calorieOverride != null) {
                    Text("Currently using your custom budget of ${profile.calorieOverride} calories.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Reminders", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Nudges at 8:00 am, 12:30 pm and 7:30 pm to weigh in and log meals.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(checked = profile.remindersOn, onCheckedChange = {
                    store.saveProfile(profile.copy(remindersOn = it))
                    onRemindersChanged(it)
                })
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Use pounds", style = MaterialTheme.typography.titleMedium)
                    Text("Off shows kilograms", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = profile.useImperial, onCheckedChange = {
                    store.saveProfile(profile.copy(useImperial = it))
                })
            }
        }

        Text(
            "Everything you log stays on this phone. WeightCoach has no account, no ads and no internet access. " +
                "It's a helper, not medical advice; check with your doctor before big diet changes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
