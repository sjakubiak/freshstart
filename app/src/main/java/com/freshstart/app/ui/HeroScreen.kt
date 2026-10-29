package com.freshstart.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshstart.app.data.AppStore
import com.freshstart.app.data.DayXp
import com.freshstart.app.data.Game
import com.freshstart.app.data.GameProgress
import com.freshstart.app.data.Gear
import com.freshstart.app.data.GearSlot
import com.freshstart.app.data.HeroClass
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
    val unlockedLevel = maxOf(store.peakLevel, progress.level)

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
                    HeroAvatar(hero, store.equipped, size = 200.dp)
                    Spacer(Modifier.height(8.dp))
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

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Gear and outfits", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "New items unlock as you level up. Tap one to wear it, tap again to take it off.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    GearSlot.entries.forEach { slot ->
                        Text(slot.label, style = MaterialTheme.typography.labelLarge)
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Game.gear.filter { it.slot == slot }.forEach { g ->
                                GearTile(
                                    gear = g,
                                    unlocked = g.level <= unlockedLevel,
                                    worn = store.equipped[slot] == g.id,
                                    onClick = { store.toggleGear(g) }
                                )
                            }
                        }
                    }
                }
            }
        }

        item { RulesCard() }

        item {
            TextButton(onClick = { changingHero = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Change hero (keeps your XP and gear)")
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

/** The hero drawn from emoji: aura behind, headgear above, outfit at the chest, item in hand and a pet at their feet. */
@Composable
fun HeroAvatar(hero: HeroClass, equipped: Map<GearSlot, String>, size: Dp) {
    val aura = Game.gearById(equipped[GearSlot.AURA])?.colors?.map { Color(it) }
    val background = if (aura != null && aura.size >= 2) {
        Brush.radialGradient(aura)
    } else {
        Brush.radialGradient(listOf(Color.White, MaterialTheme.colorScheme.surfaceVariant))
    }
    val unit = size / 10
    fun emoji(slot: GearSlot) = Game.gearById(equipped[slot])?.emoji
    Box(Modifier.size(size).background(background, CircleShape), contentAlignment = Alignment.Center) {
        Text(hero.emoji, fontSize = (unit.value * 4.6f).sp)
        emoji(GearSlot.HEAD)?.let {
            Text(it, fontSize = (unit.value * 2.2f).sp, modifier = Modifier.align(Alignment.TopCenter).offset(y = unit * 0.3f))
        }
        emoji(GearSlot.OUTFIT)?.let {
            Text(it, fontSize = (unit.value * 1.8f).sp, modifier = Modifier.align(Alignment.Center).offset(y = unit * 2.4f))
        }
        emoji(GearSlot.HAND)?.let {
            Text(it, fontSize = (unit.value * 2f).sp, modifier = Modifier.align(Alignment.CenterEnd).offset(x = -unit * 0.6f, y = unit * 0.6f))
        }
        emoji(GearSlot.PET)?.let {
            Text(it, fontSize = (unit.value * 1.9f).sp, modifier = Modifier.align(Alignment.BottomStart).offset(x = unit * 1.1f, y = -unit * 0.6f))
        }
    }
}

@Composable
private fun GearTile(gear: Gear, unlocked: Boolean, worn: Boolean, onClick: () -> Unit) {
    OutlinedCard(
        modifier = Modifier.width(84.dp).clickable(enabled = unlocked, onClick = onClick),
        border = BorderStroke(if (worn) 2.dp else 1.dp, if (worn) Brand else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(8.dp).alpha(if (unlocked) 1f else 0.45f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (gear.slot == GearSlot.AURA) {
                Box(Modifier.size(32.dp).background(Brush.radialGradient(gear.colors.map { Color(it) }), CircleShape))
            } else {
                Text(gear.emoji, fontSize = 28.sp)
            }
            Text(
                gear.name,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                minLines = 2
            )
            Text(
                when {
                    !unlocked -> "🔒 Lv ${gear.level}"
                    worn -> "Wearing"
                    else -> "Lv ${gear.level}"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (worn) Brand else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
                "XP comes from what you already log, so there's nothing extra to enter. Gear you unlock stays unlocked.",
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
                        "Each hero gets a bonus on one habit.",
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
                TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Keep my current hero") }
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
                        "Pick a hero who earns XP and unlocks gear from your healthy days.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            return@Card
        }
        val progress = store.gameProgress(today) ?: return@Card
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            HeroAvatar(hero, store.equipped, size = 64.dp)
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
                    "${progress.xpForNextLevel - progress.xpIntoLevel} XP to level ${progress.level + 1}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/** Celebrates new levels and the gear they unlocked. Shows once per level reached. */
@Composable
fun LevelUpDialog(store: AppStore) {
    val hero = store.hero ?: return
    val progress = store.gameProgress(LocalDate.now()) ?: return
    if (progress.level <= store.peakLevel) return
    val unlocked = Game.unlockedBetween(store.peakLevel, progress.level)
    AlertDialog(
        onDismissRequest = { store.reachLevel(progress.level) },
        confirmButton = { TextButton(onClick = { store.reachLevel(progress.level) }) { Text("Awesome!") } },
        icon = { HeroAvatar(hero, store.equipped, size = 96.dp) },
        title = { Text("Level ${progress.level}!", textAlign = TextAlign.Center) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Your ${hero.label} is now a level ${progress.level} ${progress.title}.")
                if (unlocked.isNotEmpty()) {
                    Text("New gear unlocked:", fontWeight = FontWeight.SemiBold)
                    unlocked.forEach { g ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (g.slot == GearSlot.AURA) {
                                Box(Modifier.size(20.dp).background(Brush.radialGradient(g.colors.map { Color(it) }), CircleShape))
                            } else {
                                Text(g.emoji, fontSize = 20.sp)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("${g.name} (${g.slot.label.lowercase()})")
                        }
                    }
                    Text("Put it on from the Hero tab.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    )
}

private fun signed(xp: Int) = if (xp > 0) "+$xp" else "$xp"
