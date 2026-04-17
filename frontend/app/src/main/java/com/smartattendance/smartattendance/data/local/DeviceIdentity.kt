package com.smartattendance.smartattendance.data.local

import android.content.Context
import android.provider.Settings

object DeviceIdentity {
    fun getDeviceId(context: Context): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )?.trim().orEmpty()

        return if (androidId.isNotBlank()) androidId else "unknown-android-device"
    }
}
