package com.example.sporthub.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

object LoadingTimeTracker {
    private var startTime: Long = 0
    private val firestore = FirebaseFirestore.getInstance()

    fun start() {
        startTime = System.currentTimeMillis()
    }

    fun stopAndRecord(screenName: String) {
        val duration = System.currentTimeMillis() - startTime
        Log.d("LoadTime", "$screenName loaded in $duration ms")

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
                Log.d("LoadTime", "Updated average load time for $screenName")
            }.addOnFailureListener {
                Log.w("LoadTime", "Failed to update document, trying set() instead")
                docRef.set(
                    mapOf(
                        "average_time" to duration,
                        "visit_count" to 1
                    )
                )
            }

        }.addOnFailureListener {
            Log.e("LoadTime", "Error reading load time data", it)
        }
    }
}
