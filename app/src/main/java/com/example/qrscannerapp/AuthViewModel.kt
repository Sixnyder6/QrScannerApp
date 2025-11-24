package com.example.qrscannerapp
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
@HiltViewModel
class AuthViewModel @Inject constructor(
    val authManager: AuthManager
) : ViewModel() {
// Эта ViewModel служит "контейнером" для AuthManager,
// чтобы Hilt мог предоставить его в Composable-функциях.
}