package com.example.fitfit

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType

private const val REQUEST_CODE_GOOGLE_FIT_PERMISSIONS = 1
private const val AUTO_FETCH_INTERVAL = 10000L  // 10 seconds

class MainActivity : AppCompatActivity() {

    private lateinit var tvStepCount: TextView
    private lateinit var tvHeartRate: TextView
    private lateinit var tvHeartRateHistory: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val autoFetchRunnable = object : Runnable {
        override fun run() {
            fetchHealthData()
            handler.postDelayed(this, AUTO_FETCH_INTERVAL)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStepCount = findViewById(R.id.tvStepCount)
        tvHeartRateHistory = findViewById(R.id.tvHeartRateHistory)

        // Request Google Fit permissions
        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), getFitnessOptions())) {
            GoogleSignIn.requestPermissions(
                this,
                REQUEST_CODE_GOOGLE_FIT_PERMISSIONS,
                GoogleSignIn.getLastSignedInAccount(this),
                getFitnessOptions()
            )
        } else {
            startAutoFetch()
        }
    }

    private fun startAutoFetch() {
        fetchHealthData()  // Fetch immediately
        handler.postDelayed(autoFetchRunnable, AUTO_FETCH_INTERVAL)  // Start auto-fetch
    }

    private fun fetchHealthData() {
        FitManager.readStepCount(this) { steps ->
            runOnUiThread { tvStepCount.text = getString(R.string.steps_count, steps) }
        }

        FitManager.getHeartRateHistory(this) { heartRates ->
            val historyText = heartRates.joinToString("\n") { "$it BPM" }
            runOnUiThread { tvHeartRateHistory.text = getString(R.string.heart_rate_history, historyText) }
        }

        // Start real-time heart rate updates
        FitManager.startRealTimeHeartRate(this) { heartRate ->
            runOnUiThread { tvHeartRate.text = getString(R.string.heart_rate, heartRate) }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_GOOGLE_FIT_PERMISSIONS) {
            startAutoFetch()
        }
    }

    private fun getFitnessOptions() = FitnessOptions.builder()
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
        .build()
}
