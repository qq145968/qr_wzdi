package com.scanrobot.app.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scanrobot.app.data.AppInfo
import com.scanrobot.app.data.CaptchaResult
import com.scanrobot.app.network.ApiClient
import com.scanrobot.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AuthScreen(onLoginSuccess: () -> Unit, appInfo: AppInfo? = null) {
    var screenMode by remember { mutableStateOf("login") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var resetToken by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var messageIsError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var captchaResult by remember { mutableStateOf<CaptchaResult?>(null) }
    var captchaCode by remember { mutableStateOf("") }
    val captchaEnabled = appInfo?.captchaEnabled ?: false
    val registrationRequired = appInfo?.registrationRequired ?: true

    LaunchedEffect(screenMode, captchaEnabled) {
        if (captchaEnabled && (screenMode == "login" || screenMode == "register")) {
            scope.launch {
                captchaResult = withContext(Dispatchers.IO) { ApiClient.fetchCaptcha() }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLight)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(parseHexColor(appInfo?.splashBgColor ?: "#1677ff"))
                .padding(top = 60.dp, bottom = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    val iconUrl = appInfo?.splashIconUrl
                    if (!iconUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = iconUrl,
                            contentDescription = "应用图标",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text("SC", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(appInfo?.splashAppName ?: "二维码管理系统", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(appInfo?.splashAppDescription ?: "专业的二维码管理工具", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
            }
        }

        Column(modifier = Modifier.padding(24.dp)) {
            when (screenMode) {
                "login" -> LoginContent(
                    username = username, onUsernameChange = { username = it },
                    password = password, onPasswordChange = { password = it },
                    showPassword = showPassword, onTogglePassword = { showPassword = !showPassword },
                    isLoading = isLoading,
                    captchaEnabled = captchaEnabled,
                    captchaResult = captchaResult,
                    captchaCode = captchaCode,
                    onCaptchaCodeChange = { captchaCode = it },
                    onRefreshCaptcha = {
                        scope.launch {
                            captchaResult = withContext(Dispatchers.IO) { ApiClient.fetchCaptcha() }
                        }
                    },
                    onLogin = {
                        if (username.isBlank() || password.isBlank()) {
                            message = "请输入用户名和密码"; messageIsError = true
                        } else if (captchaEnabled && captchaCode.isBlank()) {
                            message = "请输入验证码"; messageIsError = true
                        } else {
                            scope.launch {
                                isLoading = true
                                message = ""
                                try {
                                    Log.d("AuthScreen", "Starting login for: $username")
                                    val result = withContext(Dispatchers.IO) {
                                        ApiClient.login(
                                            username, password,
                                            captchaResult?.captchaId ?: "",
                                            captchaCode
                                        )
                                    }
                                    Log.d("AuthScreen", "Login result: success=${result.success}, msg=${result.message}")
                                    if (result.success && !result.token.isNullOrEmpty()) {
                                        val sharedPrefs = context.getSharedPreferences("scan_robot_prefs", Context.MODE_PRIVATE)
                                        sharedPrefs.edit()
                                            .putString("auth_token", result.token)
                                            .putString("auth_username", result.username ?: username)
                                            .commit()
                                        message = "登录成功"; messageIsError = false
                                        onLoginSuccess()
                                    } else {
                                        message = result.message; messageIsError = true
                                        if (captchaEnabled) {
                                            captchaCode = ""
                                            captchaResult = withContext(Dispatchers.IO) { ApiClient.fetchCaptcha() }
                                        }
                                    }
                                } catch (e: Throwable) {
                                    Log.e("AuthScreen", "Login exception", e)
                                    message = "登录失败: ${e.message ?: "未知错误"}"; messageIsError = true
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    },
                    onForgotPassword = { screenMode = "forgot"; message = "" },
                    onSwitchToRegister = {
                        if (registrationRequired) {
                            screenMode = "register"; message = ""; captchaCode = ""
                        } else {
                            message = "当前已关闭注册"; messageIsError = true
                        }
                    },
                    registrationRequired = registrationRequired
                )
                "register" -> RegisterContent(
                    username = username, onUsernameChange = { username = it },
                    password = password, onPasswordChange = { password = it },
                    confirmPassword = confirmPassword, onConfirmPasswordChange = { confirmPassword = it },
                    email = email, onEmailChange = { email = it },
                    showPassword = showPassword, onTogglePassword = { showPassword = !showPassword },
                    isLoading = isLoading,
                    captchaEnabled = captchaEnabled,
                    captchaResult = captchaResult,
                    captchaCode = captchaCode,
                    onCaptchaCodeChange = { captchaCode = it },
                    onRefreshCaptcha = {
                        scope.launch {
                            captchaResult = withContext(Dispatchers.IO) { ApiClient.fetchCaptcha() }
                        }
                    },
                    onRegister = {
                        when {
                            username.length < 3 -> { message = "用户名至少3个字符"; messageIsError = true }
                            password.length < 6 -> { message = "密码至少6位"; messageIsError = true }
                            password != confirmPassword -> { message = "两次密码不一致"; messageIsError = true }
                            email.isBlank() || !email.contains("@") -> { message = "请输入有效邮箱"; messageIsError = true }
                            captchaEnabled && captchaCode.isBlank() -> { message = "请输入验证码"; messageIsError = true }
                            else -> {
                                scope.launch {
                                    isLoading = true
                                    message = ""
                                    try {
                                        Log.d("AuthScreen", "Starting register for: $username")
                                        val result = withContext(Dispatchers.IO) {
                                            ApiClient.register(
                                                username, password, email,
                                                captchaResult?.captchaId ?: "",
                                                captchaCode
                                            )
                                        }
                                        Log.d("AuthScreen", "Register result: success=${result.success}")
                                        if (result.success) {
                                            message = "注册成功，请登录"; messageIsError = false
                                            screenMode = "login"
                                            password = ""; confirmPassword = ""; captchaCode = ""
                                        } else {
                                            message = result.message; messageIsError = true
                                            if (captchaEnabled) {
                                                captchaCode = ""
                                                captchaResult = withContext(Dispatchers.IO) { ApiClient.fetchCaptcha() }
                                            }
                                        }
                                    } catch (e: Throwable) {
                                        Log.e("AuthScreen", "Register exception", e)
                                        message = "注册失败: ${e.message ?: "未知错误"}"; messageIsError = true
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        }
                    },
                    onSwitchToLogin = { screenMode = "login"; message = ""; captchaCode = "" }
                )
                "forgot" -> ForgotPasswordContent(
                    username = username, onUsernameChange = { username = it },
                    email = email, onEmailChange = { email = it },
                    resetToken = resetToken, onResetTokenChange = { resetToken = it },
                    newPassword = newPassword, onNewPasswordChange = { newPassword = it },
                    isLoading = isLoading,
                    onSendCode = {
                        if (email.isBlank()) {
                            message = "请输入邮箱"; messageIsError = true
                        } else {
                            scope.launch {
                                isLoading = true; message = ""
                                try {
                                    Log.d("AuthScreen", "Starting forgotPassword for: $email")
                                    val result = withContext(Dispatchers.IO) {
                                        ApiClient.forgotPassword(email, username)
                                    }
                                    Log.d("AuthScreen", "ForgotPassword result: success=${result.success}")
                                    message = result.message
                                    messageIsError = !result.success
                                } catch (e: Throwable) {
                                    Log.e("AuthScreen", "ForgotPassword exception", e)
                                    message = "发送失败: ${e.message ?: "未知错误"}"; messageIsError = true
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    },
                    onResetPassword = {
                        when {
                            resetToken.isBlank() -> { message = "请输入重置码"; messageIsError = true }
                            newPassword.length < 6 -> { message = "新密码至少6位"; messageIsError = true }
                            else -> {
                                scope.launch {
                                    isLoading = true; message = ""
                                    try {
                                        Log.d("AuthScreen", "Starting resetPassword")
                                        val result = withContext(Dispatchers.IO) {
                                            ApiClient.resetPassword(resetToken, newPassword)
                                        }
                                        Log.d("AuthScreen", "ResetPassword result: success=${result.success}")
                                        if (result.success) {
                                            message = "密码重置成功，请登录"; messageIsError = false
                                            screenMode = "login"
                                            resetToken = ""; newPassword = ""
                                        } else {
                                            message = result.message; messageIsError = true
                                        }
                                    } catch (e: Throwable) {
                                        Log.e("AuthScreen", "ResetPassword exception", e)
                                        message = "重置失败: ${e.message ?: "未知错误"}"; messageIsError = true
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        }
                    },
                    onBack = { screenMode = "login"; message = "" }
                )
            }

            if (message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    message,
                    fontSize = 14.sp,
                    color = if (messageIsError) DangerRed else Color(0xFF4CAF50),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun CaptchaSection(
    captchaResult: CaptchaResult?,
    captchaCode: String,
    onCaptchaCodeChange: (String) -> Unit,
    onRefreshCaptcha: () -> Unit
) {
    Column {
        Text("验证码", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = captchaCode,
                onValueChange = onCaptchaCodeChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BluePrimary,
                    unfocusedBorderColor = BorderLight,
                    focusedContainerColor = BgWhite,
                    unfocusedContainerColor = BgWhite
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (captchaResult != null && captchaResult.captchaImage.isNotEmpty()) {
                val imageStr = captchaResult.captchaImage
                val base64Part = if (imageStr.contains(",")) imageStr.substringAfter(",") else imageStr
                val imageBytes = try {
                    Base64.decode(base64Part, Base64.DEFAULT)
                } catch (e: Throwable) {
                    null
                }
                if (imageBytes != null) {
                    val bitmap = remember(imageBytes) {
                        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    }
                    if (bitmap != null) {
                        Box(
                            modifier = Modifier
                                .size(width = 100.dp, height = 44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BgWhite)
                                .border(0.5.dp, BorderLight, RoundedCornerShape(8.dp))
                                .clickable { onRefreshCaptcha() },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "验证码",
                                modifier = Modifier.fillMaxSize().padding(2.dp)
                            )
                        }
                    } else {
                        TextButton(onClick = onRefreshCaptcha) { Text("刷新", fontSize = 13.sp) }
                    }
                } else {
                    TextButton(onClick = onRefreshCaptcha) { Text("刷新", fontSize = 13.sp) }
                }
            } else {
                TextButton(onClick = onRefreshCaptcha) { Text("加载中", fontSize = 13.sp) }
            }
        }
    }
}

@Composable
private fun LoginContent(
    username: String, onUsernameChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    showPassword: Boolean, onTogglePassword: () -> Unit,
    isLoading: Boolean,
    captchaEnabled: Boolean,
    captchaResult: CaptchaResult?,
    captchaCode: String,
    onCaptchaCodeChange: (String) -> Unit,
    onRefreshCaptcha: () -> Unit,
    onLogin: () -> Unit,
    onForgotPassword: () -> Unit,
    onSwitchToRegister: () -> Unit,
    registrationRequired: Boolean
) {
    Text("登录", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    Spacer(modifier = Modifier.height(24.dp))

    AuthTextField(
        value = username, onValueChange = onUsernameChange,
        label = "用户名", keyboardType = KeyboardType.Text
    )
    Spacer(modifier = Modifier.height(16.dp))

    AuthTextField(
        value = password, onValueChange = onPasswordChange,
        label = "密码", keyboardType = KeyboardType.Password,
        isPassword = !showPassword, onTogglePassword = onTogglePassword
    )

    if (captchaEnabled) {
        Spacer(modifier = Modifier.height(16.dp))
        CaptchaSection(captchaResult, captchaCode, onCaptchaCodeChange, onRefreshCaptcha)
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            "忘记密码？",
            fontSize = 13.sp,
            color = BluePrimary,
            modifier = Modifier.clickable { onForgotPassword() }
        )
    }
    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onLogin,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text("登录", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
    Spacer(modifier = Modifier.height(16.dp))

    if (registrationRequired) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("还没有账号？", fontSize = 14.sp, color = TextSecondary)
            Text(
                "立即注册",
                fontSize = 14.sp,
                color = BluePrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onSwitchToRegister() }
            )
        }
    } else {
        Text(
            "当前已关闭注册，如需账号请联系管理员",
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun RegisterContent(
    username: String, onUsernameChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    confirmPassword: String, onConfirmPasswordChange: (String) -> Unit,
    email: String, onEmailChange: (String) -> Unit,
    showPassword: Boolean, onTogglePassword: () -> Unit,
    isLoading: Boolean,
    captchaEnabled: Boolean,
    captchaResult: CaptchaResult?,
    captchaCode: String,
    onCaptchaCodeChange: (String) -> Unit,
    onRefreshCaptcha: () -> Unit,
    onRegister: () -> Unit,
    onSwitchToLogin: () -> Unit
) {
    Text("注册", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    Spacer(modifier = Modifier.height(24.dp))

    AuthTextField(
        value = username, onValueChange = onUsernameChange,
        label = "用户名（3-20个字符）", keyboardType = KeyboardType.Text
    )
    Spacer(modifier = Modifier.height(16.dp))

    AuthTextField(
        value = email, onValueChange = onEmailChange,
        label = "邮箱（用于找回密码）", keyboardType = KeyboardType.Email
    )
    Spacer(modifier = Modifier.height(16.dp))

    AuthTextField(
        value = password, onValueChange = onPasswordChange,
        label = "密码（至少6位）", keyboardType = KeyboardType.Password,
        isPassword = !showPassword, onTogglePassword = onTogglePassword
    )
    Spacer(modifier = Modifier.height(16.dp))

    AuthTextField(
        value = confirmPassword, onValueChange = onConfirmPasswordChange,
        label = "确认密码", keyboardType = KeyboardType.Password,
        isPassword = !showPassword
    )

    if (captchaEnabled) {
        Spacer(modifier = Modifier.height(16.dp))
        CaptchaSection(captchaResult, captchaCode, onCaptchaCodeChange, onRefreshCaptcha)
    }

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onRegister,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text("注册", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text("已有账号？", fontSize = 14.sp, color = TextSecondary)
        Text(
            "去登录",
            fontSize = 14.sp,
            color = BluePrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { onSwitchToLogin() }
        )
    }
}

@Composable
private fun ForgotPasswordContent(
    username: String, onUsernameChange: (String) -> Unit,
    email: String, onEmailChange: (String) -> Unit,
    resetToken: String, onResetTokenChange: (String) -> Unit,
    newPassword: String, onNewPasswordChange: (String) -> Unit,
    isLoading: Boolean,
    onSendCode: () -> Unit,
    onResetPassword: () -> Unit,
    onBack: () -> Unit
) {
    Text("找回密码", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    Spacer(modifier = Modifier.height(24.dp))

    AuthTextField(
        value = username, onValueChange = onUsernameChange,
        label = "用户名（选填）", keyboardType = KeyboardType.Text
    )
    Spacer(modifier = Modifier.height(16.dp))

    AuthTextField(
        value = email, onValueChange = onEmailChange,
        label = "注册邮箱", keyboardType = KeyboardType.Email
    )
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedButton(
        onClick = onSendCode,
        modifier = Modifier.fillMaxWidth().height(44.dp),
        shape = RoundedCornerShape(10.dp),
        enabled = !isLoading
    ) {
        Text("发送重置码", fontSize = 15.sp)
    }
    Spacer(modifier = Modifier.height(16.dp))

    AuthTextField(
        value = resetToken, onValueChange = onResetTokenChange,
        label = "重置码", keyboardType = KeyboardType.Text
    )
    Spacer(modifier = Modifier.height(16.dp))

    AuthTextField(
        value = newPassword, onValueChange = onNewPasswordChange,
        label = "新密码（至少6位）", keyboardType = KeyboardType.Password,
        isPassword = true
    )
    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onResetPassword,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text("重置密码", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
    Spacer(modifier = Modifier.height(16.dp))

    Text(
        "返回登录",
        fontSize = 14.sp,
        color = BluePrimary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onBack() },
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    onTogglePassword: (() -> Unit)? = null
) {
    Column {
        Text(label, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = if (onTogglePassword != null) {
                {
                    Text(
                        if (isPassword) "显示" else "隐藏",
                        fontSize = 13.sp,
                        color = BluePrimary,
                        modifier = Modifier.clickable { onTogglePassword() }
                    )
                }
            } else null,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BluePrimary,
                unfocusedBorderColor = BorderLight,
                focusedContainerColor = BgWhite,
                unfocusedContainerColor = BgWhite
            )
        )
    }
}

/** hex color string -> Compose Color */
fun parseHexColor(hex: String): Color {
    val cleaned = hex.removePrefix("#")
    return try {
        Color(
            red = cleaned.substring(0, 2).toInt(16) / 255f,
            green = cleaned.substring(2, 4).toInt(16) / 255f,
            blue = cleaned.substring(4, 6).toInt(16) / 255f,
            alpha = if (cleaned.length >= 8) cleaned.substring(6, 8).toInt(16) / 255f else 1f
        )
    } catch (e: Exception) {
        Color(0xFF1677FF)
    }
}