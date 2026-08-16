package com.scanrobot.app.network

import com.scanrobot.app.data.AppInfo
import com.scanrobot.app.data.AppMessage
import com.scanrobot.app.data.VersionInfo
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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

    fun register(username: String, password: String, email: String): ApiResult {
        val json = JSONObject().apply {
            put("username", username)
            put("password", password)
            put("email", email)
        }
        return postRequest("/register.php", json)
    }

    fun login(username: String, password: String): ApiResult {
        val json = JSONObject().apply {
            put("username", username)
            put("password", password)
        }
        return postRequest("/login.php", json)
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

    fun getAppInfo(): AppInfo? {
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/app_info.php")
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

            AppInfo(
                announcement = data.optString("announcement", "欢迎使用扫码机器人"),
                maintenanceMode = data.optBoolean("maintenance_mode", false),
                registrationRequired = data.optBoolean("registration_required", true),
                appName = data.optString("app_name", "扫码机器人"),
                appDescription = data.optString("app_description", "让手机变成扫码枪"),
                latestVersion = versionInfo,
                messages = messages,
                unreadCount = data.optInt("unread_count", 0)
            )
        } catch (e: Throwable) {
            null
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
            ApiResult(
                success = resultJson.optBoolean("success", false),
                message = resultJson.optString("message", ""),
                token = resultJson.optJSONObject("data")?.optString("token", ""),
                userId = resultJson.optJSONObject("data")?.optInt("user_id", 0) ?: 0,
                username = resultJson.optJSONObject("data")?.optString("username", "")
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
