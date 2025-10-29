package com.example.lab_week_08

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.work.*
import androidx.core.content.ContextCompat
import com.example.lab_week_08.worker.FirstWorker
import com.example.lab_week_08.worker.SecondWorker
import com.example.lab_week_08.worker.ThirdWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var workManager: WorkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        workManager = WorkManager.getInstance(this)
        startChainedWorkers()
    }

    private fun startChainedWorkers() {
        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val firstInput = workDataOf(FirstWorker.INPUT_DATA_ID to "001")

        val firstRequest = OneTimeWorkRequest.Builder(FirstWorker::class.java)
            .setInputData(firstInput)
            .setConstraints(networkConstraints)
            .build()

        val secondRequest = OneTimeWorkRequest.Builder(SecondWorker::class.java)
            .setConstraints(networkConstraints)
            .build()

        workManager.getWorkInfoByIdLiveData(firstRequest.id)
            .observe(this) { info ->
                if (info.state.isFinished) {
                    showResult("FirstWorker finished")
                }
            }

        workManager.getWorkInfoByIdLiveData(secondRequest.id)
            .observe(this) { info ->
                if (info.state.isFinished) {
                    showResult("SecondWorker finished")
                    launchNotificationService()
                }
            }

        NotificationService.trackingCompletion.observe(this) { id ->
            showResult("NotificationService finished for ID $id")

            val thirdInput = workDataOf(ThirdWorker.INPUT_DATA_ID to id)
            val thirdRequest = OneTimeWorkRequest.Builder(ThirdWorker::class.java)
                .setInputData(thirdInput)
                .setConstraints(networkConstraints)
                .build()

            workManager.enqueue(thirdRequest)

            workManager.getWorkInfoByIdLiveData(thirdRequest.id)
                .observe(this) { info ->
                    if (info.state.isFinished) {
                        showResult("ThirdWorker finished")
                        // 🔹 Now launch SecondNotificationService
                        launchSecondNotificationService()
                    }
                }
        }

        workManager.beginWith(firstRequest)
            .then(secondRequest)
            .enqueue()
    }

    private fun launchNotificationService() {
        val intent = Intent(this, NotificationService::class.java).apply {
            putExtra(NotificationService.EXTRA_ID, "001")
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun launchSecondNotificationService() {
        val intent = Intent(this, SecondNotificationService::class.java).apply {
            putExtra(SecondNotificationService.EXTRA_ID, "002")
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun showResult(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
