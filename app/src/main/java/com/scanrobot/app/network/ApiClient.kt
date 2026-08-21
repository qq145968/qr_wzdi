package com.scanrobot.app.network

import com.scanrobot.app.data.AppInfo
import com.scanrobot.app.data.AppMessage
import com.scanrobot.app.data.CaptchaResult
import com.scanrobot.app.data.UserProfile
import com.scanrobot.app.data.VersionInfo
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import android.content.Context
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "https://qr.wzdi.cn/api"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun register(username: String, password: String, email: String, captchaId: String = "", captchaCode: String = ""): ApiResult {
        val json = JSONObject().apply {
            put("username", username)
            put("password", password)
            put("email", email)
            if (captchaId.isNotEmpty()) {
                put("captcha_id", captchaId)
                put("captcha_code", captchaCode)
            }
        }
        return postRequest("/register.php", json)
    }

    fun login(username: String, password: String, captchaId: String = "", captchaCode: String = ""): ApiResult {
        val json = JSONObject().apply {
            put("username", username)
            put("password", password)
            if (captchaId.isNotEmpty()) {
                put("captcha_id", captchaId)
                put("captcha_code", captchaCode)
            }
        }
        return postRequest("/login.php", json)
    }

    fun fetchCaptcha(): CaptchaResult? {
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/captcha.php")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return null
            val json = JSONObject(responseBody)
            if (!json.optBoolean("success", false)) return null
            val data = json.optJSONObject("data") ?: return null
            CaptchaResult(
                captchaId = data.optString("captcha_id", ""),
                captchaImage = data.optString("captcha_image", "")
            )
        } catch (e: Throwable) {
            null
        }
    }

    fun forgotPassword(email: String, username: String): ApiResult {
        val json = JSONObject().apply {
            put("email", email)
            put("username", username)
        }
        return postRequest("/forgot_password.php", json)
    }

    fun resetPassword(token: String, newPassword: String): ApiResult {
        val json = JSONObject().apply {
            put("token", token)
            put("new_password", newPassword)
        }
        return postRequest("/reset_password.php", json)
    }

    fun getAppInfo(context: Context? = null): AppInfo? {
        return try {
            val urlBuilder = okhttp3.HttpUrl.Builder()
                .scheme("https")
                .host("qr.wzdi.cn")
                .addPathSegment("api")
                .addPathSegment("app_info.php")
            // 如已登录，附带 user_id / token 便于后端精准下发 single 类消息（仅发给指定用户的私信/通知）
            if (context != null) {
                val sp = context.getSharedPreferences("scan_robot_prefs", Context.MODE_PRIVATE)
                val uid = sp.getInt("auth_user_id", 0)
                val token = sp.getString("auth_token", null)
                if (uid > 0) urlBuilder.addQueryParameter("user_id", uid.toString())
                if (!token.isNullOrEmpty()) urlBuilder.addQueryParameter("token", token)
            }
            val url = urlBuilder.build().toString()
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return null
            val json = JSONObject(responseBody)
            if (!json.optBoolean("success", false)) return null
            val data = json.optJSONObject("data") ?: return null

            val versionJson = data.optJSONObject("latest_version")
            val versionInfo = if (versionJson != null) {
                VersionInfo(
                    versionCode = versionJson.optInt("version_code", 0),
                    versionName = versionJson.optString("version_name", ""),
                    downloadUrl = versionJson.optString("download_url", ""),
                    updateContent = versionJson.optString("update_content", ""),
                    forceUpdate = versionJson.optBoolean("force_update", false),
                    fileSize = versionJson.optLong("file_size", 0)
                )
            } else null

            val msgArray = data.optJSONArray("messages") ?: org.json.JSONArray()
            val messages = mutableListOf<AppMessage>()
            for (i in 0 until msgArray.length()) {
                val msg = msgArray.getJSONObject(i)
                messages.add(AppMessage(
                    id = msg.optInt("id", 0),
                    title = msg.optString("title", ""),
                    content = msg.optString("content", ""),
                    type = msg.optString("type", "system"),
                    createdAt = msg.optString("created_at", "")
                ))
            }

            val legacyCaptcha = data.optBoolean("captcha_enabled", false)
            val captchaLoginEnabled = if (data.has("captcha_login_enabled")) data.optBoolean("captcha_login_enabled", false) else legacyCaptcha
            val captchaRegisterEnabled = if (data.has("captcha_register_enabled")) data.optBoolean("captcha_register_enabled", false) else legacyCaptcha
            val slidingLoginEnabled = data.optBoolean("sliding_login_enabled", false)
            val slidingRegisterEnabled = data.optBoolean("sliding_register_enabled", false)
            val slidingForgotEnabled = data.optBoolean("sliding_forgot_enabled", false)

            AppInfo(
                announcement = data.optString("announcement", "欢迎使用扫码机器人"),
                maintenanceMode = data.optBoolean("maintenance_mode", false),
                registrationRequired = data.optBoolean("registration_required", true),
                authInvalid = data.optBoolean("auth_invalid", false),
                captchaEnabled = legacyCaptcha || captchaLoginEnabled || captchaRegisterEnabled,
                captchaLoginEnabled = captchaLoginEnabled,
                captchaRegisterEnabled = captchaRegisterEnabled,
                slidingLoginEnabled = slidingLoginEnabled,
                slidingRegisterEnabled = slidingRegisterEnabled,
                slidingForgotEnabled = slidingForgotEnabled,
                splashScreenUrl = data.optString("splash_screen_url", ""),
                appName = data.optString("app_name", "扫码机器人"),
                appDescription = data.optString("app_description", "让手机变成扫码枪"),
                splashAppName = data.optString("splash_app_name", data.optString("app_name", "二维码管理系统")),
                splashAppDescription = data.optString("splash_app_description", data.optString("app_description", "专业的二维码管理工具")),
                splashBgColor = data.optString("splash_bg_color", "#1677ff"),
                splashIconUrl = data.optString("splash_icon_url", ""),
                homeAppName = data.optString("home_app_name", data.optString("app_name", "扫码机器人")),
                homeAppDescription = data.optString("home_app_description", data.optString("app_description", "让手机变成扫码枪")),
                homeIconUrl = data.optString("home_icon_url", ""),
                latestVersion = versionInfo,
                messages = messages,
                unreadCount = data.optInt("unread_count", 0)
            )
        } catch (e: Throwable) {
            null
        }
    }

    fun getUserInfo(context: Context): UserProfile? {
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/../app_user.php")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return null
            val json = JSONObject(responseBody)
            if (!json.optBoolean("ok", false)) return null
            val userJson = json.optJSONObject("user") ?: return null
            UserProfile(
                id = userJson.optInt("id", 0),
                username = userJson.optString("username", ""),
                email = userJson.optString("email", ""),
                phone = userJson.optString("phone", ""),
                avatar = userJson.optString("avatar", ""),
                avatarUrl = userJson.optString("avatar_url", ""),
                status = userJson.optInt("status", 1),
                source = userJson.optString("source", ""),
                loginCount = userJson.optInt("login_count", 0),
                lastLogin = userJson.optString("last_login", ""),
                createdAt = userJson.optString("created_at", "")
            )
        } catch (e: Throwable) {
            null
        }
    }

    fun uploadAvatar(context: Context, avatarBase64: String): Boolean {
        return try {
            val sharedPrefs = context.getSharedPreferences("scan_robot_prefs", android.content.Context.MODE_PRIVATE)
            val userId = sharedPrefs.getInt("auth_user_id", 0)
            if (userId <= 0) return false

            val json = JSONObject().apply {
                put("action", "upload_avatar")
                put("avatar_base64", avatarBase64)
            }
            val result = postRequest("../app_user.php", json)
            result.success
        } catch (e: Throwable) {
            false
        }
    }

    private fun postRequest(path: String, json: JSONObject): ApiResult {
        return try {
            val body = json.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$BASE_URL$path")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            val resultJson = JSONObject(responseBody)
            val success = resultJson.optBoolean("success", false)
            val message = resultJson.optString("message", "")
            val dataObj = resultJson.optJSONObject("data")
            val userObj = resultJson.optJSONObject("user")
            // token / user_id / username 的读取：优先 data.*，兜底 user.*
            fun pickString(key: String): String? {
                val v1 = dataObj?.optString(key, null)?.takeIf { it.isNotEmpty() }
                if (v1 != null) return v1
                val v2 = userObj?.optString(key, null)?.takeIf { it.isNotEmpty() }
                if (v2 != null) return v2
                // 登录接口 user.id / user_id 双兼容
                if (key == "user_id") {
                    val alt = userObj?.optString("id", null)?.takeIf { it.isNotEmpty() }
                    if (alt != null) return alt
                }
                return null
            }
            fun pickInt(key: String): Int {
                val v1 = dataObj?.optInt(key, 0) ?: 0
                if (v1 > 0) return v1
                val v2 = userObj?.optInt(key, 0) ?: 0
                if (v2 > 0) return v2
                if (key == "user_id") {
                    val v3 = userObj?.optInt("id", 0) ?: 0
                    if (v3 > 0) return v3
                }
                return 0
            }
            ApiResult(
                success = success,
                message = message,
                token = pickString("token"),
                userId = pickInt("user_id"),
                username = pickString("username")
            )
        } catch (e: Throwable) {
            ApiResult(success = false, message = "网络错误: ${e.message ?: "未知错误"}")
        }
    }
}

data class ApiResult(
    val success: Boolean,
    val message: String,
    val token: String? = null,
    val userId: Int = 0,
    val username: String? = null
)
