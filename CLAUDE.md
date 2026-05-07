# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

---
## 🚨 БЫСТРЫЕ ЗАДАЧИ — БЕЗ КОНТЕКСТА

Если запрос содержит конкретную ошибку компилятора с номером строки
("Expecting '}' :459", "Unresolved reference :22", и т.п.):

1. НЕ читать CLAUDE.md дальше этого раздела
2. НЕ делать Glob/Grep по проекту
3. НЕ читать другие файлы
4. Открыть ТОЛЬКО указанный файл, ТОЛЬКО окрестности строки (±20 строк)
5. Починить и выйти

Архитектурный контекст для синтаксиса не нужен.

## ⚠️ ПРАВИЛА ОБЩЕНИЯ (читать ОБЯЗАТЕЛЬНО)

- **Все ответы и объяснения — на русском языке.** Идентификаторы кода — на английском.
- Без преамбул "Конечно, сейчас сделаю" — сразу к делу.
- Если задача неясна — задай **один** уточняющий вопрос, не пять.
- Я "vibe coder", не профессиональный разработчик. Объясняй коротко и просто.
- **Не предлагай рефакторинг "за компанию"** — делай только то, о чём попросил.
- **Не предлагай миграции** на другую архитектуру/стек если я явно не прошу.

## 🎯 ЭКОНОМИЯ ТОКЕНОВ (КРИТИЧНО — Pro подписка)

- Файлы **>200 строк** читать только нужный диапазон (view_range). без явной команды.
- Сначала grep по симптому/имени → потом view ±30 строк вокруг находки.
- Перед крупными изменениями — **Plan Mode** (показать план, не редактировать сразу).
- При неоднозначности — **спрашивай, не угадывай** (откат правок дороже).
- Не читай README/документацию без необходимости — там часто устаревшая инфа 50/50.

## 📂 МОДУЛЬНЫЕ ЗАМЕТКИ

При работе в конкретном модуле — прочитать соответствующий файл
ТОЛЬКО если задача архитектурная (новый экран, рефакторинг, новая фича):

- `street_doctor/` → docs/street_doctor.md
- `inventory/` → docs/inventory.md
- `security/` → docs/security.md

Для багфиксов и мелких правок — НЕ читать.

---

## Build Commands

```bash
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Release APK (требует keystore.jks + gradle.properties creds)
./gradlew bundleRelease          # Release AAB
./gradlew test                   # Unit tests (JVM)
./gradlew connectedAndroidTest   # Instrumented tests (требует устройство/эмулятор)
./gradlew lint                   # Lint
./gradlew clean                  # Очистить build outputs
```

Release builds требуют `KEYSTORE_PASSWORD` и `KEY_PASSWORD` в `gradle.properties`.
Keystore: `C:\Users\pankr\keystore.jks`, alias `appkey`.

### ⚠️ Build quirks (не трогать без явной просьбы)
- **`isMinifyEnabled = false`** — временный workaround. ProGuard/R8 ломает Apache POI в release builds, приложение крашится. Включать минификацию **только** после фикса POI правил.
- **`google-services.json`** — баг с placement в release ломает Google Sign-In. Не двигать файл без обсуждения.
- GitHub Actions: тег-релизы настроены, автоподпись APK через `.jks` keystore.

---

## Architecture

**Feature-based Clean Architecture** с MVVM + Jetpack Compose.

Каждая фича в `features/` следует структуре:
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

### Все 14 фич
`chat`, `delivery`, `electrician`, `interaction`, `inventory`, `profile`, `scanner`, `security`, `shift`, `street_doctor`, `tasks`, `team`, `vehicle_report`, `visual_repair`

### Приоритеты (для понимания где главное)
- 🟢 **Главные фичи (активная разработка):** `street_doctor`, `inventory`
- 🟡 **Стабильные (не трогать без причины):** `chat`, `shift`, `security`, `scanner`, `tasks`
- 🔵 **Второстепенные:** `delivery`, `electrician`, `interaction`, `profile`, `team`, `vehicle_report`, `visual_repair`

