package com.scanrobot.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.scanrobot.app.ui.AuthScreen
import com.scanrobot.app.ui.DetailScreen
import com.scanrobot.app.ui.HomeScreen
import com.scanrobot.app.ui.ScannerScreen
import com.scanrobot.app.ui.theme.ScanRobotTheme
import com.scanrobot.app.viewmodel.ScanViewModel
import com.scanrobot.app.viewmodel.Screen

class MainActivity : ComponentActivity() {

    private val viewModel: ScanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences("scan_robot_prefs", Context.MODE_PRIVATE)
        val savedToken = sharedPrefs.getString("auth_token", null)
        val savedUsername = sharedPrefs.getString("auth_username", null)

        setContent {
            ScanRobotTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()
                val toastMessage by viewModel.toastMessage.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }
                var isLoggedIn by remember { mutableStateOf(savedToken != null) }

                var lastBackPress by remember { mutableLongStateOf(0L) }

                BackHandler(enabled = true) {
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
                            is Screen.Home -> HomeScreen(viewModel)
                            is Screen.Scanner -> ScannerScreen(viewModel)
                            is Screen.Detail -> DetailScreen(viewModel, screen.batchId)
                        }

                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    } else {
                        AuthScreen {
                            isLoggedIn = true
                        }
                    }
                }
            }
        }
    }
}
