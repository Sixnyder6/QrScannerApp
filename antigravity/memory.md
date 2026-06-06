# ANTIGRAVITY Memory (Карта проекта QrScannerApp)

Этот файл содержит ключевую информацию и карту проекта для Antigravity при работе с репозиторием QrScannerApp.

---

## 🚨 БЫСТРЫЕ ЗАДАЧИ — БЕЗ КОНТЕКСТА

Если запрос содержит конкретную ошибку компилятора или рантайма с указанием номера строки (например, "Expecting '}' :459", "Unresolved reference :22" и т.д.):
1. Открыть только указанный файл в окрестности проблемной строки (±20 строк).
2. Исправить ошибку и завершить задачу.
3. Не выполнять поиск по проекту и не читать лишние файлы/документы.

---

## ⚠️ ПРАВИЛА ОБЩЕНИЯ

- **Все ответы и объяснения формулировать на русском языке.** Идентификаторы кода — на английском.
- Писать без вступительных преамбул вроде "Конечно, сейчас сделаю" — сразу переходить к сути дела.
- Если задача неясна, задавать **один** конкретный уточняющий вопрос.
- Пользователь — "vibe coder" (не профессиональный разработчик). Объяснения должны быть краткими, простыми, без лишней теории.
- **Не предлагать рефакторинг "за компанию"** — реализовывать только то, о чём явно попросил пользователь.
- **Не предлагать миграции** на другую архитектуру или технологический стек без прямой просьбы.

---

## 🧭 КАРТА ПРОЕКТА И НАВИГАЦИЯ

### Feature-based Clean Architecture

Каждая фича расположена в папке `features/` и имеет следующую структуру:
```
feature/
├── domain/model/       # Бизнес-модели и enum
├── data/
│   ├── local/          # Room entities + DAOs
│   ├── remote/         # Firestore data sources
│   ├── repository/     # Repository implementations
│   └── mapper/         # Domain ↔ entity конвертация
└── ui/
    ├── viewmodel/      # Hilt-injected StateFlow ViewModels
    └── screens/        # Compose screens
```

### Список фич и приоритеты разработки

| Приоритет | Фичи |
|-----------|------|
| 🟢 **Активная разработка** | `street_doctor`, `inventory` |
| 🟡 **Стабильные (не трогать без причины)** | `chat`, `shift`, `security`, `scanner`, `tasks`, `homescreen` (виджеты главного экрана) |
| 🔵 **Второстепенные** | `delivery`, `electrician`, `interaction`, `profile`, `team`, `vehicle_report`, `visual_repair`, `settings` (настройки) |

### Ключевые точки входа и инфраструктура

- **Навигация и Маршрутизация**: `MainActivity.kt` и `MainApp.kt` (входная Compose-точка, Drawer, BottomBar)
- **DI**: `app/src/main/java/com/.../di/` (Hilt модули: `DatabaseModule`, `GsonModule`, `WorkManagerModule`, `AppModule.kt`)
- **База данных (Room)**: `AppDatabase.kt` и `OfflineEntities.kt` (описание общих сущностей)
- **Общие компоненты (Shared UI)**: `common/` — Firebase/Cloudinary утилиты и Compose компоненты
- **Фоновые задачи**: `worker/` — WorkManager background jobs (`UpdateWorker`, `ShiftAutoEndWorker`)
- **Кросс-фичные перечисления**: `core/model/` (`CoreEnums.kt` с `ActiveTab`, `SessionType`)
- **Телеметрия**: `TelemetryManager.kt` и `TelemetryRepository.kt` (буферизация)

### Документация по модулям

