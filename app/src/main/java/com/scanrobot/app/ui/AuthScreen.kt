package com.scanrobot.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scanrobot.app.network.ApiClient
import com.scanrobot.app.network.ApiResult
import com.scanrobot.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AuthScreen(onLoginSuccess: () -> Unit) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLight)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BluePrimary)
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
                    Text("SC", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("扫码机器人", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("让手机变成扫码枪", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
            }
        }

        Column(modifier = Modifier.padding(24.dp)) {
            when (screenMode) {
                "login" -> LoginContent(
                    username = username, onUsernameChange = { username = it },
                    password = password, onPasswordChange = { password = it },
                    showPassword = showPassword, onTogglePassword = { showPassword = !showPassword },
                    isLoading = isLoading,
                    onLogin = {
                        if (username.isBlank() || password.isBlank()) {
                            message = "请输入用户名和密码"
                            messageIsError = true
                        } else {
                            scope.launch {
                                isLoading = true
                                val result = withContext(Dispatchers.IO) {
                                    ApiClient.login(username, password)
                                }
                                isLoading = false
                                if (result.success) {
                                    message = "登录成功"
                                    messageIsError = false
                                    onLoginSuccess()
                                } else {
                                    message = result.message
                                    messageIsError = true
                                }
                            }
                        }
                    },
                    onForgotPassword = { screenMode = "forgot" },
                    onSwitchToRegister = { screenMode = "register"; message = "" }
                )
                "register" -> RegisterContent(
                    username = username, onUsernameChange = { username = it },
                    password = password, onPasswordChange = { password = it },
                    confirmPassword = confirmPassword, onConfirmPasswordChange = { confirmPassword = it },
                    email = email, onEmailChange = { email = it },
                    showPassword = showPassword, onTogglePassword = { showPassword = !showPassword },
                    isLoading = isLoading,
                    onRegister = {
                        when {
                            username.length < 3 -> { message = "用户名至少3个字符"; messageIsError = true }
                            password.length < 6 -> { message = "密码至少6位"; messageIsError = true }
                            password != confirmPassword -> { message = "两次密码不一致"; messageIsError = true }
                            email.isBlank() || !email.contains("@") -> { message = "请输入有效邮箱"; messageIsError = true }
                            else -> {
                                scope.launch {
                                    isLoading = true
                                    val result = withContext(Dispatchers.IO) {
                                        ApiClient.register(username, password, email)
                                    }
                                    isLoading = false
                                    if (result.success) {
                                        message = "注册成功，请登录"
                                        messageIsError = false
                                        screenMode = "login"
                                        password = ""
                                        confirmPassword = ""
                                    } else {
                                        message = result.message
                                        messageIsError = true
                                    }
                                }
                            }
                        }
                    },
                    onSwitchToLogin = { screenMode = "login"; message = "" }
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
                                isLoading = true
                                val result = withContext(Dispatchers.IO) {
                                    ApiClient.forgotPassword(email, username)
                                }
                                isLoading = false
                                message = if (result.success) "重置码已发送" else result.message
                                messageIsError = !result.success
                            }
                        }
                    },
                    onResetPassword = {
                        when {
                            resetToken.isBlank() -> { message = "请输入重置码"; messageIsError = true }
                            newPassword.length < 6 -> { message = "新密码至少6位"; messageIsError = true }
                            else -> {
                                scope.launch {
                                    isLoading = true
                                    val result = withContext(Dispatchers.IO) {
                                        ApiClient.resetPassword(resetToken, newPassword)
                                    }
                                    isLoading = false
                                    if (result.success) {
                                        message = "密码重置成功，请登录"
                                        messageIsError = false
                                        screenMode = "login"
                                        resetToken = ""
                                        newPassword = ""
                                    } else {
                                        message = result.message
                                        messageIsError = true
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
        }
    }
}

@Composable
private fun LoginContent(
    username: String, onUsernameChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    showPassword: Boolean, onTogglePassword: () -> Unit,
    isLoading: Boolean,
    onLogin: () -> Unit,
    onForgotPassword: () -> Unit,
    onSwitchToRegister: () -> Unit
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
}

@Composable
private fun RegisterContent(
    username: String, onUsernameChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    confirmPassword: String, onConfirmPasswordChange: (String) -> Unit,
    email: String, onEmailChange: (String) -> Unit,
    showPassword: Boolean, onTogglePassword: () -> Unit,
    isLoading: Boolean,
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BgWhite)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxSize(),
                textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
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
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BluePrimary,
                    unfocusedBorderColor = BorderLight,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
        }
    }
}
