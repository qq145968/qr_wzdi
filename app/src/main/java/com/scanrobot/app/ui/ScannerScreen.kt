package com.scanrobot.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.scanrobot.app.ui.theme.*
import com.scanrobot.app.viewmodel.ScanListItem
import com.scanrobot.app.viewmodel.ScanViewModel

@Composable
fun ScannerScreen(viewModel: ScanViewModel) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        viewModel.startNewScanSession()
    }

    val settings by viewModel.settings.collectAsState()

    when (settings.scanMode) {
        "full" -> FullScreenScanner(viewModel, hasCameraPermission)
        "new_full" -> NewFullScreenScanner(viewModel, hasCameraPermission)
        "wechat" -> WeChatScanner(viewModel, hasCameraPermission)
        else -> HalfScreenScanner(viewModel, hasCameraPermission)
    }
}

// ==================== 半屏连扫 ====================
@Composable
private fun HalfScreenScanner(viewModel: ScanViewModel, hasCameraPermission: Boolean) {
    val flashOn by viewModel.flashOn.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val scanList by viewModel.scanList.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgWhite)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .background(Color.Black)
        ) {
            if (hasCameraPermission) {
                CameraPreview(
                    flashEnabled = flashOn,
                    scanType = settings.scanType,
                    onBarcodeDetected = { result ->
                        viewModel.handleScanResult(result.code, result.type)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("需要摄像头权限", color = Color.White)
                }
            }

            ScanFrameOverlay()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TopBarButton { viewModel.goBack() }
            }

            if (scanList.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(TealBright.copy(alpha = 0.9f))
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text("已扫 ${scanList.size} 条", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Text(
                    "将二维码对准框内，自动识别",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .background(BgWhite)
        ) {
            ResultHeader(viewModel)
            ScanResultList(viewModel)
            Text(
                "扫码的现场照片仅保存180天",
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==================== 全屏连扫 ====================
@Composable
private fun FullScreenScanner(viewModel: ScanViewModel, hasCameraPermission: Boolean) {
    val flashOn by viewModel.flashOn.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val scanList by viewModel.scanList.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Full-screen camera
        if (hasCameraPermission) {
            CameraPreview(
                flashEnabled = flashOn,
                scanType = settings.scanType,
                onBarcodeDetected = { result ->
                    viewModel.handleScanResult(result.code, result.type)
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("需要摄像头权限", color = Color.White)
            }
        }

        // Green laser line - full width, lower portion
        FullScreenLaserLine()

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: back + flash
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FullScreenIconButton(text = "‹") { viewModel.goBack() }
                FullScreenIconButton(
                    text = if (flashOn) "⚡" else "⚡",
                    bgColor = if (flashOn) Color(0xFFFFD700).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.2f)
                ) { viewModel.toggleFlash() }
            }
            // Right: counter badge
            if (scanList.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF00E676).copy(alpha = 0.9f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("${scanList.size}", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Bottom result bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 48.dp, start = 16.dp, end = 16.dp)
        ) {
            // Latest result or placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (scanList.isNotEmpty()) {
                    val latest = scanList.first()
                    Column {
                        Text(
                            "最新扫码：${latest.code}",
                            color = Color.White,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "已扫 ${scanList.size} 条",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Text(
                        "扫码内容将在此处展示",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (scanList.isNotEmpty()) {
                    Text(
                        "复制",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable {
                                val text = viewModel.copyAll()
                                if (text.isNotEmpty()) {
                                    clipboardManager.setText(AnnotatedString(text))
                                    viewModel.showToast("已复制")
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "导出",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable {
                                val filePath = viewModel.exportCsvToFile()
                                if (filePath.isNotEmpty()) {
                                    viewModel.showToast("已导出到: $filePath")
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

// ==================== 新版全屏连扫 ====================
@Composable
private fun NewFullScreenScanner(viewModel: ScanViewModel, hasCameraPermission: Boolean) {
    val flashOn by viewModel.flashOn.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val scanList by viewModel.scanList.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    // Bottom sheet height state: 0.3f (collapsed) to 0.7f (expanded)
    var sheetHeightFraction by remember { mutableStateOf(0.35f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Full-screen camera
        if (hasCameraPermission) {
            CameraPreview(
                flashEnabled = flashOn,
                scanType = settings.scanType,
                onBarcodeDetected = { result ->
                    viewModel.handleScanResult(result.code, result.type)
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("需要摄像头权限", color = Color.White)
            }
        }

        // Green gradient light strip at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF00E676),
                            Color(0xFF00E676),
                            Color.Transparent
                        )
                    )
                )
        )

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: back + flash
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FullScreenIconButton(text = "‹") { viewModel.goBack() }
                FullScreenIconButton(
                    text = "⚡",
                    bgColor = if (flashOn) Color(0xFFFFD700).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.2f)
                ) { viewModel.toggleFlash() }
            }
            // Right: counter badge
            if (scanList.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF00E676).copy(alpha = 0.9f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("${scanList.size}", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Bottom sheet - draggable scan list
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(sheetHeightFraction)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(Color.White)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            // Snap to nearest state
                            val target = when {
                                sheetHeightFraction < 0.25f -> 0.2f
                                sheetHeightFraction < 0.5f -> 0.35f
                                else -> 0.7f
                            }
                            sheetHeightFraction = target
                        }
                    ) { _, dragAmount ->
                        val screenHeight = this.size.height
                        if (screenHeight > 0) {
                            val delta = -dragAmount.y / screenHeight
                            sheetHeightFraction = (sheetHeightFraction + delta).coerceIn(0.15f, 0.85f)
                        }
                    }
                }
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFCCCCCC))
                )
            }

            // Header: title + copy + export
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("扫码列表", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Row {
                    Text(
                        "复制",
                        fontSize = 14.sp,
                        color = BluePrimary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable {
                            val text = viewModel.copyAll()
                            if (text.isNotEmpty()) {
                                clipboardManager.setText(AnnotatedString(text))
                                viewModel.showToast("已复制")
                            }
                        }.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "导出",
                        fontSize = 14.sp,
                        color = BluePrimary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable {
                            val filePath = viewModel.exportCsvToFile()
                            if (filePath.isNotEmpty()) {
                                viewModel.showToast("已导出到: $filePath")
                            }
                        }.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Divider(thickness = 0.5.dp, color = BorderLight)

            // Scrollable list
            if (scanList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无扫码内容", fontSize = 14.sp, color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(scanList, key = { index, _ -> index }) { index, item ->
                        NewFullScanListItem(item, index, viewModel)
                    }
                }
            }
        }
    }
}

// ==================== 微信原生扫码 ====================
@Composable
private fun WeChatScanner(viewModel: ScanViewModel, hasCameraPermission: Boolean) {
    val flashOn by viewModel.flashOn.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val scanList by viewModel.scanList.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    var showResultDialog by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<ScanListItem?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Full-screen camera
        if (hasCameraPermission) {
            CameraPreview(
                flashEnabled = flashOn,
                scanType = settings.scanType,
                onBarcodeDetected = { result ->
                    viewModel.handleScanResult(result.code, result.type)
                    lastResult = ScanListItem(result.code, result.type,
                        java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()))
                    showResultDialog = true
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("需要摄像头权限", color = Color.White)
            }
        }

        // Semi-transparent overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )

        // WeChat-style scan frame in center
        WeChatScanFrame()

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FullScreenIconButton(text = "‹") { viewModel.goBack() }
            Text(
                "扫一扫",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            if (scanList.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("${scanList.size}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Spacer(modifier = Modifier.size(40.dp))
            }
        }

        // Bottom toolbar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 48.dp, start = 32.dp, end = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album (placeholder)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    viewModel.showToast("相册功能开发中")
                }
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🖼", fontSize = 20.sp)
                }
                Text("相册", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }

            // Flash toggle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { viewModel.toggleFlash() }
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (flashOn) Color(0xFFFFD700).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (flashOn) "⚡" else "⚡", fontSize = 24.sp, color = if (flashOn) Color(0xFFFFD700) else Color.White)
                }
                Text(
                    if (flashOn) "轻触关闭" else "轻触点亮",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Scan list
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    if (scanList.isNotEmpty()) {
                        showResultDialog = false
                    } else {
                        viewModel.showToast("暂无扫码记录")
                    }
                }
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("☰", fontSize = 20.sp, color = Color.White)
                }
                Text("列表", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }

        // Result dialog when QR detected
        if (showResultDialog && lastResult != null) {
            WeChatResultDialog(
                result = lastResult!!,
                totalCount = scanList.size,
                onCopy = {
                    clipboardManager.setText(AnnotatedString(lastResult!!.code))
                    viewModel.showToast("已复制")
                    showResultDialog = false
                },
                onContinue = {
                    showResultDialog = false
                },
                onDismiss = {
                    showResultDialog = false
                }
            )
        }

        // Scan list popup
        if (scanList.isNotEmpty() && !showResultDialog) {
            // Floating mini list indicator
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 110.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable {
                        showResultDialog = true
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    "已扫 ${scanList.size} 条 · 点击查看",
                    color = Color.White,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// ==================== 共用组件 ====================

@Composable
private fun FullScreenIconButton(
    text: String,
    bgColor: Color = Color.White.copy(alpha = 0.2f),
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 18.sp)
    }
}

@Composable
private fun FullScreenLaserLine() {
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .offset(y = (progress * 250).dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF00E676),
                            Color(0xFF00E676),
                            Color.Transparent
                        )
                    )
                )
        )
        // Glow effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF00E676).copy(alpha = 0.3f),
                            Color(0xFF00E676).copy(alpha = 0f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun WeChatScanFrame() {
    val infiniteTransition = rememberInfiniteTransition(label = "wechat_scan")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progress"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(240.dp)
        ) {
            val cornerColor = Color(0xFF07C160) // WeChat green
            val cornerSize = 24.dp
            val cornerWidth = 3.dp

            // Top-left corner
            Box(modifier = Modifier
                .size(cornerSize, cornerSize)
                .border(width = cornerWidth, color = cornerColor, shape = RoundedCornerShape(topStart = 6.dp))
                .align(Alignment.TopStart)
            )
            // Top-right corner
            Box(modifier = Modifier
                .size(cornerSize, cornerSize)
                .border(width = cornerWidth, color = cornerColor, shape = RoundedCornerShape(topEnd = 6.dp))
                .align(Alignment.TopEnd)
            )
            // Bottom-left corner
            Box(modifier = Modifier
                .size(cornerSize, cornerSize)
                .border(width = cornerWidth, color = cornerColor, shape = RoundedCornerShape(bottomStart = 6.dp))
                .align(Alignment.BottomStart)
            )
            // Bottom-right corner
            Box(modifier = Modifier
                .size(cornerSize, cornerSize)
                .border(width = cornerWidth, color = cornerColor, shape = RoundedCornerShape(bottomEnd = 6.dp))
                .align(Alignment.BottomEnd)
            )

            // Animated scan line
            val lineY = progress * 240 - 120
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .offset(y = lineY.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, cornerColor, cornerColor, Color.Transparent)
                        )
                    )
            )
        }
    }
}

