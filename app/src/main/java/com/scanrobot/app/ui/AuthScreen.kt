package com.scanrobot.app.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import coil.compose.AsyncImage
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.random.Random
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
    // 弹出式滑动验证码
    var showSlidingDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Any)?>(null) }
    // 登录/注册独立的验证码开关（新版），兜底使用老的 captchaEnabled
    val loginCaptchaEnabled = appInfo?.captchaLoginEnabled ?: appInfo?.captchaEnabled ?: false
    val registerCaptchaEnabled = appInfo?.captchaRegisterEnabled ?: appInfo?.captchaEnabled ?: false
    val captchaEnabled = if (screenMode == "login") loginCaptchaEnabled else registerCaptchaEnabled
    val slidingLoginEnabled = appInfo?.slidingLoginEnabled ?: false
    val slidingRegisterEnabled = appInfo?.slidingRegisterEnabled ?: false
    val slidingForgotEnabled = appInfo?.slidingForgotEnabled ?: slidingLoginEnabled
    val registrationRequired = appInfo?.registrationRequired ?: true

    LaunchedEffect(screenMode) {
        showSlidingDialog = false
        pendingAction = null
    }

    LaunchedEffect(screenMode, loginCaptchaEnabled, registerCaptchaEnabled) {
        val enabledForScreen = when (screenMode) {
            "login" -> loginCaptchaEnabled
            "register" -> registerCaptchaEnabled
            else -> false
        }
        if (enabledForScreen) {
            scope.launch {
                captchaResult = withContext(Dispatchers.IO) { ApiClient.fetchCaptcha() }
            }
        } else {
            captchaResult = null
            captchaCode = ""
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
                        } else if (slidingLoginEnabled) {
                            pendingAction = {
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
                                            val editor = sharedPrefs.edit()
                                            editor.putString("auth_token", result.token)
                                            editor.putString("auth_username", result.username ?: username)
                                            if (result.userId > 0) {
                                                editor.putInt("auth_user_id", result.userId)
                                            }
                                            editor.commit()
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
                            showSlidingDialog = true
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
                                        val editor = sharedPrefs.edit()
                                        editor.putString("auth_token", result.token)
                                        editor.putString("auth_username", result.username ?: username)
                                        if (result.userId > 0) {
                                            editor.putInt("auth_user_id", result.userId)
                                        }
                                        editor.commit()
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
                                val doRegister = {
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
                                if (slidingRegisterEnabled) {
                                    pendingAction = doRegister
                                    showSlidingDialog = true
                                } else {
                                    doRegister()
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
                                val doReset = {
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
                                if (slidingForgotEnabled) {
                                    pendingAction = doReset
                                    showSlidingDialog = true
                                } else {
                                    doReset()
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

    if (showSlidingDialog) {
        SlidingCaptchaDialog(
            onDismiss = {
                showSlidingDialog = false
                pendingAction = null
            },
            onVerify = {
                showSlidingDialog = false
                pendingAction?.invoke()
                pendingAction = null
            }
        )
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

private const val SCENE_COUNT = 6

private fun DrawScope.drawCaptchaScene(sceneIndex: Int) {
    when (sceneIndex % SCENE_COUNT) {
        0 -> drawSunsetScene()
        1 -> drawOceanScene()
        2 -> drawNightSkyScene()
        3 -> drawForestScene()
        4 -> drawMountainLakeScene()
        5 -> drawAuroraScene()
    }
}

private fun DrawScope.drawSunsetScene() {
    drawRect(brush = Brush.verticalGradient(listOf(Color(0xFFFFB347), Color(0xFFE8735A), Color(0xFF8B4D8E))))
    val sunX = size.width * 0.65f
    val sunY = size.height * 0.35f
    drawCircle(color = Color(0xFFFFE082), radius = size.minDimension * 0.18f, center = Offset(sunX, sunY))
    drawCircle(color = Color(0xFFFFCC02), radius = size.minDimension * 0.13f, center = Offset(sunX, sunY))
    val mountainPath = Path().apply {
        moveTo(0f, size.height)
        lineTo(size.width * 0.15f, size.height * 0.55f)
        lineTo(size.width * 0.3f, size.height * 0.7f)
        lineTo(size.width * 0.5f, size.height * 0.4f)
        lineTo(size.width * 0.7f, size.height * 0.65f)
        lineTo(size.width * 0.85f, size.height * 0.5f)
        lineTo(size.width, size.height * 0.6f)
        lineTo(size.width, size.height)
        close()
    }
    drawPath(mountainPath, color = Color(0xFF4A2C5E))
    val mountainPath2 = Path().apply {
        moveTo(0f, size.height)
        lineTo(size.width * 0.2f, size.height * 0.75f)
        lineTo(size.width * 0.45f, size.height * 0.85f)
        lineTo(size.width * 0.65f, size.height * 0.72f)
        lineTo(size.width * 0.9f, size.height * 0.8f)
        lineTo(size.width, size.height * 0.78f)
        lineTo(size.width, size.height)
        close()
    }
    drawPath(mountainPath2, color = Color(0xFF2D1B3D))
}

private fun DrawScope.drawOceanScene() {
    drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF87CEEB), Color(0xFF4A90D9), Color(0xFF1E5C8A))))
    drawCircle(color = Color(0xFFFFE082), radius = size.minDimension * 0.12f, center = Offset(size.width * 0.2f, size.height * 0.2f))
    for (i in 0..4) {
        val y = size.height * (0.6f + i * 0.08f)
        val wavePath = Path().apply {
            moveTo(0f, y)
            cubicTo(size.width * 0.25f, y - 6f, size.width * 0.5f, y + 6f, size.width * 0.75f, y - 3f)
            lineTo(size.width, y)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(wavePath, color = Color.White.copy(alpha = 0.15f - i * 0.02f))
    }
    drawCircle(color = Color.White.copy(alpha = 0.6f), radius = 2f, center = Offset(size.width * 0.3f, size.height * 0.7f))
    drawCircle(color = Color.White.copy(alpha = 0.5f), radius = 1.5f, center = Offset(size.width * 0.6f, size.height * 0.75f))
    drawCircle(color = Color.White.copy(alpha = 0.7f), radius = 2.5f, center = Offset(size.width * 0.8f, size.height * 0.68f))
}

private fun DrawScope.drawNightSkyScene() {
    drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460))))
    val moonX = size.width * 0.75f
    val moonY = size.height * 0.25f
    drawCircle(color = Color(0xFFFFF9C4), radius = size.minDimension * 0.12f, center = Offset(moonX, moonY))
    drawCircle(color = Color(0xFFE8EAF6), radius = size.minDimension * 0.1f, center = Offset(moonX - 4f, moonY - 2f))
    val stars = listOf(
        0.05f to 0.1f, 0.15f to 0.25f, 0.25f to 0.08f, 0.35f to 0.15f,
        0.45f to 0.3f, 0.55f to 0.12f, 0.08f to 0.4f, 0.3f to 0.45f,
        0.5f to 0.5f, 0.65f to 0.35f, 0.85f to 0.45f, 0.92f to 0.2f,
        0.12f to 0.55f, 0.42f to 0.6f, 0.7f to 0.55f, 0.88f to 0.6f
    )
    for ((sx, sy) in stars) {
        drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 1.5f, center = Offset(size.width * sx, size.height * sy))
    }
    for ((sx, sy) in stars.filterIndexed { i, _ -> i % 3 == 0 }) {
        drawCircle(color = Color(0xFFFFE082).copy(alpha = 0.7f), radius = 2f, center = Offset(size.width * sx, size.height * sy))
    }
}

