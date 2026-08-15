package com.scanrobot.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScanStore(context: Context) {

    private val prefs = context.getSharedPreferences("scan_robot", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val type = object : TypeToken<MutableList<ScanBatch>>() {}.type

    private var batches: MutableList<ScanBatch> = loadBatches()

    private fun loadBatches(): MutableList<ScanBatch> {
        val json = prefs.getString("batches", null) ?: return mutableListOf()
        return try {
            gson.fromJson(json, type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun saveBatches() {
        prefs.edit().putString("batches", gson.toJson(batches)).apply()
    }

    fun getBatches(): MutableList<ScanBatch> = batches

    fun getCurrentOrCreateBatch(): ScanBatch {
        val now = Date()
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now)

        if (batches.isNotEmpty()) {
            val last = batches[0]
            if (last.date == dateStr) return last
        }

        val batch = ScanBatch(
            name = "扫码批次 " + SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(now),
            time = timeStr,
            date = dateStr
        )
        batches.add(0, batch)
        saveBatches()
        return batch
    }

    fun addRecord(code: String, type: String) {
        val now = Date()
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now)
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)

        val batch = getCurrentOrCreateBatch()
        batch.items.add(0, ScanRecord(code, type, timeStr, dateStr))
        saveBatches()
    }

    fun getBatchById(id: String): ScanBatch? = batches.find { it.id == id }

    fun clearAll() {
        batches.clear()
        saveBatches()
    }

    fun deleteBatch(id: String) {
        batches.removeAll { it.id == id }
        saveBatches()
    }

    fun deleteRecord(batchId: String, index: Int) {
        val batch = batches.find { it.id == batchId }
        if (batch != null && index in batch.items.indices) {
            batch.items.removeAt(index)
            saveBatches()
        }
    }

    // Settings
    fun loadSettings(): ScanSettings {
        val json = prefs.getString("settings", null) ?: return ScanSettings()
        return try {
            gson.fromJson(json, ScanSettings::class.java) ?: ScanSettings()
        } catch (e: Exception) {
            ScanSettings()
        }
    }

    fun saveSettings(settings: ScanSettings) {
        prefs.edit().putString("settings", gson.toJson(settings)).apply()
    }
}
