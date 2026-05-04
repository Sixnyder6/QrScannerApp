package com.example.qrscannerapp.features.scanner.domain.util

object ScannerCodeUtils {

    fun extractScooterCode(raw: String): String? {
        if (raw.contains("number=")) {
            val extracted = raw.substringAfter("number=")
                .split('&', '?', '#').firstOrNull()?.trim()
            return if (extracted.isNullOrBlank()) null else extracted.uppercase()
        }
        if (raw.contains('/')) {
            val segment = raw.split('/').lastOrNull { it.isNotBlank() }
            if (!segment.isNullOrBlank() && segment.matches(Regex("[A-Za-z0-9]{2,12}"))) {
                return segment.uppercase()
            }
        }
        val cleaned = raw.trim()
        if (cleaned.matches(Regex("[A-Za-z0-9]{2,12}"))) return cleaned.uppercase()
        return null
    }
}
