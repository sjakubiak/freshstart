package com.weightcoach.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.weightcoach.app.data.AppStore
import com.weightcoach.app.data.FoodColor
import com.weightcoach.app.data.FoodDatabase
import com.weightcoach.app.data.FoodEntry
import com.weightcoach.app.data.FoodItem
import com.weightcoach.app.data.Meal
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddFoodDialog(store: AppStore, date: String, initialMeal: Meal, onDismiss: () -> Unit) {
    var meal by remember { mutableStateOf(initialMeal) }
    var query by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf<FoodItem?>(null) }
    var creatingCustom by remember { mutableStateOf(false) }

    val recent = store.foods
        .sortedByDescending { it.id }
        .distinctBy { it.name.lowercase() }
        .take(8)
        .map { FoodItem(it.name, "1 serving", (it.calories / it.servings).roundToInt(), it.color) }

    val all = (store.customFoods + FoodDatabase.foods).distinctBy { it.name.lowercase() }
    val results = if (query.isBlank()) all else all.filter { it.name.contains(query.trim(), ignoreCase = true) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().imePadding()) {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Close") }
                    Text("Log food", style = MaterialTheme.typography.titleLarge)
                }
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Meal.entries.forEach { m ->
                            FilterChip(selected = meal == m, onClick = { meal = m }, label = { Text(m.label) })
                        }
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        placeholder = { Text("Search foods") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(onClick = { creatingCustom = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (query.isBlank()) "Add your own food" else "Add \"${query.trim()}\" as your own food")
                    }
                }
                LazyColumn(Modifier.fillMaxSize()) {
                    if (query.isBlank() && recent.isNotEmpty()) {
                        item { SectionLabel("Recent") }
                        items(recent, key = { "recent-" + it.name }) { FoodRow(it, onClick = { picked = it }) }
                        item { SectionLabel("All foods") }
                    }
                    items(results, key = { (if (it.custom) "custom-" else "db-") + it.name }) { item ->
                        FoodRow(
                            item,
                            onClick = { picked = item },
                            onDelete = if (item.custom) ({ store.removeCustomFood(item) }) else null
                        )
                    }
                    if (results.isEmpty()) {
                        item {
                            Text(
                                "No matches. Add it as your own food above.",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    picked?.let { item ->
        ServingsDialog(
            item = item,
            onDismiss = { picked = null },
            onAdd = { servings ->
                store.addFood(
                    FoodEntry(
                        id = System.currentTimeMillis(),
                        date = date,
                        meal = meal,
                        name = item.name,
                        calories = (item.calories * servings).roundToInt(),
                        color = item.color,
                        servings = servings
                    )
                )
                picked = null
                onDismiss()
            }
        )
    }

    if (creatingCustom) {
        CustomFoodDialog(
            initialName = query.trim(),
            onDismiss = { creatingCustom = false },
            onSave = { item, keep ->
                if (keep) store.addCustomFood(item)
                store.addFood(
                    FoodEntry(System.currentTimeMillis(), date, meal, item.name, item.calories, item.color, 1.0)
                )
                creatingCustom = false
                onDismiss()
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun FoodRow(item: FoodItem, onClick: () -> Unit, onDelete: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ColorDot(item.color)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                item.serving + if (item.custom) " · my food" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text("${item.calories} cal", style = MaterialTheme.typography.bodyMedium)
        if (onDelete != null) {
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete ${item.name}") }
        }
    }
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun ServingsDialog(item: FoodItem, onDismiss: () -> Unit, onAdd: (Double) -> Unit) {
    var servings by remember { mutableStateOf(1.0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ColorDot(item.color)
                    Spacer(Modifier.width(8.dp))
                    Text("${item.color.label} food · ${item.calories} cal per ${item.serving}")
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { if (servings > 0.5) servings -= 0.5 }) { Text("−") }
                    Text(
                        "${servings.trimmed()} serving${if (servings == 1.0) "" else "s"}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    OutlinedButton(onClick = { if (servings < 20) servings += 0.5 }) { Text("+") }
                }
                Text(
                    "${(item.calories * servings).roundToInt()} calories",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(servings) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CustomFoodDialog(initialName: String, onDismiss: () -> Unit, onSave: (FoodItem, Boolean) -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    var serving by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(FoodColor.YELLOW) }
    var keep by remember { mutableStateOf(true) }
    val kcal = calories.toIntOrNull()
    val valid = name.isNotBlank() && kcal != null && kcal in 0..5000

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your own food") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it.take(40) }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(serving, { serving = it.take(30) }, label = { Text("Serving (e.g. 1 bowl)") }, singleLine = true)
                NumberField(calories, { calories = it }, "Calories", decimal = false)
                Text("Color", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FoodColor.entries.forEach { c ->
                        FilterChip(
                            selected = color == c,
                            onClick = { color = c },
                            label = { Text(c.label) },
                            leadingIcon = { ColorDot(c) }
                        )
                    }
                }
                Text(color.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = keep, onCheckedChange = { keep = it })
                    Text("Save to my foods for next time")
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onSave(FoodItem(name.trim(), serving.trim().ifBlank { "1 serving" }, kcal!!, color, custom = true), keep)
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