### Текущий фокус — `street_doctor` (Field Repair / уличный ремонт)
Активные файлы и в работе:
- `FieldRepairAdminScreen` — админский мониторинг выездов
- `FieldRepairAdminViewModel` — управление сессиями техников
- `TechnicScannerScreen` — QR-сканер для техника
- `FieldRepairImportSheet` + `FieldRepairImportViewModel` — импорт XLSX с очередью назначения по техникам
- `StreetDoctorHost` — навигационный хаб (TECHNIC роль роутится сюда **минуя главный NavHost**)
- `FieldHubScreen` / `FieldHubViewModel` — центральная панель модуля
- `PassportScreen` — паспорт самоката + форма отчёта о ремонте
- `TasksScreen` — список задач техника с фильтрацией по дистанции

Safety в импорте: дедуп, лимит строк, partial upload detection, закрытие активных сессий перед новой.

### Shared infrastructure
- `common/` — общие Firebase утилиты и Compose компоненты
- `core/model/` — кросс-фичные enum (`ActiveTab`, `SessionType`)
- `di/` — Hilt модули (`DatabaseModule`, `GsonModule`, `WorkManagerModule`)
- `data/local/` — общие Room DAO (telemetry)
- `worker/` — WorkManager background jobs

---

## Roles (8 ролей)

```
ADMIN, INVENTORY, MOVER, ELECTRICIAN, TECHNIC,
SECURITY, SHADOW_SECURITY, SUPERVISOR
```

- **`TECHNIC`** маршрутизируется напрямую в `StreetDoctorHost`, **минуя главный NavHost**.
- **`SHADOW_SECURITY`** отображается в UI как **"СБ"**. Назначается **только напрямую через Firestore**, не через UI. Имеет доступ к: fleet import, heartbeat analytics, history.
- **`SUPERVISOR`** — урезанное меню.
- **`SECURITY`** — паспорт скутера, lost/found, battery tracking, equipment issuance.

### Single-device session
Через `ANDROID_ID`, логика "last login wins". **Не ломать без обсуждения.**

---

## Key Tech Stack

- **UI**: Jetpack Compose + Material3, Compose Navigation
- **DI**: Dagger Hilt 2.51.1 (KSP)
- **Database**: Room 2.6.1 v15 — `fallbackToDestructiveMigration()`
- **Backend**: Firebase (Firestore, Auth, Storage, FCM, Analytics)
- **Camera/ML**: CameraX 1.3.1 + ML Kit barcode + ML Kit text recognition
- **Excel I/O**: Apache POI (запись XLSX) + FastExcel (чтение XLSX)
- **Background**: WorkManager 2.9.0 с Hilt factory
- **Security**: freeRASP (Talsec), Play Integrity API, EncryptedSharedPreferences
- **Theme**: AGSL шейдеры (Engine, Nebula, Biosphere) + DataStore

### Design system (StardustTheme)
- Background: `#0D0D10`
- Accent purple: `#6A5AE0`
- Font: **Manrope**
- Все новые экраны — в `StardustTheme`. Не отступать без явной просьбы.

---

## Firestore — главное хранилище данных

**Firestore — single source of truth.** Room используется как локальный кеш для оффлайна.

### ⚠️ КРИТИЧНОЕ ПРАВИЛО: запись в Firestore

**НИКОГДА не писать в Firestore через data class напрямую.** Null поля **молча отбрасываются** — это уже ловило в проде.

```kotlin
// ❌ ПЛОХО — null поля исчезнут
data class Battery(val id: String, val voltage: Double?, ...)
docRef.set(battery)

// ✅ ХОРОШО — null корректно записывается
val data = hashMapOf<String, Any?>(
    "id" to battery.id,
    "voltage" to battery.voltage,  // null будет в БД
    ...
)
docRef.set(data)
```

### Архитектурный сплит fleet
- **`fleet_catalog`** — стабильная идентичность самоката (VIN, модель, поколение)
- **`fleet_vehicles`** — волатильные операционные данные (статус, локация, батарея)
- Не смешивать. Импорт пишет в `fleet_catalog`, операции — в `fleet_vehicles`.