@Composable
private fun WeChatResultDialog(
    result: ScanListItem,
    totalCount: Int,
    onCopy: () -> Unit,
    onContinue: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("扫码结果", fontWeight = FontWeight.Bold, color = TextPrimary)
        },
        text = {
            Column {
                Text("内容：", fontSize = 13.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    result.code,
                    fontSize = 16.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (result.type == "qrcode") TealBg else WarningBg)
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            if (result.type == "qrcode") "二维码" else "条形码",
                            fontSize = 10.sp,
                            color = if (result.type == "qrcode") TealAccent else WarningAmber
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(result.time, fontSize = 11.sp, color = TextSecondary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("已扫 $totalCount 条", fontSize = 13.sp, color = TextSecondary)
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onDismiss) { Text("关闭", color = TextSecondary) }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onCopy) { Text("复制", color = BluePrimary, fontWeight = FontWeight.SemiBold) }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onContinue) { Text("继续", color = Color(0xFF07C160), fontWeight = FontWeight.SemiBold) }
            }
        }
    )
}

@Composable
private fun NewFullScanListItem(item: ScanListItem, index: Int, viewModel: ScanViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(BgLight)
                .border(0.5.dp, BorderLight, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            QrThumbnail()
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.code,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (item.type == "qrcode") TealBg else WarningBg)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        if (item.type == "qrcode") "二维码" else "条形码",
                        fontSize = 10.sp,
                        color = if (item.type == "qrcode") TealAccent else WarningAmber
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(item.time, fontSize = 11.sp, color = TextSecondary)
            }
        }
        Column(
            modifier = Modifier
                .clickable { viewModel.deleteScanItem(index) }
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⋮", fontSize = 16.sp, color = TextSecondary)
        }
    }
    Divider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = Color(0xFFF0F0F0)
    )
}

