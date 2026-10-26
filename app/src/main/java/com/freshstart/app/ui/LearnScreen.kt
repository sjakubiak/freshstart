package com.freshstart.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freshstart.app.data.AppStore
import com.freshstart.app.data.Calc
import com.freshstart.app.data.Lessons
import java.time.LocalDate

@Composable
fun LearnScreen(store: AppStore, openIndex: Int?) {
    val profile = store.profile ?: return
    val todayIndex = Lessons.forDay(Calc.daysSince(profile.startDate, LocalDate.now()))
    var expanded by rememberSaveable { mutableStateOf(openIndex ?: todayIndex) }
    val listState = rememberLazyListState()

    LaunchedEffect(openIndex) {
        if (openIndex != null) {
            expanded = openIndex
            listState.scrollToItem(openIndex + 1)
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column {
                Text("Lessons", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "${store.lessonsRead.size} of ${Lessons.all.size} read. A new one is highlighted each day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        itemsIndexed(Lessons.all) { index, lesson ->
            val read = store.lessonsRead.contains(index)
            val isToday = index == todayIndex
            Card(
                Modifier.fillMaxWidth().clickable { expanded = if (expanded == index) -1 else index },
                colors = if (isToday) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                else CardDefaults.cardColors()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (isToday) "Day ${index + 1} · Today" else "Day ${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(lesson.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        if (read) Icon(Icons.Filled.CheckCircle, contentDescription = "Read", tint = MaterialTheme.colorScheme.primary)
                    }
                    if (expanded == index) {
                        Text(lesson.body, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
                        TextButton(onClick = { store.markLessonRead(index, !read) }) {
                            Text(if (read) "Mark as unread" else "Mark as read")
                        }
                    }
                }
            }
        }
    }
}
