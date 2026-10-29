package com.freshstart.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshstart.app.data.AppStore
import com.freshstart.app.data.AvatarPhoto
import com.freshstart.app.data.Badge
import com.freshstart.app.data.DayXp
import com.freshstart.app.data.Game
import com.freshstart.app.data.GameProgress
import com.freshstart.app.data.HeroClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HeroScreen(store: AppStore) {
    val today = LocalDate.now()
    LaunchedEffect(store.healthOn) { store.refreshSteps(today) }
    val hero = store.hero
    var changingHero by remember { mutableStateOf(false) }

    if (hero == null || changingHero) {
        HeroPicker(current = hero, onPick = {
            store.chooseHero(it)
            changingHero = false
        }, onCancel = if (hero != null) ({ changingHero = false }) else null)
        return
    }
    val progress = store.gameProgress(today) ?: return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var photoError by remember { mutableStateOf(false) }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val encoded = withContext(Dispatchers.IO) { AvatarPhoto.encode(context, uri) }
                photoError = encoded == null
                if (encoded != null) store.setAvatarPhoto(encoded)
            }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HeroAvatar(store, size = 160.dp, level = progress.level)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            Text(if (store.avatarPhoto.isEmpty()) "Use my photo" else "Change photo")
                        }
                        if (store.avatarPhoto.isNotEmpty()) {
                            TextButton(onClick = { store.setAvatarPhoto("") }) { Text("Use ${hero.emoji} instead") }
                        }
                    }
                    if (photoError) {
                        Text("Couldn't read that photo. Try another one.", style = MaterialTheme.typography.bodySmall, color = FoodRed)
                    }
                    val name = store.profile?.name?.takeIf { it.isNotBlank() }
                    Text(
                        if (name != null) "$name the ${hero.label}" else hero.label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Level ${progress.level} ${progress.title}", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    XpBar(progress)
                    Text(
                        "${progress.xpIntoLevel} / ${progress.xpForNextLevel} XP to level ${progress.level + 1}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Bonus: +${(Game.HERO_BONUS * 100).toInt()}% XP from ${hero.boost.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        item { TodayXpCard(progress.days[today.toString()], store.healthOn) }

        item { WeekCard(progress, today) }

        item { BadgesCard(store.badges) }

        item { RulesCard() }

        item {
            TextButton(onClick = { changingHero = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Change hero (keeps your XP and badges)")
            }
        }
    }
}

@Composable
private fun XpBar(progress: GameProgress, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progress.xpIntoLevel.toFloat() / progress.xpForNextLevel },
        modifier = modifier.fillMaxWidth().height(12.dp),
        color = Brand,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {}
    )
}

/**
 * The user's photo in a circle, or their hero's emoji if they haven't picked one,
 * with their level in a small badge at the bottom right.
 */
@Composable
fun HeroAvatar(store: AppStore, size: Dp, level: Int? = null) {
    val hero = store.hero ?: return
    val photo = remember(store.avatarPhoto) {
        store.avatarPhoto.takeIf { it.isNotEmpty() }?.let { AvatarPhoto.toImage(it) }
    }
    Box(Modifier.size(size)) {
        if (photo != null) {
            Image(
                photo,
                contentDescription = "Your avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape).border(3.dp, Brand, CircleShape)
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.radialGradient(listOf(Color.White, MaterialTheme.colorScheme.surfaceVariant)), CircleShape)
                    .border(3.dp, Brand, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Sized from dp so the phone's font size setting doesn't change it.
                Text(hero.emoji, fontSize = with(LocalDensity.current) { (size * 0.55f).toSp() })
            }
        }
        if (level != null) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.3f)
                    .background(Brand, CircleShape)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$level",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = with(LocalDensity.current) { (size * 0.13f).toSp() }
                )
            }
        }
    }
}