private fun DrawScope.drawForestScene() {
    drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF81C784), Color(0xFF4CAF50), Color(0xFF2E7D32))))
    drawCircle(color = Color(0xFFFFE082).copy(alpha = 0.4f), radius = size.minDimension * 0.15f, center = Offset(size.width * 0.8f, size.height * 0.2f))
    val treePositions = listOf(0.08f to 0.7f, 0.22f to 0.65f, 0.38f to 0.72f, 0.55f to 0.68f, 0.72f to 0.75f, 0.88f to 0.7f)
    for ((tx, ty) in treePositions) {
        val cx = size.width * tx
        val cy = size.height * ty
        drawRect(color = Color(0xFF4E342E), topLeft = Offset(cx - 2f, cy), size = Size(4f, size.height - cy))
        val treePath = Path().apply {
            moveTo(cx, cy - size.height * 0.22f)
            lineTo(cx - size.width * 0.06f, cy)
            lineTo(cx + size.width * 0.06f, cy)
            close()
            moveTo(cx, cy - size.height * 0.15f)
            lineTo(cx - size.width * 0.08f, cy + size.height * 0.02f)
            lineTo(cx + size.width * 0.08f, cy + size.height * 0.02f)
            close()
        }
        drawPath(treePath, color = Color(0xFF1B5E20))
    }
}

