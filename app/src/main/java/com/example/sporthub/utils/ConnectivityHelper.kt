package com.example.sporthub.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar

object ConnectivityHelper {
    @JvmStatic
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            networkInfo.isConnected
        }
    }

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
