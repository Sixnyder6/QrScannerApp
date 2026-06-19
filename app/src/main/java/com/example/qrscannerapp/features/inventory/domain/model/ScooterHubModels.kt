package com.example.qrscannerapp.features.inventory.domain.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import java.util.UUID

enum class HubEntryType {
    FRAME, IOT
}

data class HubEntry(
    val id: String = UUID.randomUUID().toString(),
    val type: HubEntryType = HubEntryType.FRAME,
    val scooterId: String = "",
    val userId: String = "",
    val userName: String = "",
    @ServerTimestamp val timestamp: Date? = null,
    
    // Frame specific
    val oldFrameId: String? = null,
    val newFrameId: String? = null,
    val mileage: Int? = null,
    
    // IOT specific
    val oldImei: String? = null,
    val newImei: String? = null,
    
    val comment: String? = null
) {
    // No-arg constructor for Firestore
    constructor() : this(id = UUID.randomUUID().toString())
}
