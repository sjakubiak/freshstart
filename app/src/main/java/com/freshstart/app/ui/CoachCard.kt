package com.freshstart.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.freshstart.app.data.AppStore
import com.freshstart.app.data.Coach
import com.freshstart.app.data.GeminiClient
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Asks Gemini to read the user's log and suggest how to lose weight. */
@Composable
fun CoachCard(store: AppStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var keyText by remember { mutableStateOf("") }
    val advice = store.coachAdvice

    fun ask() {
        val today = LocalDate.now()
        val summary = store.coachSummary(today) ?: return
        loading = true
        message = null
        scope.launch {
            val result = runCatching { GeminiClient(store.geminiKey).coachAdvice(summary, today.toString()) }
            loading = false
            result.onSuccess {
                if (it.tips.isEmpty()) message = "The coach didn't send any tips. Try again."
                else store.saveCoachAdvice(it)
            }.onFailure { message = friendlyError(it) }
        }
    }

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("✨ AI coach", style = MaterialTheme.typography.titleMedium)

            if (store.geminiKey.isBlank()) {
                Text(
                    "Get personal tips from your own weigh-ins, meals and steps. The coach uses Google's Gemini AI " +
                        "with your own free key (the same one used for kitchen photos).",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(GeminiClient.KEY_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }) { Text("Get a free key") }
                OutlinedTextField(
                    value = keyText,
                    onValueChange = { keyText = it },
                    label = { Text("Gemini API key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(enabled = keyText.isNotBlank(), onClick = { store.updateGeminiKey(keyText) }) { Text("Save") }
                return@Column
            }

            if (advice == null) {
                Text(
                    "The coach reads your last ${Coach.DAYS} days of meals and steps plus your weigh-ins, " +
                        "then suggests what to change to keep the weight coming off.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    "From ${formatLongDate(LocalDate.parse(advice.date))}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                if (advice.headline.isNotBlank()) {
                    Text(advice.headline, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
                advice.tips.forEachIndexed { i, tip ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("${i + 1}. ${tip.title}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(tip.detail, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(enabled = !loading, onClick = { ask() }) {
                    Text(if (advice == null) "Get my tips" else "Refresh tips")
                }
                if (loading) CircularProgressIndicator(Modifier.size(24.dp))
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text(
                "Your log summary is sent to Google to write these tips. It's a helper, not medical advice.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
