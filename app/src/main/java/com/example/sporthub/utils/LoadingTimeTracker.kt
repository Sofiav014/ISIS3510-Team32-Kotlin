package com.example.sporthub.utils

import android.content.Context
import android.util.Log
import com.example.sporthub.utils.ConnectivityHelper
import com.google.firebase.firestore.FirebaseFirestore

object LoadingTimeTracker {
    private var startTime: Long = 0
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "LoadingTimeTracker"

    fun start() {
        startTime = System.currentTimeMillis()
        Log.d(TAG, "LoadingTimeTracker started at: $startTime")
    }

    fun stopAndRecord(screenName: String, context: Context) {
        if (startTime == 0L) {
            Log.w(TAG, "LoadingTimeTracker was not started before calling stopAndRecord")
            return
        }

        if (!ConnectivityHelper.isNetworkAvailable(context)) {
            Log.w(TAG, "Skipping Firebase update for $screenName due to no connection")
            return
        }

        val duration = (System.currentTimeMillis() - startTime) / 1000.0
        Log.d(TAG, "$screenName loaded in $duration seconds")

        val docRef = firestore
            .collection("analytics")
            .document("loading_time")
            .collection("all")
            .document(screenName)

        docRef.get().addOnSuccessListener { document ->
            val currentAvg = (document.getDouble("average_time") ?: 0.0)
            val visitCount = (document.getLong("visit_count") ?: 0L)

            val newAvg = ((currentAvg * visitCount) + duration) / (visitCount + 1)

            docRef.update(
                mapOf(
                    "average_time" to newAvg,
                    "visit_count" to visitCount + 1
                )
            ).addOnSuccessListener {
                Log.d(TAG, "Updated average load time for $screenName")
            }.addOnFailureListener {
                Log.w(TAG, "Failed to update document, trying set() instead")
                docRef.set(
                    mapOf(
                        "average_time" to duration,
                        "visit_count" to 1
                    )
                )
            }

        }.addOnFailureListener {
            Log.e(TAG, "Error reading load time data", it)
        }

        // Reset start time
        startTime = 0L
    }
}