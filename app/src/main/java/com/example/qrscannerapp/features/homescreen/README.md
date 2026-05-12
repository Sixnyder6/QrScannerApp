# Homescreen Module

Главный экран приложения — bento-grid из виджетов и иконок навигации. Отображается всем ролям кроме `TECHNIC` (тот уходит напрямую в `StreetDoctorHost`).

---

## Структура файлов

```
homescreen/
├── data/
│   ├── HomescreenBackgroundRepository.kt   # управление фоновым изображением
│   └── local/
│       └── HomescreenBackgroundEntity.kt   # Room entity + DAO
└── ui/
    ├── HomescreenScreen.kt                 # корневой Composable, точка входа
    ├── HomescreenStatusBar.kt              # верхняя полоска (склад / батарея / сеть)
    ├── HomescreenUserCard.kt               # плавающая карточка пользователя (низ)
    ├── HomescreenDialog.kt                 # модальное окно (iOS-стиль)
    ├── TaskDialogContent.kt                # контент задач внутри HomescreenDialog
    ├── TaskQuickLookSheet.kt               # быстрый просмотр задачи
    └── widgets/
        ├── WidgetCard.kt                   # базовый компонент всех виджетов
        ├── ShiftWidget.kt                  # смена: время + прогресс
        ├── WeatherWidget.kt                # погода + дата
        ├── TasksWidget.kt                  # задачи: счётчик + приоритеты
        ├── ChatWidget.kt                   # чат: непрочитанные + превью
        ├── TodayStatsWidget.kt             # статистика дня
        ├── TeamOnlineWidget.kt             # онлайн-команда (Firestore)
        └── AppIconTile.kt                  # иконка-ярлык (одна ячейка сетки)
```

---

## Экран (`HomescreenScreen`)

**Сетка:** `LazyVerticalGrid` — 4 колонки, виджеты занимают 1–3 ячейки через `GridItemSpan`.

**Фон:** `#02020A` + dot grid (точки `#161625`, шаг 28dp) + radial vignette. Фон является `hazeSource` — поверх него все виджеты рендерят frosted glass через библиотеку `haze`.

**Shimmer-фаза:** каждый виджет получает `sheenPhase = PHI * N` (`PHI = 0.618`), чтобы блики не совпадали.

### Layout виджетов (сверху вниз)

| Позиция | Виджет | Span | Высота |
|---------|--------|------|--------|
| 1 | `ShiftWidget` | 2 col | 160dp |
| 2 | `WeatherWidget` | 2 col | 160dp |
| 3 | `TasksWidget` | 2 col | 110dp |
| 4 | `ChatWidget` | 2 col | 110dp |
| 5 | `TodayStatsWidget` | 3 col | 110dp |
| 6 | `TeamOnlineWidget` | 1 col | 110dp |
| 7+ | `AppIconTile` × N | 1 col | авто |

### Диалоги

Ботом-шиты заменены на `HomescreenDialog` — компактное центральное окно с анимацией scale+fade:
- **Задачи** — открывается тапом по виджету или иконке, кнопка "Все задачи" → полный экран
- **Настройки** — открывается иконкой, кнопка "Все настройки" → полный экран

---

## Роль-зависимый контент

```kotlin
if (isStrictSecurity) {
    // только 4 иконки СБ: Дашборд, Самокаты, АКБ СБ, Склад СБ
} else {
    // стандартный набор: Сканер, Взаимод., Задачи, АКБ, Самокаты, Склад, Доставка
    if (isUserManager) {
        // + Дашборд, Сводка, Поле (FieldRepairAdmin)
    }
    // + Команда, Чат, QR, История, Настройки
    if (isSecurity || isAdmin) {
        // + значок СБ
    }
}
```

`isUserManager` = `ADMIN` || `INVENTORY_MANAGER` || имя содержит "ситников"/"miha.sklad"/"nikasov".

---

## Компоненты UI

### `WidgetCard` — базовый виджет

Используется во всех виджетах. Параметры:
- `accentColor` — цвет glow, border, shimmer
- `glowStrength` — интенсивность glow (0–1), уменьшается когда виджет "пустой"
- `sheenPhase` — сдвиг фазы блика (0–1)
- `onClick` — если `null`, parallax работает, но тап не реагирует

