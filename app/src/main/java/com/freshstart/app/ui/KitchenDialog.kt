package com.freshstart.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.freshstart.app.data.AppStore
import com.freshstart.app.data.GeminiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val staples = listOf(
    "eggs", "milk", "greek yogurt", "cheese", "bread", "oats", "rice", "pasta", "tortilla",
    "chicken", "ground turkey", "ground beef", "salmon", "tuna", "black beans", "lentils",
    "spinach", "lettuce", "broccoli", "carrots", "peppers", "onion", "tomato", "cucumber",
    "potato", "sweet potato", "apple", "banana", "berries", "peanut butter", "hummus", "almonds"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KitchenDialog(store: AppStore, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var newItem by remember { mutableStateOf("") }
    var scanning by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var keyText by remember { mutableStateOf(store.geminiKey) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    fun scan(uri: Uri) {
        scanning = true
        message = "Looking at your photo…"
        scope.launch {
            val result = runCatching {
                val bitmap = withContext(Dispatchers.IO) { loadScaled(context, uri, 1280) }
                    ?: throw IllegalStateException("couldn't open the photo")
                GeminiClient(store.geminiKey).foodsInPhoto(bitmap)
            }
            scanning = false
            message = result.fold(
                onSuccess = { found ->
                    if (found.isEmpty()) "No food spotted. Try a closer, brighter photo."
                    else {
                        val added = store.addPantryItems(found)
                        "Found ${found.size} foods (${added} new): ${found.take(12).joinToString(", ")}${if (found.size > 12) "…" else ""}"
                    }
                },
                onFailure = { friendlyError(it) }
            )
        }
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = photoUri
        if (ok && uri != null) scan(uri)
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scan(uri)
    }

    fun addTyped() {
        val parts = newItem.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (parts.isNotEmpty()) store.addPantryItems(parts)
        newItem = ""
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().imePadding()) {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Close") }
                    Text("My kitchen", style = MaterialTheme.typography.titleLarge)
                }
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Photo scanning
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Scan your fridge or cupboard", style = MaterialTheme.typography.titleMedium)
                            if (store.geminiKey.isBlank()) {
                                Text(
                                    "Photo scanning uses Google's Gemini AI with your own free key. " +
                                        "It takes about 2 minutes to set up:",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "1. Tap \"Get a free key\" and sign in with your Google account.\n" +
                                        "2. Tap \"Create API key\" and copy it.\n" +
                                        "3. Paste it below and tap Save.",
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
                                Button(enabled = keyText.isNotBlank(), onClick = {
                                    store.updateGeminiKey(keyText)
                                    message = "Key saved. Now snap a photo!"
                                }) { Text("Save") }
                                Text(
                                    "Photos you scan are sent to Google to be analyzed. Everything else stays on your phone. " +
                                        "Skip this and just add foods by hand below if you prefer.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    "Take a photo of your open fridge, freezer or cupboard. FreshStart adds what it sees to your list. " +
                                        "Do one photo per shelf area for the best results.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(enabled = !scanning, onClick = {
                                        val uri = newPhotoUri(context)
                                        photoUri = uri
                                        takePicture.launch(uri)
                                    }) { Text("Take photo") }
                                    OutlinedButton(enabled = !scanning, onClick = {
                                        pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }) { Text("Choose photo") }
                                    if (scanning) CircularProgressIndicator(Modifier.size(24.dp).align(Alignment.CenterVertically))
                                }
                                TextButton(onClick = {
                                    store.updateGeminiKey("")
                                    keyText = ""
                                }) { Text("Remove my Gemini key") }
                            }
                            message?.let { Text(it, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold) }
                        }
                    }

                    // Add by hand
                    Text("Add foods by hand", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newItem,
                            onValueChange = { newItem = it.take(60) },
                            placeholder = { Text("e.g. eggs, spinach") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { addTyped() }),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { addTyped() }) { Icon(Icons.Filled.Add, contentDescription = "Add") }
                    }
                    val missing = staples.filter { s -> store.pantry.none { it.equals(s, ignoreCase = true) } }
                    if (missing.isNotEmpty()) {
                        Text("Tap to add common foods:", style = MaterialTheme.typography.bodySmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            missing.forEach { s ->
                                AssistChip(onClick = { store.addPantryItems(listOf(s)) }, label = { Text("+ $s") })
                            }
                        }
                    }

                    // Current list
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "In your kitchen (${store.pantry.size})",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (store.pantry.isNotEmpty()) TextButton(onClick = { store.clearPantry() }) { Text("Clear all") }
                    }
                    if (store.pantry.isEmpty()) {
                        Text("Nothing yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            store.pantry.toList().forEach { item ->
                                InputChip(
                                    selected = false,
                                    onClick = { store.removePantryItem(item) },
                                    label = { Text(item) },
                                    trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Remove $item", Modifier.size(16.dp)) }
                                )
                            }
                        }
                        Text("Tap a food to remove it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(1.dp).padding(bottom = 24.dp))
                }
            }
        }
    }
}

private fun newPhotoUri(context: Context): Uri {
    val dir = File(context.cacheDir, "photos").apply { mkdirs() }
    dir.listFiles()?.forEach { it.delete() }
    val file = File(dir, "kitchen-${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.photos", file)
}

/** Opens a photo shrunk so its longest side is about [maxDim] pixels, to keep uploads small. */
private fun loadScaled(context: Context, uri: Uri, maxDim: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDim) sample *= 2
    val decoded = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: return null
    val longest = maxOf(decoded.width, decoded.height)
    if (longest <= maxDim) return decoded
    val scale = maxDim.toFloat() / longest
    return Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt(), (decoded.height * scale).toInt(), true)
}