private fun DrawScope.drawMountainLakeScene() {
    drawRect(brush = Brush.verticalGradient(listOf(Color(0xFFB3E5FC), Color(0xFF4FC3F7), Color(0xFF0277BD))))
    drawCircle(color = Color(0xFFFFE082).copy(alpha = 0.5f), radius = size.minDimension * 0.1f, center = Offset(size.width * 0.75f, size.height * 0.2f))
    val snowMountain = Path().apply {
        moveTo(0f, size.height * 0.55f)
        lineTo(size.width * 0.2f, size.height * 0.25f)
        lineTo(size.width * 0.3f, size.height * 0.35f)
        lineTo(size.width * 0.5f, size.height * 0.15f)
        lineTo(size.width * 0.65f, size.height * 0.3f)
        lineTo(size.width * 0.85f, size.height * 0.2f)
        lineTo(size.width, size.height * 0.35f)
        lineTo(size.width, size.height * 0.55f)
        close()
    }
    drawPath(snowMountain, color = Color(0xFF90A4AE))
    val snowCap = Path().apply {
        moveTo(size.width * 0.42f, size.height * 0.22f)
        lineTo(size.width * 0.5f, size.height * 0.15f)
        lineTo(size.width * 0.58f, size.height * 0.22f)
        lineTo(size.width * 0.55f, size.height * 0.25f)
        lineTo(size.width * 0.5f, size.height * 0.2f)
        lineTo(size.width * 0.45f, size.height * 0.25f)
        close()
    }
    drawPath(snowCap, color = Color.White)
    drawRect(color = Color(0xFF01579B), topLeft = Offset(0f, size.height * 0.55f), size = Size(size.width, size.height * 0.45f))
    for (i in 0..3) {
        val y = size.height * (0.6f + i * 0.08f)
        val wavePath = Path().apply {
            moveTo(0f, y)
            cubicTo(size.width * 0.3f, y - 4f, size.width * 0.6f, y + 4f, size.width, y)
        }
        drawPath(wavePath, color = Color.White.copy(alpha = 0.2f), style = Stroke(width = 2f))
    }
}

private fun DrawScope.drawAuroraScene() {
    drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF0D1B2A), Color(0xFF1B3A5B), Color(0xFF2D527C))))
    val stars = listOf(0.1f to 0.05f, 0.3f to 0.12f, 0.5f to 0.06f, 0.7f to 0.15f, 0.9f to 0.08f, 0.2f to 0.2f, 0.6f to 0.25f, 0.85f to 0.3f)
    for ((sx, sy) in stars) {
        drawCircle(color = Color.White.copy(alpha = 0.7f), radius = 1.5f, center = Offset(size.width * sx, size.height * sy))
    }
    for (i in 0..3) {
        val auroraPath = Path().apply {
            moveTo(0f, size.height * (0.3f + i * 0.08f))
            cubicTo(
                size.width * 0.3f, size.height * (0.15f + i * 0.05f),
                size.width * 0.6f, size.height * (0.4f + i * 0.08f),
                size.width, size.height * (0.2f + i * 0.06f)
            )
            lineTo(size.width, size.height * (0.4f + i * 0.08f))
            cubicTo(
                size.width * 0.6f, size.height * (0.55f + i * 0.08f),
                size.width * 0.3f, size.height * (0.35f + i * 0.05f),
                0f, size.height * (0.45f + i * 0.08f)
            )
            close()
        }
        val auroraColors = listOf(Color(0xFF00E676), Color(0xFF00B0FF), Color(0xFF7C4DFF), Color(0xFFE040FB))
        drawPath(auroraPath, color = auroraColors[i].copy(alpha = 0.2f))
    }
    val mountainPath = Path().apply {
        moveTo(0f, size.height)
        lineTo(size.width * 0.2f, size.height * 0.7f)
        lineTo(size.width * 0.4f, size.height * 0.78f)
        lineTo(size.width * 0.6f, size.height * 0.68f)
        lineTo(size.width * 0.8f, size.height * 0.75f)
        lineTo(size.width, size.height * 0.72f)
        lineTo(size.width, size.height)
        close()
    }
    drawPath(mountainPath, color = Color(0xFF1A1A2E))
}

