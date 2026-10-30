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
import androidx.compose.material.icons.filled.Star
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
import com.freshstart.app.data.Account
import com.freshstart.app.data.AppStore
import com.freshstart.app.data.Subscription
import com.freshstart.app.ui.PaywallScreen
import com.freshstart.app.ui.SignInScreen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.freshstart.app.reminders.Reminders
import com.freshstart.app.ui.HeroScreen
import com.freshstart.app.ui.LearnScreen
import com.freshstart.app.ui.CelebrationDialog
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
        val account = accountFor(this, store)
        val subscription = subscriptionFor(this)
        setContent {
            FreshStartTheme { App(store, account, subscription) }
        }
    }

    override fun onResume() {
        super.onResume()
        // Picks up a purchase, renewal or cancellation made while the app was in the background.
        subscriptionFor(this).refresh()
    }

    companion object {
        private var store: AppStore? = null

        private var account: Account? = null

        private var subscription: Subscription? = null

        fun storeFor(activity: ComponentActivity): AppStore =
            store ?: AppStore(activity.applicationContext).also { store = it }

        fun accountFor(activity: ComponentActivity, store: AppStore): Account =
            account ?: Account(activity.applicationContext, store).also { account = it }

        fun subscriptionFor(activity: ComponentActivity): Subscription =
            subscription ?: Subscription(activity.applicationContext).also { subscription = it }
    }
}

private const val HERO_TAB = 2
private const val LEARN_TAB = 4

private data class Tab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab("Today", Icons.Filled.Home),
    Tab("Plan", Icons.AutoMirrored.Filled.List),
    Tab("Hero", Icons.Filled.Star),
    Tab("Progress", Icons.Filled.DateRange),
    Tab("Learn", Icons.Filled.Info),
    Tab("Settings", Icons.Filled.Settings)
)

@Composable
fun App(store: AppStore, account: Account, subscription: Subscription) {
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

    val user = account.user
    androidx.compose.runtime.LaunchedEffect(user?.uid) {
        if (user != null) account.startSync(user) else account.stopSync()
    }

    if (account.configured && user == null && !store.useWithoutAccount) {
        Scaffold { padding ->
            SignInScreen(account, onSkip = { store.chooseWithoutAccount(true) }, modifier = Modifier.padding(padding))
        }
        return
    }

    if (user != null && !account.ready) {
        Scaffold { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Loading your data…")
            }
        }
        return
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

    if (subscription.required && !subscription.active) {
        Scaffold { padding -> PaywallScreen(subscription, Modifier.padding(padding)) }
        return
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        store.importFromHealth()
        store.refreshSteps(java.time.LocalDate.now())
    }
    CelebrationDialog(store)

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
                            if (i != LEARN_TAB) lessonToOpen = null
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
                    tab = LEARN_TAB
                }, onOpenPlan = { tab = 1 }, onOpenHero = { tab = HERO_TAB })
                1 -> PlanScreen(store)
                HERO_TAB -> HeroScreen(store)
                3 -> ProgressScreen(store)
                LEARN_TAB -> LearnScreen(store, lessonToOpen)
                else -> SettingsScreen(store, account, subscription, onRemindersChanged = { applyReminders(it) })
            }
        }
    }
}
