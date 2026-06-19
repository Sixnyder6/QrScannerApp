package com.example.qrscannerapp.features.inventory.ui.storage.utils

import java.text.SimpleDateFormat
import java.util.*

enum class StorageFilter(val title: String) {
    ALL("Все"),
    AVAILABLE("Есть место"),
    FULL("Заполненные"),
    EMPTY("Пустые")
}

enum class StorageTab(val title: String) {
    CELLS("Ячейки"),
    HUB("Хаб")
}

enum class HubCategory(val title: String) {
    FRAME("Рама"),
    IOT("IOT"),
    LISTS("Списки")
}

fun formatHubDate(date: Date?): String {
    if (date == null) return ""
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(date)
}

