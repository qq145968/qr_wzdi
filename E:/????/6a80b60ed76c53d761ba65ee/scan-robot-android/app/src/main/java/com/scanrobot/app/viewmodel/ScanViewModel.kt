package com.scanrobot.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scanrobot.app.ScanApp
import com.scanrobot.app.data.ScanBatch
import com.scanrobot.app.data.ScanRecord
import com.scanrobot.app.data.ScanSettings
import com.scanrobot.app.data.ScanStore
import com.scanrobot.app.util.BeepManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class Screen {
    data object Home : Screen()
    data object Scanner : Screen()
    data class Detail(val batchId: String) : Screen()
}

data class ScanListItem(
    val code: String,
    val type: String,
    val time: String
)

class ScanViewModel(app: Application) : AndroidViewModel(app) {

    private val store: ScanStore = ScanApp.instance.scanStore
    val beepManager = BeepManager(app)

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _settings = MutableStateFlow(store.loadSettings())
    val settings: StateFlow<ScanSettings> = _settings.asStateFlow()

    private val _batches = MutableStateFlow<MutableList<ScanBatch>>(mutableListOf())
    val batches: StateFlow<MutableList<ScanBatch>> = _batches.asStateFlow()

    private val _scanList = MutableStateFlow<List<ScanListItem>>(emptyList())
    val scanList: StateFlow<List<ScanListItem>> = _scanList.asStateFlow()

    private val _flashOn = MutableStateFlow(false)
    val flashOn: StateFlow<Boolean> = _flashOn.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val recentCodes = mutableMapOf<String, Long>()

    init {
        loadBatches()
    }

    fun loadBatches() {
        _batches.value = store.getBatches()
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun goBack() {
        _currentScreen.value = Screen.Home
        loadBatches()
    }

    // Settings
    fun updateSettings(settings: ScanSettings) {
        _settings.value = settings
        store.saveSettings(settings)
    }

    fun toggleDuplicate() {
        val s = _settings.value
        s.allowDuplicate = !s.allowDuplicate
        updateSettings(s)
    }

    fun togglePhoto() {
        val s = _settings.value
        s.autoSavePhoto = !s.autoSavePhoto
        updateSettings(s)
    }

    fun setScanMode(mode: String) {
        val s = _settings.value
        s.scanMode = mode
        updateSettings(s)
    }

    fun setScanType(type: String) {
        val s = _settings.value
        s.scanType = type
        updateSettings(s)
    }

    fun setAlertType(type: String) {
        val s = _settings.value
        s.alertType = type
        updateSettings(s)
    }

    // Flash
    fun toggleFlash() {
        _flashOn.value = !_flashOn.value
        showToast(if (_flashOn.value) "闪光灯已开" else "闪光灯已关")
    }

    // Scanner
    fun startNewScanSession() {
        _scanList.value = emptyList()
        recentCodes.clear()
    }

    fun handleScanResult(code: String, type: String) {
        val now = System.currentTimeMillis()

        if (recentCodes[code] != null && now - (recentCodes[code] ?: 0) < 2000) return

        val settings = _settings.value
        if (!settings.allowDuplicate) {
            if (_scanList.value.any { it.code == code }) return
        }

        recentCodes[code] = now

        beepManager.playBeep(settings.alertType)

        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val newList = listOf(ScanListItem(code, type, timeStr)) + _scanList.value
        _scanList.value = newList

        store.addRecord(code, type)
        loadBatches()
    }

    fun deleteScanItem(index: Int) {
        val list = _scanList.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _scanList.value = list
        }
    }

    fun copyAll(): String {
        return _scanList.value.joinToString("\n") { it.code }
    }

    fun exportCsv(): String {
        val sb = StringBuilder()
        sb.append("序号,编码,类型,时间\n")
        _scanList.value.forEachIndexed { idx, item ->
            val t = if (item.type == "qrcode") "二维码" else "条形码"
            sb.append("${idx + 1},${item.code},$t,${item.time}\n")
        }
        return sb.toString()
    }

    // Batch management
    fun getBatchById(id: String): ScanBatch? = store.getBatchById(id)

    fun deleteBatch(id: String) {
        store.deleteBatch(id)
        loadBatches()
    }

    fun deleteRecord(batchId: String, index: Int) {
        store.deleteRecord(batchId, index)
        loadBatches()
    }

    fun clearAll() {
        store.clearAll()
        loadBatches()
        _scanList.value = emptyList()
    }

    fun exportHistory(): String {
        val sb = StringBuilder()
        _batches.value.forEach { batch ->
            sb.append("${batch.name} (${batch.time})\n")
            batch.items.forEach { item ->
                sb.append("  ${item.code} [${item.time}]\n")
            }
            sb.append("\n")
        }
        return sb.toString()
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    override fun onCleared() {
        beepManager.release()
        super.onCleared()
    }
}
