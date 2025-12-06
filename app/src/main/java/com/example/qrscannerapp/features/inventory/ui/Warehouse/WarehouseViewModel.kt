package com.example.qrscannerapp.features.inventory.ui.Warehouse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.features.inventory.data.*
import com.example.qrscannerapp.features.inventory.ui.Warehouse.components.GroupedActivity
import com.example.qrscannerapp.features.inventory.ui.Warehouse.components.TakenItem
import com.example.qrscannerapp.features.inventory.ui.Warehouse.components.warehouseCatalogItems
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// --- data class для UI состояния ---
data class ShiftState(
    val employee: Employee,
    val status: EmployeeStatus
)

class WarehouseViewModel : ViewModel() {

    private val repository = WarehouseRepository()

    // --- СОСТОЯНИЕ UI (StateFlow) ---

    private val _items = MutableStateFlow<List<WarehouseItem>>(emptyList())
    val items: StateFlow<List<WarehouseItem>> = _items.asStateFlow()

    private val _logs = MutableStateFlow<List<WarehouseLog>>(emptyList())
    val logs: StateFlow<List<WarehouseLog>> = _logs.asStateFlow()

    private val _groupedActivities = MutableStateFlow<List<GroupedActivity>>(emptyList())
    val groupedActivities: StateFlow<List<GroupedActivity>> = _groupedActivities.asStateFlow()

    private val _newsItems = MutableStateFlow<List<NewsItem>>(emptyList())
    val newsItems: StateFlow<List<NewsItem>> = _newsItems.asStateFlow()

    private val _scannedItem = MutableStateFlow<WarehouseItem?>(null)
    val scannedItem: StateFlow<WarehouseItem?> = _scannedItem.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // --- StateFlow для списка сотрудников ---
    private val _employees = MutableStateFlow(demoEmployees) // Заглушка на время загрузки
    val employees: StateFlow<List<Employee>> = _employees.asStateFlow()

    // StateFlow для всего состояния смены
    private val _shiftState = MutableStateFlow(
        ShiftState(employee = demoEmployees.first(), status = EmployeeStatus.ON_SHIFT)
    )
    val shiftState: StateFlow<ShiftState> = _shiftState.asStateFlow()


