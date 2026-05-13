package com.privabrowser

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class VideoDownloadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val CHANNEL_ID = "download_channel"
    private val NOTIF_ID = 1001

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra("url") ?: return START_NOT_STICKY
        val fileName = intent.getStringExtra("fileName") ?: "video_${System.currentTimeMillis()}.mp4"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, buildNotification("Downloading...", 0),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIF_ID, buildNotification("Downloading...", 0))
        }
        downloadVideo(url, fileName, startId)

        return START_NOT_STICKY
    }

    private fun downloadVideo(url: String, fileName: String, startId: Int) {
        serviceScope.launch {
            try {
                val outputDir = File(
                    getExternalFilesDir(null),
                    "PrivaBrowser/Downloads"
                ).apply { mkdirs() }

                val outputFile = File(outputDir, fileName)
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connect()

                val fileSize = connection.contentLength.toLong()
                var downloaded = 0L

                FileOutputStream(outputFile).use { output ->
                    connection.inputStream.use { input ->
                        val buffer = ByteArray(8192)
                        var bytes: Int
                        while (input.read(buffer).also { bytes = it } != -1) {
                            output.write(buffer, 0, bytes)
                            downloaded += bytes

                            if (fileSize > 0) {
                                val progress = ((downloaded * 100) / fileSize).toInt()
                                updateNotification("Downloading $fileName", progress)
                            }
                        }
                    }
                }

                // Mark as downloaded in DB
                val db = AppDatabase.getDatabase(this@VideoDownloadService)
                db.videoDao().insert(
                    VideoEntity(
                        title = fileName,
                        url = url,
                        localPath = outputFile.absolutePath,
                        isDownloaded = true,
                        fileSize = outputFile.length(),
                        timestamp = System.currentTimeMillis()
                    )
                )

                showCompleteNotification(fileName)
                connection.disconnect()

            } catch (e: Exception) {
                showErrorNotification(e.message ?: "Download failed")
            } finally {
                stopSelf(startId)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Video Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "PrivaBrowser download progress"
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String, progress: Int): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PrivaBrowser Download")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String, progress: Int) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(text, progress))
    }

    private fun showCompleteNotification(fileName: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Download Complete ✅")
            .setContentText(fileName)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID + 1, notif)
    }

    private fun showErrorNotification(error: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Download Failed ❌")
            .setContentText(error)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID + 2, notif)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
