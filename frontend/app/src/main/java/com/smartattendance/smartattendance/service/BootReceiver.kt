package com.smartattendance.smartattendance.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smartattendance.smartattendance.data.local.SessionManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val session = SessionManager(context)
            if (session.isLoggedIn() && session.getRole() == "STUDENT") {
                GeofenceManager(context).startGeofencing()
            }
        }
    }
}
