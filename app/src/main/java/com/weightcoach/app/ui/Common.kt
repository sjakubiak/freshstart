package com.weightcoach.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.weightcoach.app.data.FoodColor

val Brand = Color(0xFF2E7D5B)
val FoodGreen = Color(0xFF3FA65B)
val FoodYellow = Color(0xFFF2B830)
val FoodRed = Color(0xFFE0533D)

fun FoodColor.ui(): Color = when (this) {
    FoodColor.GREEN -> FoodGreen
    FoodColor.YELLOW -> FoodYellow
    FoodColor.RED -> FoodRed
}

private val Scheme = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEBD8),
    onPrimaryContainer = Color(0xFF0B3A24),
    secondary = Color(0xFF4F6F5F),
    secondaryContainer = Color(0xFFD9E8DE),
    background = Color(0xFFF7FAF7),
    surface = Color(0xFFF7FAF7),
    surfaceVariant = Color(0xFFE4EDE6)
)

@Composable
fun WeightCoachTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}

@Composable
fun ColorDot(color: FoodColor, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(12.dp)
            .background(color.ui(), CircleShape)
    )
}

@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    decimal: Boolean = true,
    suffix: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = { new ->
            val allowed = if (decimal) new.filter { it.isDigit() || it == '.' } else new.filter { it.isDigit() }
            onValueChange(allowed.take(7))
        },
        label = { Text(label) },
        singleLine = true,
        suffix = suffix?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = modifier
    )
}

fun Double.trimmed(): String =
    if (this == Math.floor(this)) this.toLong().toString() else String.format(java.util.Locale.US, "%.1f", this)
