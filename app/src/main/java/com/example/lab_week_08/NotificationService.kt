package com.example.lab_week_08


import android.os.Looper
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

class NotificationService : Service() {

    // Notification builder
    private lateinit var notificationBuilder: NotificationCompat.Builder

    // Handler to manage background thread
    private lateinit var serviceHandler: Handler

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        // Create and start the notification
        notificationBuilder = startForegroundServiceCustom()

        // Create a background handler thread
        val handlerThread = HandlerThread("SecondThread").apply { start() }
        serviceHandler = Handler(handlerThread.looper)
    }

    /**
     * Builds and starts the foreground service notification.
     */
    private fun startForegroundServiceCustom(): NotificationCompat.Builder {
        // Create the PendingIntent for when the user taps the notification
        val pendingIntent = getPendingIntent()

        // Create a notification channel (required for Android 8.0+)
        val channelId = createNotificationChannel()

        // Build the actual notification
        val notificationBuilder = getNotificationBuilder(pendingIntent, channelId)

        // Start the service in the foreground
        startForeground(NOTIFICATION_ID, notificationBuilder.build())

        return notificationBuilder
    }

    /**
     * Creates a PendingIntent to open MainActivity when the user taps the notification.
     */
    private fun getPendingIntent(): PendingIntent {
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_IMMUTABLE else 0

        return PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            flag
        )
    }

    /**
     * Creates a NotificationChannel for Android 8.0+ devices.
     */
    private fun createNotificationChannel(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "001"
            val channelName = "001 Channel"
            val channelPriority = NotificationManager.IMPORTANCE_DEFAULT

            val channel = NotificationChannel(channelId, channelName, channelPriority)

            val service = requireNotNull(
                ContextCompat.getSystemService(this, NotificationManager::class.java)
            )
            service.createNotificationChannel(channel)

            channelId
        } else {
            ""
        }
    }

    /**
     * Builds the notification itself.
     */
    private fun getNotificationBuilder(
        pendingIntent: PendingIntent,
        channelId: String
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Second worker process is done")
            .setContentText("Check it out!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setTicker("Second worker process is done, check it out!")
            .setOngoing(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        val returnValue = super.onStartCommand(intent,
            flags, startId)
        //Gets the channel id passed from the MainActivity through the Intent
        val Id = intent?.getStringExtra(EXTRA_ID)
            ?: throw IllegalStateException("Channel ID must be provided")
        //Posts the notification task to the handler,
        //which will be executed on a different thread
        serviceHandler.post {
            //Sets up what happens after the notification is posted
            //Here, we're counting down from 10 to 0 in the notification
            countDownFromTenToZero(notificationBuilder)
            //Here we're notifying the MainActivity that the service process is
            //done
            //by returning the channel ID through LiveData
            notifyCompletion(Id)
            //Stops the foreground service, which closes the notification
            //but the service still goes on
            stopForeground(STOP_FOREGROUND_REMOVE)
            //Stop and destroy the service
            stopSelf()
        };return returnValue
    }
//A function to update the notification to display a count down from 10 to
    //0
    private fun countDownFromTenToZero(notificationBuilder:
                                       NotificationCompat.Builder) {
        //Gets the notification manager
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as
                NotificationManager
        //Count down from 10 to 0
        for (i in 10 downTo 0) {
            Thread.sleep(1000L)
            //Updates the notification content text
            notificationBuilder.setContentText("$i seconds until last warning")
                .setSilent(true)
            //Notify the notification manager about the content update
            notificationManager.notify(
                NOTIFICATION_ID,
                notificationBuilder.build()
            )
        }
    }
    //Update the LiveData with the returned channel id through the Main Thread
//the Main Thread is identified by calling the "getMainLooper()" method
//This function is called after the count down has completed
    private fun notifyCompletion(Id: String) {
        Handler(Looper.getMainLooper()).post {
            mutableID.value = Id
        }
    }


    companion object {
        const val NOTIFICATION_ID = 0xCA7
        const val EXTRA_ID = "Id"

        // LiveData that can be observed by other components to track service completion
        private val mutableID = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableID
    }
}
