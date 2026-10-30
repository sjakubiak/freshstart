package com.freshstart.app.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
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
import com.freshstart.app.data.Account
import kotlinx.coroutines.launch
import com.freshstart.app.data.AppStore
import com.freshstart.app.data.Calc

@Composable
fun SettingsScreen(store: AppStore, account: Account, onRemindersChanged: (Boolean) -> Unit) {
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

        if (account.configured) AccountCard(store, account)

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

        HealthCard(store)

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
            "Without an account, everything stays on this phone. With an account, your data is backed up privately to your account. FreshStart has no ads and never sells data. " +
                "If you connect Health Connect, data is shared only with the health apps on this phone. " +
                "If you add a Gemini key, only the kitchen photos you scan, your kitchen list (for meal ideas) and, when you ask the AI coach, a summary of your log are sent to Google. " +
                "It's a helper, not medical advice; check with your doctor before big diet changes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HealthCard(store: AppStore) {
    val context = LocalContext.current
    val status = remember { store.health.status() }
    var granted by remember { mutableStateOf<Set<String>>(emptySet()) }
    var message by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { result ->
        granted = result
        if (result.isNotEmpty()) {
            store.setHealthEnabled(true)
            message = "Connected. Your weigh-ins and meals will now be shared."
        } else {
            message = "No access was given. You can allow it in Health Connect settings."
        }
    }

    LaunchedEffect(Unit) {
        granted = runCatching { store.health.grantedPermissions() }.getOrDefault(emptySet())
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Health Connect", style = MaterialTheme.typography.titleMedium)
            Text(
                "Link with your Pixel's health apps (Fitbit, Pixel Watch, smart scales). " +
                    "Weigh-ins come in automatically, your weigh-ins and meal calories go out, " +
                    "and your steps show on the Today screen.",
                style = MaterialTheme.typography.bodySmall
            )
            when {
                status == HealthConnectClient.SDK_UNAVAILABLE ->
                    Text("Health Connect isn't available on this phone.", style = MaterialTheme.typography.bodyMedium)

                status == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                    Button(onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }) { Text("Install or update Health Connect") }

                store.healthOn && granted.isNotEmpty() -> {
                    Text("✓ Connected", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            store.importFromHealth { n ->
                                message = if (n == 0) "Up to date. No new weigh-ins found." else "Added $n weigh-in${if (n == 1) "" else "s"}."
                            }
                        }) { Text("Sync now") }
                        OutlinedButton(onClick = { launcher.launch(store.health.permissions) }) { Text("Permissions") }
                    }
                    TextButton(onClick = {
                        store.setHealthEnabled(false)
                        message = "Disconnected. Nothing new will be shared."
                    }) { Text("Turn off sharing") }
                }

                else -> {
                    Button(onClick = { launcher.launch(store.health.permissions) }) { Text("Connect") }
                    TextButton(onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }) { Text("Open Health Connect settings") }
                }
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun AccountCard(store: AppStore, account: Account) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val user = account.user

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Account", style = MaterialTheme.typography.titleMedium)
            if (user == null) {
                Text(
                    "You're using FreshStart without an account, so your data is only on this phone.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = { store.chooseWithoutAccount(false) }) { Text("Sign in to back up") }
            } else {
                Text("Signed in as ${user.email ?: "your Google account"}", style = MaterialTheme.typography.bodyMedium)
                account.lastSyncMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = !busy, onClick = {
                        busy = true
                        scope.launch { account.signOut(); busy = false }
                    }) { Text("Sign out") }
                    TextButton(enabled = !busy, onClick = { confirmDelete = true }) {
                        Text("Delete account", color = MaterialTheme.colorScheme.error)
                    }
                }
                Text(
                    "Signing out removes your data from this phone. It stays safe in your account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }

    if (confirmDelete) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete your account?") },
            text = { Text("This permanently deletes your account and everything you've logged, on this phone and in the cloud. It can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    busy = true
                    error = null
                    scope.launch {
                        runCatching { account.deleteAccount() }
                            .onFailure { error = it.message ?: "Couldn't delete the account. Please try again." }
                        busy = false
                    }
                }) { Text("Delete everything", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}
