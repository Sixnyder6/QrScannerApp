# Документация по модулю Warehouse

Этот документ описывает назначение каждого файла в пакете `com.example.qrscannerapp.features.inventory.ui.Warehouse`.

### [`WarehouseModels.kt`](file:///C:/Users/pankr/AndroidStudioProjects/QrScannerApp/app/src/main/java/com/example/qrscannerapp/features/inventory/ui/Warehouse/WarehouseModels.kt)

**Назначение:** Определяет основные модели данных (data classes) для работы со складом и Firebase Firestore.
- **`WarehouseItem`**: Модель товара на складе (название, SKU/артикул, количество, категория, порог дефицита).
- **`WarehouseLog`**: Модель записи в журнале операций. Отслеживает списания и поступления, автора и время операции.
- **`WarehouseOrder`**: Модель заказа/заявки на запчасти от техников.
- **`OrderItem`**: Модель элемента в заказе.
- **`OrderStatus`**: Enum статусов заказа (`CREATED`, `PROCESSING`, `READY`, `COMPLETED`, `CANCELLED`).

### [`WarehouseScreen.kt`](file:///C:/Users/pankr/AndroidStudioProjects/QrScannerApp/app/src/main/java/com/example/qrscannerapp/features/inventory/ui/Warehouse/WarehouseScreen.kt)

**Назначение:** Компонент пользовательского интерфейса (UI), который интегрируется в другие части приложения (например, в экран сканера).
- Связывает `WarehouseViewModel` с экраном каталога.
- Обеспечивает реакцию на отсканированные QR-коды для мгновенного списания/просмотра запчасти.

### [`WarehouseViewModel.kt`](file:///C:/Users/pankr/AndroidStudioProjects/QrScannerApp/app/src/main/java/com/example/qrscannerapp/features/inventory/ui/Warehouse/WarehouseViewModel.kt)

**Назначение:** `ViewModel` для модуля склада, управляющая бизнес-логикой и реактивным состоянием интерфейса.
- Подписывается на потоки данных реального времени из `WarehouseRepository` (товары, логи, новости, сотрудники).
- Предоставляет `StateFlow` для корзины (`cart`), заявок (`orders`), журнала операций (`logs`/`groupedActivities`) и состояния смены (`shiftState`).
- Обрабатывает корзину (добавление, удаление, очистка, изменение количества) и отправку заказов.
- Содержит бизнес-логику для сборки и выдачи заказов кладовщиком, изменения статуса смены, импорта демонстрационных данных и поиска/загрузки истории конкретного сотрудника.

### [`WarehouseNewsModels.kt`](file:///C:/Users/pankr/AndroidStudioProjects/QrScannerApp/app/src/main/java/com/example/qrscannerapp/features/inventory/ui/Warehouse/WarehouseNewsModels.kt)

**Назначение:** Определяет модели данных для внутренней системы объявлений и новостей склада.
- **`NewsTag`**: Enum тегов объявлений ("Сегодня", "Завтра", "Срочно") с цветовой индикацией.
- **`NewsItem`**: Модель отдельной новости с заголовком, текстом и тегом.

### [`WarehouseRepository.kt`](file:///C:/Users/pankr/AndroidStudioProjects/QrScannerApp/app/src/main/java/com/example/qrscannerapp/features/inventory/ui/Warehouse/WarehouseRepository.kt)

**Назначение:** Репозиторий для доступа и синхронизации данных с облачной БД Firebase Firestore.
- Получает в реальном времени обновления по товарам, логам, новостям, заказам и сменам.
- Реализует транзакции для атомарного изменения остатков на складе при прямом списании или при выдаче заказа.
- Содержит логику создания заказов, обновления статусов заявок и выполнения запросов истории по сотрудникам.

### [`WarehouseCatalogData.kt`](file:///C:/Users/pankr/AndroidStudioProjects/QrScannerApp/app/src/main/java/com/example/qrscannerapp/features/inventory/ui/Warehouse/WarehouseCatalogData.kt)

**Назначение:** Содержит статические демонстрационные данные для каталога склада.
- Предоставляет список `warehouseCatalogItems` для первоначального заполнения базы данных.

### [`WarehouseAddItemScreen.kt`](file:///C:/Users/pankr/AndroidStudioProjects/QrScannerApp/app/src/main/java/com/example/qrscannerapp/features/inventory/ui/Warehouse/WarehouseAddItemScreen.kt)

**Назначение:** Форма добавления нового товара на склад.
- Позволяет задать полное и короткое название, SKU, описание, категорию, единицу измерения, количество и порог дефицита.
- Поддерживает динамическое создание новых категорий.

