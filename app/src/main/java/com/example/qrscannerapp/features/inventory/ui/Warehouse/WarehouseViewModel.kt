package com.example.qrscannerapp.features.inventory.ui.Warehouse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.features.inventory.domain.model.SparePartItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WarehouseViewModel @Inject constructor(
    // TODO: Здесь в будущем будет репозиторий для получения данных о запчастях
    // private val sparePartRepository: SparePartRepository
) : ViewModel() {

    // "Приватное" состояние списка, которое можно изменять только внутри ViewModel
    private val _scannedParts = MutableStateFlow<List<SparePartItem>>(emptyList())
    // "Публичное" состояние, на которое будет подписываться UI (только для чтения)
    val scannedParts: StateFlow<List<SparePartItem>> = _scannedParts

    /**
     * Вызывается, когда сканер распознал новый код запчасти.
     */
    fun onPartScanned(code: String) {
        viewModelScope.launch {
            // TODO: Здесь будет логика для получения реального названия запчасти по ее коду
            val partName = "Запчасть (код: $code)" // Временная заглушка

            val existingPart = _scannedParts.value.find { it.code == code }

            if (existingPart != null) {
                // Если запчасть уже есть в списке, просто увеличиваем ее количество на 1
                _scannedParts.value = _scannedParts.value.map {
                    if (it.code == code) it.copy(quantity = it.quantity + 1) else it
                }
            } else {
                // Если это новая запчасть, добавляем ее в начало списка с количеством 1
                val newPart = SparePartItem(code = code, name = partName, quantity = 1)
                _scannedParts.value = listOf(newPart) + _scannedParts.value
            }
        }
    }

    /**
     * Обновляет количество для указанной запчасти.
     */
    fun updateQuantity(partCode: String, newQuantity: Int) {
        if (newQuantity <= 0) {
            // Если новое количество 0 или меньше, удаляем элемент из списка
            _scannedParts.value = _scannedParts.value.filterNot { it.code == partCode }
        } else {
            // Иначе обновляем количество у нужного элемента
            _scannedParts.value = _scannedParts.value.map {
                if (it.code == partCode) it.copy(quantity = newQuantity) else it
            }
        }
    }

    /**
     * Полностью очищает список отсканированных запчастей.
     */
    fun clearList() {
        _scannedParts.value = emptyList()
    }
}