package com.example.lab_week_08

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class SecondNotificationService : Service() {

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var serviceHandler: Handler

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        notificationBuilder = startForegroundServiceCustom()

        val handlerThread = HandlerThread("ThirdThread").apply { start() }
        serviceHandler = Handler(handlerThread.looper)
    }

    private fun startForegroundServiceCustom(): NotificationCompat.Builder {
        val pendingIntent = getPendingIntent()
        val channelId = createNotificationChannel()
        val notificationBuilder = getNotificationBuilder(pendingIntent, channelId)

        startForeground(NOTIFICATION_ID, notificationBuilder.build())
        return notificationBuilder
    }

    private fun getPendingIntent(): PendingIntent {
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_IMMUTABLE else 0

        return PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), flag
        )
    }

    private fun createNotificationChannel(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "002"
            val channelName = "Second Notification Channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance)

            val manager = requireNotNull(
                ContextCompat.getSystemService(this, NotificationManager::class.java)
            )
            manager.createNotificationChannel(channel)
            channelId
        } else ""
    }

    private fun getNotificationBuilder(pendingIntent: PendingIntent, channelId: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("All processes are complete!")
            .setContentText("Great job — the workflow has finished.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setTicker("All tasks done!")
            .setOngoing(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = intent?.getStringExtra(EXTRA_ID) ?: "002"

        serviceHandler.post {
            countDownFromFive(notificationBuilder)
            notifyCompletion(channelId)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun countDownFromFive(builder: NotificationCompat.Builder) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        for (i in 5 downTo 0) {
            Thread.sleep(1000L)
            builder.setContentText("$i seconds remaining…").setSilent(true)
            manager.notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun notifyCompletion(id: String) {
        mutableID.postValue(id)
    }

    companion object {
        const val NOTIFICATION_ID = 0xCA8
        const val EXTRA_ID = "Id"

        private val mutableID = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableID
    }
}
