package com.example.qrscannerapp.features.inventory.ui.storage.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

fun formatRelativeTime(date: Date?): String {
    if (date == null) return "—"
    val now = System.currentTimeMillis()
    val diff = now - date.time
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "только что"
        diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} мин назад"
        diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)} ч назад"
        diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)} дн назад"
        else -> SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(date)
    }
}

fun formatRelativeTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return "—"
    return formatRelativeTime(Date(timestamp))
}

fun formatLogTimestamp(timestamp: Long): String {
    return SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(timestamp))
}

fun formatAbsoluteDate(date: Date?): String {
    if (date == null) return "—"
    return SimpleDateFormat("dd.MM 'в' HH:mm", Locale.getDefault()).format(date)
}

fun formatLogTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

