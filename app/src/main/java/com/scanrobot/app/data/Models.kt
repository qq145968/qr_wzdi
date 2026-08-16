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

data class ScanModeOption(
    val key: String,
    val title: String,
    val description: String
)

data class AppInfo(
    val announcement: String = "",
    val maintenanceMode: Boolean = false,
    val registrationRequired: Boolean = true,
    val captchaEnabled: Boolean = false,
    val splashScreenUrl: String = "",
    val appName: String = "扫码机器人",
    val appDescription: String = "让手机变成扫码枪",
    val latestVersion: VersionInfo? = null,
    val messages: List<AppMessage> = emptyList(),
    val unreadCount: Int = 0
)

data class VersionInfo(
    val versionCode: Int = 0,
    val versionName: String = "",
    val downloadUrl: String = "",
    val updateContent: String = "",
    val forceUpdate: Boolean = false,
    val fileSize: Long = 0
)

data class AppMessage(
    val id: Int = 0,
    val title: String = "",
    val content: String = "",
    val type: String = "system",
    val createdAt: String = "",
    val read: Boolean = false
)

data class CaptchaResult(
    val captchaId: String = "",
    val captchaImage: String = ""
)

val scanModeOptions = listOf(
    ScanModeOption("full", "全屏连扫", "沉浸式扫码，连续效率高，自动保存清晰照片"),
    ScanModeOption("new_full", "新版全屏连扫", "基于全屏，支持扫码列表显示，体验更流畅"),
    ScanModeOption("half", "半屏连扫", "同步列表显示，效率高，自动保存清晰照片"),
    ScanModeOption("wechat", "微信原生扫码", "识别率高，切页体验差，照片模糊风险")
)
