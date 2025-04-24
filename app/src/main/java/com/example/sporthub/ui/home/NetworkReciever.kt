package com.example.sporthub.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.sporthub.utils.ConnectivityHelper

class NetworkReceiver(private val onReconnect: () -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            if (ConnectivityHelper.isNetworkAvailable(it)) {
                onReconnect()
            }
        }
    }
}
