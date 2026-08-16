package com.scanrobot.app.network

import android.content.Context
import com.scanrobot.app.data.ScanStore
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
