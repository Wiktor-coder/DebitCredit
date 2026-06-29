package ru.github.debitcredit.utils

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
import ru.github.debitcredit.R
import ru.github.debitcredit.presentation.ui.MainActivity

object NotificationHelper {

    private const val CHANNEL_ID = "app_notifications"
    private const val CHANNEL_NAME = "Уведомления приложения"
    private const val NOTIFICATION_ID = 2001

    fun showAppreciationNotification(context: Context) {
        // Проверяем, было ли уже показано уведомление сегодня
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val lastNotificationDate = prefs.getString("last_appreciation_notification", "")
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())

        // Показываем уведомление только раз в день
        if (lastNotificationDate == today) {
            return
        }

        // Создаем канал уведомлений для Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Уведомления приложения"
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Создаем Intent для открытия приложения при нажатии на уведомление
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
            "Спасибо, что пользуетесь приложением! 🙏" to "Мы ценим каждого пользователя. Желаем вам финансового благополучия!",
            "Вы на шаг ближе к финансовой свободе! 🚀" to "Продолжайте отслеживать свои доходы и расходы с нами!",
            "Спасибо за доверие! 💪" to "Мы работаем над улучшением приложения каждый день!",
            "Ваши финансы в надежных руках! 💰" to "Спасибо, что выбираете наше приложение!",
            "Отличный день для учета финансов! 📊" to "Продолжайте в том же духе!"
        )

        val (title, text) = messages.random()

        // Создаем уведомление
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_ruble)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$text Мы стараемся сделать приложение лучше для вас! 💪")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Показываем уведомление
        val notificationManager = NotificationManagerCompat.from(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notificationManager.notify(NOTIFICATION_ID, notification)
                prefs.edit {
                    putString("last_appreciation_notification", today)
                }
            }
        } else {
            notificationManager.notify(NOTIFICATION_ID, notification)
            prefs.edit {
                putString("last_appreciation_notification", today)
            }
        }
    }
}