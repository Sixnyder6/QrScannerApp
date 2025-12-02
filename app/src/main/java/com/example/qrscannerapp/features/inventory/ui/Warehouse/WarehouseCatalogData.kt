// Полное содержимое для ОБНОВЛЕННОГО файла WarehouseCatalogData.kt

package com.example.qrscannerapp.features.inventory.ui.Warehouse.components

// ИЗМЕНЕНИЕ: Модель данных расширена новыми полями
data class DemoCatalogItem(
    val id: String,
    val shortName: String, // Короткое название для плитки
    val fullName: String,  // Полное название для диалога
    val category: String,
    val unit: String = "шт.",
    val stockCount: Int,   // Демонстрационное количество на складе
    val imageName: String? = null
)

private const val GITHUB_IMAGE_BASE_URL = "https://raw.githubusercontent.com/Sixnyder6/QrScannerApp/master/images/"

/**
 * Основной источник данных для каталога запчастей.
 */
val warehouseCatalogItems = listOf(
    // V-- НАЧАЛО ОБНОВЛЕННОГО СПИСКА С НОВЫМИ ПОЛЯМИ --V
    DemoCatalogItem(id = "wh_front_01", shortName = "Колесо", fullName = "Колесо переднее", category = "Колеса", stockCount = 88, imageName = "koleso_pered.jpg"),
    DemoCatalogItem(id = "wh_motor_01", shortName = "Мотор-колесо", fullName = "Мотор-колесо заднее", category = "Колеса", stockCount = 42, imageName = "Motor_koleso.jpg"),
    DemoCatalogItem(id = "br_zad_01", shortName = "Тормоз. барабан", fullName = "Тормозной барабан задний", category = "Механика", stockCount = 112, imageName = "baraban_zad.jpg"),
    DemoCatalogItem(id = "am_zad_01", shortName = "Аморт. задний", fullName = "Амортизатор задний", category = "Ходовая", stockCount = 76, imageName = "Amort_zad.jpg"),
    DemoCatalogItem(id = "dk_pered_01", shortName = "Коврик пер.", fullName = "Коврик деки передний", category = "Пластик", stockCount = 250, imageName = "Kovrik_pered.jpg"),
    DemoCatalogItem(id = "dk_zad_01", shortName = "Коврик задний", fullName = "Коврик деки задний", category = "Пластик", stockCount = 310, imageName = "Kovrik_zad.jpg"),
    DemoCatalogItem(id = "pl_pered_01", shortName = "Пластик пер.", fullName = "Пластик корпуса передний", category = "Пластик", stockCount = 180, imageName = "Plastik_pered.jpg"),
    DemoCatalogItem(id = "pl_zad_01", shortName = "Пластик задний", fullName = "Пластик корпуса задний", category = "Пластик", stockCount = 195, imageName = "Plastik_zad.jpg"),

    DemoCatalogItem(id = "fp_bolt_m5", shortName = "Болтики М5", fullName = "Болты М5x20", category = "Крепеж", unit = "грамм", stockCount = 15230),
    DemoCatalogItem(id = "mc_brake_lvr_01", shortName = "Ручка тормоза", fullName = "Ручка тормоза (левая)", category = "Механика", stockCount = 220),
    DemoCatalogItem(id = "mc_grip_01", shortName = "Грипса", fullName = "Грипса руля (правая)", category = "Механика", stockCount = 400),
    DemoCatalogItem(id = "el_ctrl_v3", shortName = "Контроллер", fullName = "Контроллер V3.1 (48V)", category = "Электроника", stockCount = 35),
    DemoCatalogItem(id = "el_light_front", shortName = "Фара", fullName = "Фара передняя (Arctic V3)", category = "Электроника", stockCount = 95),
    DemoCatalogItem(id = "el_light_rear", shortName = "Задний стоп", fullName = "Задний стоп-сигнал", category = "Электроника", stockCount = 130),
    DemoCatalogItem(id = "cn_gloves_01", shortName = "Перчатки", fullName = "Перчатки рабочие (размер L)", category = "Расходники", unit = "пар", stockCount = 500)
    // ^-- КОНЕЦ ОБНОВЛЕННОГО СПИСКА --^
)

fun getImageUrl(item: DemoCatalogItem): String? {
    val imageNameWithExt = item.imageName?.let {
        if (it.endsWith(".jpg") || it.endsWith(".png") || it.endsWith(".jpeg")) it else "$it.jpg"
    }
    return imageNameWithExt?.let { GITHUB_IMAGE_BASE_URL + it }
}