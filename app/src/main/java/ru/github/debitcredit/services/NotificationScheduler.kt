package ru.github.debitcredit.services

import android.content.Context
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
            val workManager = WorkManager.getInstance(context)

            // Проверяем, запланирована ли уже работа
            val workInfos = workManager.getWorkInfosForUniqueWork(WORK_NAME).get()
            if (workInfos.isNotEmpty() && workInfos[0].state == WorkInfo.State.ENQUEUED) {
                Log.d(TAG, "Work already scheduled")
                return
            }

            // Вычисляем задержку до 8:00
            val delay = calculateDelayTo8AM()
            Log.d(TAG, "Delay to 8:00 AM: ${delay / 1000 / 60} minutes")

            // Создаем constraints
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresDeviceIdle(false)
                .setRequiresBatteryNotLow(false)
                .build()

            // Создаем WorkRequest - БЕЗ setExpedited для отложенных задач
            val workRequest = OneTimeWorkRequestBuilder<DailyNotificationWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .addTag(WORK_NAME)
                .build()

            // Запускаем работу
            workManager.enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

            Log.d(TAG, "Daily notification scheduled for 8:00 AM")

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

        return calendar.timeInMillis - now
    }

    fun cancelNotification(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        Log.d(TAG, "Notification cancelled")
    }
}