### [`WarehouseDashboardScreen.kt`](file:///C:/Users/pankr/AndroidStudioProjects/QrScannerApp/app/src/main/java/com/example/qrscannerapp/features/inventory/ui/Warehouse/WarehouseDashboardScreen.kt)

**Назначение:** Главный экран (панель управления) складского модуля с премиальным и динамичным дизайном.
- **Умный iPhone-style виджет (`WarehouseStockActivityWidget`)**: Ротирующийся каждые 7 секунд слайдер с 3 карточками:
  - **Слайд 1**: График тренда списаний за последние 7 дней (`StockActivityChart`) на основе кривых Безье и пульсирующей точки.
  - **Слайд 2**: Линейные индикаторы дефицитных товаров (топ-3 позиции, требующие пополнения).
  - **Слайд 3**: Лента недавних логов активности в реальном времени.
- **Активные заказы**: Отображение входящих заявок (для кладовщиков) или отправленных заказов (для техников) с переходом в BottomSheet сборки/выдачи.
- **Кнопки быстрого доступа**: Стилизованные карточки "История" и "Каталог" с микро-анимацией нажатия и иконками-шевронами.
- **Интегрированные новости (`EmbeddedNewsWidget`)**: Полноширинный анимированный баннер с автопрокруткой объявлений.
- **Инструменты кладовщика**: Быстрый выбор сменного сотрудника и его статуса, детальный просмотр истории списаний по конкретному сотруднику и имитация синхронизации с 1С.

### [`WarehouseHistoryScreen.kt`](file:///C:/Users/pankr/AndroidStudioProjects/QrScannerApp/app/src/main/java/com/example/qrscannerapp/features/inventory/ui/Warehouse/WarehouseHistoryScreen.kt)

**Назначение:** Экран детальной истории и аналитики операций склада.
- Поддерживает переключение вкладок "Все списания" и "Мои списания".
- Содержит текстовый поиск по запчастям или сотрудникам, а также фильтр периодов (Сегодня, Все время, За диапазон дат с вызовом календаря).
- **Панель статистики (`StatsPanel`)**: Сводные карточки транзакций, списаний, поступлений и топ-3 часто списываемых товаров.
- Отображает хронологический таймлайн операций, сгруппированных по календарным дням, с цветовой дифференциацией операций (зеленый для поступления, красный для списания).

### [`WarehouseActivityComponents.kt`](file:///C:/Users/pankr/AndroidStudioProjects/QrScannerApp/app/src/main/java/com/example/qrscannerapp/features/inventory/ui/Warehouse/WarehouseActivityComponents.kt) (в пакете `Warehouse.components`)

**Назначение:** Набор модальных окон для отображения операций.
- **`ActivityLogSheet`**: Всплывающий журнал сгруппированных транзакций (`GroupedActivity`), объединяющий списания одного человека за короткое время.
- **`ActivityDetailSheet`**: Детальный состав конкретной сгруппированной операции.
- **`EmployeeHistorySheet`**: Интерфейс для менеджеров, позволяющий выбрать сотрудника и увидеть сводную статистику его операций (количество транзакций, штук и дней активности) вместе с таймлайном его списаний.

### [`WarehouseCatalogComponents.kt`](file:///C:/Users/pankr/AndroidStudioProjects/QrScannerApp/app/src/main/java/com/example/qrscannerapp/features/inventory/ui/Warehouse/WarehouseCatalogComponents.kt) (в пакете `Warehouse.components`)

**Назначение:** UI-компоненты для работы с каталогом товаров и заказами.
- **`WarehouseCatalogScreen`**: Интегрирует сетку товаров, поиск, фильтр по категориям (`CategoryChip`), сканирование и интерфейс корзины.
- **Встроенный сканер камеры (`WarehouseScannerOverlay`)**: Полноэкранный видоискатель с анимацией лазерного луча, фонариком, вибро/аудио подтверждением и отслеживанием товаров в корзине.
- **Корзина заказов (Cart BottomSheet)**: Панель для управления выбранными деталями перед отправкой заявки или мгновенным списанием.
- Содержит диалоговые окна подтверждения списания, удаления и редактирования информации о товарах.

### [`WarehouseNewsComponents.kt`](file:///C:/Users/pankr/AndroidStudioProjects/QrScannerApp/app/src/main/java/com/example/qrscannerapp/features/inventory/ui/Warehouse/WarehouseNewsComponents.kt) (в пакете `Warehouse.components`)

**Назначение:** Компоненты для администрирования новостей склада.
- **`NewsEditSheet`**: Модальный диалог создания, изменения и удаления новостных карточек с возможностью выбора тегов важности.
