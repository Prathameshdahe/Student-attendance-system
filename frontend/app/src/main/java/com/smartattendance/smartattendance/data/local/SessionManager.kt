package com.smartattendance.smartattendance.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                "kiwi_session",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Android AutoBackup restored the file but the Keystore key was wiped across uninstalls.
            val prefFile = java.io.File("${context.applicationInfo.dataDir}/shared_prefs/kiwi_session.xml")
            if (prefFile.exists()) {
                prefFile.delete()
            }
            EncryptedSharedPreferences.create(
                context,
                "kiwi_session",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_ROLE = "user_role"
        private const val KEY_NAME = "user_name"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_USER_ID = "user_id"
    }

    fun saveSession(token: String, role: String, name: String, email: String, userId: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_ROLE, role)
            .putString(KEY_NAME, name)
            .putString(KEY_EMAIL, email)
            .putString(KEY_USER_ID, userId)
            .apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun getRole(): String? = prefs.getString(KEY_ROLE, null)
    fun getUserName(): String? = prefs.getString(KEY_NAME, null)
    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun isLoggedIn(): Boolean = getToken() != null

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
