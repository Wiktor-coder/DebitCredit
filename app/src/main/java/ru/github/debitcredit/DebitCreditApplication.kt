package ru.github.debitcredit

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.github.debitcredit.services.NotificationScheduler
import ru.github.debitcredit.services.UpdateChecker
import ru.github.debitcredit.utils.NotificationHelper

@HiltAndroidApp
class DebitCreditApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        try {
            // Инициализируем Firebase
            FirebaseApp.initializeApp(this)
            Log.d("Application", "Firebase initialized")

            // Настраиваем Firestore для офлайн-режима
            setupFirestore()

            // Запускаем ежедневное уведомление в 8:00
            NotificationScheduler.scheduleDailyNotification(this)

            // Проверяем обновления в фоновом потоке
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    UpdateChecker.checkForUpdates(this@DebitCreditApplication)
                } catch (e: Exception) {
                    Log.d("Application", "Update check skipped: ${e.message}")
                }
            }

            // Показываем благодарственное уведомление (с задержкой)
            CoroutineScope(Dispatchers.IO).launch {
                kotlinx.coroutines.delay(5000) // Задержка 3 секунды
                NotificationHelper.showAppreciationNotification(this@DebitCreditApplication)
            }

        } catch (e: Exception) {
            Log.e("Application", "Error in onCreate", e)
        }
    }

    @Suppress("DEPRECATION")
    private fun setupFirestore() {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)  // Включаем офлайн-режим
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            firestore.firestoreSettings = settings
            Log.d("Application", "Firestore configured with offline persistence")
        } catch (e: Exception) {
            Log.e("Application", "Error configuring Firestore", e)
        }
    }
}