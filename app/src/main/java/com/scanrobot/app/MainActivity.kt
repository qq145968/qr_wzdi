package com.scanrobot.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.scanrobot.app.data.AppInfo
import com.scanrobot.app.network.ApiClient
import com.scanrobot.app.ui.AuthScreen
import com.scanrobot.app.ui.DetailScreen
import com.scanrobot.app.ui.HomeScreen
import com.scanrobot.app.ui.ScannerScreen
import com.scanrobot.app.ui.theme.ScanRobotTheme
import com.scanrobot.app.viewmodel.ScanViewModel
import com.scanrobot.app.viewmodel.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val viewModel: ScanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences("scan_robot_prefs", Context.MODE_PRIVATE)
        val savedToken = sharedPrefs.getString("auth_token", null)

        // Check for crash log
        val crashPrefs = getSharedPreferences("crash_log", Context.MODE_PRIVATE)
        val lastCrash = crashPrefs.getString("last_crash", null)

        setContent {
            ScanRobotTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()
                val toastMessage by viewModel.toastMessage.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }
                var isLoggedIn by remember { mutableStateOf(savedToken != null) }
                var lastBackPress by remember { mutableLongStateOf(0L) }
                var showCrashDialog by remember { mutableStateOf(lastCrash != null) }
                // 启动时先读缓存，避免硬编码默认值一闪
                var authAppInfo by remember {
                    val cached = sharedPrefs.getString("cached_app_info", null)
                    mutableStateOf(cached?.let { AppInfo.fromJsonString(it) })
                }

                LaunchedEffect(Unit, isLoggedIn) {
                    val info = withContext(Dispatchers.IO) { ApiClient.getAppInfo(this@MainActivity) }
                    if (info != null) {
                        authAppInfo = info
                        // 成功获取后写入缓存
                        sharedPrefs.edit().putString("cached_app_info", info.toJsonString()).apply()
                    }
                }

                // Show crash dialog if there was a crash
                if (showCrashDialog && lastCrash != null) {
                    AlertDialog(
                        onDismissRequest = {
                            crashPrefs.edit().clear().commit()
                            showCrashDialog = false
                        },
                        title = { Text("应用崩溃日志") },
                        text = {
                            LazyColumn {
                                items(lastCrash.split("\n")) { line ->
                                    Text(
                                        text = line,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(vertical = 1.dp)
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                crashPrefs.edit().clear().commit()
                                showCrashDialog = false
                            }) {
                                Text("知道了")
                            }
                        }
                    )
                }

                BackHandler(enabled = !isLoggedIn) {
                    val now = System.currentTimeMillis()
                    if (now - lastBackPress < 2000) {
                        finish()
                    } else {
                        lastBackPress = now
                        viewModel.showToast("再按一次退出应用")
                    }
                }

                BackHandler(enabled = isLoggedIn) {
                    when (currentScreen) {
                        is Screen.Scanner, is Screen.Detail -> {
                            viewModel.goBack()
                        }
                        is Screen.Home -> {
                            val now = System.currentTimeMillis()
                            if (now - lastBackPress < 2000) {
                                finish()
                            } else {
                                lastBackPress = now
                                viewModel.showToast("再按一次退出应用")
                            }
                        }
                        else -> {}
                    }
                }

                LaunchedEffect(toastMessage) {
                    val msg = toastMessage
                    if (msg != null) {
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearToast()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (isLoggedIn) {
                        when (val screen = currentScreen) {
                            is Screen.Home -> HomeScreen(
                                viewModel = viewModel,
                                onLogout = {
                                    sharedPrefs.edit()
                                        .remove("auth_token")
                                        .remove("auth_username")
                                        .remove("auth_user_id")
                                        .remove("cached_app_info")
                                        .commit()
                                    isLoggedIn = false
                                }
                            )
                            is Screen.Scanner -> ScannerScreen(viewModel)
                            is Screen.Detail -> DetailScreen(viewModel, screen.batchId)
                        }
                    } else {
                        AuthScreen(
                            onLoginSuccess = { isLoggedIn = true },
                            appInfo = authAppInfo
                        )
                    }

                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}
