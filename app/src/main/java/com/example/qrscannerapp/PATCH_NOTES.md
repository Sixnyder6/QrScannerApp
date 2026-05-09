# VERSION CONTROL — Force Update Mechanism

## 📋 Цель
Реализовать механизм **принудительного обновления приложения** — блокировка входа для пользователей со старой версией APK, даже если они физически установили старый файл.

## ✅ Что сделано

### 1. AuthManager.kt — добавлена проверка версии
- Добавлена константа `MIN_REQUIRED_APP_VERSION = "1.4.5"`
- Методы: `getCurrentAppVersion()`, `compareVersions()`
- Проверка в **3 местах**:
    - При логине (до запроса в Firestore)
    - В snapshot listener (при изменении документа)
    - В heartbeat (периодически во время работы)
- Добавлено поле `versionError` в `AuthState`

### 2. AccountScreen.kt — UI блокировки
- Добавлен `ForceUpdateDialog` — диалог, который **нельзя закрыть**
- Отслеживание `versionError` и показ диалога
- Кнопки:
    - **"Скачать обновление"** (ведёт на GitHub Releases)
    - **"Выйти"** (закрывает приложение)

### 3. FieldRepairStats.kt — создан новый файл
- **Путь:** `features/street_doctor/domain/model/FieldRepairStats.kt`
- **Поля:**
    - `doneToday`, `totalToday`, `remainingToday`
    - `doneAllTime`, `totalAllTime`, `avgMinutesPerScooter`

### 4. GitHub Actions workflow — обновлен
- Добавлены поля `minRequiredVersionCode` и `minRequiredVersionName` в `update.json`

## ⚙️ Как работает защита
Старая версия (1.4.0)
↓
Попытка входа
↓
Сервер/клиент проверяют версию
↓
Блокировка
↓
Показ диалога обновления
↓
Вход невозможен ❌

## 📁 Файлы, которые были изменены/созданы

| Файл | Действие |
|------|----------|
| `AuthManager.kt` | Изменен |
| `AccountScreen.kt` | Изменен |
| `FieldRepairStats.kt` | Создан |
| `.github/workflows/release.yml` | Изменен |

## 💡 Контекст для разработчика

| Что | Значение |
|-----|----------|
| Минимальная версия задается в коде | `MIN_REQUIRED_APP_VERSION = "1.4.5"` |
| Что делать при каждом релизе | Обновить эту константу |
| update.json на GitHub | Генерируется автоматически при сборке через Actions |

## 🧠 Для AI-агента (Android Studio)

Если ты читаешь этот файл — значит проект имеет **механизм принудительного обновления**. При любых изменениях в логине, аутентификации или Firestore — **не ломай** проверки версии в:

- `AuthManager.login()`
- `attachUserListener()`
- `startHeartbeat()`

А также **не удаляй** `ForceUpdateDialog` из `AccountScreen.kt`.

---

**Дата:** май 2026  
**Версия механизма:** 1.0  
**Статус:** ✅ Работает