При работе над сложными архитектурными задачами (новый экран, новая фича) обращаться к документации:
- `street_doctor/` → [street_doctor.md](file:///C:/Users/pankr/AndroidStudioProjects/QrScannerApp/docs/street_doctor.md)
- `inventory/` → [inventory.md](file:///C:/Users/pankr/AndroidStudioProjects/QrScannerApp/docs/inventory.md)
- `security/` → [security.md](file:///C:/Users/pankr/AndroidStudioProjects/QrScannerApp/docs/security.md)

Для мелких багфиксов и правок читать эти документы не требуется.

---

## 👥 Роли пользователей (8 ролей)

```
ADMIN, INVENTORY, MOVER, ELECTRICIAN, TECHNIC,
SECURITY, SHADOW_SECURITY, SUPERVISOR
```

- **`TECHNIC`**: Маршрутизируется напрямую в хост `StreetDoctorHost`, минуя главный `NavHost`.
- **`SHADOW_SECURITY`**: Отображается в интерфейсе как **"СБ"**. Назначается только напрямую в Firestore (не через UI). Имеет доступ к fleet import, аналитике heartbeat и истории.
- **`SUPERVISOR`**: Обладает урезанным меню.
- **`SECURITY`**: Доступ к паспортам скутеров, утерянным/найденным устройствам (lost/found), отслеживанию батарей и выдаче оборудования.
- **Single-device session**: Проверка по `ANDROID_ID`, логика "last login wins". Не изменять без явного обсуждения.

---

## 🔥 Firestore — Главное хранилище

> [!WARNING]
> **СТРОГИЙ СУТОЧНЫЙ ЛИМИТ FIRESTORE — 50,000 ЧТЕНИЙ И ЗАПИСЕЙ В ДЕНЬ!**
> Любые операции чтения и записи должны быть предельно оптимизированы. Запрещено выполнять get-запросы списков без ограничений `.limit(...)` или регистрировать realtime-слушатели на полные коллекции. Нарушение лимита полностью заблокирует работу приложения на складе. Используйте предрасчитанные агрегаты и Room как SSOT.

**Firestore является Single Source of Truth.** Локальная база Room используется только как офлайн-кеш.

### ⚠️ КРИТИЧЕСКОЕ ПРАВИЛО: запись в Firestore

**Никогда не записывать данные в Firestore напрямую через Data Class!** При прямой сериализации поля со значениями `null` будут молча отброшены базой данных, что приведёт к багам в продакшене. Запись должна осуществляться только через `HashMap`.

```kotlin
// ❌ НЕПРАВИЛЬНО — null поля будут удалены из документа
data class Battery(val id: String, val voltage: Double?, ...)
docRef.set(battery)

// ✅ ПРАВИЛЬНО — null запишется корректно
val data = hashMapOf<String, Any?>(
    "id" to battery.id,
    "voltage" to battery.voltage, // null будет корректно сохранён в БД
    ...
)
docRef.set(data)
```

### Архитектура данных Fleet (самокаты)
- **`fleet_catalog`**: Стабильные паспортные данные самоката (VIN, модель, поколение). Сюда пишет только импорт.
- **`fleet_vehicles`**: Волатильные операционные данные (текущий статус, геолокация, уровень заряда батареи). Сюда пишут все операционные процессы.
*Не смешивать эти структуры.*

### Коллекции Firestore
- `activity_log`, `app_config`, `batteries`, `battery_repair_log`, `chats`, `deliveries`, `device_telemetry`, `direct_messages`, `field_repair_sessions`, `field_repair_tasks`, `fleet_catalog`, `fleet_history`, `fleet_import_meta`, `fleet_vehicles`, `interaction_sessions`, `internal_users`, `pallet_activity_log`, `roles`, `scan_sessions`, `scooter_passports`, `scooters` (legacy), `security_events`, `security_lost_list`, `sessions`, `shifts`, `storage_activity_log`, `storage_cells`, `storage_pallets`, `tasks`, `vehicle_reports`, `warehouse_items`, `warehouse_logs`, `warehouse_news`, `warehouse_orders`, `warehouse_state`.

---

## 💾 Локальная БД (Room)

- `AppDatabase` (версия 17).
- Содержит 9 сущностей: `BatteryRepairLogEntity`, `ScanSessionEntity`, `TaskEntity`, `VehicleReportHistory`, `StorageCellEntity`, `StoragePalletEntity`, `InteractionSessionEntity`, `TelemetryBuffer`, `SpyderAnimationLog`.
- **Внимание**: Миграции настроены через `fallbackToDestructiveMigration()`. Любое изменение схемы с увеличением версии БД полностью сбросит и пересоздаст локальные данные на устройстве. Использовать осторожно!

---

## ⚡ Руководство по оптимизации взаимодействия с Firestore & Room

Руководство по минимизации сетевого трафика и соблюдению жестких лимитов (50,000 чтений/день) с использованием паттерна Single Source of Truth (SSOT) via Room.

### 1. Дельта-синхронизация (Pull Changes) вместо полной загрузки
Не запрашивайте список всех товаров или заказов при каждом открытии приложения.

1. **Хранение временной метки**: Сохраняйте `last_sync_time` (Long) в `DataStore` или `SharedPreferences`.
2. **Запрос изменений (Delta Query)**:
   При запуске приложения или по Swipe-to-refresh делайте запрос только изменённых документов:
   ```kotlin
   import com.google.firebase.Timestamp
   import com.google.firebase.firestore.FirebaseFirestore
   import kotlinx.coroutines.tasks.await
   import java.util.Date

   suspend fun fetchUpdatedItems(lastSyncTimestamp: Long): List<WarehouseItem> {
       // ВНИМАНИЕ: Для корректной дельта-синхронизации в модель WarehouseItem 
       // и документ в Firestore должны быть добавлены поля:
       // val updatedAt: Timestamp? = null
       // val isDeleted: Boolean = false
       
       val querySnapshot = FirebaseFirestore.getInstance()
           .collection("warehouse_items")
           .whereGreaterThan("updatedAt", Timestamp(Date(lastSyncTimestamp)))
           .get()
           .await()
           
       return querySnapshot.documents.mapNotNull { doc ->
           doc.toObject(WarehouseItem::class.java)?.apply { id = doc.id }
       }
   }
   ```
3. **Обновление Room**:
   * Полученные изменённые документы записываются в Room через операцию `Insert(onConflict = OnConflictStrategy.REPLACE)`.
   * Если у товара стоит флаг `isDeleted == true`, удаляйте его из Room локально.
4. **Обновление временной метки**: Сохраняйте максимальный `updatedAt` из полученной пачки как новый `last_sync_time`.

### 2. Пакетная отправка изменений (Push Queue) с WorkManager
Для выполнения операций в условиях нестабильной связи и экономии лимитов:

1. **Таблица очереди в Room**:
   Создайте сущность `OfflineOperation(id, type, itemId, quantity, timestamp, status)`.
2. **Оптимистичный UI**:
   При действиях пользователя мгновенно обновите количество локально в Room и запишите операцию в локальную таблицу `OfflineOperation`.
3. **Синхронизация через WorkManager**:
   Запустите `CoroutineWorker` с условием наличия сети (`NetworkType.CONNECTED`):
   ```kotlin
   import android.content.Context
   import androidx.work.Constraints
   import androidx.work.NetworkType
   import androidx.work.OneTimeWorkRequestBuilder
   import androidx.work.BackoffPolicy
   import androidx.work.ExistingWorkPolicy
   import androidx.work.WorkManager
   import java.util.concurrent.TimeUnit

   fun scheduleOfflineSync(context: Context) {
       val constraints = Constraints.Builder()
           .setRequiredNetworkType(NetworkType.CONNECTED)
           .build()

       val syncWorkRequest = OneTimeWorkRequestBuilder<UploadSyncWorker>()
           .setConstraints(constraints)
           .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10L, TimeUnit.SECONDS)
           .build()

       WorkManager.getInstance(context).enqueueUniqueWork(
           "offline_sync",
           ExistingWorkPolicy.KEEP,
           syncWorkRequest
       )
   }
   ```
4. **Пакетная отправка (Batch Push)**:
   Внутри `UploadSyncWorker` считайте все локальные операции из очереди и отправьте их на бэкенд FastAPI или запишите пакетно в Firestore через `WriteBatch` / `runTransaction`.

### 3. Переход на агрегированные эндпоинты бэкенда
Для дашборда мобильного приложения **категорически запрещено** запрашивать все товары, заказы и логи для подсчета сумм на клиенте.
* Используйте эндпоинт бэкенда FastAPI: `GET /api/warehouse/dashboard-summary`.
* Он возвращает готовую легковесную структуру `DashboardSummary` с предсчитанными данными.

### 4. Real-time Listeners (SnapshotListener) в виде Flow callbackFlow
Используйте `callbackFlow` для интеграции Firebase SnapshotListener с корутинами и Flow:
```kotlin
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

fun getActiveOrdersStream(): Flow<List<WarehouseOrder>> = callbackFlow {
    val query = FirebaseFirestore.getInstance()
        .collection("warehouse_orders")
        .whereIn("status", listOf("CREATED", "PROCESSING", "READY"))
        
    val listener = query.addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error)
            return@addSnapshotListener
        }
        if (snapshot != null) {
            val orders = snapshot.documents.mapNotNull { doc ->
                doc.toObject(WarehouseOrder::class.java)?.apply { id = doc.id }
            }
            trySend(orders)
        }
    }
    awaitClose { listener.remove() }
}
```

### 5. Ограничение выборок (Limit & Pagination)
На экранах истории или логов используйте курсорную пагинацию (cursor-based pagination):
```kotlin
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await

suspend fun fetchWarehouseLogsPage(
    limitSize: Long = 50L,
    startAfterDoc: DocumentSnapshot? = null
): List<WarehouseLog> {
    var query = FirebaseFirestore.getInstance()
        .collection("warehouse_logs")
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .limit(limitSize)
        
    if (startAfterDoc != null) {
        query = query.startAfter(startAfterDoc)
    }
    
): List<WarehouseLog> {
    var query = FirebaseFirestore.getInstance()
        .collection("warehouse_logs")
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .limit(limitSize)
        
    if (startAfterDoc != null) {
        query = query.startAfter(startAfterDoc)
    }
    
    val snapshot = query.get().await()
    return snapshot.documents.mapNotNull { doc ->
        doc.toObject(WarehouseLog::class.java)?.apply { id = doc.id }
    }
}
```

---

## 🗄️ Реестр и аудит всех запросов к БД (Firestore & Room)

В целях предотвращения исчерпания суточных лимитов Firebase (50,000 чтений) при масштабировании от 10 до 100 пользователей, все запросы в приложении должны следовать строгим правилам оптимизации.

### 🔴 Критические проблемы и точки отказа (Текущие утечки лимитов)

#### 1. Утечка в `AccountViewModel` (История и статистика)
* **Запрос**: `fetchUserActivitySummary()` -> `db.collection("activity_log")` с фильтром по `creatorId`.
  - **Проблема**: Делает `.get().await()` без лимитов для получения **всех** логов пользователя за всё время, чтобы посчитать `totalScans`, `totalSessions` и личный рекорд. При 100 пользователях, накопивших тысячи записей, каждый вход в профиль будет тратить тысячи чтений!
* **Запрос**: `loadFieldRepairStats()` -> `db.collection("field_repair_tasks")` с фильтром по `assignedToId`.
  - **Проблема**: Выгружает **все** исторические задачи техника без лимитов. Вызывает лавинообразный рост чтений по мере работы.

#### 2. Утечка в `ChatViewModel` (Непрочитанные сообщения)
* **Запрос**: `listenToUnreadCounts()` -> `db.collection("chats/{roomId}/messages")`.
  - **Проблема**: Регистрирует realtime-слушатели на **все** сообщения во всех доступных пользователю комнатах без ограничений по дате или лимитам, чтобы подсчитать непрочитанные на клиенте: `snapshot.documents.count { ... }`. При 100 активных пользователях лимиты Firestore закончатся за несколько часов!

#### 3. Утечка в `DeliveryRepository` (История доставок)
* **Запрос**: `getDeliveryHistory()` -> `deliveries.orderBy("timestamp", DESC)`.
  - **Проблема**: Вызывает `.get().await()` без ограничения `.limit(...)`. С ростом базы вернет тысячи записей.

#### 4. Нелокализованный Sync в `StorageRepository` (Cells & Pallets)
* **Запрос**: `startCellsRealtimeSync()` и `startPalletsRealtimeSync()` слушают коллекции `storage_cells` и `storage_pallets` целиком.
  - **Проблема**: Приложение скачивает все ячейки и паллеты всех складов компании. Необходима фильтрация по `warehouseId`.

---

### 📋 Реестр запросов к базе по компонентам

| Компонент / Файл | База / Коллекция | Тип операции | Оптимальность и Лимиты |
|---|---|---|---|
| **`AccountViewModel`** | `activity_log` | `get()` (без лимита) | 🔴 **Критично**: Выгружает всю историю юзера. |
| | `field_repair_tasks` | `get()` (без лимита) | 🔴 **Критично**: Выгружает все задачи техника. |
| | `shifts` | `get()` (limit 60) | 🟢 **Норма**. |
| | `internal_users` | `get()` / `update()` | 🟢 **Норма** (точечно по UID). |
| **`ChatViewModel`** | `chats/{roomId}/messages` | `addSnapshotListener` (limit 50) | 🟢 **Норма** (для активной комнаты). |
| | `chats/{roomId}/messages` | `addSnapshotListener` (без лимита) | 🔴 **Критично**: Для unreadCounts во всех комнатах. |
| | `chats/{roomId}/typing` | `addSnapshotListener` | 🟢 **Норма** (слушатель пинга). |
| | `internal_users` | `addSnapshotListener` | 🟡 **Умеренно** (только при открытом меню). |
| **`DashboardViewModel`** | `activity_log` | `addSnapshotListener` (today) | 🟡 **Умеренно** (фильтр по `startOfToday`). |
| | `battery_repair_log` | `addSnapshotListener` (today) | 🟡 **Умеренно** (фильтр по `startOfToday`). |
| | `field_repair_sessions` | `addSnapshotListener` (active) | 🟢 **Норма** (фильтр по статусу). |
| | `field_repair_tasks` | `addSnapshotListener` | 🟡 **Умеренно** (слушает задачи сессии). |
| | `internal_users` | `addSnapshotListener` | 🟡 **Умеренно** (все юзеры, нужен limit/pagination). |
| **`DeliveryRepository`** | `deliveries` | `get()` (без лимита) | 🔴 **Критично**: Нет `.limit(50)` в `getDeliveryHistory`. |
| **`StorageRepository`** | `storage_cells` | `addSnapshotListener` | 🟡 **Умеренно**: Слушает все ячейки (нужен `warehouseId`). |
| | `storage_pallets` | `addSnapshotListener` | 🟡 **Умеренно**: Слушает все паллеты (нужен `warehouseId`). |
| **`SecurityViewModel`** | `scooter_passports` | `addSnapshotListener` (limit 500) | 🟡 **Умеренно** (лимит 500 защищает, но велик). |
| | `fleet_vehicles` | `addSnapshotListener` | 🟡 **Умеренно** (слушает розыск СБ во флите). |
| | `equipment_issues` | `addSnapshotListener` (limit 200) | 🟢 **Норма**. |
| | `security_storage_cells` | `addSnapshotListener` | 🟢 **Норма**. |
| **`TelemetryRepository`** | `telemetry` | `batch.set()` (Room Buffer) | ✅ **Отлично**: Запись буферизуется локально в Room и отправляется пачками по 500 через WorkManager. |
| **`ScanSessionRepository`**| `scan_sessions` | `set()` (Room Buffer) | ✅ **Отлично**: Офлайн-буфер в Room, отправка по сети через WorkManager. |
| **`RepairLogRepository`** | `battery_repair_log` | `add()` / `set()` (Room Buffer) | ✅ **Отлично**: Офлайн-буфер в Room, отправка по сети через WorkManager. |

---

### 🛡️ Инструкции и Рекомендации по оптимизации при масштабировании

1. **Замена полных сканов агрегированными полями**:
   * *Для профиля*: Вместо загрузки всех логов из `activity_log` и `field_repair_tasks` для расчета статистики, хранить предрасчитанные счетчики (например, `totalScansCount`, `totalRepairsCount`, `personalRecord`) прямо в документе пользователя `internal_users` и обновлять их атомарно при каждой операции записи с помощью `FieldValue.increment(1)`.
2. **Ограничение выборок (Limit & Pagination)**:
   * Все методы получения списков (история доставок, логи, список активных задач) **обязаны** использовать лимиты `.limit(50)` и курсорную пагинацию `.startAfter(...)` при бесконечном скролле.
3. **Оптимизация счетчика непрочитанных в чате**:
   * Вместо прослушивания всей коллекции сообщений, хранить в профиле пользователя карту последних прочитанных таймстампов для каждой комнаты `lastReadTimestamps: Map<String, Long>`.
   * Запрашивать количество непрочитанных через Firestore Aggregation Query:
     ```kotlin
     firestore.collection("chats").document(roomId).collection("messages")
         .whereGreaterThan("timestamp", Timestamp(Date(lastReadTime)))
         .count()
         .get(AggregateSource.SERVER)
     ```
     *Примечание*: Вызов `count()` тарифицируется как 1 чтение на каждые 1000 совпавших документов, что в 1000 раз дешевле обычной загрузки документов!
4. **Фильтрация Realtime Sync по складам**:
   * Realtime sync ячеек и паллет в `StorageRepository` должен производиться строго с фильтром по текущему складу сотрудника:
     ```kotlin
     val myWarehouseId = authManager.authState.value.warehouseId ?: "bestuzhevskaya_10"
     cellsCollection.whereEqualTo("warehouseId", myWarehouseId)
         .addSnapshotListener { ... }
     ```

---

## 🔋 Модуль Батарей ("АКБ")

- Поддержка **4 типов батарей в 2 поколениях**:
  - WIND 4.0: FUJIAN (`4BB`), BYD (`4BZ`)
  - WIND 5.0: новый `5BB`, старый `SF`
- Основные компоненты: перечисление `CellType`, диалог `CreateCellDialog`, шторка `PalletDetailsSheet` (три вкладки: Список / Операции / Паспорт), оформление истории в виде терминала.

---

## 🛠️ Сборка и деплой

Основные Gradle-команды:
```bash
./gradlew assembleDebug          # Сборка Debug APK
./gradlew assembleRelease        # Сборка Release APK (требуются keystore.jks и creds в gradle.properties)
./gradlew bundleRelease          # Сборка Release AAB
./gradlew test                   # Юнит-тесты
./gradlew connectedAndroidTest   # Инструментальные тесты (требуется девайс/эмулятор)
./gradlew clean                  # Очистить директории сборки
```

### ⚠️ Особенности сборки (не изменять без просьбы)
- **`isMinifyEnabled = false`**: Workaround. Оптимизация ProGuard/R8 ломает библиотеку Apache POI в release-сборках, вызывая краш. Включать только после исправления правил для POI.
- **`google-services.json`**: Ошибка расположения файла в релизных сборках ломает Google Sign-In. Не перемещать.
- **Keystore**: alias `appkey`, путь `C:\Users\pankr\keystore.jks`.

---

## 🛡️ Безопасность и ProGuard

- Логика детекции рута, эмуляторов, фреймворков внедрения (Frida, Xposed), виртуальных сред и совпадения подписи находится в `AppSecurityGuard.kt`.
- Все проверки безопасности должны выполняться строго на `Dispatchers.IO`.
- При добавлении новых data-классов для десериализации Firestore или рефлексии необходимо обязательно добавлять правила сохранения в `app/proguard-rules.pro`.

---

## 🎨 Дизайн и оформление (StardustTheme)

- Фоновый цвет: `#0D0D10`
- Акцентный фиолетовый: `#6A5AE0`
- Используемый шрифт: **Manrope**
- Все новые экраны должны соответствовать дизайн-системе `StardustTheme`.

---

## ⚡ Именование и конфликты

- При разработке полевого ремонта (StreetDoctor) использовать префикс **`FieldHub*`** (например, `FieldHubScreen`, `FieldHubViewModel`), чтобы избежать конфликтов с модулем инвентаризации (`inventory`).
- Перед созданием нового экрана (`*Screen`) или модели представления (`*ViewModel`) обязательно проверить уникальность имени через текстовый поиск по проекту.
