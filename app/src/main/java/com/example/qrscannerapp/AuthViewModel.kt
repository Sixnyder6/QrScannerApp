package com.example.qrscannerapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    val authManager: AuthManager
) : ViewModel() {

    // Проксируем состояние из AuthManager для удобного наблюдения в UI
    private val _authState = MutableStateFlow(authManager.authState.value)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        // Подписываемся на изменения состояния AuthManager
        viewModelScope.launch {
            authManager.authState.collect { newState ->
                _authState.value = newState
            }
        }
    }

    fun login(username: String, password: String) {
        authManager.login(username, password)
    }

    fun logout() {
        authManager.logout()
    }

    fun clearError() {
        authManager.clearError()
    }

    fun startShiftLocally() {
        authManager.startShiftLocally()
    }

    fun endShiftLocally() {
        authManager.endShiftLocally()
    }

    fun goOffline() {
        authManager.goOffline()
    }
}