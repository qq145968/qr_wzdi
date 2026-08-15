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
    var scanMode: String = "half",
    var allowDuplicate: Boolean = true,
    var autoSavePhoto: Boolean = true,
    var scanType: String = "all",
    var alertType: String = "sound"
)
