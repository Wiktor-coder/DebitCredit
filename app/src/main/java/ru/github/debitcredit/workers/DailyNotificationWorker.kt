package ru.github.debitcredit.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ru.github.debitcredit.R
import ru.github.debitcredit.presentation.ui.MainActivity
import java.util.Calendar

class DailyNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val CHANNEL_ID = "daily_channel"
        private const val CHANNEL_NAME = "Ежедневные уведомления"
        private const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        return try {
            showDailyNotification()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun showDailyNotification() {
        val context = applicationContext

        // Проверяем, было ли уже показано уведомление сегодня
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(Calendar.getInstance().time)
        val lastDate = prefs.getString("daily_notification_date", "")

        if (lastDate == today) {
            return
        }

        // Создаем канал для Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Ежедневные утренние уведомления"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        // Интент для открытия приложения
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        // Разные сообщения
        val messages = listOf(
            "Доброе утро! ☀️" to "Проверьте свои финансы сегодня",
            "Начните день с учета!" to "Отслеживайте доходы и расходы ежедневно",
            "Ваши финансы под контролем!" to "Загляните в приложение сегодня",
            "Хорошего дня!" to "Не забудьте записать свои траты",
            "Будьте в курсе!" to "Проверьте статистику за вчера"
        )

        val (title, body) = messages.random()

        // Создаем уведомление
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_ruble)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$body. Управляйте своими финансами с нами! 💪")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = NotificationManagerCompat.from(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                manager.notify(NOTIFICATION_ID, notification)
                prefs.edit {
                    putString("daily_notification_date", today)
                }
            }
        } else {
            manager.notify(NOTIFICATION_ID, notification)
            prefs.edit {
                putString("daily_notification_date", today)
            }
        }
    }
}