// ==================== 半屏连扫共用组件 ====================

@Composable
private fun ScanFrameOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLine"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(230.dp)
        ) {
            val cornerColor = TealBright
            val cornerSize = 24.dp
            val cornerWidth = 3.dp

            Box(modifier = Modifier
                .size(cornerSize)
                .border(width = cornerWidth, color = cornerColor, shape = RoundedCornerShape(topStart = 4.dp))
                .align(Alignment.TopStart)
            )
            Box(modifier = Modifier
                .size(cornerSize)
                .border(width = cornerWidth, color = cornerColor, shape = RoundedCornerShape(topEnd = 4.dp))
                .align(Alignment.TopEnd)
            )
            Box(modifier = Modifier
                .size(cornerSize)
                .border(width = cornerWidth, color = cornerColor, shape = RoundedCornerShape(bottomStart = 4.dp))
                .align(Alignment.BottomStart)
            )
            Box(modifier = Modifier
                .size(cornerSize)
                .border(width = cornerWidth, color = cornerColor, shape = RoundedCornerShape(bottomEnd = 4.dp))
                .align(Alignment.BottomEnd)
            )

            CanvasScanLine()
        }
    }
}

@Composable
private fun CanvasScanLine() {
    val infiniteTransition = rememberInfiniteTransition(label = "line")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .offset(y = (progress * 230).dp - 115.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, TealBright, Color.Transparent)
                )
            )
    )
}

