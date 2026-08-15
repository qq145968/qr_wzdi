package com.scanrobot.app.data

import java.util.UUID

data class ScanRecord(
    val code: String,
    val type: String,
    val time: String,
    val date: String
)

data class ScanBatch(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val time: String,
    val date: String,
    val items: MutableList<ScanRecord> = mutableListOf()
) {
    val count: Int get() = items.size
}

data class ScanSettings(
    val scanMode: String = "half",
    val allowDuplicate: Boolean = true,
    val autoSavePhoto: Boolean = true,
    val scanType: String = "all",
    val alertType: String = "sound"
)
