# CLAUDE_USER.md

Этот файл — твои личные настройки для работы с Claude в этом проекте.

---

## 🚨 БЫСТРЫЕ ЗАДАЧИ — БЕЗ КОНТЕКСТА

Если прошу починить конкретную ошибку компилятора/рантайма с номером строки:

1. Не читать этот файл целиком
2. Не делать поиск по проекту
3. Открыть ТОЛЬКО указанный файл, окрестности строки
4. Починить и выйти

---

## ⚠️ ПРАВИЛА ОБЩЕНИЯ

- **Все ответы и объяснения — на русском языке.** Код — на английском.
- Без преамбул — сразу к делу.
- Если задача неясна — задать **один** уточняющий вопрос, не больше.
- Я "vibe coder". Объяснять коротко и просто, без лишней теории.
- **Не предлагать рефакторинг "за компанию"** — только то, о чём попросил.
- **Не предлагать миграции** на другую архитектуру/стек без явной просьбы.

---

## 🎯 ЭКОНОМИЯ ТОКЕНОВ

- Файлы > 200 строк — читать только нужный диапазон (view_range).
- Сначала grep по симптому/имени → потом чтение ±30 строк вокруг находки.
- Перед крупными изменениями — показать план, не редактировать сразу.
- При неоднозначности в задаче — **спрашивать, не угадывать**.
- Не читать README/документацию без необходимости.

---

## 🧭 НАВИГАЦИЯ ПО ПРОЕКТУ

### Feature-based Clean Architecture

Каждая фича в `features/`:

```
feature/
├── domain/model/
├── data/
│   ├── local/
│   ├── remote/
│   ├── repository/
│   └── mapper/
└── ui/
    ├── viewmodel/
    └── screens/
```

### Все 14 фич (приоритеты)

| Приоритет | Фичи |
|-----------|------|
| 🟢 **Активная разработка** | `street_doctor`, `inventory` |
| 🟡 **Стабильные (не трогать без причины)** | `chat`, `shift`, `security`, `scanner`, `tasks` |
| 🔵 **Второстепенные** | `delivery`, `electrician`, `interaction`, `profile`, `team`, `vehicle_report`, `visual_repair` |

### Ключевые точки входа

- **Навигация**: `app/.../navigation/`
- **DI**: `app/.../di/` (Hilt модули)
- **ViewModel по умолчанию**: `.../ui/viewmodel/` — StateFlow<UiState>
- **Шаред компоненты**: `app/.../common/ui/`
- **База данных**: Room Entity → `app/.../data/local/`

### 8 ролей

`ADMIN`, `INVENTORY`, `MOVER`, `ELECTRICIAN`, `TECHNIC`, `SECURITY`, `SHADOW_SECURITY`, `SUPERVISOR`

- `TECHNIC` → роутится напрямую в `StreetDoctorHost`, минуя главный NavHost
- `SHADOW_SECURITY` → в UI как **"СБ"**, назначается напрямую через Firestore

### Naming conflicts (важно!)

- Field repair → префикс **`FieldHub*`** (не `FieldRepair*`) — чтобы не конфликтовать с inventory
- Перед созданием нового `*Screen` / `*ViewModel` — сделать grep на существование

---

## 🔥 Firestore — главное хранилище

**Запись только через `HashMap`**, не через data class (null поля отбрасываются иначе):

```kotlin
// ✅ ПРАВИЛЬНО
val data = hashMapOf<String, Any?>("field" to value, ...)
docRef.set(data)
```

### Основные коллекции

`activity_log`, `app_config`, `batteries`, `fleet_catalog`, `fleet_vehicles`, `field_repair_sessions`, `field_repair_tasks`, `internal_users`, `shifts`, `storage_cells`, `storage_pallets`, `warehouse_items`, `warehouse_orders` и др.

- **`fleet_catalog`** — стабильные данные (VIN, модель)
- **`fleet_vehicles`** — операционные (статус, локация, батарея)

---

## 🧪 Build & Deploy

```bash
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Release APK
./gradlew bundleRelease          # Release AAB
./gradlew test                   # Unit tests
./gradlew connectedAndroidTest   # Instrumented tests
```

### ⚠️ Не трогать без явной просьбы
- `isMinifyEnabled = false` — workaround для Apache POI
- `google-services.json` — баг с placement в release
- `Security module` (`AppSecurityGuard.kt`) — отбалансированная логика детекции
- `ProGuard rules` — менять только при добавлении новых data class для Firestore

---

## 🎨 Дизайн (StardustTheme)

- Background: `#0D0D10`
- Accent: `#6A5AE0`
- Font: **Manrope**
- Все новые экраны — в StardustTheme

---

## 📂 Модульные заметки

Если задача архитектурная (новый экран / фича / рефакторинг) → прочитать соответствующий doc:

- `street_doctor/` → `docs/street_doctor.md`
- `inventory/` → `docs/inventory.md`
- `security/` → `docs/security.md`

Для багфиксов и мелких правок — не читать.