@Composable
private fun TopBarButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text("‹", color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(start = 0.dp))
    }
}

@Composable
private fun ResultHeader(viewModel: ScanViewModel) {
    val flashOn by viewModel.flashOn.collectAsState()
    val scanList by viewModel.scanList.collectAsState()
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var showClearConfirm by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("扫码列表", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.width(10.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (flashOn) Color(0xFFFFF9E6) else BgLight)
                    .border(0.5.dp, if (flashOn) GoldYellow else BorderLight, RoundedCornerShape(10.dp))
                    .clickable { viewModel.toggleFlash() }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp, 12.dp)
                        .background(if (flashOn) GoldYellow else Color(0xFFCCCCCC))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "闪光灯",
                    fontSize = 11.sp,
                    color = if (flashOn) Color(0xFFD4A000) else TextSecondary,
                    fontWeight = if (flashOn) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
        Row {
            Text(
                "清空",
                fontSize = 14.sp,
                color = Color(0xFFFF3B30),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { showClearConfirm = true }
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                "复制",
                fontSize = 14.sp,
                color = BluePrimary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable {
                    val text = viewModel.copyAll()
                    if (text.isNotEmpty()) {
                        clipboardManager.setText(AnnotatedString(text))
                        viewModel.showToast("已复制")
                    }
                }
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                "导出",
                fontSize = 14.sp,
                color = BluePrimary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable {
                    val filePath = viewModel.exportCsvToFile()
                    if (filePath.isNotEmpty()) {
                        viewModel.showToast("已导出到: $filePath")
                    } else {
                        viewModel.showToast("暂无可导出的数据")
                    }
                }
            )
        }
    }
    Divider(thickness = 0.5.dp, color = BorderLight)

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空扫码列表", fontWeight = FontWeight.SemiBold) },
            text = { Text("确定要清空当前扫码列表中的所有记录吗？此操作无法撤销。") },
            confirmButton = {
                val count = scanList.size
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        viewModel.clearAll()
                        if (count > 0) {
                            viewModel.showToast("已清空 $count 条记录")
                        } else {
                            viewModel.showToast("暂无记录可清空")
                        }
                    }
                ) {
                    Text("确定清空", color = Color(0xFFFF3B30), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun ColumnScope.ScanResultList(viewModel: ScanViewModel) {
    val list by viewModel.scanList.collectAsState()

    if (list.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(30.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("暂无扫码内容", fontSize = 14.sp, color = TextSecondary)
        }
    } else {
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(list, key = { index, _ -> index }) { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(BgLight)
                            .border(0.5.dp, BorderLight, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        QrThumbnail()
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            item.code,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (item.type == "qrcode") TealBg else WarningBg)
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    if (item.type == "qrcode") "二维码" else "条形码",
                                    fontSize = 10.sp,
                                    color = if (item.type == "qrcode") TealAccent else WarningAmber
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(item.time, fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                    Column(
                        modifier = Modifier
                            .clickable { viewModel.deleteScanItem(index) }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("⋮", fontSize = 16.sp, color = TextSecondary)
                    }
                }
                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = Color(0xFFF0F0F0)
                )
            }
        }
    }
}

@Composable
private fun QrThumbnail() {
    Box(
        modifier = Modifier.size(28.dp)
    ) {
        Box(modifier = Modifier.size(7.dp).background(TextPrimary).align(Alignment.TopStart))
        Box(modifier = Modifier.size(7.dp).background(TextPrimary).align(Alignment.TopEnd))
        Box(modifier = Modifier.size(7.dp).background(TextPrimary).align(Alignment.BottomStart))
        Box(modifier = Modifier.size(7.dp).background(TextPrimary).align(Alignment.BottomEnd))
        Box(
            modifier = Modifier
                .size(14.dp)
                .align(Alignment.Center)
                .background(TextPrimary.copy(alpha = 0.3f))
        )
    }
}
