package com.example.lab_week_08.worker

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.lab_week_08.NotificationService

class SecondWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val id = inputData.getString(INPUT_DATA_ID)

        Thread.sleep(3000L)

        val serviceIntent = Intent(applicationContext, NotificationService::class.java).apply {
            putExtra(NotificationService.EXTRA_ID, id ?: "unknown")
        }

        ContextCompat.startForegroundService(applicationContext, serviceIntent)
        val outputData = Data.Builder()
            .putString(OUTPUT_DATA_ID, id)
            .build()

        return Result.success(outputData)
    }

    companion object {
        const val INPUT_DATA_ID = "inId"
        const val OUTPUT_DATA_ID = "outId"
    }
}
