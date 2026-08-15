package com.scanrobot.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scanrobot.app.data.ScanBatch
import com.scanrobot.app.ui.theme.*

@Composable
fun DetailScreen(
    viewModel: com.scanrobot.app.viewmodel.ScanViewModel,
    batchId: String
) {
    val batch = remember(batchId) { viewModel.getBatchById(batchId) }
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLight)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgWhite)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "‹",
                fontSize = 20.sp,
                color = BluePrimary,
                modifier = Modifier.clickable { viewModel.goBack() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    batch?.name ?: "批次详情",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${batch?.time ?: ""} · ${batch?.count ?: 0}条",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = BorderLight)

        // Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                "复制全部",
                fontSize = 14.sp,
                color = BluePrimary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable {
                    batch?.let {
                        val text = it.items.joinToString("\n") { item -> item.code }
                        clipboardManager.setText(AnnotatedString(text))
                        viewModel.showToast("已复制${it.count}条")
                    }
                }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                "删除批次",
                fontSize = 14.sp,
                color = DangerRed,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable {
                    viewModel.deleteBatch(batchId)
                    viewModel.goBack()
                }
            )
        }

        // Records list
        if (batch == null || batch.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无记录", fontSize = 14.sp, color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(batch.items, key = { index, _ -> index }) { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(BgWhite)
                            .border(0.5.dp, BorderLight, RoundedCornerShape(10.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(BluePrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${batch.items.size - index}",
                                fontSize = 12.sp,
                                color = BluePrimary,
                                fontWeight = FontWeight.Bold
                            )
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
                        Text(
                            "×",
                            fontSize = 16.sp,
                            color = TextSecondary,
                            modifier = Modifier.clickable {
                                viewModel.deleteRecord(batchId, index)
                            }
                        )
                    }
                }
                item {
                    Text(
                        "扫码的现场照片仅保存180天",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
