package com.telco.btsfieldapp.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.telco.btsfieldapp.data.remote.ApiService
import com.telco.btsfieldapp.data.remote.LoginRequest
import com.telco.btsfieldapp.data.remote.LoginResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.ResponseBody
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: ApiService,
    private val dataStore: DataStore<Preferences>,
    private val gson: Gson
) {
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
    }

    val token: Flow<String?> = dataStore.data.map { prefs ->
        prefs[TOKEN_KEY]
    }

    val userName: Flow<String?> = dataStore.data.map { prefs ->
        prefs[USER_NAME_KEY]
    }

    val userEmail: Flow<String?> = dataStore.data.map { prefs ->
        prefs[USER_EMAIL_KEY]
    }

    suspend fun getToken(): String? = dataStore.data.first()[TOKEN_KEY]

    suspend fun isLoggedIn(): Boolean = getToken() != null

    suspend fun login(email: String, password: String): Result<String> {
        return try {
            val response = api.login(LoginRequest(email, password))
            val token = response.token
            if (token != null) {
                dataStore.edit { prefs ->
                    prefs[TOKEN_KEY] = token
                    prefs[USER_NAME_KEY] = response.user?.fullName ?: response.user?.name ?: email
                    prefs[USER_EMAIL_KEY] = response.user?.email ?: email
                }
                Result.success(token)
            } else {
                Result.failure(
                    AuthException(
                        message = response.message ?: "Login failed",
                        code = response.code,
                        attemptsLeft = response.attemptsLeft,
                        retryAfterSeconds = response.retryAfterSeconds
                    )
                )
            }
        } catch (e: HttpException) {
            if (e.code() == 401) {
                val body = e.response()?.errorBody()
                val parsed = parseLoginError(body)
                Result.failure(
                    AuthException(
                        message = parsed.message,
                        code = parsed.code,
                        attemptsLeft = parsed.attemptsLeft,
                        retryAfterSeconds = parsed.retryAfterSeconds
                    )
                )
            } else {
                Result.failure(Exception("Connection error — please check your internet."))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Login failed — please try again."))
        }
    }

    private fun parseLoginError(body: ResponseBody?): AuthException {
        return try {
            val parsed = gson.fromJson(body?.string(), LoginResponse::class.java)
            AuthException(
                message = parsed.message ?: parsed.error ?: "Incorrect email or password",
                code = parsed.code,
                attemptsLeft = parsed.attemptsLeft,
                retryAfterSeconds = parsed.retryAfterSeconds
            )
        } catch (_: Exception) {
            AuthException(message = "Incorrect email or password")
        }
    }

    suspend fun logout() {
        dataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
            prefs.remove(USER_NAME_KEY)
            prefs.remove(USER_EMAIL_KEY)
        }
    }
}

/**
 * Structured auth error carrying server-provided code and retry metadata.
 */
class AuthException(
    override val message: String,
    val code: String? = null,
    val attemptsLeft: Int? = null,
    val retryAfterSeconds: Int? = null
) : Exception(message)
