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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Person
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
import com.scanrobot.app.data.ScanModeOption
import com.scanrobot.app.data.ScanSettings
import com.scanrobot.app.BuildConfig
import com.scanrobot.app.data.scanModeOptions
import com.scanrobot.app.ui.theme.*
import com.scanrobot.app.viewmodel.ScanViewModel

@Composable
fun HomeScreen(viewModel: ScanViewModel) {
    var activeTab by remember { mutableStateOf("scan") }
    var showModePicker by remember { mutableStateOf(false) }
    var showTypePicker by remember { mutableStateOf(false) }
    var showAlertPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLight)
    ) {
        AppHeader()

        Box(modifier = Modifier.weight(1f)) {
            when (activeTab) {
                "scan" -> ScanTab(
                    viewModel = viewModel,
                    onModePickerOpen = { showModePicker = true },
                    onTypePickerOpen = { showTypePicker = true },
                    onAlertPickerOpen = { showAlertPicker = true }
                )
                "manage" -> ManageTab(viewModel)
                "profile" -> ProfileTab(viewModel)
            }
        }

        BottomNavBar(activeTab) { activeTab = it }
    }

    val settings by viewModel.settings.collectAsState()

    if (showModePicker) {
        ScanModePickerSheet(
            currentMode = settings.scanMode,
            onDismiss = { showModePicker = false },
            onSelect = { value ->
                viewModel.setScanMode(value)
                showModePicker = false
            }
        )
    }

    if (showTypePicker) {
        SimplePickerSheet(
            title = "选择扫码类型",
            options = listOf("条形码+二维码" to "all", "仅条形码" to "barcode", "仅二维码" to "qrcode"),
            currentValue = settings.scanType,
            onDismiss = { showTypePicker = false },
            onSelect = { value ->
                viewModel.setScanType(value)
                showTypePicker = false
            }
        )
    }

    if (showAlertPicker) {
        SimplePickerSheet(
            title = "选择提示方式",
            options = listOf("\"滴\"声" to "sound", "震动" to "vibrate", "无提示" to "none"),
            currentValue = settings.alertType,
            onDismiss = { showAlertPicker = false },
            onSelect = { value ->
                viewModel.setAlertType(value)
                showAlertPicker = false
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
private fun BottomNavBar(active: String, onSwitch: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BgWhite,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavItem("扫码", active == "scan",
                if (active == "scan") Icons.Filled.CenterFocusStrong else Icons.Outlined.CenterFocusStrong
            ) { onSwitch("scan") }
            NavItem("管理", active == "manage",
                if (active == "manage") Icons.Filled.Folder else Icons.Outlined.Folder
            ) { onSwitch("manage") }
            NavItem("个人中心", active == "profile",
                if (active == "profile") Icons.Filled.Person else Icons.Outlined.Person
            ) { onSwitch("profile") }
        }
    }
}

@Composable
private fun NavItem(label: String, active: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) BluePrimary else Color(0xFF999999),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            label,
            fontSize = 11.sp,
            color = if (active) BluePrimary else Color(0xFF999999),
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun ScanTab(
    viewModel: ScanViewModel,
    onModePickerOpen: () -> Unit,
    onTypePickerOpen: () -> Unit,
    onAlertPickerOpen: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BgWhite),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderLight)
        ) {
            Column {
                SettingRowClick("扫码模式", modeText(settings.scanMode)) { onModePickerOpen() }
                SettingRowToggle("允许二维码重复录入", settings.allowDuplicate) {
                    viewModel.toggleDuplicate()
                }
                SettingRowToggle("自动保存扫码照片", settings.autoSavePhoto) {
                    viewModel.togglePhoto()
                }
                SettingRowClick("扫码类型", typeText(settings.scanType)) { onTypePickerOpen() }
                SettingRowClick("扫码提示音", alertText(settings.alertType)) { onAlertPickerOpen() }
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
    val batches by viewModel.batches.collectAsState()
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
                        viewModel.showToast("已复制${batches.size}个批次")
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
private fun ProfileTab(viewModel: ScanViewModel) {
    val batches by viewModel.batches.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val totalCount = batches.sumOf { it.count }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BluePrimary)
                .padding(top = 20.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = "头像",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("扫码机器人", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("v${BuildConfig.VERSION_NAME}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgWhite)
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("$totalCount", "累计扫码")
            Divider(color = BorderLight, modifier = Modifier.width(0.5.dp).height(32.dp))
            StatItem("${batches.size}", "扫码批次")
            Divider(color = BorderLight, modifier = Modifier.width(0.5.dp).height(32.dp))
            StatItem(modeText(settings.scanMode), "当前模式")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BgWhite),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderLight)
        ) {
            Column {
                ProfileRow("当前扫码模式", modeText(settings.scanMode))
                ProfileRow("扫码类型", typeText(settings.scanType))
                ProfileRow("提示方式", alertText(settings.alertType))
                ProfileRow("允许重复录入", if (settings.allowDuplicate) "已开启" else "已关闭")
                ProfileRow("自动保存照片", if (settings.autoSavePhoto) "已开启" else "已关闭")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BgWhite),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderLight)
        ) {
            Column {
                ProfileRow("关于应用", "v${BuildConfig.VERSION_NAME}")
                Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color(0xFFF0F0F0))
                ProfileRowClickable("清除所有数据", "点击清除", DangerRed) {
                    viewModel.clearAll()
                    viewModel.showToast("所有数据已清除")
                }
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 15.sp, color = TextPrimary)
        Text(value, fontSize = 14.sp, color = TextSecondary)
    }
}

@Composable
private fun ProfileRowClickable(label: String, value: String, valueColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 15.sp, color = TextPrimary)
        Text(value, fontSize = 14.sp, color = valueColor)
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
    "full" -> "全屏连扫"
    "new_full" -> "新版全屏连扫"
    "wechat" -> "微信原生扫码"
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

// Card-style scan mode picker (matches the reference design)
@Composable
private fun ScanModePickerSheet(
    currentMode: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() }
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = BgWhite
        ) {
            Column(
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "请选择扫码模式",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF5F5F5))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("×", fontSize = 20.sp, color = TextSecondary)
                    }
                }

                scanModeOptions.forEach { option ->
                    ModeCard(
                        option = option,
                        isSelected = currentMode == option.key,
                        onClick = { onSelect(option.key) }
                    )
                    if (option != scanModeOptions.last()) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeCard(option: ScanModeOption, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(BgWhite)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) BluePrimary else Color(0xFFE0E0E0),
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column {
                Text(
                    option.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    option.description,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(0.dp)
                            .background(Color.Transparent)
                    )
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(0.dp))
                            .background(BluePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Simple picker for scan type and alert type
@Composable
private fun SimplePickerSheet(
    title: String,
    options: List<Pair<String, String>>,
    currentValue: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
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
                            .clickable { onSelect(value) }
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, fontSize = 16.sp, color = TextPrimary)
                        if (currentValue == value) {
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
