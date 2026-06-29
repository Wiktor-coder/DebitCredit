package ru.github.debitcredit.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import ru.github.debitcredit.R

object UpdateChecker {

    private const val TAG = "UpdateChecker"
    private const val COLLECTION = "app_updates"
    private const val DOCUMENT = "latest_version"
    private const val CHANNEL_ID = "update_channel"
    private const val CHANNEL_NAME = "Обновления приложения"
    private const val NOTIFICATION_ID = 1002

    fun checkForUpdates(context: Context) {

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FirebaseFirestore.getInstance()

                val docSnapshot = db.collection(COLLECTION)
                    .document(DOCUMENT)
                    .get()
                    .await()

                if (docSnapshot.exists()) {
                    val data = docSnapshot.data

                    if (data != null) {
                        val versionCode = (data["versionCode"] as? Number)?.toInt() ?: 0
                        val versionName = data["versionName"] as? String ?: ""
                        val downloadUrl = data["downloadUrl"] as? String ?: ""
                        val releaseNotes = data["releaseNotes"] as? String ?: ""

                        val currentVersionCode = getVersionCode(context)

                        if (versionCode > currentVersionCode) {
                            showUpdateNotification(context, versionName, releaseNotes, downloadUrl)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for updates", e)
            }
        }
    }
    @Suppress("DEPRECATION")
    private fun getVersionCode(context: Context): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                packageInfo.versionCode
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting version code", e)
            0
        }
    }

    private fun showUpdateNotification(
        context: Context,
        versionName: String,
        releaseNotes: String,
        downloadUrl: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        // Создаем канал уведомлений (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о новых версиях приложения"
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = downloadUrl.toUri()
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

        val notificationManager = NotificationManagerCompat.from(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Новая версия $versionName")
            .setContentText(releaseNotes)
            .setSmallIcon(R.drawable.ic_ruble)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}