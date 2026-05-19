# features/settings


Модуль настроек приложения. Содержит единый экран настроек и встроенный графический движок Spyder3000.

---

## Структура

```
settings/
└── ui/
    ├── UnifiedSettingsScreen.kt       # Главный экран настроек
    ├── Spyder3000Dialog.kt            # Диалог и точка входа движка
    ├── Spyder3000Engine.kt            # Ядро движка (профили, FPS, spring)
    ├── SpyderSettingsViewModel.kt     # HiltViewModel + UiState
    └── SpyderAnimationRepository.kt   # Room DAO + репозиторий логов анимаций
```

---

## UnifiedSettingsScreen

Единственный экран настроек (`@Composable fun UnifiedSettingsScreen(authManager: AuthManager)`).

Управляет через `SettingsManager` (DataStore):
- Звук / вибрация
- Тема приложения (`AppTheme`: ENGINE / NEBULA / BIOSPHERE)
- Анимация диалогов (`DialogAnimation`)
- Авто-обновление Spyder

Также держит `UpdateManager` (hiltViewModel) для проверки обновлений APK.

Секции:
| Секция | Что делает |
|--------|-----------|
| Профиль | Аватар, имя, email пользователя (Firebase Auth) |
| Уведомления | Запрос разрешения POST_NOTIFICATIONS |
| Геолокация | Запрос ACCESS_FINE/COARSE_LOCATION |
| Звук / Вибрация | Toggle через SettingsManager |
| Тема | Выбор шейдерной темы |
| Графический движок | Блок Spyder3000 → `Spyder3000SettingsItem()` |
| Об авторе | Ссылка на GitHub, версия приложения |
| Выход | Sign out через AuthManager |

---

## Spyder3000 — графический движок

### Точка входа: `Spyder3000SettingsItem`

```kotlin
Spyder3000SettingsItem()  // без параметров — ViewModel внутри
```

Строка в настройках с логотипом, переключателем авто-обновления и тапом для открытия диалога.

### Диалог: `Spyder3000Dialog`

4 вкладки:

| Вкладка | Содержимое |
|---------|-----------|
| О движке | Версия, build, список возможностей |
| Демо | Живые демо: spring-шарики, shader wave, border sweep, FPS |
| Настройки | Авто-обновление, выбор анимации диалогов, профиль производительности |
| Анализатор | Живой FPS, статистика анимаций, история логов |

---

## Spyder3000Engine

`@Singleton`, инжектируется через Hilt.

Хранит и раздаёт через `StateFlow`:
- `currentProfile: SpyderPerformanceProfile` — активный профиль
- `fps`, `frameTime`, `droppedFrames` — метрики производительности
- `totalAnimationsCount` — счётчик анимаций

**Профили производительности:**

| Профиль | FPS | Shader | Описание |
|---------|-----|--------|----------|
| Power Saving | 30 | LOW | Экономия батареи |
| Balanced | 60 | MEDIUM | Оптимальный баланс (по умолчанию) |
| Performance | 90 | HIGH | Максимальная частота |
| Spyder Max | 120 | ULTRA | Для 120Hz устройств |

Spring-параметры (`dampingRatio`, `stiffness`) берутся из активного профиля.

---

## SpyderSettingsViewModel

`@HiltViewModel`. Зависимости: `Spyder3000Engine` + `SpyderAnimationRepository`.

Публичные StateFlow:
- `uiState: SpyderUiState` — FPS, frameTime, dropped, totalAnimations, recentAnimations
- `autoUpdateEnabled: Boolean`
- `currentDialogAnimation: DialogAnimation`
- `showFpsOverlay: Boolean`

Методы:
- `toggleAutoUpdate()` — переключить авто-обновление
- `setDialogAnimation(DialogAnimation)` — сменить анимацию + записать лог
- `setPerformanceProfile(SpyderPerformanceProfile)` — сменить профиль + записать лог
- `resetStatistics()` — сбросить счётчик и очистить логи

Внутри `init` запускает корутину симуляции FPS (16ms тик).

---

## SpyderAnimationRepository + SpyderAnimationDao

Room entity: **`SpyderAnimationLog`** (таблица `spyder_animation_logs`).

Поля: `id`, `animationType`, `durationMs`, `fpsAtMoment`, `frameTimeMs`, `timestamp`.

Зарегистрирована в `AppDatabase` (version 16). DAO предоставляется через `DatabaseModule`.

Методы репозитория:
- `getRecentAnimations()` — Flow последних 50 записей
- `logAnimation(type, durationMs, fps, frameTimeMs)` — записать событие
- `clearOldLogs(daysToKeep)` — удалить старые записи
- `getAverageFpsLastMinute()` — средний FPS за последнюю минуту

---

## Анимации диалогов (`DialogAnimation`)

Выбор хранится в DataStore через `SettingsManager.dialogAnimationFlow`. Применяется в `AnimatedDialogWrapper`.

| Вариант | Описание |
|---------|----------|
| SCALE_BOUNCY | Пружинный масштаб |
| SLIDE_BOTTOM | Снизу вверх |
| FALL_TOP | Сверху вниз |
| FADE | Появление |
| FLIP | 3D-переворот |
| ZOOM_SOFT | Мягкий зум |

---

## Зависимости Hilt

```
DatabaseModule
  └── provideSpyderAnimationDao(AppDatabase) → SpyderAnimationDao

SpyderAnimationRepository(@Singleton)
  └── SpyderAnimationDao

Spyder3000Engine(@Singleton)

SpyderSettingsViewModel(@HiltViewModel)
  ├── Spyder3000Engine
  └── SpyderAnimationRepository
```