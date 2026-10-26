package com.freshstart.app.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.freshstart.app.MainActivity
import com.freshstart.app.R
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Daily nudges: weigh in and log breakfast in the morning, log the rest of the day in the evening. */
object Reminders {
    private const val CHANNEL_ID = "reminders"

    private data class Slot(val tag: String, val time: LocalTime, val title: String, val text: String)

    private val slots = listOf(
        Slot("morning", LocalTime.of(8, 0), "Good morning!", "Step on the scale and log your breakfast."),
        Slot("lunch", LocalTime.of(12, 30), "Lunch time", "Take a minute to log your lunch."),
        Slot("evening", LocalTime.of(19, 30), "Evening check-in", "Log your dinner and snacks, and read today's lesson.")
    )

    fun schedule(context: Context) {
        val wm = WorkManager.getInstance(context)
        val now = LocalDateTime.now()
        for (slot in slots) {
            var next = now.toLocalDate().atTime(slot.time)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val delay = Duration.between(now, next).toMinutes()
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MINUTES)
                .setInputData(workDataOf("title" to slot.title, "text" to slot.text, "id" to slot.tag.hashCode()))
                .build()
            wm.enqueueUniquePeriodicWork("reminder-${slot.tag}", ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }

    fun cancel(context: Context) {
        val wm = WorkManager.getInstance(context)
        slots.forEach { wm.cancelUniqueWork("reminder-${it.tag}") }
    }

    fun show(context: Context, id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Daily reminders", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        Reminders.show(
            applicationContext,
            inputData.getInt("id", 1),
            inputData.getString("title") ?: "FreshStart",
            inputData.getString("text") ?: "Time to check in."
        )
        return Result.success()
    }
}