Эффекты:
1. Внешний glow (`drawBehind`) с двумя радиальными градиентами
2. `hazeEffect` — frosted glass (blur 30dp)
3. Gradient background (accent → `#05050E`)
4. Gradient border (2.5dp) + рефракционная линия сверху
5. Shimmer — узкая полоска света, проходит раз в 9 сек (фаза сдвинута по PHI)
6. Parallax tilt при drag (±12°, spring-анимация)
7. Scale 0.97 при нажатии

### `ShiftWidget`

- **Активная смена:** пульсирующая зелёная точка + время H:MM + 4-сегментный прогрессбар (12h cap = авто-завершение WorkManager)
- **Неактивна:** статичная фиолетовая точка + "тапни чтобы начать"
- Тик каждую секунду через `LaunchedEffect(isActive)`, не перерисовывает всю сетку
- Тап → `openAccount`

### `WeatherWidget`

- Данные из `WeatherRepo`, обновляется каждые 15 мин
- Показывает: температуру, иконку условий, дату

### `TasksWidget`

- Активные задачи (`NEW` + `IN_PROGRESS`) из `MyTasksViewModel`
- Priority bars: столбики 100% / 70% / 50% для HIGH / MEDIUM / LOW (до 5 штук каждого)
- Тап → `HomescreenDialog` с задачами
- Долгое нажатие → полный экран задач
- Жест через единый `pointerInput` (не через `onClick` WidgetCard, чтобы не конфликтовать)

### `ChatWidget`

- Суммирует непрочитанные из группового чата (`ChatViewModel`) и личных сообщений (`DirectInboxViewModel`)
- `AvatarStack` — стопка аватаров отправителей (до 3 штук, перекрытие -6dp)
- glow усиливается при наличии непрочитанных

### `TodayStatsWidget`

- Данные из `AccountViewModel`: сканирований сегодня, сессий, за неделю, личный рекорд
- Круговой arc-прогрессбар (прогресс к личному рекорду)

### `TeamOnlineWidget`

- Напрямую читает Firestore (`internal_users`) — `isShiftActive == true`
- Обновляется каждые 30 сек

### `HomescreenStatusBar`

Три `GlowPill`-капсулы (glow + gradient border):
- **Склад** — хардкод "Бестужевская 10" (параметр `warehouseName`)
- **Батарея** — цвет: зелёный ≥50%, жёлтый ≥20%, красный <20%
- **Сеть** — WiFi / 5G / 4G / 3G / "—"

Обновляется каждые 30 сек через `TelemetryManager`.

### `HomescreenUserCard`

Плавающая карточка внизу (поверх сетки):
- Аватар (фото из `getEmployeePhotoUrl` или инициалы)
- Имя + роль + статус смены
- Тап → `openAccount`

### `HomescreenDialog`

Универсальное модальное окно:
- Анимация: `scale` 0.85→1 (spring MediumBouncy) + `alpha` 0→1 (220ms)
- Клик на затемнённый фон закрывает
- Параметры: `title`, `accentColor`, `icon`, `onExpand` (кнопка "Открыть"), `content`

---

## Data Layer

### `HomescreenBackgroundEntity` + DAO

Room entity, **одна строка** (id = 0). Хранит:
- `imagePath: String?` — абсолютный путь к файлу в `filesDir`. `null` = дефолтный шейдерный фон
- `blurAmount: Float` — 0..60, дефолт 24
- `darkenAmount: Float` — 0..0.85, дефолт 0.35

DAO: `observe()` (Flow), `get()` (suspend), `upsert()`, `clear()`.

### `HomescreenBackgroundRepository`

- Копирует выбранное изображение в `filesDir/homescreen_bg_{timestamp}.jpg`
- Timestamped filename нужен чтобы Coil не подал кешированную версию
- Старый файл удаляется **после** того как новый записан на диск
- `clearBackground()` — возврат к дефолту, файл тоже удаляется
- `updateAppearance(blur, darken)` — правит параметры без смены изображения

---

## Зависимости на входе `HomescreenScreen`

- `AuthManager` — авторизация, роль, статус смены
- `TelemetryManager` — батарея, сеть
- `ChatViewModel` — групповой чат
- `HomescreenActions` — лямбды навигации (20 штук)

Внутри через `hiltViewModel()`:
- `MyTasksViewModel`
- `DirectInboxViewModel`
- `AccountViewModel`