@Composable
private fun BadgesCard(earned: Map<String, String>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Badges", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    "${earned.size} of ${Game.badges.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Earned badges first, newest first, then the ones still to win.
            val sorted = Game.badges.sortedWith(
                compareByDescending<Badge> { it.id in earned }.thenByDescending { earned[it.id] ?: "" }
            )
            sorted.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { b -> BadgeTile(b, earned[b.id], Modifier.weight(1f)) }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun BadgeTile(badge: Badge, earnedOn: String?, modifier: Modifier = Modifier) {
    val won = earnedOn != null
    Column(
        modifier
            .background(
                if (won) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(12.dp)
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (won) badge.emoji else "🔒",
            fontSize = 30.sp,
            modifier = Modifier.alpha(if (won) 1f else 0.5f)
        )
        Text(
            badge.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            if (earnedOn != null) "Earned ${LocalDate.parse(earnedOn).format(DateTimeFormatter.ofPattern("MMM d"))}" else badge.hint,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 3
        )
    }
}

@Composable
private fun TodayXpCard(day: DayXp?, healthOn: Boolean) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Today's XP", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(signed(day?.total ?: 0), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (day == null || day.lines.isEmpty()) {
                Text("Log a meal or go for a walk to start earning XP today.", style = MaterialTheme.typography.bodyMedium)
            } else {
                day.lines.forEach { line ->
                    Row {
                        Text(line.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(
                            signed(line.xp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (line.xp < 0) FoodRed else Brand
                        )
                    }
                }
            }
            Text(
                "Finish today within your calorie budget for +25 XP, added tomorrow.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!healthOn) {
                Text(
                    "Turn on Health Connect in Settings to earn XP from your steps.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun WeekCard(progress: GameProgress, today: LocalDate) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Last 7 days", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth()) {
                for (i in 6 downTo 0) {
                    val d = today.minusDays(i.toLong())
                    val xp = progress.days[d.toString()]?.total ?: 0
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(d.format(DateTimeFormatter.ofPattern("EEE")), style = MaterialTheme.typography.labelSmall)
                        Text(
                            signed(xp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                xp < 0 -> FoodRed
                                xp > 0 -> Brand
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
            Text(
                "Total ${"%,d".format(progress.totalXp)} XP",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RulesCard() {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("How to earn XP", style = MaterialTheme.typography.titleMedium)
            listOf(
                "+10 for logging food, +5 more for 3 or more meals",
                "+3 for each green food (up to 8 a day)",
                "+25 for finishing a day within your budget, +10 if just over",
                "+1 per 500 steps (up to 30), +15 for 10,000 steps",
                "+5 for weighing in, plus up to +10 for your logging streak",
                "−3 for each red food, and −1 per 10 cal of red over its 25% share",
                "−10 for a day well over budget"
            ).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            Text(
                "XP comes from what you already log, so there's nothing extra to enter. Badges stay yours once you've earned them.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun HeroPicker(current: HeroClass?, onPick: (HeroClass) -> Unit, onCancel: (() -> Unit)?) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("Choose your hero", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Your hero levels up from healthy days: green foods, staying on budget and steps. Red foods cost XP. " +
                        "Each hero gets a bonus on one habit. You can use your own photo as the avatar afterwards.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        HeroClass.entries.forEach { h ->
            item(key = h.name) {
                Card(
                    Modifier.fillMaxWidth().clickable { onPick(h) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (h == current) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(h.emoji, fontSize = 48.sp)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(h.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(h.blurb, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "+${(Game.HERO_BONUS * 100).toInt()}% XP from ${h.boost.label}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
        if (onCancel != null) {
            item {
                OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Keep my current hero") }
            }
        }
    }
}

/** A small hero summary for the Today screen. */
@Composable
fun HeroSummaryCard(store: AppStore, onOpen: () -> Unit) {
    val today = LocalDate.now()
    val hero = store.hero
    Card(
        Modifier.fillMaxWidth().clickable { onOpen() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        if (hero == null) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🎮", fontSize = 36.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Level up your health", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Pick a hero who earns XP and badges from your healthy days.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            return@Card
        }
        val progress = store.gameProgress(today) ?: return@Card
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            HeroAvatar(store, size = 56.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Level ${progress.level} ${hero.label}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    val todayXp = progress.days[today.toString()]?.total ?: 0
                    Text(
                        "${signed(todayXp)} today",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (todayXp < 0) FoodRed else Brand
                    )
                }
                XpBar(progress)
                Text(
                    "${progress.xpForNextLevel - progress.xpIntoLevel} XP to level ${progress.level + 1} · " +
                        "${store.badges.size} badges",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/** Celebrates new levels and newly earned badges, once each. */
@Composable
fun CelebrationDialog(store: AppStore) {
    if (store.hero == null) return
    val today = LocalDate.now()
    val progress = store.gameProgress(today) ?: return
    val earned = store.newBadges(progress)
    val levelUp = progress.level > store.peakLevel
    if (!levelUp && earned.isEmpty()) return
    val done = { store.celebrate(progress.level, earned, today) }
    AlertDialog(
        onDismissRequest = done,
        confirmButton = { TextButton(onClick = done) { Text("Awesome!") } },
        icon = { HeroAvatar(store, size = 88.dp, level = progress.level) },
        title = {
            Text(
                when {
                    levelUp -> "Level ${progress.level}!"
                    earned.size == 1 -> "New badge!"
                    else -> "${earned.size} new badges!"
                },
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (levelUp) Text("You're now a level ${progress.level} ${progress.title}.")
                earned.forEach { b ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(b.emoji, fontSize = 28.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(b.name, fontWeight = FontWeight.SemiBold)
                            Text(b.hint, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    )
}

private fun signed(xp: Int) = if (xp > 0) "+$xp" else "$xp"
