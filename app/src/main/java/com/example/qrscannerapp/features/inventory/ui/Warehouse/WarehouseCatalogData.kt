package com.example.qrscannerapp.features.inventory.ui.Warehouse.components

/**
 * Модель данных для элемента каталога.
 * Содержит поля для текущего остатка (stockCount) и общего количества (totalStock).
 */
data class DemoCatalogItem(
    val id: String,
    val sku: String? = null,
    val shortName: String,
    val fullName: String,
    val category: String,
    val unit: String = "шт.",
    val stockCount: Int,     // Текущий остаток на складе
    val totalStock: Int,     // Общее/плановое количество
    val imageName: String? = null
)

private const val GITHUB_IMAGE_BASE_URL = "https://raw.githubusercontent.com/Sixnyder6/QrScannerApp/master/images/"

val warehouseCatalogItems = listOf(

    // ==========================================
    // НОВЫЕ ПОЗИЦИИ (30 шт.)
    // ==========================================

    // 1
    DemoCatalogItem(
        id = "new_01",
        sku = "194BY40-01-0",
        shortName = "Левая защита",
        fullName = "Rear Flat Fork Left Side Protect Cover Левая защита задней вилки",
        category = "Пластик",
        stockCount = 1072,
        totalStock = 1072,
        imageName = "zashita_left_zad_vilka"
    ),
    // 2
    DemoCatalogItem(
        id = "new_02",
        sku = "194BY40-02-0",
        shortName = "Правая защита",
        fullName = "Rear Flat Fork Right Side Protect Cover Правая защита задней вилки",
        category = "Пластик",
        stockCount = 1021,
        totalStock = 1021,
        imageName = "zashita_right_zad_vilka"
    ),
    // 3
    DemoCatalogItem(
        id = "new_03",
        sku = "703SP40-01-0",
        shortName = "Передний фонарь",
        fullName = "Headlight Light Передний фонарь",
        category = "Электроника",
        stockCount = 9352,
        totalStock = 9352,
        imageName = "pered_fonar"
    ),
    // 4
    DemoCatalogItem(
        id = "new_04",
        sku = "174XH40-02-0",
        shortName = "Правая защита РГБ",
        fullName = "Scooter Frame Right Side Cover Правая защита бокового индикатора",
        category = "Пластик",
        stockCount = 714,
        totalStock = 714,
        imageName = "zashita_rgb_right"
    ),
    // 5
    DemoCatalogItem(
        id = "new_05",
        sku = "174XH40-01-0",
        shortName = "Левая защита РГБ",
        fullName = "Scooter Frame Left Side Cover Левая защита бокового индикатора",
        category = "Пластик",
        stockCount = 724,
        totalStock = 724,
        imageName = "zashita_rgb_left"
    ),
    // 6
    DemoCatalogItem(
        id = "new_06",
        sku = "484RX40-01-0",
        shortName = "Держатель",
        fullName = "Phone Holder Держатель телефона",
        category = "Механика",
        stockCount = 4075,
        totalStock = 4075,
        imageName = "derjatel_phone"
    ),
    // 7
    DemoCatalogItem(
        id = "new_07",
        sku = "204BY40-02-0",
        shortName = "IOT",
        fullName = "Устройство управления самоката 4.0",
        category = "Электроника",
        stockCount = 1167,
        totalStock = 1167,
        imageName = "IOT"
    ),
    // 8
    DemoCatalogItem(
        id = "new_08",
        sku = "234JY35-01-0-01",
        shortName = "Обод 'Ромашка'",
        fullName = "Motor Wheel Hub Cap Обод \"Ромашка\" Заднего колеса с двигателем",
        category = "Колеса",
        stockCount = 2800,
        totalStock = 2800,
        imageName = "obod_motor_coleso"
    ),
    // 9
    DemoCatalogItem(
        id = "new_09",
        sku = "261DP40-01-0",
        shortName = "Курок газа",
        fullName = "Gas Throttle/Accelerator Курок газа",
        category = "Электроника",
        stockCount = 1950,
        totalStock = 1950,
        imageName = "kurok_gaza"
    ),
    // 10
    DemoCatalogItem(
        id = "new_10",
        sku = "266ZH40-01-0",
        shortName = "Замок",
        fullName = "Electronic Battery Lock Assembly Замок",
        category = "Электроника",
        stockCount = 1900,
        totalStock = 1900,
        imageName = "zamok_electonic"
    ),
    // 11
    DemoCatalogItem(
        id = "new_11",
        sku = "271XX40-01-1",
        shortName = "Контроллер РГБ",
        fullName = "Side Lamp Controller Контроллер боковых фонарей",
        category = "Электроника",
        stockCount = 869,
        totalStock = 869,
        imageName = "controler_rgb"
    ),
    // 12
    DemoCatalogItem(
        id = "new_12",
        sku = "433TH40-01-0",
        shortName = "Кожух IOT",
        fullName = "IOT Holder Кожух-держатель IOT-устройства",
        category = "Пластик",
        stockCount = 893,
        totalStock = 893,
        imageName = "kojuh_iot"
    ),
    // 13
    DemoCatalogItem(
        id = "new_13",
        sku = "743SP40-01-0",
        shortName = "Катофот Перед",
        fullName = "Передний светоотражатель",
        category = "Пластик",
        stockCount = 5859,
        totalStock = 5859,
        imageName = "katafot_pered"
    ),
    // 14
    DemoCatalogItem(
        id = "new_14",
        sku = "763SP40-01-0",
        shortName = "Катофот Бок",
        fullName = "Side Reflector Боковой светоотражатель",
        category = "Пластик",
        stockCount = 3824,
        totalStock = 3824,
        imageName = "katafot_bok"
    ),
    // 15
    DemoCatalogItem(
        id = "new_15",
        sku = "773SP40-01-0",
        shortName = "Кольцо РГБ",
        fullName = "Status Indicate Light Кольцо индикации статуса",
        category = "Электроника",
        stockCount = 1115,
        totalStock = 1115,
        imageName = "kolco_led"
    ),
    // 16
    DemoCatalogItem(
        id = "new_16",
        sku = "783XD40-01-0",
        shortName = "Левая РГБ",
        fullName = "Left Side Lamp Боковой левый индикатор",
        category = "Электроника",
        stockCount = 4768,
        totalStock = 4768,
        imageName = "led_rgb_left"
    ),
    // 17
    DemoCatalogItem(
        id = "new_17",
        sku = "783XD40-02-0",
        shortName = "Правая РГБ",
        fullName = "Right Side Lamp Боковой правый индикатор",
        category = "Электроника",
        stockCount = 4628,
        totalStock = 4628,
        imageName = "led_rgb_right"
    ),
    // 18
    DemoCatalogItem(
        id = "new_18",
        sku = "024HJ40-01-0",
        shortName = "Подножка",
        fullName = "Kickstand Ножка-подставка электросамоката Wind 4.0",
        category = "Механика",
        stockCount = 2948,
        totalStock = 2948,
        imageName = "podnojka"
    ),
    // 19
    DemoCatalogItem(
        id = "new_19",
        sku = "191HF40-01-0",
        shortName = "Задняя Вилка",
        fullName = "Rear Flat Fork Задняя вилка",
        category = "Ходовая",
        stockCount = 1015,
        totalStock = 1015,
        imageName = "vilka_zad"
    ),
    // 20
    DemoCatalogItem(
        id = "new_20",
        sku = "325JS40-01-0",
        shortName = "Передняя вилка",
        fullName = "Front Fork Shoulder Cover Передняя вилка",
        category = "Ходовая",
        stockCount = 824,
        totalStock = 824,
        imageName = "pered_vilka"
    ),
    // 21
    DemoCatalogItem(
        id = "new_21",
        sku = "326CD40-02-0",
        shortName = "Левый амортизатор",
        fullName = "Front Left Rear Shock Absorber Передний левый амортизатор",
        category = "Ходовая",
        stockCount = 725,
        totalStock = 725,
        imageName = "pered_amort_left"
    ),
    // 22
    DemoCatalogItem(
        id = "new_22",
        sku = "326CD40-03-0",
        shortName = "Правый амортизатор",
        fullName = "Front Right Rear Shock Absorber Передний правый амортизатор",
        category = "Ходовая",
        stockCount = 718,
        totalStock = 718,
        imageName = "pered_amort_right"
    ),
    // 23
    DemoCatalogItem(
        id = "new_23",
        sku = "434JB40-01-0",
        shortName = "Крепеж фары",
        fullName = "Holer Front Cover Крепежный элемент фары из дюралюминия",
        category = "Механика",
        stockCount = 1709,
        totalStock = 1709,
        imageName = "krepej_faru"
    ),
    // 24
    DemoCatalogItem(
        id = "new_24",
        sku = "446MF55-01-0",
        shortName = "Стакан Стойки",
        fullName = "Top Main Pole Clamp Зажим желтой основной трубы с принтом (стакан)",
        category = "Ходовая",
        stockCount = 3520,
        totalStock = 3520,
        imageName = "sazhim_stoiki"
    ),
    // 25
    DemoCatalogItem(
        id = "new_25",
        sku = "448SQ55-01-0",
        shortName = "Крючок",
        fullName = "Hook Крючок из пластмассы для сумки Wind 4.0",
        category = "Пластик",
        stockCount = 2867,
        totalStock = 2867,
        imageName = "krujok"
    ),
    // 26
    DemoCatalogItem(
        id = "new_26",
        sku = "449SQ40-01-0",
        shortName = "Рефлектор",
        fullName = "Top Clamp Front Cover Крепежный элемент из пластмассы для светоотражателя",
        category = "Пластик",
        stockCount = 3757,
        totalStock = 3757,
        imageName = "reflektor"
    ),
    // 27
    DemoCatalogItem(
        id = "new_27",
        sku = "633SQ40-01-0",
        shortName = "Переднее крыло",
        fullName = "Front Fender Переднее крыло",
        category = "Пластик",
        stockCount = 383,
        totalStock = 383,
        imageName = "pered_krulo"
    ),
    // 28
    DemoCatalogItem(
        id = "new_28",
        sku = "643SQ40-01-0",
        shortName = "Заднее крыло",
        fullName = "Front Rear Fender Заднее крыло",
        category = "Пластик",
        stockCount = 998,
        totalStock = 998,
        imageName = "zad_krulo"
    ),
    // 29
    DemoCatalogItem(
        id = "new_29",
        sku = "262DP40-01-0",
        shortName = "Левый тормоз",
        fullName = "Left Brake Assembly Левая ручка тормоза (в сборе)",
        category = "Механика",
        stockCount = 701,
        totalStock = 701,
        imageName = "left_sbor_ruchka"
    ),
    // 30
    DemoCatalogItem(
        id = "new_30",
        sku = "262DP40-02-0",
        shortName = "Правый Тормоз",
        fullName = "Right Brake Assembly Правая ручка тормоза (в сборе)",
        category = "Механика",
        stockCount = 780,
        totalStock = 780,
        imageName = "right_sbor_ruchka"
    ),

    // ==========================================
    // ОБНОВЛЕННЫЕ СУЩЕСТВУЮЩИЕ (8 шт.)
    // ==========================================

    // 1 (Обновлено)
    DemoCatalogItem(
        id = "am_zad_01",
        sku = "193LY40-01-0",
        shortName = "Задний амортизатор",
        fullName = "Rear Shock Absorber Задний Амортизатор",
        category = "Ходовая",
        stockCount = 3024,
        totalStock = 3024,
        imageName = "Amort_zad"
    ),
    // 2 (Обновлено)
    DemoCatalogItem(
        id = "dk_zad_01",
        sku = "168ZD40-01-0",
        shortName = "Задний Коврик",
        fullName = "Grip for Tail Seat Cover Pad Задний коврик",
        category = "Пластик",
        stockCount = 9986,
        totalStock = 9986,
        imageName = "Kovrik_zad"
    ),
    // 3 (Обновлено)
    DemoCatalogItem(
        id = "dk_pered_01",
        sku = "162ZD40-01-0",
        shortName = "Передний Коврик",
        fullName = "Curved Deck Base Grip Передний коврик",
        category = "Пластик",
        stockCount = 5619,
        totalStock = 5619,
        imageName = "Kovrik_pered"
    ),
    // 4 (Обновлено)
    DemoCatalogItem(
        id = "wh_front_01",
        sku = "613TZ10-01-0",
        shortName = "Колесо",
        fullName = "Front Wheel Set Переднее колесо",
        category = "Колеса",
        stockCount = 881,
        totalStock = 881,
        imageName = "koleso_pered"
    ),
    // 5 (Обновлено)
    DemoCatalogItem(
        id = "el_ctrl_v4",
        sku = "214ST36-01-0",
        shortName = "Контроллер",
        fullName = "ECU Контроллер управления самоката Wind 4.0",
        category = "Электроника",
        stockCount = 1734,
        totalStock = 1734,
        imageName = "kontoler"
    ),
    // 6 (Обновлено)
    DemoCatalogItem(
        id = "wh_motor_01",
        sku = "234XD35-01-0",
        shortName = "Мотор-колесо",
        fullName = "Motor Заднее колесо с двигателем",
        category = "Колеса",
        stockCount = 3616,
        totalStock = 3616,
        imageName = "Motor_koleso"
    ),
    // 7 (Обновлено)
    DemoCatalogItem(
        id = "el_switch_main_01",
        sku = "268KH40-01-0",
        shortName = "Блок кнопок",
        fullName = "Light Button Кнопочный переключатель для вкл/выкл фар Wind 4.0",
        category = "Электроника",
        stockCount = 2175,
        totalStock = 2175,
        imageName = "knopki_sveta"
    ),
    // 8 (Обновлено)---------------------------------------------------------------------------------------------------------------------------------------------------------+-------------
    DemoCatalogItem(
        id = "el_light_rear_02",
        sku = "723SP40-01-0",
        shortName = "Задний фонарь",
        fullName = "Brake Light Задний стоп-сигнал",
        category = "Электроника",
        stockCount = 837,
        totalStock = 837,
        imageName = "fara_zad"
    ),


    // ==========================================
    // ОСТАЛЬНЫЕ СТАРЫЕ ПОЗИЦИИ (Без изменений)
    // ==========================================

    DemoCatalogItem(id = "am_truba_stoiki_01", sku = null, shortName = "Труба стойки", fullName = "Труба рулевой стойки", category = "Ходовая", stockCount = 520, totalStock = 600, imageName = "truba_stoiki.jpg"),
    DemoCatalogItem(id = "am_gaika_stoiki_01", sku = null, shortName = "Гайка стойки", fullName = "Гайка рулевой стойки", category = "Ходовая", stockCount = 750, totalStock = 1000, imageName = "gaika_stoiki.jpg"),
    DemoCatalogItem(id = "br_zad_01", sku = null, shortName = "Тормоз. барабан", fullName = "Тормозной барабан задний", category = "Механика", stockCount = 112, totalStock = 150, imageName = "baraban_zad.jpg"),
    DemoCatalogItem(id = "mc_brake_lvr_01", sku = null, shortName = "Ручка тормоза", fullName = "Ручка тормоза (левая)", category = "Механика", stockCount = 220, totalStock = 300),
    DemoCatalogItem(id = "mc_grip_01", sku = null, shortName = "Грипса", fullName = "Грипса руля (правая)", category = "Механика", stockCount = 400, totalStock = 500),
    DemoCatalogItem(id = "pl_pered_01", sku = null, shortName = "Пластик пер.", fullName = "Пластик корпуса передний", category = "Пластик", stockCount = 180, totalStock = 200, imageName = "Plastik_pered.jpg"),
    DemoCatalogItem(id = "pl_zad_01", sku = null, shortName = "Пластик задний", fullName = "Пластик корпуса задний", category = "Пластик", stockCount = 195, totalStock = 200, imageName = "Plastik_zad.jpg"),
    DemoCatalogItem(id = "el_light_front", sku = null, shortName = "Фара", fullName = "Фара передняя (Arctic V3)", category = "Электроника", stockCount = 95, totalStock = 100),
    DemoCatalogItem(id = "el_light_rear", sku = null, shortName = "Задний стоп", fullName = "Задний стоп-сигнал", category = "Электроника", stockCount = 130, totalStock = 150),
    DemoCatalogItem(id = "fp_bolt_m5", sku = null, shortName = "Болтики М5", fullName = "Болты М5x20", category = "Крепеж", unit = "грамм", stockCount = 15230, totalStock = 20000),
    DemoCatalogItem(id = "con_lube_valera", sku = "CHEM-VB-400", shortName = "Жидкий ключ", fullName = "Смазка проникающая 'Валера'", category = "Расходники", stockCount = 115, totalStock = 144, imageName = "Valera_smazka"),
    DemoCatalogItem(id = "con_glue_cosmo", sku = null, shortName = "Суперклей", fullName = "Клей Cosmofen CA-500.200", category = "Расходники", stockCount = 230, totalStock = 300, imageName = "cosmo_cley"),
    DemoCatalogItem(id = "con_glue_activator", sku = null, shortName = "Активатор клея", fullName = "Активатор для цианакрилатного клея", category = "Расходники", stockCount = 78, totalStock = 100, imageName = "aktivator_kleya"),
    DemoCatalogItem(id = "con_paint_black", sku = null, shortName = "Эмаль черная", fullName = "Эмаль акриловая черная (аэрозоль)", category = "Расходники", stockCount = 55, totalStock = 72, imageName = "emal_akril_black"),
    DemoCatalogItem(id = "con_disk_125", sku = null, shortName = "Круг отрезной", fullName = "Круг отрезной по металлу 125мм", category = "Расходники", stockCount = 890, totalStock = 1000, imageName = "krug_otreznoy125"),
    DemoCatalogItem(id = "cn_gloves_01", sku = null, shortName = "Перчатки", fullName = "Перчатки рабочие (размер L)", category = "Расходники", unit = "пар", stockCount = 500, totalStock = 600),
    DemoCatalogItem(id = "con_gloves_nylon", sku = null, shortName = "Перчатки нейлон", fullName = "Перчатки нейлоновые черные", category = "Расходники", unit = "пар", stockCount = 450, totalStock = 500, imageName = "perchatki_black_neilon"),
    DemoCatalogItem(id = "con_gloves_zubr", sku = null, shortName = "Перчатки 'Зубр'", fullName = "Перчатки рабочие 'Зубр' (синие)", category = "Расходники", unit = "пар", stockCount = 620, totalStock = 700, imageName = "perchatki_zubr")
)

fun getImageUrl(item: DemoCatalogItem): String? {
    val imageNameWithExt = item.imageName?.let {
        if (it.endsWith(".jpg") || it.endsWith(".png") || it.endsWith(".jpeg")) it else "$it.jpg"
    }
    return imageNameWithExt?.let { GITHUB_IMAGE_BASE_URL + it }
}