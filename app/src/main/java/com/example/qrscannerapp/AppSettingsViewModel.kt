package com.example.qrscannerapp

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    @ApplicationContext context: Context
) : ViewModel() {

    private val settingsManager = SettingsManager(context)

    val dialogAnimation: StateFlow<DialogAnimation> = settingsManager.dialogAnimationFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, DialogAnimation.SCALE_BOUNCY)
}