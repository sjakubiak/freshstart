package com.freshstart.app.ui

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freshstart.app.data.Subscription

private val features = listOf(
    "🍏 Your personal calorie plan and color-coded food log",
    "🍽️ Daily meal ideas, plus AI ideas from what's in your kitchen",
    "📸 Snap your fridge and FreshStart lists the food for you",
    "✨ An AI coach that reads your log and gives you personal tips",
    "🦸 The Hero game, badges, lessons and Health Connect sync",
    "☁️ Private backup of your data to your account"
)

/** Shown in the Play build until the user has an active subscription or free trial. */
@Composable
fun PaywallScreen(subscription: Subscription, modifier: Modifier = Modifier) {
    val activity = LocalContext.current as? Activity
    val plans = subscription.plans
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val plan = plans.firstOrNull { it.basePlanId == selected } ?: plans.firstOrNull()

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("FreshStart Premium", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            if (plan != null && plan.trialDays > 0) "Try everything free for ${plan.trialDays} days."
            else "Everything you need to lose weight, in one app.",
            style = MaterialTheme.typography.titleMedium
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            features.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
        }

        when {
            plans.isNotEmpty() -> {
                plans.forEach { p ->
                    val isSelected = p == plan
                    Card(
                        Modifier.fillMaxWidth().selectable(selected = isSelected, onClick = { selected = p.basePlanId }),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = isSelected, onClick = { selected = p.basePlanId })
                            Column {
                                Text(
                                    if (p.yearly) "Yearly: ${p.price} a year" else "Monthly: ${p.price} a month",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (p.trialDays > 0) Text("First ${p.trialDays} days free", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                Button(
                    enabled = plan != null && activity != null,
                    onClick = { if (plan != null && activity != null) subscription.buy(activity, plan) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (plan != null && plan.trialDays > 0) "Start my free trial" else "Subscribe")
                }
                if (plan != null) {
                    Text(
                        (if (plan.trialDays > 0) "Free for ${plan.trialDays} days, then " else "") +
                            "${plan.price} per ${if (plan.yearly) "year" else "month"}. Renews automatically until you cancel. " +
                            (if (plan.trialDays > 0) "Cancel before the trial ends and you won't be charged. " else "") +
                            "Cancel any time in Google Play → Payments & subscriptions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            subscription.problem == null -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(24.dp))
                Text("Checking with Google Play…")
            }
        }

        subscription.problem?.let { Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold) }
        TextButton(onClick = { subscription.refresh() }) {
            Text(if (subscription.problem != null && plans.isEmpty()) "Try again" else "Already subscribed? Restore")
        }
        Text(
            "FreshStart is a general wellness app, not medical advice. Check with your doctor before big diet changes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
