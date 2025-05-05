package com.example.sporthub.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import java.util.concurrent.TimeUnit

object RegistrationTimerManager {
    private const val TAG = "RegistrationTimerManager"

    // Firebase path for analytics storage
    private const val ANALYTICS_PATH = "analytics/registration_time/all/Registration Flow"

    // Start time of the registration process
    private var startTimeMillis: Long = 0

    // Flag to track if timer is running
    private var isTimerRunning = false

    /**
     * Start the registration timer when the user begins the registration flow
     */
    fun startTimer() {
        if (!isTimerRunning) {
            startTimeMillis = System.currentTimeMillis()
            isTimerRunning = true
            Log.d(TAG, "Registration timer started")
        }
    }

    /**
     * Stop the timer and record the time spent in Firebase
     */
    fun stopTimerAndSave() {
        if (isTimerRunning) {
            val endTimeMillis = System.currentTimeMillis()
            val durationSeconds = TimeUnit.MILLISECONDS.toSeconds(endTimeMillis - startTimeMillis)

            saveRegistrationTime(durationSeconds)

            isTimerRunning = false
            Log.d(TAG, "Registration completed in $durationSeconds seconds")
        }
    }

    /**
     * Save the registration time to Firebase analytics
     */
    private fun saveRegistrationTime(durationSeconds: Long) {
        val db = FirebaseFirestore.getInstance()
        val analyticsRef = db.document(ANALYTICS_PATH)

        // Update the average time and increment visit count
        analyticsRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                // Existing document - update average and count
                val currentAverage = document.getLong("average_time") ?: 0
                val currentCount = document.getLong("visit_count") ?: 0

                // Calculate new average
                val newCount = currentCount + 1
                val newAverage = ((currentAverage * currentCount) + durationSeconds) / newCount

                // Update with new values
                analyticsRef.update(
                    mapOf(
                        "average_time" to newAverage,
                        "visit_count" to newCount,
                        "last_updated" to FieldValue.serverTimestamp()
                    )
                ).addOnSuccessListener {
                    Log.d(TAG, "Registration analytics updated: avg=$newAverage, count=$newCount")
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Error updating registration analytics", e)
                }
            } else {
                // First registration - create document
                val data = hashMapOf(
                    "average_time" to durationSeconds,
                    "visit_count" to 1L,
                    "last_updated" to FieldValue.serverTimestamp()
                )

                analyticsRef.set(data)
                    .addOnSuccessListener {
                        Log.d(TAG, "First registration analytics saved: time=$durationSeconds")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error saving first registration analytics", e)
                    }
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error reading registration analytics", e)
        }
    }
}