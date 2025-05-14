package com.example.sporthub.utils

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import java.util.concurrent.TimeUnit

object BookingTimeTracker {
    private const val TAG = "BookingTimeTracker"

    // Firebase path for booking creation analytics
    private const val ANALYTICS_PATH = "analytics/screen_time/all/Create Booking View"

    // Start time of the booking process
    private var startTimeMillis: Long = 0

    // Flag to track if timer is running
    private var isTimerRunning = false

    /**
     * Start the booking timer when the user begins the booking creation process
     */
    fun startTimer() {
        if (!isTimerRunning) {
            startTimeMillis = System.currentTimeMillis()
            isTimerRunning = true
            Log.d(TAG, "Booking creation timer started")
        }
    }

    /**
     * Stop the timer and record the time spent in Firebase
     */
    fun stopTimerAndSave(context: Context? = null) {
        if (isTimerRunning) {
            val endTimeMillis = System.currentTimeMillis()
            val durationSeconds = TimeUnit.MILLISECONDS.toSeconds(endTimeMillis - startTimeMillis)

            // Check connectivity if context is provided
            if (context != null && !ConnectivityHelper.isNetworkAvailable(context)) {
                Log.w(TAG, "No internet connection, cannot save booking creation time")
                isTimerRunning = false
                return
            }

            saveBookingCreationTime(durationSeconds)
            isTimerRunning = false
            Log.d(TAG, "Booking creation completed in $durationSeconds seconds")
        }
    }

    /**
     * Save the booking creation time to Firebase analytics
     */
    private fun saveBookingCreationTime(durationSeconds: Long) {
        val db = FirebaseFirestore.getInstance()
        val analyticsRef = db.document(ANALYTICS_PATH)

        // Update the average time and increment visit count
        analyticsRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                // Existing document - update average and count
                val currentAverage = document.getDouble("average_time") ?: 0.0
                val currentCount = document.getLong("visit_count") ?: 0L

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
                    Log.d(TAG, "Booking creation analytics updated: avg=$newAverage, count=$newCount")
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Error updating booking creation analytics", e)
                }
            } else {
                // First booking creation - create document
                val data = hashMapOf(
                    "average_time" to durationSeconds.toDouble(),
                    "visit_count" to 1L,
                    "last_updated" to FieldValue.serverTimestamp()
                )

                analyticsRef.set(data)
                    .addOnSuccessListener {
                        Log.d(TAG, "First booking creation analytics saved: time=$durationSeconds")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error saving first booking creation analytics", e)
                    }
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error reading booking creation analytics", e)
        }
    }
}