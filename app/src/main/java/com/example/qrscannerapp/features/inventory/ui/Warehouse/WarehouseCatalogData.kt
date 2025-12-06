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

    // --- Колеса и Ходовая ---
    DemoCatalogItem(id = "wh_front_01", sku = null, shortName = "Колесо", fullName = "Колесо переднее", category = "Колеса", stockCount = 88, totalStock = 100, imageName = "koleso_pered.jpg"),
    DemoCatalogItem(id = "wh_motor_01", sku = "MOT-WH-48-500", shortName = "Мотор-колесо", fullName = "Мотор-колесо заднее", category = "Колеса", stockCount = 42, totalStock = 50, imageName = "Motor_koleso.jpg"),
    DemoCatalogItem(id = "am_zad_01", sku = null, shortName = "Аморт. задний", fullName = "Амортизатор задний", category = "Ходовая", stockCount = 76, totalStock = 120, imageName = "Amort_zad.jpg"),
    DemoCatalogItem(id = "am_truba_stoiki_01", sku = null, shortName = "Труба стойки", fullName = "Труба рулевой стойки", category = "Ходовая", stockCount = 520, totalStock = 600, imageName = "truba_stoiki.jpg"),
    DemoCatalogItem(id = "am_gaika_stoiki_01", sku = null, shortName = "Гайка стойки", fullName = "Гайка рулевой стойки", category = "Ходовая", stockCount = 750, totalStock = 1000, imageName = "gaika_stoiki.jpg"),

    // --- Механика ---
    DemoCatalogItem(id = "br_zad_01", sku = null, shortName = "Тормоз. барабан", fullName = "Тормозной барабан задний", category = "Механика", stockCount = 112, totalStock = 150, imageName = "baraban_zad.jpg"),
    DemoCatalogItem(id = "mc_brake_lvr_01", sku = null, shortName = "Ручка тормоза", fullName = "Ручка тормоза (левая)", category = "Механика", stockCount = 220, totalStock = 300),
    DemoCatalogItem(id = "mc_grip_01", sku = null, shortName = "Грипса", fullName = "Грипса руля (правая)", category = "Механика", stockCount = 400, totalStock = 500),

    // --- Пластик и Корпус ---
    DemoCatalogItem(id = "dk_pered_01", sku = null, shortName = "Коврик пер.", fullName = "Коврик деки передний", category = "Пластик", stockCount = 250, totalStock = 400, imageName = "Kovrik_pered.jpg"),
    DemoCatalogItem(id = "dk_zad_01", sku = null, shortName = "Коврик задний", fullName = "Коврик деки задний", category = "Пластик", stockCount = 310, totalStock = 400, imageName = "Kovrik_zad.jpg"),
    DemoCatalogItem(id = "pl_pered_01", sku = null, shortName = "Пластик пер.", fullName = "Пластик корпуса передний", category = "Пластик", stockCount = 180, totalStock = 200, imageName = "Plastik_pered.jpg"),
    DemoCatalogItem(id = "pl_zad_01", sku = null, shortName = "Пластик задний", fullName = "Пластик корпуса задний", category = "Пластик", stockCount = 195, totalStock = 200, imageName = "Plastik_zad.jpg"),
    DemoCatalogItem(id = "pl_zashita_leva_01", sku = null, shortName = "Защита мотора лев.", fullName = "Защита мотора левая", category = "Пластик", stockCount = 510, totalStock = 600, imageName = "zashita_motor_leva.jpg"),
    DemoCatalogItem(id = "pl_zashita_prava_01", sku = null, shortName = "Защита деки прав.", fullName = "Защита деки правая (с подсветкой)", category = "Пластик", stockCount = 515, totalStock = 600, imageName = "zashita_rgb_prava.jpg"),
    DemoCatalogItem(id = "pl_derjatel_sklad_01", sku = null, shortName = "Держатель склад.", fullName = "Держатель механизма складывания", category = "Пластик", stockCount = 680, totalStock = 800, imageName = "derjatel.jpg"),

    // --- Электроника ---
    DemoCatalogItem(id = "el_ctrl_v3", sku = "EL-CTR-V31-48", shortName = "Контроллер", fullName = "Контроллер V3.1 (48V)", category = "Электроника", stockCount = 35, totalStock = 50),
    DemoCatalogItem(id = "el_light_front", sku = null, shortName = "Фара", fullName = "Фара передняя (Arctic V3)", category = "Электроника", stockCount = 95, totalStock = 100),
    DemoCatalogItem(id = "el_light_rear", sku = null, shortName = "Задний стоп", fullName = "Задний стоп-сигнал", category = "Электроника", stockCount = 130, totalStock = 150),
    DemoCatalogItem(id = "el_ctrl_v4", sku = "EL-CTR-V40-48", shortName = "Контроллер V4", fullName = "Контроллер V4.0 (48V)", category = "Электроника", stockCount = 505, totalStock = 550, imageName = "kontoler.jpg"),
    DemoCatalogItem(id = "el_switch_main_01", sku = null, shortName = "Блок кнопок", fullName = "Блок кнопок управления светом и сигналом", category = "Электроника", stockCount = 610, totalStock = 750, imageName = "knopki_sveta.jpg"),
    DemoCatalogItem(id = "el_light_rear_02", sku = null, shortName = "Задний фонарь", fullName = "Задний фонарь (стоп-сигнал)", category = "Электроника", stockCount = 550, totalStock = 700, imageName = "fara_zad.jpg"),

    // --- Крепеж ---
    DemoCatalogItem(id = "fp_bolt_m5", sku = null, shortName = "Болтики М5", fullName = "Болты М5x20", category = "Крепеж", unit = "грамм", stockCount = 15230, totalStock = 20000),

    // --- Расходники и Химия ---
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