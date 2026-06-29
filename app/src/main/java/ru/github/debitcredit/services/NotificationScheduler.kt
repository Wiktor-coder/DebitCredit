package ru.github.debitcredit.services

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.*
import ru.github.debitcredit.workers.DailyNotificationWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    private const val TAG = "NotificationScheduler"
    private const val WORK_NAME = "daily_notification_work"

    fun scheduleDailyNotification(context: Context) {
        try {
            // Проверяем, запланирована ли уже работа
            val workManager = WorkManager.getInstance(context)
            val workInfos = workManager.getWorkInfosForUniqueWork(WORK_NAME).get()

            if (workInfos.isNotEmpty() && workInfos[0].state == WorkInfo.State.ENQUEUED) {
                return
            }

            // Вычисляем задержку до 8:00
            val delay = calculateDelayTo8AM()

            // Создаем PeriodicWorkRequest для ежедневного запуска
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresDeviceIdle(false)
                .setRequiresBatteryNotLow(false)
                .build()

            // Для Android 12+ нужно использовать setExpedited для точного времени
            val workRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                OneTimeWorkRequestBuilder<DailyNotificationWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setConstraints(constraints)
                    .addTag(WORK_NAME)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
            } else {
                OneTimeWorkRequestBuilder<DailyNotificationWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setConstraints(constraints)
                    .addTag(WORK_NAME)
                    .build()
            }

            // Запускаем работу
            workManager.enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling notification", e)
        }
    }

    private fun calculateDelayTo8AM(): Long {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        // Устанавливаем время на 8:00 утра
        calendar.set(Calendar.HOUR_OF_DAY, 8)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Если уже больше 8:00, переносим на следующий день
        if (calendar.timeInMillis < now) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        val delay = calendar.timeInMillis - now
        return delay
    }
}