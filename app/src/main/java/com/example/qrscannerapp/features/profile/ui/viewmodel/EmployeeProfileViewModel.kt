// Файл: features/profile/ui/viewmodel/EmployeeProfileViewModel.kt
package com.example.qrscannerapp.features.profile.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.features.profile.data.repository.EmployeeProfileRepository
import com.example.qrscannerapp.features.profile.domain.model.EmployeeProfileUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmployeeProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EmployeeProfileRepository // Инжектим наш новый репозиторий
) : ViewModel() {

    private val userId: String = savedStateHandle.get<String>("userId")!!

    private val _uiState = MutableStateFlow(EmployeeProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        if (userId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Ошибка: ID пользователя не найден.") }
        } else {
            loadInitialData()
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 1. Загружаем статический профиль пользователя
            repository.getUserProfile(userId)
                .onSuccess { userProfile ->
                    _uiState.update { it.copy(userProfile = userProfile) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = "Не удалось загрузить профиль: ${error.message}") }
                    return@launch // Если профиль не загрузился, дальше нет смысла
                }

            // 2. Подписываемся на обновления последней активности
            listenForLastActivity()
        }
    }

    private fun listenForLastActivity() {
        repository.listenForLastActivity(userId)
            .onEach { result ->
                result
                    .onSuccess { lastActivityLog ->
                        // Получаем детали производительности из репозитория
                        val performanceDetails = repository.getPerformanceDetails(lastActivityLog)

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = null,
                                // Если lastActivityLog == null, список будет пустым
                                activityHistory = listOfNotNull(lastActivityLog),
                                performanceDetails = performanceDetails
                            )
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(isLoading = false, error = "Ошибка загрузки активности: ${error.message}")
                        }
                    }
            }
            .launchIn(viewModelScope) // Запускаем и автоматически отменяем подписку
    }
}