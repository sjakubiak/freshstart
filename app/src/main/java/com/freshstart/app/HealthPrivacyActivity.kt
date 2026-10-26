package com.freshstart.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freshstart.app.ui.FreshStartTheme

/** Explains what FreshStart does with health data. Health Connect links here from its permission screen. */
class HealthPrivacyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FreshStartTheme {
                Scaffold { padding ->
                    Column(
                        Modifier.fillMaxSize().padding(padding).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("How FreshStart uses your health data", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("• Weight: reads weigh-ins from your scale or other apps to chart your progress, and saves weigh-ins you log in FreshStart.")
                        Text("• Nutrition: saves the calories of meals you log, so they show up in your other health apps.")
                        Text("• Steps and active calories: shown on your Today screen.")
                        Text("Your data stays on your phone. FreshStart has no account, no servers and no ads, and never sends your data anywhere.")
                        Button(onClick = { finish() }) { Text("Close") }
                    }
                }
            }
        }
    }
}
