# VERSION CONTROL — Force Update Mechanism

## Цель
Реализовать механизм **принудительного обновления приложения** — блокировка входа для пользователей со старой версией APK, даже если они физически установили старый файл.

## Что сделано

### 1. AuthManager.kt — клиентская проверка версии
- Константа `MIN_REQUIRED_APP_VERSION = "1.4.5"` захардкожена в коде (строка 36)
- Методы: `getCurrentAppVersion()`, `compareVersions()`
- Проверка в **3 местах**:
    - При логине (до запроса в Firestore)
    - В snapshot listener (при изменении документа пользователя)
    - В heartbeat (периодически во время работы)
- Добавлено поле `versionError` в `AuthState`
- Закомментированный вариант с загрузкой минимальной версии из Firestore (`app_config/version_config`) — готов к раскомментированию

### 2. UpdateManager.kt — серверная проверка версии (основная защита)
- Загружает `update.json` с GitHub Releases при запуске
- URL: `https://github.com/Sixnyder6/QrScannerApp/releases/latest/download/update.json`
- Поля: `minRequiredVersionCode`, `minRequiredVersionName`
- Сравнивает `currentVersionCode` устройства с `minRequiredVersionCode` из сети
- При несоответствии — блокирует через `VersionCheckResult`

### 3. AccountScreen.kt — UI блокировки
- `ForceUpdateDialog` — диалог, который **нельзя закрыть**
- Отслеживание `versionError` и показ диалога
- Кнопки:
    - **"Скачать обновление"** (ведёт на GitHub Releases)
    - **"Выйти"** (закрывает приложение)

### 4. FieldRepairStats.kt — создан новый файл
- Путь: `features/street_doctor/domain/model/FieldRepairStats.kt`
- Поля: `doneToday`, `totalToday`, `remainingToday`, `doneAllTime`, `totalAllTime`, `avgMinutesPerScooter`

### 5. GitHub Actions workflow — обновлён
- В `update.json` добавлены поля `minRequiredVersionCode` и `minRequiredVersionName`
- Генерируется автоматически при каждом релизе

## Как работает защита

```
Старая версия (1.4.0 — без UpdateManager)
→ Не заблокирована ретроактивно (кода проверки нет в APK)

Новая версия (1.4.5+ — с UpdateManager)
→ При запуске скачивает update.json с GitHub
→ Сравнивает versionCode с minRequiredVersionCode
→ Если ниже — показ ForceUpdateDialog, вход невозможен

Дополнительно — при логине / heartbeat / snapshot:
→ Клиентская проверка против MIN_REQUIRED_APP_VERSION (захардкожено)
```

## Ограничения текущей реализации

| Сценарий | Результат |
|----------|-----------|
| Версия 1.4.5+ пытается откатиться до 1.4.4 | Заблокирована (UpdateManager есть) |
| Версия 1.4.0 уже установлена до добавления механизма | **Не заблокирована** (UpdateManager отсутствует в старой APK) |
| Нет интернета при запуске | UpdateManager не отработает, клиентский guard остаётся |

Для блокировки старых APK ретроактивно — нужны серверные правила (Firestore Security Rules или Cloud Function). Пока не реализовано.

## Файлы

| Файл | Действие |
|------|----------|
| `AuthManager.kt` | Изменён — клиентская проверка, 3 места |
| `UpdateManager.kt` | Изменён — серверная проверка через GitHub |
| `AccountScreen.kt` | Изменён — ForceUpdateDialog |
| `FieldRepairStats.kt` | Создан |
| `.github/workflows/release.yml` | Изменён — добавлены minRequired поля в update.json |

## Для AI-агента

При любых изменениях в логине, аутентификации или Firestore — **не ломать** проверки версии в:
- `AuthManager.login()`
- `attachUserListener()`
- `startHeartbeat()`
- `UpdateManager.checkForUpdates()`

**Не удалять** `ForceUpdateDialog` из `AccountScreen.kt`.

Минимальная версия задаётся в **двух местах**:
1. `MIN_REQUIRED_APP_VERSION` в `AuthManager.kt` — клиентская константа
2. `minRequiredVersionCode` / `minRequiredVersionName` в `update.json` — генерируется через GitHub Actions

При каждом релизе обновлять оба места.

---

**Дата:** май 2026
**Версия механизма:** 1.1
**Статус:** Частичная защита — новые версии (1.4.5+) защищены, старые APK (до 1.4.5) не блокируются ретроактивно