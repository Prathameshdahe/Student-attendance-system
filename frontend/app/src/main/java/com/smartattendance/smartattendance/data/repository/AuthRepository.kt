package com.smartattendance.smartattendance.data.repository

import android.content.Context
import com.smartattendance.smartattendance.data.local.SessionManager
import com.smartattendance.smartattendance.data.model.User
import com.smartattendance.smartattendance.data.remote.ApiClient
import com.smartattendance.smartattendance.data.remote.LoginRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(private val context: Context) {

    private val api = ApiClient.api

    suspend fun login(email: String, password: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                // Network call
                val response = api.login(LoginRequest(email.trim(), password))

                // EncryptedSharedPreferences write (crypto — keep on IO thread)
                val session = SessionManager(context)
                session.saveSession(
                    token = response.token,
                    role = response.role,
                    name = response.name,
                    email = response.email,
                    userId = response.user_id
                )

                Result.success(
                    User(
                        id = response.user_id,
                        name = response.name,
                        email = response.email,
                        role = response.role
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun logout() {
        // SessionManager creation is quick after first time (key already exists in keystore)
        // but still best to avoid on main thread — callers should use withContext(IO) if needed
        try {
            SessionManager(context).clearSession()
        } catch (e: Exception) {
            // Ignore keystore errors during logout
        }
    }

    fun isLoggedIn(): Boolean = try {
        SessionManager(context).isLoggedIn()
    } catch (e: Exception) {
        false
    }

    fun getRole(): String? = try {
        SessionManager(context).getRole()
    } catch (e: Exception) {
        null
    }

    fun getUserName(): String? = try {
        SessionManager(context).getUserName()
    } catch (e: Exception) {
        null
    }
}