@Composable
private fun SlidingCaptchaDialog(onDismiss: () -> Unit, onVerify: () -> Unit) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val dialogWidth = 300.dp
    val padding = 12.dp
    val imageHeight = 130.dp
    val pieceSize = 36.dp
    val sliderHeight = 44.dp
    val sliderHandleSize = 44.dp

    val imageWidthPx = with(density) { (dialogWidth - padding * 2).toPx() }
    val pieceSizePx = with(density) { pieceSize.toPx() }
    val sliderWidthPx = imageWidthPx
    val handleSizePx = with(density) { sliderHandleSize.toPx() }
    val maxSliderOffsetPx = sliderWidthPx - handleSizePx
    val pieceMaxX = imageWidthPx - pieceSizePx
    val tolerancePx = with(density) { 12.dp.toPx() }

    var refreshKey by remember { mutableStateOf(0) }
    var targetX by remember(refreshKey) {
        mutableStateOf(Random.nextFloat() * (pieceMaxX - pieceSizePx - 30f) + pieceSizePx + 30f)
    }
    var sliderX by remember(refreshKey) { mutableStateOf(0f) }
    var verified by remember(refreshKey) { mutableStateOf(false) }

    val sceneIndex = refreshKey
    val pieceY = with(density) { ((imageHeight - pieceSize) / 2).toPx() }
    val pieceX = (sliderX / maxSliderOffsetPx) * pieceMaxX

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.width(dialogWidth),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("安全验证", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("×", fontSize = 20.sp, color = Color(0xFF999999), modifier = Modifier.clickable { onDismiss() })
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight)
                        .padding(horizontal = padding)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCaptchaScene(sceneIndex)

                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.35f),
                            topLeft = Offset(targetX, pieceY),
                            size = Size(pieceSizePx, pieceSizePx),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.6f),
                            topLeft = Offset(targetX, pieceY),
                            size = Size(pieceSizePx, pieceSizePx),
                            cornerRadius = CornerRadius(6f, 6f),
                            style = Stroke(width = 2f)
                        )

                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.75f),
                            topLeft = Offset(pieceX, pieceY),
                            size = Size(pieceSizePx, pieceSizePx),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        drawRoundRect(
                            color = Color(0xFF4A90D9),
                            topLeft = Offset(pieceX, pieceY),
                            size = Size(pieceSizePx, pieceSizePx),
                            cornerRadius = CornerRadius(6f, 6f),
                            style = Stroke(width = 2f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f))
                            .clickable {
                                refreshKey++
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("↻", fontSize = 15.sp, color = BluePrimary, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = padding)
                        .height(sliderHeight)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (verified) Color(0xFFE8F5E9) else Color(0xFFF0F0F0))
                ) {
                    Text(
                        if (verified) "验证成功" else "拖动滑块完成拼图",
                        fontSize = 13.sp,
                        color = if (verified) Color(0xFF4CAF50) else TextSecondary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(if (verified) maxSliderOffsetPx.toInt() else sliderX.toInt(), 0) }
                            .width(sliderHandleSize)
                            .height(sliderHeight)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (verified) Color(0xFF4CAF50) else BluePrimary)
                            .pointerInput(verified, refreshKey) {
                                if (verified) return@pointerInput
                                detectDragGestures(
                                    onDragEnd = {
                                        val currentPieceX = (sliderX / maxSliderOffsetPx) * pieceMaxX
                                        if (abs(currentPieceX - targetX) < tolerancePx) {
                                            verified = true
                                            onVerify()
                                        } else {
                                            sliderX = 0f
                                        }
                                    }
                                ) { change, dragAmount ->
                                    change.consume()
                                    sliderX = (sliderX + dragAmount.x).coerceIn(0f, maxSliderOffsetPx)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("≫", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
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