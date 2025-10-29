package com.example.lab_week_08

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.lab_week_08.worker.FirstWorker
import com.example.lab_week_08.worker.SecondWorker

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Handle edge-to-edge insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Ask for POST_NOTIFICATIONS permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {

                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        val workManager = WorkManager.getInstance(this)

        // Network constraint: only run when connected
        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val id = "001"

        // First worker request
        val firstRequest = OneTimeWorkRequest.Builder(FirstWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(FirstWorker.INPUT_DATA_ID, id))
            .build()

        // Second worker request
        val secondRequest = OneTimeWorkRequest.Builder(SecondWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(SecondWorker.INPUT_DATA_ID, id))
            .build()

        // Chain both workers: first then second
        workManager.beginWith(firstRequest)
            .then(secondRequest)
            .enqueue()

        // Observe completion of first worker
        workManager.getWorkInfoByIdLiveData(firstRequest.id)
            .observe(this) { info ->
                if (info != null && info.state.isFinished) {
                    showResult("First process is done")
                }
            }

        // Observe completion of second worker
        workManager.getWorkInfoByIdLiveData(secondRequest.id)
            .observe(this) { info ->
                if (info != null && info.state.isFinished) {
                    showResult("Second process is done")

                    // When second worker is done, launch the notification service
                    launchNotificationService()
                }
            }
    }

    private fun getIdInputData(idKey: String, idValue: String): Data {
        return Data.Builder()
            .putString(idKey, idValue)
            .build()
    }

    private fun showResult(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Launch NotificationService after second worker finishes
    private fun launchNotificationService() {
        // Observe service completion
        NotificationService.trackingCompletion.observe(this) { id ->
            showResult("Process for Notification Channel ID $id is done!")
        }

        // Create Intent to start the NotificationService
        val serviceIntent = Intent(this, NotificationService::class.java).apply {
            putExtra(EXTRA_ID, "001")
        }

        // Start foreground service
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    companion object {
        const val EXTRA_ID = "Id"
    }
}
