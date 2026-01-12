package com.yian.studentdict

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
// [Fix] 補上 horizontalScroll 的引用
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// [Version Update] 定義教育部標準楷書字體變數
// 請確認 res/font/edukai_std.ttf 檔案存在
val edukaiFont = FontFamily(
    Font(R.font.edukai_std, FontWeight.Normal)
)

// 1. 【列表視圖 - 字典索引版】
// [Version Update] UI優化：
// 1. 單字與注音皆套用教育部標準楷書 (edukaiFont)。
// 2. 改為「上下排列」佈局，解決長單詞(如成語)導致注音被擠壓難以閱讀的問題。
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
            // 左側內容區：改用 Column 將單字與注音上下排列
            Column(modifier = Modifier.weight(1f)) {

                // 1. 上方：單字 (標題)
                Text(
                    text = item.word,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = edukaiFont,
                    color = AppTheme.TextWhite,
                    lineHeight = 30.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 2. 下方：注音 (副標題)
                if (item.phonetic.isNotEmpty()) {
                    Text(
                        text = item.phonetic,
                        fontSize = 18.sp,
                        color = AppTheme.Secondary,
                        fontFamily = edukaiFont,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            // 右側：索引資訊
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.radical.isNotEmpty()) {
                    IndexTag(text = "部首:${item.radical}")
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.DarkGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Divider(color = Color(0xFF2C2C2E), thickness = 1.dp, modifier = Modifier.padding(top = 12.dp))
    }
}

// 輔助元件：索引標籤
@Composable
fun IndexTag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF2C2C2E))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

// [Version Update] 詳情頁面：Ruby Text 排版 (字在對音在上/下對齊) + 水平滑動支援
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordDetailScreen(
    item: DictItem,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("詳細釋義", color = AppTheme.TextWhite) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AppTheme.Primary)
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
                colors = CardDefaults.cardColors(containerColor = AppTheme.CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {

                    // --- [UI 重構] 字音對齊與滑動區 ---
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState()) // 👈 現在這裡不會報錯了
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            // 1. 資料處理
                            val wordChars = item.word.toCharArray().map { it.toString() }
                            val phoneticSounds = item.phonetic.trim().split("\\s+".toRegex())

                            // 2. 迴圈生成
                            wordChars.forEachIndexed { index, char ->
                                val sound = phoneticSounds.getOrElse(index) { "" }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(end = 12.dp)
                                ) {
                                    // 上方：漢字
                                    Text(
                                        text = char,
                                        fontSize = 48.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = edukaiFont,
                                        color = AppTheme.Primary
                                    )

                                    // 下方：注音
                                    if (sound.isNotEmpty()) {
                                        Text(
                                            text = sound,
                                            fontSize = 20.sp,
                                            color = AppTheme.Secondary,
                                            fontFamily = edukaiFont,
                                            fontWeight = FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Divider(color = Color.DarkGray, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- 基本資料區 ---
                    Row(modifier = Modifier.fillMaxWidth()) {
                        InfoBadge(label = "部首", value = item.radical.ifEmpty { "無" })
                        Spacer(modifier = Modifier.width(24.dp))
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

                    val formattedDef = item.definition.replace("n", "\n\n")

                    Text(
                        text = formattedDef,
                        fontSize = 20.sp,
                        fontFamily = edukaiFont,
                        color = AppTheme.TextWhite,
                        lineHeight = 32.sp
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
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 20.sp, color = AppTheme.TextWhite, fontWeight = FontWeight.Medium)
    }
}