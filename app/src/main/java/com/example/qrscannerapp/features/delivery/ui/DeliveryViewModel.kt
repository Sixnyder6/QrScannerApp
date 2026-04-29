package com.example.qrscannerapp.features.delivery.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.features.delivery.data.DeliveryRepository
import com.example.qrscannerapp.features.delivery.domain.model.DeliveryLog
import com.example.qrscannerapp.features.delivery.domain.model.DeliveryType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ============================================================================================
// STATE-МОДЕЛИ ДЛЯ ФОРМЫ
// ============================================================================================

/**
 * Ошибки валидации полей формы
 */
data class FormErrors(
    val licensePlate: String? = null,
    val itemCount: String? = null,
    val description: String? = null
) {
    fun hasErrors() = licensePlate != null || itemCount != null || description != null
    fun isEmpty() = !hasErrors()
}

/**
 * Результат отправки формы
 */
sealed class SubmitResult {
    data class Success(val message: String) : SubmitResult()
    data class Error(val message: String) : SubmitResult()
}

/**
 * Единое состояние формы доставки
 */
data class DeliveryFormState(
    val isLoading: Boolean = false,
    val errors: FormErrors = FormErrors(),
    val showConfirmation: Boolean = false,
    val submitResult: SubmitResult? = null,
    val pendingDelivery: DeliveryLog? = null // данные для диалога подтверждения
)

// ============================================================================================
// VIEWMODEL
// ============================================================================================

@HiltViewModel
class DeliveryViewModel @Inject constructor(
    private val repository: DeliveryRepository
) : ViewModel() {

    // ✅ ЕДИНЫЙ источник состояния формы
    private val _formState = MutableStateFlow(DeliveryFormState())
    val formState: StateFlow<DeliveryFormState> = _formState

    // История доставок (для экрана истории)
    private val _history = MutableStateFlow<List<DeliveryLog>>(emptyList())
    val history: StateFlow<List<DeliveryLog>> = _history

    init {
        loadHistory()
    }

    /**
     * Загрузка истории из Firestore
     */
    fun loadHistory() {
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true) }
            repository.getDeliveryHistory()
                .onSuccess { logs ->
                    _history.value = logs
                }
                .onFailure { /* можно добавить логирование */ }
            _formState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * ✅ НОВАЯ: Валидация полей формы
     * @return FormErrors с описанием проблем (если есть)
     */
    fun validateForm(
        lpLetter1: String,
        lpDigits: String,
        lpRegion: String,
        itemCount: String
    ): FormErrors {
        val lpEmpty = lpLetter1.isBlank() || lpDigits.isBlank() || lpRegion.isBlank()
        val countEmpty = itemCount.isBlank()
        val countInvalid = itemCount.toIntOrNull() == null && itemCount.isNotEmpty()

        return FormErrors(
            licensePlate = if (lpEmpty) "Заполните номер автомобиля" else null,
            itemCount = when {
                countEmpty -> "Укажите количество"
                countInvalid -> "Только цифры"
                else -> null
            }
        )
    }

    /**
     * ✅ НОВАЯ: Подготовка к отправке — показывает диалог подтверждения
     */
    fun prepareSubmit(
        type: String,
        licensePlate: String,
        itemCount: Int,
        description: String,
        employeeName: String,
        photoUris: List<Uri>
    ) {
        val deliveryType = when (type) {
            "receive" -> DeliveryType.RECEIVE
            "send" -> DeliveryType.SEND
            else -> DeliveryType.EXPECTED
        }

        // Создаём черновик доставки с фото-заглушками (как в текущей реализации)
        val pendingLog = DeliveryLog(
            type = deliveryType,
            licensePlate = licensePlate.trim().uppercase(),
            itemCount = itemCount,
            description = description.trim(),
            employeeName = employeeName,
            photoUrls = photoUris.map { it.toString() }
        )

        _formState.update {
            it.copy(
                showConfirmation = true,
                pendingDelivery = pendingLog,
                submitResult = null
            )
        }
    }

    /**
     * ✅ НОВАЯ: Подтверждение и реальная отправка в Firestore
     */
    fun confirmSubmit(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val pending = _formState.value.pendingDelivery ?: return@launch

            _formState.update { it.copy(isLoading = true, showConfirmation = false) }

            // Фото уже в log.photoUrls как строки, передаём пустой список для загрузки
            repository.saveDelivery(pending, emptyList())
                .onSuccess {
                    _formState.update {
                        it.copy(
                            isLoading = false,
                            submitResult = SubmitResult.Success("✅ Доставка оформлена!")
                        )
                    }
                    loadHistory() // обновляем список
                    onSuccess()
                }
                .onFailure { e ->
                    _formState.update {
                        it.copy(
                            isLoading = false,
                            submitResult = SubmitResult.Error("❌ Ошибка: ${e.localizedMessage ?: "Неизвестная"}")
                        )
                    }
                }
        }
    }

    /**
     * Отмена диалога подтверждения
     */
    fun dismissConfirmation() {
        _formState.update {
            it.copy(
                showConfirmation = false,
                pendingDelivery = null
            )
        }
    }

    /**
     * Очистка результата отправки (после показа Snackbar/Toast)
     */
    fun clearSubmitResult() {
        _formState.update { it.copy(submitResult = null) }
    }

    /**
     * Сброс ошибок валидации (при изменении поля)
     */
    fun clearErrors() {
        _formState.update { it.copy(errors = FormErrors()) }
    }
}