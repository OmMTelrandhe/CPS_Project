package com.example.fitfit

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import java.util.Calendar
import java.util.concurrent.TimeUnit

object FitManager {

    fun readStepCount(context: Context, onResult: (Int) -> Unit) {
        val account = GoogleSignIn.getAccountForExtension(context, getFitnessOptions())
        val endTime = Calendar.getInstance().timeInMillis
        val startTime = endTime - TimeUnit.DAYS.toMillis(1)

        val request = DataReadRequest.Builder()
            .aggregate(DataType.TYPE_STEP_COUNT_DELTA)
            .bucketByTime(1, TimeUnit.DAYS)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()

        Fitness.getHistoryClient(context, account)
            .readData(request)
            .addOnSuccessListener { response ->
                var totalSteps = 0
                response.buckets.forEach { bucket ->
                    bucket.dataSets.forEach { dataSet ->
                        dataSet.dataPoints.forEach { dataPoint ->
                            dataPoint.dataType.fields.forEach { field ->
                                totalSteps += dataPoint.getValue(field).asInt()
                            }
                        }
                    }
                }
                onResult(totalSteps)
            }
            .addOnFailureListener { e ->
                Log.e("GoogleFit", "Failed to read data", e)
                onResult(0)
            }
    }

    private fun getFitnessOptions() = FitnessOptions.builder()
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .build()
}