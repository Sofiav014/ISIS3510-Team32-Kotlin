package com.example.sporthub.utils

import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar

/**
 * Extension to the existing ConnectivityHelper to add user-friendly error notifications
 */
object ConnectivityHelperExt {

    /**
     * Checks if network is available and shows a message if it's not
     * @param context The context to use for checking connectivity and showing message
     * @param rootView The view to show Snackbar on (optional)
     * @return true if network is available, false otherwise
     */
    fun checkNetworkAndNotify(context: Context, rootView: View? = null): Boolean {
        val isNetworkAvailable = ConnectivityHelper.isNetworkAvailable(context)

        if (!isNetworkAvailable) {
            val message = "No internet connection. Please check your Wi-Fi or mobile data and try again."

            // Show snackbar if rootView is provided, otherwise fallback to Toast
            if (rootView != null) {
                Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                    .setAction("Retry") {
                        // Retry option if needed
                    }
                    .show()
            } else {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }

        return isNetworkAvailable
    }
}