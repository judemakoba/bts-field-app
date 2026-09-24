package com.telco.btsfieldapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telco.btsfieldapp.data.repository.AuthException
import com.telco.btsfieldapp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val LOCKOUT_DURATION_SECONDS = 30L

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val attemptsLeft: Int? = null,
    val lockoutSeconds: Long = 0L,
    val isLockedOut: Boolean = false
)

sealed class LoginEvent {
    data object NavigateToSites : LoginEvent()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>()
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    private var lockoutJob: Job? = null

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, error = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }

    fun login() {
        val state = _uiState.value

        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(error = "Please enter your email and password") }
            return
        }

        if (state.isLockedOut) {
            _uiState.update { it.copy(error = "Too many attempts. Please wait ${state.lockoutSeconds}s.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, attemptsLeft = null) }

            authRepository.login(state.email, state.password).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, isLockedOut = false, attemptsLeft = null) }
                    _events.emit(LoginEvent.NavigateToSites)
                },
                onFailure = { e ->
                    val authErr = e as? AuthException
                    val serverCode = authErr?.code
                    val attemptsLeft = authErr?.attemptsLeft
                    val retryAfter = authErr?.retryAfterSeconds

                    val friendlyMessage = when (serverCode) {
                        "INVALID_CREDENTIALS" -> {
                            if ((attemptsLeft ?: 0) > 0) {
                                "Incorrect email or password. $attemptsLeft attempt${if (attemptsLeft == 1) "" else "s"} remaining."
                            } else {
                                "Incorrect email or password."
                            }
                        }
                        "ACCOUNT_LOCKED" -> {
                            "Account temporarily locked. Try again in ${retryAfter ?: LOCKOUT_DURATION_SECONDS} seconds."
                        }
                        "MISSING_FIELDS" -> "Please enter both email and password."
                        else -> authErr?.message ?: "Login failed. Please try again."
                    }

                    // Server-side lockout: use server's retry-after if provided
                    if (serverCode == "ACCOUNT_LOCKED" && (retryAfter ?: 0) > 0) {
                        val seconds = retryAfter!!.toLong()
                        _uiState.update {
                            it.copy(isLoading = false, isLockedOut = true, lockoutSeconds = seconds, attemptsLeft = null)
                        }
                        startLockoutCountdown(seconds)
                    } else if ((attemptsLeft ?: 3) <= 1) {
                        // Client-side precaution: last attempt failed, start 30s lockout
                        _uiState.update {
                            it.copy(isLoading = false, isLockedOut = true, lockoutSeconds = LOCKOUT_DURATION_SECONDS, attemptsLeft = null)
                        }
                        startLockoutCountdown(LOCKOUT_DURATION_SECONDS)
                    } else {
                        _uiState.update {
                            it.copy(isLoading = false, error = friendlyMessage, attemptsLeft = attemptsLeft)
                        }
                    }
                }
            )
        }
    }

    private fun startLockoutCountdown(seconds: Long) {
        lockoutJob?.cancel()
        lockoutJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                _uiState.update { it.copy(lockoutSeconds = remaining) }
                delay(1000)
                remaining--
            }
            _uiState.update { it.copy(isLockedOut = false, lockoutSeconds = 0L) }
        }
    }
}
