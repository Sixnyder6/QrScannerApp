package com.example.qrscannerapp.features.scanner.domain.model

import com.example.qrscannerapp.core.model.SessionType // <-- ИСПРАВЛЕНО
import java.util.UUID

data class ScanItem(
    val id: String = UUID.randomUUID().toString(),
    val code: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val thumbnail: ByteArray? = null
) {
    constructor() : this("", "", 0L, null)
    // equals и hashCode...
}

data class ScanSession(
    val id: String = UUID.randomUUID().toString(),
    val name: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val items: List<ScanItem> = emptyList(),
    val type: SessionType = SessionType.SCOOTERS,
    val creatorId: String? = null,
    val creatorName: String? = null
) {
    constructor() : this("", null, 0L, emptyList(), SessionType.SCOOTERS, null, null)
}