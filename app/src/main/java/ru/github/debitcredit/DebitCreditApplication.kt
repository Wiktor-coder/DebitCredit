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
            FirebaseApp.initializeApp(this)

            setupFirestore()

            // Запускаем ежедневное уведомление через WorkManager
            NotificationScheduler.scheduleDailyNotification(this)

            // Проверяем обновления в фоновом потоке
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    UpdateChecker.checkForUpdates(this@DebitCreditApplication)
                } catch (e: Exception) {
                    Log.d("Application", "Update check skipped: ${e.message}")
                }
            }

            CoroutineScope(Dispatchers.IO).launch {
//                kotlinx.coroutines.delay(3000)
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
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            firestore.firestoreSettings = settings
        } catch (e: Exception) {
            Log.e("Application", "Error configuring Firestore", e)
        }
    }
}