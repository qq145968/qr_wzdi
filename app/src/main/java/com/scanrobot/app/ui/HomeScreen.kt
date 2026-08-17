package com.scanrobot.app.ui

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.IntOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scanrobot.app.BuildConfig
import com.scanrobot.app.data.AppInfo
import com.scanrobot.app.data.AppMessage
import com.scanrobot.app.data.ScanBatch
import com.scanrobot.app.data.ScanModeOption
import com.scanrobot.app.data.ScanSettings
import com.scanrobot.app.data.VersionInfo
import com.scanrobot.app.data.scanModeOptions
import com.scanrobot.app.network.ApiClient
import com.scanrobot.app.ui.theme.*
import com.scanrobot.app.viewmodel.ScanViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(viewModel: ScanViewModel, onLogout: () -> Unit = {}) {
    var activeTab by remember { mutableStateOf("scan") }
    var showModePicker by remember { mutableStateOf(false) }
    var showTypePicker by remember { mutableStateOf(false) }
    var showAlertPicker by remember { mutableStateOf(false) }

    var showMessageDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("scan_robot_prefs", Context.MODE_PRIVATE) }
    var readMessageIds by remember { mutableStateOf<Set<String>>(sharedPrefs.getStringSet("read_message_ids", emptySet()) ?: emptySet()) }
    // 启动时先读缓存，避免硬编码默认值一闪
    var appInfo by remember {
        val cached = sharedPrefs.getString("cached_app_info", null)
        mutableStateOf(cached?.let { AppInfo.fromJsonString(it) })
    }

    LaunchedEffect(Unit) {
        scope.launch {
            val info = withContext(Dispatchers.IO) { ApiClient.getAppInfo() }
            if (info != null) {
                val unreadCount = info.messages.count { it.id.toString() !in readMessageIds }
                val newInfo = info.copy(unreadCount = unreadCount)
                appInfo = newInfo
                // 成功获取后写入缓存，下次启动直接用
                sharedPrefs.edit().putString("cached_app_info", newInfo.toJsonString()).apply()
                val currentCode = BuildConfig.VERSION_CODE
                val latestVersion = info.latestVersion
                if (latestVersion != null && latestVersion.versionCode > currentCode) {
                    showUpdateDialog = true
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLight)
    ) {
        AppHeader(appInfo)

        Box(modifier = Modifier.weight(1f)) {
            when (activeTab) {
                "scan" -> ScanTab(
                    viewModel = viewModel,
                    onModePickerOpen = { showModePicker = true },
                    onTypePickerOpen = { showTypePicker = true },
                    onAlertPickerOpen = { showAlertPicker = true }
                )
                "manage" -> ManageTab(viewModel)
                "profile" -> ProfileTab(
                    viewModel = viewModel,
                    appInfo = appInfo,
                    onMessageClick = { showMessageDialog = true },
                    onLogout = onLogout
                )
            }
        }

        if (!appInfo?.announcement.isNullOrEmpty()) {
            AnnouncementBar(text = appInfo!!.announcement)
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

    if (showMessageDialog && appInfo != null) {
        val messages = appInfo!!.messages.map { msg ->
            msg.copy(read = msg.id.toString() in readMessageIds)
        }
        MessageListDialog(
            messages = messages,
            onDismiss = {
                val newReadIds = (readMessageIds + appInfo!!.messages.map { it.id.toString() }).toSet()
                sharedPrefs.edit().putStringSet("read_message_ids", newReadIds).commit()
                readMessageIds = newReadIds
                appInfo = appInfo!!.copy(unreadCount = 0)
                showMessageDialog = false
            }
        )
    }

    if (showUpdateDialog && appInfo?.latestVersion != null) {
        val version = appInfo!!.latestVersion!!
        VersionUpdateDialog(
            version = version,
            onDismiss = {
                if (!version.forceUpdate) showUpdateDialog = false
            }
        )
    }
}

@Composable
private fun AnnouncementBar(text: String) {
    // 文本实际宽度（px），通过 onTextLayout 获取
    var textWidthPx by remember { mutableStateOf(0f) }

    val transition = rememberInfiniteTransition(label = "announcement")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scroll"
    )

    // 滚动总距离 = 文本宽度 + 间距；进度 0→1 对应偏移 0→-distance
    val gap = if (textWidthPx > 0f) textWidthPx * 0.3f else 0f
    val distance = textWidthPx + gap
    val offsetX = if (distance > 0f) -progress * distance else 0f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFFFF8E1),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "\uD83D\uDCE2",
                fontSize = 14.sp,
                modifier = Modifier.padding(end = 6.dp)
            )
            // 高度由内部文本自动撑开，不做固定height裁剪
            Box(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(4.dp))
            ) {
                // 第一份文本：从原始位置向左滚到 -distance（完全滚出左边）
                Text(
                    text = text,
                    fontSize = 13.sp,
                    color = Color(0xFFE65100),
                    maxLines = 1,
                    softWrap = false,
                    onTextLayout = { result ->
                        textWidthPx = result.size.width.toFloat()
                    },
                    modifier = Modifier
                        .offset { IntOffset(x = offsetX.toInt(), y = 0) }
                        .padding(vertical = 2.dp)
                )
                // 第二份文本：从 distance（右边外）同步向左滚到 0，实现无缝衔接
                if (textWidthPx > 0f) {
                    Text(
                        text = text,
                        fontSize = 13.sp,
                        color = Color(0xFFE65100),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .offset { IntOffset(x = (offsetX + distance).toInt(), y = 0) }
                            .padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppHeader(appInfo: AppInfo? = null) {
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
            Text(appInfo?.homeAppName ?: "扫码机器人", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(appInfo?.homeAppDescription ?: "让手机变成扫码枪", fontSize = 12.sp, color = TextSecondary)
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
                    modifier = Modifier.clickable {
                        viewModel.clearAll()
                        viewModel.showToast("所有数据已清除")
                    }
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
private fun ProfileTab(
    viewModel: ScanViewModel,
    appInfo: AppInfo?,
    onMessageClick: () -> Unit,
    onLogout: () -> Unit
) {
    val batches by viewModel.batches.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("scan_robot_prefs", Context.MODE_PRIVATE)
    val username = sharedPrefs.getString("auth_username", "扫码机器人") ?: "扫码机器人"
    var avatarPath by remember { mutableStateOf(sharedPrefs.getString("user_avatar_path", null)) }

    val scope = rememberCoroutineScope()
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        // 复制到应用私有目录并持久化 path
        scope.launch(Dispatchers.IO) {
            runCatching {
                val dest = File(context.filesDir, "avatars").apply { mkdirs() }
                val target = File(dest, "user_avatar_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri).use { input ->
                    if (input == null) error("无法读取图片")
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                // 删除旧头像（避免残留）
                avatarPath?.let { old -> File(old).takeIf { it.exists() && it.parentFile?.name == "avatars" }?.delete() }
                avatarPath = target.absolutePath
                sharedPrefs.edit().putString("user_avatar_path", avatarPath).apply()
            }.onFailure {
                withContext(Dispatchers.Main) { viewModel.showToast("头像设置失败") }
            }
        }
    }

    val totalCount = batches.sumOf { it.count }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BluePrimary)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { onMessageClick() },
                    contentAlignment = Alignment.Center
                ) {
                    BadgedBox(
                        badge = {
                            if (appInfo != null && appInfo.unreadCount > 0) {
                                Badge { Text(if (appInfo.unreadCount > 99) "99+" else appInfo.unreadCount.toString()) }
                            }
                        }
                    ) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = "消息",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { onLogout() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Logout,
                        contentDescription = "退出登录",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val avPath = avatarPath
                // 加载头像 bitmap（每次切换 path 或首次进入时解析）
                val avatarBitmap = remember(avPath) {
                    if (avPath != null && File(avPath).exists()) {
                        runCatching {
                            BitmapFactory.Options().apply { inJustDecodeBounds = false }
                            BitmapFactory.decodeFile(avPath)
                        }.getOrNull()
                    } else null
                }
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = avatarBitmap.asImageBitmap(),
                            contentDescription = "头像",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = "头像",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "点击更换头像",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.clickable {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(username, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
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

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun MessageListDialog(messages: List<AppMessage>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("消息通知", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                val unread = messages.count { !it.read }
                if (unread > 0) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(DangerRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (unread > 99) "99+" else unread.toString(),
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        text = {
            if (messages.isEmpty()) {
                Text("暂无消息", color = TextSecondary, modifier = Modifier.padding(vertical = 16.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages) { msg ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (msg.read) BgLight else Color(0xFFE8F0FE))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!msg.read) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(DangerRed)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        msg.title,
                                        fontSize = 15.sp,
                                        fontWeight = if (msg.read) FontWeight.Normal else FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                                Text(
                                    msgTypeText(msg.type),
                                    fontSize = 12.sp,
                                    color = if (msg.read) TextSecondary else BluePrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(msg.content, fontSize = 14.sp, color = TextSecondary, lineHeight = 20.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(msg.createdAt, fontSize = 12.sp, color = Color(0xFFBBBBBB))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("全部已读并关闭") }
        }
    )
}

@Composable
private fun VersionUpdateDialog(version: VersionInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloadState by remember { mutableStateOf("idle") }
    var downloadProgress by remember { mutableStateOf(0) }
    var downloadId by remember { mutableStateOf(-1L) }
    var statusMessage by remember { mutableStateOf("") }

    DisposableEffect(downloadState) {
        if (downloadState != "downloading") return@DisposableEffect onDispose {}
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                if (id == downloadId) {
                    val dm = ctx?.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                    val query = DownloadManager.Query().setFilterById(id)
                    val cursor = dm?.query(query)
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                downloadState = "complete"
                                val uri = it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                                statusMessage = "下载完成，正在安装..."
                                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(uri), "application/vnd.android.package-archive")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(installIntent)
                            } else if (status == DownloadManager.STATUS_FAILED) {
                                downloadState = "failed"
                                statusMessage = "下载失败，请重试"
                            }
                        }
                    }
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        onDispose {
            try { context.unregisterReceiver(receiver) } catch (_: Throwable) {}
        }
    }

    LaunchedEffect(downloadState) {
        if (downloadState == "downloading" && downloadId != -1L) {
            while (downloadState == "downloading") {
                delay(500)
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = dm?.query(query)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val downloaded = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        if (total > 0) {
                            downloadProgress = ((downloaded * 100) / total).toInt()
                        }
                    }
                }
            }
        }
    }

    fun startDownload() {
        val url = version.downloadUrl
        if (url.isBlank()) {
            statusMessage = "下载地址无效"
            downloadState = "failed"
            return
        }
        try {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle("扫码机器人 v${version.versionName}")
                setDescription("正在下载更新...")
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "scan-robot-v${version.versionName}.apk")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                setMimeType("application/vnd.android.package-archive")
            }
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            downloadId = dm?.enqueue(request) ?: -1L
            downloadState = "downloading"
            downloadProgress = 0
            statusMessage = ""
        } catch (e: Throwable) {
            statusMessage = "下载失败: ${e.message}"
            downloadState = "failed"
        }
    }

    AlertDialog(
        onDismissRequest = { if (!version.forceUpdate && downloadState != "downloading") onDismiss() },
        title = { Text("发现新版本", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("v${version.versionName}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = BluePrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    version.updateContent.ifEmpty { "优化体验，修复已知问题" },
                    fontSize = 14.sp, color = TextSecondary, lineHeight = 20.sp
                )

                if (downloadState == "downloading") {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = BluePrimary,
                        trackColor = Color(0xFFE0E0E0)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "下载进度: $downloadProgress%",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                if (downloadState == "complete") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(statusMessage, fontSize = 13.sp, color = Color(0xFF4CAF50))
                }

                if (downloadState == "failed") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(statusMessage, fontSize = 13.sp, color = DangerRed)
                }

                if (version.forceUpdate && downloadState == "idle") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("此版本为强制更新，请下载后安装", fontSize = 13.sp, color = DangerRed)
                }
            }
        },
        confirmButton = {
            when (downloadState) {
                "idle" -> {
                    TextButton(onClick = { startDownload() }) {
                        Text("去下载", fontWeight = FontWeight.SemiBold)
                    }
                }
                "downloading" -> {
                    TextButton(onClick = {}, enabled = false) {
                        Text("下载中...")
                    }
                }
                "complete" -> {
                    TextButton(onClick = {
                        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                        val query = DownloadManager.Query().setFilterById(downloadId)
                        val cursor = dm?.query(query)
                        cursor?.use {
                            if (it.moveToFirst()) {
                                val uri = it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(uri), "application/vnd.android.package-archive")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(installIntent)
                            }
                        }
                    }) { Text("安装") }
                }
                "failed" -> {
                    TextButton(onClick = { startDownload() }) { Text("重试") }
                }
            }
        },
        dismissButton = {
            if (!version.forceUpdate && downloadState != "downloading") {
                TextButton(onClick = onDismiss) { Text("稍后") }
            }
        }
    )
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

private fun msgTypeText(type: String): String = when (type) {
    "system" -> "系统"
    "update" -> "更新"
    "activity" -> "活动"
    "custom" -> "通知"
    else -> "通知"
}

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
