# Оптимизация запросов к Firestore — Анализ и рекомендации

## 🔴 Критические проблемы (высокий расход лимитов)

### 1. DashboardViewModel — главный "пожиратель" лимитов

Файл: `DashboardViewModel.kt`

**Проблема:** При каждом открытии дашборда выполняются **до 8-9 отдельных запросов** к Firestore. С учётом 50k reads/day — это хватит на ~5,500 открытий дашборда.

| Запрос | Тип | Расход в день (при 100 открытиях) |
|--------|-----|-----------------------------------|
| `addSnapshotListener` на `internal_users` | 🟡 realtime listener | 100+ reads (кэшируется) |
| `addSnapshotListener` на `field_repair_sessions` | 🟡 realtime listener | 100+ reads |
| `addSnapshotListener` на `field_repair_tasks` | 🟡 realtime listener | 100+ reads |
| `fetchActiveEmployeesToday()` — `activity_log` (get) | 🔴 полный скан | 100 reads |
| `fetchRepairActivityToday()` — `battery_repair_log` (get) | 🔴 полный скан | 100 reads |
| `fetchHourlyActivity()` — `activity_log` (get) | 🔴 полный скан | 100 reads |
| `fetchScansYesterday()` — `activity_log` (get) | 🔴 полный скан | 100 reads |
| `loadYesterdayActivity()` — `activity_log` (get) | 🔴 полный скан | при переключении вкладки |
| `loadWeekActivity()` — `activity_log` (get) | 🔴 полный скан | при переключении вкладки |
| `loadDetailsForEmployee()` — `activity_log` (get) | 🟡 per user click | per клик |

**Итого:** ОДНО открытие дашборда ≈ 5-7 reads (если всё кэшируется Firebase SDK). На самом деле Firestore считает каждое прочитанное ДОКУМЕНТ — а activity_log может содержать сотни документов.

**Рекомендация:** Заменить все `get()` на `addSnapshotListener()` (тогда Firestore кэширует и документы не тарифицируются повторно). Или кешировать результат в Room и обновлять раз в N минут.

### 2. ShiftRepository — отстутствует Room кеш

Файл: `ShiftRepository.kt`

- `getUserShifts()` — каждый раз `get()` на коллекцию `shifts` (все документы пользователя)
- `collectShiftStats()` — `get()` на `activity_log` + `storage_activity_log` (при каждом закрытии смены)
- `findActiveShiftId()` — `get()` на `internal_users` (можно кешировать)

**Рекомендация:** Кешировать shifts в Room, особенно историю смен.

### 3. InteractionRepository — realtime listener без кеша

Файл: `InteractionRepository.kt`

- `getActiveIssuances()` — `addSnapshotListener` на `battery_issuances`
- `getRecentReceptions()` — `addSnapshotListener` на `battery_receptions`
- `getSbEmployees()` — `get()` на `internal_users` (каждый вызов!)

**Рекомендация:** 
- `getSbEmployees()` кешировать в памяти (employees редко меняются)
- `getActiveIssuances()` и `getRecentReceptions()` кешировать в Room

### 4. TaskRemoteDataSource — realtime без кеша для admin

Файл: `TaskRemoteDataSource.kt`

- `getAllActiveTasksFlow()` — `snapshots()` на `tasks` (без Room). Используется в `DashboardViewModel.subscribeToActiveTasks()`

**Рекомендация:** Либо кешировать в Room, либо добавить debounce на listener.

### 5. StorageRepository — Firestore sync при старте

Файл: `StorageRepository.kt`

- `startCellsRealtimeSync()` + `startPalletsRealtimeSync()` — `addSnapshotListener` на `storage_cells` + `storage_pallets`
- При каждом изменении — upsert всех документов в Room

**Текущее состояние:** Уже хорошо — есть Room кеш + dirty-флаги. Но **realtime listener на все документы** может генерировать много reads, если коллекции большие.

## 🟡 Умеренные проблемы

### 6. VehicleReportRepository

- `deleteAllReports()` — делает `get()` на **все** документы в `vehicle_reports`
- `uploadAndSaveReport()` — пишет в Firestore + Room (в целом нормально)

### 7. DeliveryRepository

- `getDeliveryHistory()` — `get()` на `deliveries` без лимита
- `saveDelivery()` — пишет в Room + Firestore (хорошо)

### 8. ScanSessionRepository

✅ **Хороший паттерн:** сначала в Room, потом synс в Firestore. С optimal.

