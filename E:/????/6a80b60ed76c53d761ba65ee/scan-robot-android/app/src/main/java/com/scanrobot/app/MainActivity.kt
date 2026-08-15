package com.scanrobot.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        setContent {
            ScanRobotTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()
                val toastMessage by viewModel.toastMessage.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(toastMessage) {
                    val msg = toastMessage
                    if (msg != null) {
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearToast()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (val screen = currentScreen) {
                        is Screen.Home -> HomeScreen(viewModel)
                        is Screen.Scanner -> ScannerScreen(viewModel)
                        is Screen.Detail -> DetailScreen(viewModel, screen.batchId)
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
