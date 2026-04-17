package com.smartattendance.smartattendance.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.smartattendance.smartattendance.data.local.DeviceIdentity
import com.smartattendance.smartattendance.data.local.SessionManager
import com.smartattendance.smartattendance.data.model.User
import com.smartattendance.smartattendance.data.remote.ApiClient
import com.smartattendance.smartattendance.data.remote.LoginRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class AuthRepository(private val context: Context) {

    private val api = ApiClient.api
    private val deviceId by lazy { DeviceIdentity.getDeviceId(context) }
    private val clientType = "android-app"

    suspend fun login(email: String, password: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                // Network call
                val response = api.login(
                    clientType = clientType,
                    deviceId = deviceId,
                    request = LoginRequest(email.trim(), password)
                )

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
            } catch (e: HttpException) {
                Result.failure(Exception(extractErrorMessage(e)))
            } catch (e: Exception) {
                Result.failure(Exception(e.message ?: "Login failed"))
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

    private fun extractErrorMessage(error: HttpException): String {
        return try {
            val body = error.response()?.errorBody()?.string().orEmpty()
            val detail = Gson().fromJson(body, JsonObject::class.java)?.get("detail")?.asString
            detail ?: error.message()
        } catch (e: Exception) {
            error.message()
        }
    }
}