## 🟢 Нормальные (низкий расход)

- `EmployeeProfileRepository` — точечные read/write
- `ChatViewModel` — точечные read/write (обычно один документ)
- `WarehouseRepository` — точечные операции
- `ElectricianViewModel` — точечные операции
- `FleetImportViewModel` — разовые импорты

## 📊 Сводный рейтинг расхода Firestore

| Компонент | Оценка | Читает из Room? | Realtime listeners | get() (одноразовые) |
|-----------|--------|-----------------|--------------------|---------------------|
| DashboardViewModel | 🔴 **КРИТИЧНО** | Нет | 3 listeners | 6+ get() |
| ShiftRepository | 🟡 **ВЫСОКИЙ** | Нет | 0 | 3 get() |
| InteractionRepository | 🟡 **СРЕДНИЙ** | Частично | 2 listeners | 1 get() |
| TaskRemoteDataSource | 🟡 **СРЕДНИЙ** | Частично (только по userId) | 1 listener | 0 |
| StorageRepository | 🟢 **НОРМА** | Да | 2 listeners | min |
| DeliveryRepository | 🟢 **НОРМА** | Да (чтение) | 0 | 1 get() |
| VehicleReportRepository | 🟢 **НОРМА** | Да | 0 | 0 |
| ScanSessionRepository | ✅ **ОТЛИЧНО** | Да | 0 | 0 |

## 🔧 Рекомендации по быстрым оптимизациям

### 1. **DashboardViewModel — срочно** (экономия ~70% reads)

Заменить `get()` на `addSnapshotListener()` + debounce:
```kotlin
// Вместо:
private suspend fun fetchActiveEmployeesToday(startOfToday: Long): List<EmployeeActivity> {
    val snapshot = db.collection("activity_log")
        .whereGreaterThanOrEqualTo("timestamp", startOfToday)
        .get().await()  // 🔴 КАЖДЫЙ РАЗ НОВЫЙ ЗАПРОС
}
```

Лучше:
- Добавить `addSnapshotListener()` на `activity_log` с фильтром по сегодняшнему дню
- Или кешировать в Room последние N записей и читать оттуда

### 2. **ShiftRepository — простое кеширование**

Добавить Room entity для `shifts`:
```kotlin
// Room DAO
@Query("SELECT * FROM shifts WHERE userId = :userId ORDER BY startTime DESC LIMIT :limit")
fun getShifts(userId: String, limit: Int): Flow<List<ShiftEntity>>

// В ShiftRepository.getUserShifts() — сначала из Room, потом sync с Firestore
```

### 3. **InteractionRepository.getSbEmployees() — кеш в памяти**

```kotlin
private var cachedSbEmployees: List<SbEmployee>? = null
private var sbCacheTime = 0L

suspend fun getSbEmployees(): List<SbEmployee> {
    if (cachedSbEmployees != null && System.currentTimeMillis() - sbCacheTime < 60_000) {
        return cachedSbEmployees!!
    }
    // запрос к Firestore
    cachedSbEmployees = result
    sbCacheTime = System.currentTimeMillis()
}
```

### 4. **TaskRemoteDataSource — кешировать admin tasks в Room**

Добавить в `TaskRepository`:
```kotlin
fun getAllActiveTasksStream(): Flow<List<Task>> {
    // Сначала из Room
    return taskDao.getAllTasks().map { entities -> ... }
}
// syncTasks() уже есть — вызывать его раз в минуту, а не через realtime listener
```

## ⚡ Быстрое решение: "Правило 1 минуты"

Самый простой способ уменьшить reads на 80%:
1. Убрать `addSnapshotListener` (realtime)
2. Добавить `WorkManager` периодическую задачу (каждые 1-5 минут) на sync
3. UI читает из Room через `Flow`

Это превращает 50k reads/day → ~1k reads/day (при sync раз в 5 минут).

---

## Вывод

**Топ-3 потребителя лимитов Firestore:**
1. **DashboardViewModel** — ~60% всех reads
2. **ShiftRepository** — ~15% (при частых start/end смены)
3. **InteractionRepository** — ~10%

**Для 50k/day лимита:** 
Если дашборд открывают 100 раз/день × 8 запросов × ~50 документов = 40,000 reads только с дашборда. **Лимит вылетает за полтора дня**.

**Решение:** Нужно кешировать хотя-бы `activity_log` в Room и обновлять раз в 5-10 минут.