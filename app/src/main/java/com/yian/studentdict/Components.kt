package com.yian.studentdict

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 1. 【列表視圖】
@Composable
fun SearchResultRow(
    item: DictItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 單字
            Text(
                text = item.word,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 注音
            Text(
                text = item.phonetic,
                fontSize = 18.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.weight(1f))

            // 箭頭
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.LightGray
            )
        }
        Divider(color = Color(0xFFEEEEEE), thickness = 1.dp, modifier = Modifier.padding(top = 12.dp))
    }
}

// 2. 【詳情頁面】
// 👇 關鍵修改：加入這個註解來消除 Experimental 錯誤
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordDetailScreen(
    item: DictItem,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            // Material3 的 TopAppBar 需要 OptIn
            SmallTopAppBar(
                title = { Text("詳細釋義") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = AppTheme.Background
                )
            )
        },
        containerColor = AppTheme.Background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {

                    // --- 頂部單字與彩色注音區 ---
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        // 超大單字
                        Text(
                            text = item.word,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.Primary
                        )

                        Spacer(modifier = Modifier.width(24.dp))

                        // 彩色注音
                        Column {
                            val chars = item.phonetic.toCharArray().map { it.toString() }
                            Row {
                                chars.forEach { char ->
                                    val color = when {
                                        BopomofoData.initials.contains(char) -> Color(0xFF1976D2)
                                        BopomofoData.medials.contains(char) -> Color(0xFF388E3C)
                                        BopomofoData.finals.contains(char) -> Color(0xFFF57C00)
                                        BopomofoData.tones.contains(char) -> Color(0xFFD32F2F)
                                        else -> Color.Gray
                                    }
                                    Text(
                                        text = char,
                                        fontSize = 24.sp,
                                        color = color,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Divider(color = Color.LightGray, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- 基本資料區 ---
                    Row(modifier = Modifier.fillMaxWidth()) {
                        InfoBadge(label = "部首", value = item.radical.ifEmpty { "無" })
                        Spacer(modifier = Modifier.width(16.dp))
                        InfoBadge(label = "總筆畫", value = "${item.strokeCount}")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- 釋義區 ---
                    Text(
                        text = "解釋：",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.Secondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val formattedDef = item.definition.replace("n", "nn")
                    Text(
                        text = formattedDef,
                        fontSize = 18.sp,
                        color = Color(0xFF333333),
                        lineHeight = 28.sp
                    )
                }
            }
        }
    }
}

@Composable
fun InfoBadge(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Text(text = value, fontSize = 18.sp, color = Color.Black, fontWeight = FontWeight.Medium)
    }
}