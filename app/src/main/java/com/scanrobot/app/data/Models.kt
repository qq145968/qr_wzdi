package com.scanrobot.app.data

import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

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
    /** 兼容旧版：任一验证码开启就为 true */
    val captchaEnabled: Boolean = false,
    /** 登录页是否开启图形验证码（新版拆分键）*/
    val captchaLoginEnabled: Boolean = false,
    /** 注册页是否开启图形验证码（新版拆分键）*/
    val captchaRegisterEnabled: Boolean = false,
    /** 登录页是否开启滑动验证码 */
    val slidingLoginEnabled: Boolean = false,
    /** 注册页是否开启滑动验证码 */
    val slidingRegisterEnabled: Boolean = false,
    /** 找回密码页是否开启滑动验证码 */
    val slidingForgotEnabled: Boolean = false,
    val splashScreenUrl: String = "",
    val appName: String = "扫码机器人",
    val appDescription: String = "让手机变成扫码枪",
    val splashAppName: String = "二维码管理系统",
    val splashAppDescription: String = "专业的二维码管理工具",
    val splashBgColor: String = "#1677ff",
    val splashIconUrl: String = "",
    val homeAppName: String = "扫码机器人",
    val homeAppDescription: String = "让手机变成扫码枪",
    val homeIconUrl: String = "",
    val latestVersion: VersionInfo? = null,
    val messages: List<AppMessage> = emptyList(),
    val unreadCount: Int = 0
) {
    fun toJsonString(): String {
        val json = JSONObject()
        json.put("announcement", announcement)
        json.put("maintenance_mode", maintenanceMode)
        json.put("registration_required", registrationRequired)
        json.put("captcha_enabled", captchaEnabled)
        json.put("captcha_login_enabled", captchaLoginEnabled)
        json.put("captcha_register_enabled", captchaRegisterEnabled)
        json.put("sliding_login_enabled", slidingLoginEnabled)
        json.put("sliding_register_enabled", slidingRegisterEnabled)
        json.put("sliding_forgot_enabled", slidingForgotEnabled)
        json.put("splash_screen_url", splashScreenUrl)
        json.put("app_name", appName)
        json.put("app_description", appDescription)
        json.put("splash_app_name", splashAppName)
        json.put("splash_app_description", splashAppDescription)
        json.put("splash_bg_color", splashBgColor)
        json.put("splash_icon_url", splashIconUrl)
        json.put("home_app_name", homeAppName)
        json.put("home_app_description", homeAppDescription)
        json.put("home_icon_url", homeIconUrl)
        json.put("unread_count", unreadCount)
        latestVersion?.let { json.put("latest_version", it.toJson()) }
        val msgArray = JSONArray()
        messages.forEach { msgArray.put(it.toJson()) }
        json.put("messages", msgArray)
        return json.toString()
    }

    companion object {
        fun fromJsonString(str: String): AppInfo? {
            return try {
                val json = JSONObject(str)
                val legacyCaptcha = json.optBoolean("captcha_enabled", false)
                val loginCap = if (json.has("captcha_login_enabled")) json.optBoolean("captcha_login_enabled", false) else legacyCaptcha
                val regCap = if (json.has("captcha_register_enabled")) json.optBoolean("captcha_register_enabled", false) else legacyCaptcha
                val slidingLogin = json.optBoolean("sliding_login_enabled", false)
                val slidingReg = json.optBoolean("sliding_register_enabled", false)
                val slidingForgot = json.optBoolean("sliding_forgot_enabled", false)
                AppInfo(
                    announcement = json.optString("announcement", ""),
                    maintenanceMode = json.optBoolean("maintenance_mode", false),
                    registrationRequired = json.optBoolean("registration_required", true),
                    captchaEnabled = legacyCaptcha || loginCap || regCap,
                    captchaLoginEnabled = loginCap,
                    captchaRegisterEnabled = regCap,
                    slidingLoginEnabled = slidingLogin,
                    slidingRegisterEnabled = slidingReg,
                    slidingForgotEnabled = slidingForgot,
                    splashScreenUrl = json.optString("splash_screen_url", ""),
                    appName = json.optString("app_name", "扫码机器人"),
                    appDescription = json.optString("app_description", "让手机变成扫码枪"),
                    splashAppName = json.optString("splash_app_name", json.optString("app_name", "二维码管理系统")),
                    splashAppDescription = json.optString("splash_app_description", json.optString("app_description", "专业的二维码管理工具")),
                    splashBgColor = json.optString("splash_bg_color", "#1677ff"),
                    splashIconUrl = json.optString("splash_icon_url", ""),
                    homeAppName = json.optString("home_app_name", json.optString("app_name", "扫码机器人")),
                    homeAppDescription = json.optString("home_app_description", json.optString("app_description", "让手机变成扫码枪")),
                    homeIconUrl = json.optString("home_icon_url", ""),
                    latestVersion = json.optJSONObject("latest_version")?.let { VersionInfo.fromJson(it) },
                    messages = json.optJSONArray("messages")?.let { arr ->
                        (0 until arr.length()).map { AppMessage.fromJson(arr.getJSONObject(it)) }
                    } ?: emptyList(),
                    unreadCount = json.optInt("unread_count", 0)
                )
            } catch (_: Exception) { null }
        }
    }
}

data class VersionInfo(
    val versionCode: Int = 0,
    val versionName: String = "",
    val downloadUrl: String = "",
    val updateContent: String = "",
    val forceUpdate: Boolean = false,
    val fileSize: Long = 0
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("version_code", versionCode)
            put("version_name", versionName)
            put("download_url", downloadUrl)
            put("update_content", updateContent)
            put("force_update", forceUpdate)
            put("file_size", fileSize)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): VersionInfo {
            return VersionInfo(
                versionCode = json.optInt("version_code", 0),
                versionName = json.optString("version_name", ""),
                downloadUrl = json.optString("download_url", ""),
                updateContent = json.optString("update_content", ""),
                forceUpdate = json.optBoolean("force_update", false),
                fileSize = json.optLong("file_size", 0)
            )
        }
    }
}

data class AppMessage(
    val id: Int = 0,
    val title: String = "",
    val content: String = "",
    val type: String = "system",
    val createdAt: String = "",
    val read: Boolean = false
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("content", content)
            put("type", type)
            put("created_at", createdAt)
            put("read", read)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): AppMessage {
            return AppMessage(
                id = json.optInt("id", 0),
                title = json.optString("title", ""),
                content = json.optString("content", ""),
                type = json.optString("type", "system"),
                createdAt = json.optString("created_at", ""),
                read = json.optBoolean("read", false)
            )
        }
    }
}

data class CaptchaResult(
    val captchaId: String = "",
    val captchaImage: String = ""
)

data class UserProfile(
    val id: Int = 0,
    val username: String = "",
    val email: String = "",
    val phone: String = "",
    val avatar: String = "",
    val avatarUrl: String = "",
    val status: Int = 1,
    val source: String = "",
    val loginCount: Int = 0,
    val lastLogin: String = "",
    val createdAt: String = ""
)

val scanModeOptions = listOf(
    ScanModeOption("full", "全屏连扫", "沉浸式扫码，连续效率高，自动保存清晰照片"),
    ScanModeOption("new_full", "新版全屏连扫", "基于全屏，支持扫码列表显示，体验更流畅"),
    ScanModeOption("half", "半屏连扫", "同步列表显示，效率高，自动保存清晰照片"),
    ScanModeOption("wechat", "微信原生扫码", "识别率高，切页体验差，照片模糊风险")
)
