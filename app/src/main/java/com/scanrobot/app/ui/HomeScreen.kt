package com.scanrobot.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scanrobot.app.data.ScanBatch
import com.scanrobot.app.data.ScanSettings
import com.scanrobot.app.ui.theme.*
import com.scanrobot.app.viewmodel.ScanViewModel

@Composable
fun HomeScreen(viewModel: ScanViewModel) {
    var activeTab by remember { mutableStateOf("scan") }
    var showPicker by remember { mutableStateOf(false) }
    var pickerType by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLight)
    ) {
        AppHeader()
        TabBar(activeTab) { activeTab = it }

        if (activeTab == "scan") {
            ScanTab(
                viewModel = viewModel,
                onPickerOpen = { type ->
                    pickerType = type
                    showPicker = true
                }
            )
        } else {
            ManageTab(viewModel)
        }
    }

    if (showPicker) {
        PickerSheet(
            type = pickerType,
            settings = viewModel.settings.value,
            onDismiss = { showPicker = false },
            onSelect = { value ->
                when (pickerType) {
                    "mode" -> viewModel.setScanMode(value)
                    "scanType" -> viewModel.setScanType(value)
                    "alert" -> viewModel.setAlertType(value)
                }
                showPicker = false
            }
        )
    }
}

@Composable
private fun AppHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgWhite)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(BluePrimary),
            contentAlignment = Alignment.Center
        ) {
            Text("SC", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text("扫码机器人", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("让手机变成扫码枪", fontSize = 12.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun TabBar(active: String, onSwitch: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgWhite)
            .padding(bottom = 0.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        TabItem("扫码", active == "scan") { onSwitch("scan") }
        Spacer(modifier = Modifier.width(40.dp))
        TabItem("管理", active == "manage") { onSwitch("manage") }
    }
    Divider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 0.5.dp,
        color = BorderLight
    )
}

@Composable
private fun TabItem(label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            fontSize = 15.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            color = if (active) BluePrimary else TextSecondary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(3.dp)
                .background(if (active) BluePrimary else Color.Transparent, RoundedCornerShape(2.dp))
        )
    }
}

@Composable
private fun ScanTab(
    viewModel: ScanViewModel,
    onPickerOpen: (String) -> Unit
) {
    val settings = viewModel.settings.value

    Column(modifier = Modifier.padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BgWhite),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderLight)
        ) {
            Column {
                SettingRowClick("扫码模式", modeText(settings.scanMode)) { onPickerOpen("mode") }
                SettingRowToggle("允许二维码重复录入", settings.allowDuplicate) {
                    viewModel.toggleDuplicate()
                }
                SettingRowToggle("自动保存扫码照片", settings.autoSavePhoto) {
                    viewModel.togglePhoto()
                }
                SettingRowClick("扫码类型", typeText(settings.scanType)) { onPickerOpen("scanType") }
                SettingRowClick("扫码提示音", alertText(settings.alertType)) { onPickerOpen("alert") }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = { viewModel.navigateTo(com.scanrobot.app.viewmodel.Screen.Scanner) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
        ) {
            Text("开始扫码", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SettingRowClick(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 15.sp, color = TextPrimary)
            Spacer(modifier = Modifier.width(4.dp))
            InfoIcon()
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, fontSize = 14.sp, color = TextSecondary)
            Spacer(modifier = Modifier.width(4.dp))
            ChevronRight()
        }
    }
    if (label != "扫码提示音") {
        Divider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 0.5.dp,
            color = Color(0xFFF0F0F0)
        )
    }
}

@Composable
private fun SettingRowToggle(label: String, isOn: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 15.sp, color = TextPrimary)
            Spacer(modifier = Modifier.width(4.dp))
            InfoIcon()
        }
        ToggleSwitch(isOn, onToggle)
    }
    Divider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = Color(0xFFF0F0F0)
    )
}