    // --- СОБЫТИЯ UI (Channel) ---
    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        object NavigateBack : UiEvent()
    }

    private val _uiEvents = Channel<UiEvent>()
    val uiEvents = _uiEvents.receiveAsFlow()

    // --- БЛОК ИНИЦИАЛИЗАЦИИ ---
    init {
        subscribeToWarehouseItems()
        subscribeToWarehouseLogs()
        subscribeToNewsStream()
        subscribeToEmployees()
        subscribeToShiftState()
    }

    private fun subscribeToWarehouseItems() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getWarehouseItems()
                .catch { e ->
                    _isLoading.value = false
                    _uiEvents.send(UiEvent.ShowSnackbar("Ошибка загрузки товаров: ${e.message}"))
                }
                .collect { newItems ->
                    _isLoading.value = false
                    _items.value = newItems
                }
        }
    }

    private fun subscribeToWarehouseLogs() {
        viewModelScope.launch {
            repository.getWarehouseLogs()
                .catch { e ->
                    _uiEvents.send(UiEvent.ShowSnackbar("Ошибка загрузки журнала: ${e.message}"))
                }
                .collect { newLogs ->
                    _logs.value = newLogs
                    _groupedActivities.value = groupLogs(newLogs)
                }
        }
    }

    private fun subscribeToNewsStream() {
        viewModelScope.launch {
            repository.getNewsStream()
                .catch { e ->
                    _uiEvents.send(UiEvent.ShowSnackbar("Ошибка загрузки новостей: ${e.message}"))
                }
                .collect { updatedNews ->
                    _newsItems.value = updatedNews.sortedBy { it.tag }
                }
        }
    }

    private fun subscribeToEmployees() {
        viewModelScope.launch {
            repository.getEmployeesStream()
                .catch { e ->
                    _uiEvents.send(UiEvent.ShowSnackbar("Ошибка загрузки сотрудников: ${e.message}"))
                }
                .collect { employeeList ->
                    if (employeeList.isNotEmpty()) {
                        _employees.value = employeeList
                    }
                }
        }
    }

    private fun subscribeToShiftState() {
        viewModelScope.launch {
            repository.getShiftState()
                .combine(_employees) { stateFromDb, employeeList ->
                    val employee = employeeList.find { it.id == stateFromDb.employeeOnShiftId }
                        ?: employeeList.firstOrNull()
                        ?: demoEmployees.first()

                    val status = try {
                        stateFromDb.employeeStatus?.let { EmployeeStatus.valueOf(it) } ?: EmployeeStatus.ON_SHIFT
                    } catch (e: IllegalArgumentException) {
                        EmployeeStatus.ON_SHIFT
                    }

                    ShiftState(employee, status)
                }
                .catch { e ->
                    _uiEvents.send(UiEvent.ShowSnackbar("Ошибка состояния смены: ${e.message}"))
                }
                .collect { combinedState ->
                    _shiftState.value = combinedState
                }
        }
    }

    private fun groupLogs(logs: List<WarehouseLog>): List<GroupedActivity> {
        if (logs.isEmpty()) return emptyList()
        val groupedList = mutableListOf<GroupedActivity>()
        var currentGroup = mutableListOf(logs.first())
        for (i in 1 until logs.size) {
            val currentLog = logs[i]
            val lastLoginGroup = currentGroup.last()
            val timeDifference = lastLoginGroup.timestamp.toDate().time - currentLog.timestamp.toDate().time
            if (currentLog.userName != lastLoginGroup.userName || timeDifference > 90000) {
                groupedList.add(createGroupedActivity(currentGroup))
                currentGroup = mutableListOf(currentLog)
            } else {
                currentGroup.add(currentLog)
            }
        }
        if (currentGroup.isNotEmpty()) {
            groupedList.add(createGroupedActivity(currentGroup))
        }
        return groupedList
    }

    private fun createGroupedActivity(logsInGroup: List<WarehouseLog>): GroupedActivity {
        val referenceLog = logsInGroup.first()
        val items = logsInGroup.map { log ->
            TakenItem(itemName = log.itemName, quantity = log.quantityChange)
        }
        return GroupedActivity(
            userName = referenceLog.userName,
            items = items,
            timestamp = referenceLog.timestamp.toDate().time
        )
    }

    fun uploadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.uploadDemoData(warehouseCatalogItems)
            _isLoading.value = false
            result.onSuccess { count ->
                _uiEvents.send(UiEvent.ShowSnackbar("Успешно загружено $count товаров!"))
            }.onFailure { e ->
                _uiEvents.send(UiEvent.ShowSnackbar("Ошибка загрузки: ${e.message}"))
            }
        }
    }

    fun onPartScanned(code: String) {
        viewModelScope.launch {
            val foundItem = _items.value.find { item ->
                item.sku == code ||
                        item.id == code ||
                        (item.sku != null && code.contains(item.sku))
            }

            if (foundItem != null) {
                _scannedItem.value = foundItem
                _uiEvents.send(UiEvent.ShowSnackbar("Найдено: ${foundItem.shortName}"))
            } else {
                _uiEvents.send(UiEvent.ShowSnackbar("Товар не найден: $code"))
            }
        }
    }

    fun clearScannedItem() {
        _scannedItem.value = null
    }

    // --- ДЕЙСТВИЯ ПОЛЬЗОВАТЕЛЯ ---

    fun onEmployeeSelected(employee: Employee) {
        viewModelScope.launch {
            val result = repository.updateEmployeeOnShift(employee.id)
            result.onFailure { e ->
                _uiEvents.send(UiEvent.ShowSnackbar("Ошибка смены сотрудника: ${e.message}"))
            }
        }
    }

    fun onStatusSelected(status: EmployeeStatus) {
        viewModelScope.launch {
            val result = repository.updateEmployeeStatus(status.name)
            result.onFailure { e ->
                _uiEvents.send(UiEvent.ShowSnackbar("Ошибка смены статуса: ${e.message}"))
            }
        }
    }

    fun onAddNewItem(
        fullName: String, shortName: String, sku: String,
        category: String, unit: String, totalStock: Int
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val newItem = WarehouseItem(
                fullName = fullName, shortName = shortName, sku = sku.ifBlank { null },
                category = category, unit = unit,
                stockCount = totalStock,
                totalStock = totalStock,
                lowStockThreshold = (totalStock * 0.1).toInt().coerceAtLeast(1)
            )
            val result = repository.addNewItem(newItem)
            _isLoading.value = false
            result.onSuccess {
                _uiEvents.send(UiEvent.ShowSnackbar("Товар '$shortName' создан!"))
                _uiEvents.send(UiEvent.NavigateBack)
            }.onFailure { e ->
                _uiEvents.send(UiEvent.ShowSnackbar("Ошибка создания: ${e.message}"))
            }
        }
    }

    // --- НОВОЕ: Редактирование товара ---
    fun onEditItem(
        originalItem: WarehouseItem,
        fullName: String, shortName: String, sku: String,
        category: String, unit: String, totalStock: Int
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            // Создаем копию с обновленными данными
            // ВАЖНО: При редактировании мы сбрасываем текущий остаток до полного (stockCount = totalStock)
            val updatedItem = originalItem.copy(
                fullName = fullName,
                shortName = shortName,
                sku = sku.ifBlank { null },
                category = category,
                unit = unit,
                totalStock = totalStock,
                stockCount = totalStock,
                lowStockThreshold = (totalStock * 0.1).toInt().coerceAtLeast(1)
            )

            val result = repository.updateItem(updatedItem)
            _isLoading.value = false
            result.onSuccess {
                _uiEvents.send(UiEvent.ShowSnackbar("Товар '$shortName' обновлен!"))
            }.onFailure { e ->
                _uiEvents.send(UiEvent.ShowSnackbar("Ошибка обновления: ${e.message}"))
            }
        }
    }

    // --- НОВОЕ: Удаление товара ---
    fun onDeleteItem(item: WarehouseItem) {
        viewModelScope.launch {
            val result = repository.deleteItem(item.id)
            result.onSuccess {
                _uiEvents.send(UiEvent.ShowSnackbar("Товар '${item.shortName}' удален"))
            }.onFailure { e ->
                _uiEvents.send(UiEvent.ShowSnackbar("Ошибка удаления: ${e.message}"))
            }
        }
    }

    fun onTakeItem(item: WarehouseItem, quantity: Int, userName: String) {
        viewModelScope.launch {
            val result = repository.takeItem(item.id, quantity, userName)
            result.onSuccess {
                _uiEvents.send(UiEvent.ShowSnackbar("Взято ${item.shortName}: $quantity ${item.unit}"))
                _scannedItem.value = null
            }.onFailure { e ->
                _uiEvents.send(UiEvent.ShowSnackbar("Ошибка: ${e.message}"))
            }
        }
    }

    fun onUpdateItemImageUrl(item: WarehouseItem, newUrl: String?) {
        viewModelScope.launch {
            val finalUrl = if (newUrl.isNullOrBlank()) null else newUrl
            val result = repository.updateItemImageUrl(item.id, finalUrl)
            result.onSuccess {
                _uiEvents.send(UiEvent.ShowSnackbar("Фото для '${item.shortName}' обновлено"))
            }.onFailure { e ->
                _uiEvents.send(UiEvent.ShowSnackbar("Ошибка обновления фото: ${e.message}"))
            }
        }
    }

    fun onAddNewsItem(title: String, content: String, tag: NewsTag) {
        viewModelScope.launch {
            val newItem = NewsItem(title = title, content = content, tag = tag)
            val result = repository.addNewsItem(newItem)
            result.onFailure { e ->
                _uiEvents.send(UiEvent.ShowSnackbar("Ошибка добавления: ${e.message}"))
            }
        }
    }

    fun onUpdateNewsItem(item: NewsItem) {
        viewModelScope.launch {
            val result = repository.updateNewsItem(item)
            result.onFailure { e ->
                _uiEvents.send(UiEvent.ShowSnackbar("Ошибка обновления: ${e.message}"))
            }
        }
    }

    fun onDeleteNewsItem(itemId: String) {
        viewModelScope.launch {
            val result = repository.deleteNewsItem(itemId)
            result.onSuccess {
                _uiEvents.send(UiEvent.ShowSnackbar("Новость удалена"))
            }.onFailure { e ->
                _uiEvents.send(UiEvent.ShowSnackbar("Ошибка удаления: ${e.message}"))
            }
        }
    }
}