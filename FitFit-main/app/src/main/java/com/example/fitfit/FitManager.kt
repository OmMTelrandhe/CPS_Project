package com.example.fitfit

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.*
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.request.SensorRequest
import java.util.concurrent.TimeUnit

object FitManager {

    // 🔹 Function to get real-time heart rate updates every 5 seconds
    fun startRealTimeHeartRate(context: Context, onResult: (Float) -> Unit) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) {
            Log.e("GoogleFit", "Google account not found!")
            return
        }

        val sensorRequest = SensorRequest.Builder()
            .setDataType(DataType.TYPE_HEART_RATE_BPM)
            .setSamplingRate(5, TimeUnit.SECONDS)  // Fetch every 5 seconds
            .build()

        Fitness.getSensorsClient(context, account)
            .add(sensorRequest) { dataPoint ->
                val heartRate = dataPoint.getValue(Field.FIELD_BPM).asFloat()
                Log.d("GoogleFit", "Real-time Heart Rate: $heartRate BPM")
                onResult(heartRate)
            }
            .addOnSuccessListener {
                Log.d("GoogleFit", "Real-time heart rate monitoring started")
            }
            .addOnFailureListener { e ->
                Log.e("GoogleFit", "Failed to start real-time heart rate", e)
            }
    }

    // 🔹 Function to fetch all heart rate history entries for today
    fun getHeartRateHistory(context: Context, onResult: (List<Float>) -> Unit) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) {
            Log.e("GoogleFit", "Google account not found!")
            return
        }

        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.HOURS.toMillis(24)  // Last 24 hours of heart rate data

        val readRequest = DataReadRequest.Builder()
            .read(DataType.TYPE_HEART_RATE_BPM)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()

        Fitness.getHistoryClient(context, account)
            .readData(readRequest)
            .addOnSuccessListener { dataReadResponse ->
                val heartRates = mutableListOf<Float>()
                for (dataSet in dataReadResponse.dataSets) {
                    for (dataPoint in dataSet.dataPoints) {
                        val value = dataPoint.getValue(Field.FIELD_BPM).asFloat()
                        heartRates.add(value)
                        Log.d("GoogleFit", "Historical Heart Rate: $value BPM at ${dataPoint.getTimestamp(TimeUnit.MILLISECONDS)}")
                    }
                }
                onResult(heartRates)
            }
            .addOnFailureListener { e ->
                Log.e("GoogleFit", "Failed to fetch heart rate history", e)
            }
    }

    // 🔹 Function to fetch daily step count
    fun readStepCount(context: Context, onResult: (Int) -> Unit) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) {
            Log.e("GoogleFit", "Google account not found!")
            return
        }

        Fitness.getHistoryClient(context, account)
            .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
            .addOnSuccessListener { dataSet ->
                val totalSteps = if (dataSet.dataPoints.isNotEmpty()) {
                    dataSet.dataPoints[0].getValue(Field.FIELD_STEPS).asInt()
                } else 0

                Log.d("GoogleFit", "Steps Retrieved: $totalSteps")
                onResult(totalSteps)
            }
            .addOnFailureListener { e ->
                Log.e("GoogleFit", "Failed to retrieve steps", e)
            }
    }
}