### Все Firestore коллекции
```
activity_log              # Логи действий пользователей
app_config                # Глобальный конфиг приложения
batteries                 # Батареи (АКБ)
battery_repair_log        # История ремонта батарей
chats                     # Чаты
deliveries                # Доставки
device_telemetry          # Телеметрия устройств
direct_messages           # Личные сообщения
field_repair_sessions     # Сессии полевого ремонта (StreetDoctor)
field_repair_tasks        # Задачи полевого ремонта
fleet_catalog             # Каталог самокатов (стабильные данные)
fleet_history             # История изменений флота
fleet_import_meta         # Метаданные импортов флота
fleet_vehicles            # Операционные данные самокатов
interaction_sessions      # Сессии взаимодействий
internal_users            # Внутренние пользователи (сотрудники)
pallet_activity_log       # Активность по паллетам
roles                     # Роли (справочник)
scan_sessions             # Сессии сканирования QR
scooter_passports         # Паспорта самокатов
scooters                  # Самокаты (legacy, мигрирует во fleet_*)
security_events           # События безопасности
security_lost_list        # Список потерянных самокатов (СБ)
sessions                  # Общие сессии
shifts                    # Смены сотрудников
storage_activity_log      # Лог складской активности
storage_cells             # Ячейки на складе
storage_pallets           # Складские паллеты
tasks                     # Задачи общего пула
vehicle_reports           # Отчёты по самокатам
warehouse_items           # Складские позиции
warehouse_logs            # Логи склада
warehouse_news            # Новости/уведомления склада
warehouse_orders          # Складские заказы
warehouse_state           # Состояние склада
```

---

## Database (Room — локальный кеш)

`AppDatabase` (Room, **version 15**) с 8 entities:
- `BatteryRepairLogEntity`, `ScanSessionEntity`, `TaskEntity`,
- `VehicleReportHistory`, `StorageCellEntity`, `StoragePalletEntity`,
- `InteractionSessionEntity`, `TelemetryBuffer`

⚠️ Миграции через **destructive fallback** — увеличение `DATABASE_VERSION` в `AppDatabase.kt` **сносит и пересоздаёт БД на устройстве**. Использовать только для dev, в проде — нужны нормальные миграции.

---

## Battery module ("АКБ", бывшее "Приёмка")

**4 типа батарей в 2 поколениях:**
- **WIND 4.0:** FUJIAN `4BB`, BYD `4BZ`
- **WIND 5.0:** новый `5BB`, старый `SF`

Архитектура:
- Enum `CellType`
- Диалог `CreateCellDialog`
- `PalletDetailsSheet` с 3 вкладками: Список / Операции / Паспорт
- Терминал-эстетика для истории операций

---

## Naming conflicts (история граблей)

- Field repair классы → префикс **`FieldHub*`** (FieldHubScreen, FieldHubViewModel) чтобы не конфликтовать с inventory модулем.
- **Перед созданием нового класса со словом Screen/ViewModel** — сделай grep на наличие такого имени.

---

## Security module — НЕ ТРОГАТЬ без явной просьбы

`AppSecurityGuard.kt`:
- Root / emulator / Frida / Xposed / virtual app / signature detection
- Все проверки через `withContext(Dispatchers.IO)` — никогда на main thread
- `BuildConfig.DEBUG` гард обязателен
- Использует `ANDROID_ID`, не deprecated device ID API

**Логика детекции отбалансирована — изменения могут сломать прод.**

---

## State Management

ViewModels отдают `StateFlow<UiState>` (или sealed class).
Screens собирают через `collectAsStateWithLifecycle()`.
Repositories возвращают `Flow<T>` из Room и `suspend fun` из Firestore.

---

## ProGuard

`app/proguard-rules.pro` — агрессивная обфускация (`-repackageclasses`, `-overloadaggressively`).
Явно сохраняются: Firebase Firestore data classes, Hilt, Room, ML Kit, Apache POI, security classes.

При добавлении новых data class для Firestore сериализации или reflection — **обязательно добавить `-keep` правило**.

---

## TL;DR для Claude Code

1. **Отвечать на русском**, кратко, без преамбул.
2. **Не читать большие файлы целиком** — grep + view диапазон.
3. **Firestore writes — через HashMap**, не data class.
4. **Не лезть в security/release config** без просьбы.
5. **Перед созданием новых классов** — проверять конфликты имён через grep.
6. **Текущий фокус — `street_doctor` (FieldRepair)**.
7. **При сомнениях — Plan Mode и уточняющий вопрос.**
