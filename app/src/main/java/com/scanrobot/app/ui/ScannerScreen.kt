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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.scanrobot.app.ui.theme.*
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

    val flashOn by viewModel.flashOn.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val scanList by viewModel.scanList.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgWhite)
    ) {
        // Top half: camera
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

            // Scan frame overlay
            ScanFrameOverlay()

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TopBarButton {
                    viewModel.goBack()
                }
            }

            // Counter
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

        // Bottom half: result list
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
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

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
            // Corners
            val cornerColor = TealBright
            val cornerSize = 24.dp
            val cornerWidth = 3.dp

            // Top-left
            Box(modifier = Modifier
                .size(cornerSize)
                .border(width = cornerWidth, color = cornerColor, shape = RoundedCornerShape(topStart = 4.dp))
                .align(Alignment.TopStart)
            )
            // Top-right
            Box(modifier = Modifier
                .size(cornerSize)
                .border(width = cornerWidth, color = cornerColor, shape = RoundedCornerShape(topEnd = 4.dp))
                .align(Alignment.TopEnd)
            )
            // Bottom-left
            Box(modifier = Modifier
                .size(cornerSize)
                .border(width = cornerWidth, color = cornerColor, shape = RoundedCornerShape(bottomStart = 4.dp))
                .align(Alignment.BottomStart)
            )
            // Bottom-right
            Box(modifier = Modifier
                .size(cornerSize)
                .border(width = cornerWidth, color = cornerColor, shape = RoundedCornerShape(bottomEnd = 4.dp))
                .align(Alignment.BottomEnd)
            )

            // Scan line
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
            // Flash toggle
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
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        val count = viewModel.clearAll()
                        if (count > 0) {
                            viewModel.showToast("已清空 $count 条记录")
                        } else {
                            viewModel.showToast("暂无记录可清空")
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFFF3B30)
                    )
                ) {
                    Text("确定清空")
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
                        .clickable { /* preview/copy */ }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // QR thumbnail
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
                    // More button
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
        // Simple QR pattern representation
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
