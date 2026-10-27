package com.freshstart.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.freshstart.app.data.AppStore
import com.freshstart.app.reminders.Reminders
import com.freshstart.app.ui.LearnScreen
import com.freshstart.app.ui.PlanScreen
import com.freshstart.app.ui.ProfileForm
import com.freshstart.app.ui.ProgressScreen
import com.freshstart.app.ui.SettingsScreen
import com.freshstart.app.ui.TodayScreen
import com.freshstart.app.ui.FreshStartTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = storeFor(this)
        setContent {
            FreshStartTheme { App(store) }
        }
    }

    companion object {
        private var store: AppStore? = null

        fun storeFor(activity: ComponentActivity): AppStore =
            store ?: AppStore(activity.applicationContext).also { store = it }
    }
}

private data class Tab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab("Today", Icons.Filled.Home),
    Tab("Plan", Icons.AutoMirrored.Filled.List),
    Tab("Progress", Icons.Filled.DateRange),
    Tab("Learn", Icons.Filled.Info),
    Tab("Settings", Icons.Filled.Settings)
)

@Composable
fun App(store: AppStore) {
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun applyReminders(on: Boolean) {
        if (on) {
            if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            Reminders.schedule(context)
        } else {
            Reminders.cancel(context)
        }
    }

    val profile = store.profile
    if (profile == null) {
        Scaffold { padding ->
            ProfileForm(
                existing = null,
                submitLabel = "Start my plan",
                onSubmit = {
                    store.saveProfile(it)
                    applyReminders(it.remindersOn)
                },
                modifier = Modifier.padding(padding),
                header = {
                    Column {
                        Text("Welcome to FreshStart", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "A few questions to build your plan. It takes less than a minute, and everything stays on your phone.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            )
        }
        return
    }

    androidx.compose.runtime.LaunchedEffect(Unit) { store.importFromHealth() }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var lessonToOpen by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = {
                            tab = i
                            if (i != 3) lessonToOpen = null
                        },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> TodayScreen(store, onOpenLesson = {
                    lessonToOpen = it
                    tab = 3
                }, onOpenPlan = { tab = 1 })
                1 -> PlanScreen(store)
                2 -> ProgressScreen(store)
                3 -> LearnScreen(store, lessonToOpen)
                else -> SettingsScreen(store, onRemindersChanged = { applyReminders(it) })
            }
        }
    }
}
