package com.example.qrscannerapp.features.inventory.ui.Warehouse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.features.inventory.data.*
import com.example.qrscannerapp.features.inventory.ui.Warehouse.components.GroupedActivity
import com.example.qrscannerapp.features.inventory.ui.Warehouse.components.TakenItem
import com.example.qrscannerapp.features.inventory.ui.Warehouse.components.warehouseCatalogItems
import kotlinx.coroutines.Job
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
    private val _employees = MutableStateFlow(demoEmployees)
    val employees: StateFlow<List<Employee>> = _employees.asStateFlow()

    // StateFlow для всего состояния смены
    private val _shiftState = MutableStateFlow(
        ShiftState(employee = demoEmployees.first(), status = EmployeeStatus.ON_SHIFT)
    )
    val shiftState: StateFlow<ShiftState> = _shiftState.asStateFlow()

    // ==========================================
    // НОВЫЕ STATEFLOW ДЛЯ ЗАКАЗОВ И КОРЗИНЫ
    // ==========================================

    // Корзина (локальный список товаров перед отправкой заказа)
    private val _cart = MutableStateFlow<List<OrderItem>>(emptyList())
    val cart: StateFlow<List<OrderItem>> = _cart.asStateFlow()

    // Список активных заказов (для кладовщика - все, для техника - свои)
    private val _orders = MutableStateFlow<List<WarehouseOrder>>(emptyList())
    val orders: StateFlow<List<WarehouseOrder>> = _orders.asStateFlow()

    private var ordersJob: Job? = null

    private val _employeeHistory = MutableStateFlow<List<WarehouseLog>>(emptyList())
    val employeeHistory: StateFlow<List<WarehouseLog>> = _employeeHistory.asStateFlow()

    private val _isEmployeeHistoryLoading = MutableStateFlow(false)
    val isEmployeeHistoryLoading: StateFlow<Boolean> = _isEmployeeHistoryLoading.asStateFlow()

    private var employeeHistoryJob: Job? = null

    private val _allUsers = MutableStateFlow<List<Employee>>(emptyList())
    val allUsers: StateFlow<List<Employee>> = _allUsers.asStateFlow()


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
        subscribeToAllUsers()
        // Подписка на заказы происходит позже, когда мы знаем User ID и роль (в Composable)
    }

    // ==========================================
    // ЛОГИКА КОРЗИНЫ (CART)
    // ==========================================

    fun onAddToCart(item: WarehouseItem, quantity: Int) {
        val currentCart = _cart.value.toMutableList()
        val existingIndex = currentCart.indexOfFirst { it.itemId == item.id }

        if (existingIndex != -1) {
            // Если товар уже есть, обновляем количество
            val existingItem = currentCart[existingIndex]
            val newQuantity = existingItem.quantity + quantity
            // Проверка, чтобы не превысить складской остаток (опционально, но полезно)
            if (newQuantity > item.stockCount) {
                viewModelScope.launch { _uiEvents.send(UiEvent.ShowSnackbar("Нельзя заказать больше, чем есть на складе")) }
                return
            }
            currentCart[existingIndex] = existingItem.copy(quantity = newQuantity)
        } else {
            // Добавляем новый
            if (quantity > item.stockCount) {
                viewModelScope.launch { _uiEvents.send(UiEvent.ShowSnackbar("Нельзя заказать больше, чем есть на складе")) }
                return
            }
            currentCart.add(
                OrderItem(
                    itemId = item.id,
                    itemName = item.shortName,
                    itemImageUrl = item.imageUrl,
                    quantity = quantity,
                    unit = item.unit
                )
            )
        }
        _cart.value = currentCart
        viewModelScope.launch { _uiEvents.send(UiEvent.ShowSnackbar("Добавлено в корзину")) }
    }

    fun onRemoveFromCart(itemId: String) {
        val currentCart = _cart.value.toMutableList()
        currentCart.removeIf { it.itemId == itemId }
        _cart.value = currentCart
    }

    fun onClearCart() {
        _cart.value = emptyList()
    }

    fun onSubmitOrder(userId: String, userName: String, userRole: String) {
        if (_cart.value.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            val newOrder = WarehouseOrder(
                userId = userId,
                userName = userName,
                userRole = userRole,
                items = _cart.value,
                status = OrderStatus.CREATED
            )

            val result = repository.createOrder(newOrder)
            _isLoading.value = false

            result.onSuccess {
                _cart.value = emptyList() // Очищаем корзину
                _uiEvents.send(UiEvent.ShowSnackbar("Заказ успешно отправлен!"))
                _uiEvents.send(UiEvent.NavigateBack) // Возвращаемся на главную
            }.onFailure { e ->
                _uiEvents.send(UiEvent.ShowSnackbar("Ошибка создания заказа: ${e.message}"))
            }
        }
    }

    // ==========================================
    // ЛОГИКА ЗАКАЗОВ (ORDERS)
    // ==========================================

    /**
     * Инициализирует подписку на заказы.
     * Вызывать из UI один раз при входе на экран склада.
     * @param userId ID текущего пользователя
     * @param isAdmin Если true (кладовщик), видит ВСЕ заказы. Если false (техник), видит только свои.
     */
    fun subscribeToOrders(userId: String, isAdmin: Boolean) {
        // Отменяем предыдущую подписку, если есть
        ordersJob?.cancel()

        ordersJob = viewModelScope.launch {
            val filterId = if (isAdmin) null else userId
            repository.getActiveOrdersStream(filterId)
                .catch { e ->
                    _uiEvents.send(UiEvent.ShowSnackbar("Ошибка загрузки заказов: ${e.message}"))
                }
                .collect { activeOrders ->
                    _orders.value = activeOrders
                }
        }
    }

    // 1. Кладовщик нажимает "Принять" -> Статус "В сборке"
    fun onAcceptOrder(order: WarehouseOrder) {
        updateStatus(order, OrderStatus.PROCESSING, "Заказ принят в сборку")
    }

    // 2. Кладовщик собрал -> Статус "Готов к выдаче"
    fun onMarkOrderReady(order: WarehouseOrder) {
        updateStatus(order, OrderStatus.READY, "Заказ готов к выдаче")
    }

    // 3. Кладовщик отдает -> Статус "Выдан" + Списание со склада
    fun onFinishOrder(order: WarehouseOrder, warehouseManName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.completeOrder(order.id, warehouseManName)
            _isLoading.value = false

            result.onSuccess {
                _uiEvents.send(UiEvent.ShowSnackbar("Заказ завершен. Товары списаны."))
            }.onFailure { e ->
                _uiEvents.send(UiEvent.ShowSnackbar("Ошибка завершения заказа: ${e.message}"))
            }
        }
    }

    // 4. Отмена заказа
    fun onCancelOrder(order: WarehouseOrder) {
        updateStatus(order, OrderStatus.CANCELLED, "Заказ отменен")
    }

    private fun updateStatus(order: WarehouseOrder, status: OrderStatus, successMsg: String) {
        viewModelScope.launch {
            val result = repository.updateOrderStatus(order.id, status)
            result.onSuccess {
                _uiEvents.send(UiEvent.ShowSnackbar(successMsg))
            }.onFailure { e ->
                _uiEvents.send(UiEvent.ShowSnackbar("Ошибка обновления статуса: ${e.message}"))
            }
        }
    }

    // ==========================================
    // СТАРЫЙ КОД (БЕЗ ИЗМЕНЕНИЙ)
    // ==========================================

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

    private fun subscribeToAllUsers() {
        viewModelScope.launch {
            repository.getAllInternalUsers()
                .catch { /* тихо — список не критичен */ }
                .collect { users -> _allUsers.value = users }
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
        description: String?, category: String, unit: String,
        totalStock: Int, lowStockThreshold: Int
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val newItem = WarehouseItem(
                fullName = fullName, shortName = shortName, sku = sku.ifBlank { null },
                description = description,
                category = category, unit = unit,
                stockCount = totalStock,
                totalStock = totalStock,
                lowStockThreshold = lowStockThreshold
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

    fun onEditItem(
        originalItem: WarehouseItem,
        fullName: String, shortName: String, sku: String,
        description: String?, category: String, unit: String,
        totalStock: Int, lowStockThreshold: Int
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val updatedItem = originalItem.copy(
                fullName = fullName,
                shortName = shortName,
                sku = sku.ifBlank { null },
                description = description,
                category = category,
                unit = unit,
                totalStock = totalStock,
                stockCount = totalStock,
                lowStockThreshold = lowStockThreshold
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

    fun onTakeItem(item: WarehouseItem, quantity: Int, userName: String, userId: String = "") {
        viewModelScope.launch {
            val result = repository.takeItem(item.id, quantity, userName, userId)
            result.onSuccess {
                _uiEvents.send(UiEvent.ShowSnackbar("Взято ${item.shortName}: $quantity ${item.unit}"))
                _scannedItem.value = null
            }.onFailure { e ->
                _uiEvents.send(UiEvent.ShowSnackbar("Ошибка: ${e.message}"))
            }
        }
    }

    fun loadEmployeeHistory(userId: String) {
        employeeHistoryJob?.cancel()
        _isEmployeeHistoryLoading.value = true
        employeeHistoryJob = viewModelScope.launch {
            repository.getLogsForEmployee(userId)
                .catch { _isEmployeeHistoryLoading.value = false }
                .collect { logs ->
                    _employeeHistory.value = logs
                    _isEmployeeHistoryLoading.value = false
                }
        }
    }

    fun clearEmployeeHistory() {
        employeeHistoryJob?.cancel()
        _employeeHistory.value = emptyList()
        _isEmployeeHistoryLoading.value = false
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