@Composable
private fun ToggleSwitch(isOn: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .width(44.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isOn) BluePrimary else Color(0xFFE0E0E0))
            .clickable { onToggle() },
        contentAlignment = if (isOn) Alignment.TopEnd else Alignment.TopStart
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .padding(2.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
private fun ManageTab(viewModel: ScanViewModel) {
    val batches = viewModel.batches.value
    val clipboardManager = LocalClipboardManager.current

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("共${batches.size}个扫码批次", fontSize = 14.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            Row {
                Text(
                    "导出历史",
                    fontSize = 14.sp,
                    color = BluePrimary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable {
                        val text = viewModel.exportHistory()
                        clipboardManager.setText(AnnotatedString(text))
                        viewModel.showToast("已复制${viewModel.batches.value.size}个批次")
                    }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    "清除",
                    fontSize = 14.sp,
                    color = DangerRed,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { viewModel.clearAll() }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (batches.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(batches, key = { it.id }) { batch ->
                    BatchItem(batch) {
                        viewModel.navigateTo(com.scanrobot.app.viewmodel.Screen.Detail(batch.id))
                    }
                }
            }
            Text(
                "扫码的现场照片仅保存180天",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun BatchItem(batch: ScanBatch, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BgWhite)
            .border(0.5.dp, BorderLight, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                batch.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(batch.time, fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFE8F0FE))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        "${batch.count}条",
                        fontSize = 12.sp,
                        color = BluePrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        ChevronRight()
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(BorderLight),
            contentAlignment = Alignment.Center
        ) {
            Text("扫", fontSize = 24.sp, color = TextSecondary)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("暂无扫码批次", fontSize = 15.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(6.dp))
        Text("点击\"扫码\"开始扫描", fontSize = 13.sp, color = Color(0xFFBBBBBB))
    }
}

@Composable
private fun InfoIcon() {
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .border(1.dp, Color(0xFFCCCCCC), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("i", fontSize = 9.sp, color = Color(0xFFCCCCCC), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
    }
}

@Composable
private fun ChevronRight() {
    Text(
        "›",
        fontSize = 18.sp,
        color = Color(0xFFCCCCCC)
    )
}

// Helper text functions
private fun modeText(mode: String) = when (mode) {
    "half" -> "半屏连扫"
    "full" -> "全屏单扫"
    else -> "半屏连扫"
}

private fun typeText(type: String) = when (type) {
    "all" -> "条形码+二维码"
    "barcode" -> "仅条形码"
    "qrcode" -> "仅二维码"
    else -> "条形码+二维码"
}

private fun alertText(alert: String) = when (alert) {
    "sound" -> "\"滴\"声"
    "vibrate" -> "震动"
    "none" -> "无提示"
    else -> "\"滴\"声"
}

@Composable
private fun PickerSheet(
    type: String,
    settings: ScanSettings,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val title = when (type) {
        "mode" -> "选择扫码模式"
        "scanType" -> "选择扫码类型"
        "alert" -> "选择提示方式"
        else -> ""
    }

    val options = when (type) {
        "mode" -> listOf("半屏连扫" to "half", "全屏单扫" to "full")
        "scanType" -> listOf("条形码+二维码" to "all", "仅条形码" to "barcode", "仅二维码" to "qrcode")
        "alert" -> listOf("\"滴\"声" to "sound", "震动" to "vibrate", "无提示" to "none")
        else -> emptyList()
    }

    val currentVal = when (type) {
        "mode" -> settings.scanMode
        "scanType" -> settings.scanType
        "alert" -> settings.alertType
        else -> ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() }
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = BgWhite
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("取消", fontSize = 15.sp, color = TextSecondary, modifier = Modifier.clickable { onDismiss() })
                    Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("确定", fontSize = 15.sp, color = BluePrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onDismiss() })
                }
                Divider(thickness = 0.5.dp, color = BorderLight)
                options.forEach { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(value)
                            }
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, fontSize = 16.sp, color = TextPrimary)
                        if (currentVal == value) {
                            Text("✓", fontSize = 18.sp, color = BluePrimary)
                        }
                    }
                    if (label != options.last().first) {
                        Divider(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            thickness = 0.5.dp,
                            color = Color(0xFFF0F0F0)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
