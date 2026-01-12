package com.yian.studentdict

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 統一管理教育楷體，增加安全性
val EduKaiFont = try {
    FontFamily(Font(R.font.edukai_std, FontWeight.Normal))
} catch (e: Exception) {
    FontFamily.Default
}

/**
 * 列表搜尋結果列
 */
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.word,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = EduKaiFont,
                    color = AppTheme.TextWhite,
                    lineHeight = 30.sp
                )

                if (item.phonetic.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.phonetic,
                        fontSize = 18.sp,
                        color = AppTheme.Secondary,
                        fontFamily = EduKaiFont,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.radical.isNotEmpty()) {
                    CommonIndexTag(text = "部首:${item.radical}")
                    Spacer(modifier = Modifier.width(4.dp))
                }
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

@Composable
fun CommonIndexTag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF2C2C2E))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, color = Color.Gray, fontSize = 12.sp)
    }
}

/**
 * 詳細釋義頁面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordDetailScreen(
    item: DictItem,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("詳細釋義", color = AppTheme.TextWhite) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AppTheme.Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.Background)
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
                colors = CardDefaults.cardColors(containerColor = AppTheme.CardBackground)
            ) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                    // 字音對齊區
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val wordChars = item.word.toCharArray().map { it.toString() }
                        val phoneticSounds = item.phonetic.trim().split("\\s+".toRegex())

                        wordChars.forEachIndexed { index, char ->
                            val sound = phoneticSounds.getOrElse(index) { "" }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Text(
                                    text = char,
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = EduKaiFont,
                                    color = AppTheme.Primary
                                )
                                if (sound.isNotEmpty()) {
                                    Text(
                                        text = sound,
                                        fontSize = 20.sp,
                                        color = AppTheme.Secondary,
                                        fontFamily = EduKaiFont
                                    )
                                }
                            }
                        }
                    }

                    Divider(color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        DetailBadge(label = "部首", value = item.radical.ifEmpty { "無" })
                        Spacer(modifier = Modifier.width(24.dp))
                        DetailBadge(label = "總筆畫", value = "${item.strokeCount}")
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("解釋：", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppTheme.Secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.definition.replace("n", "\n\n"),
                        fontSize = 20.sp,
                        fontFamily = EduKaiFont,
                        color = AppTheme.TextWhite,
                        lineHeight = 32.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DetailBadge(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Text(text = value, fontSize = 20.sp, color = AppTheme.TextWhite, fontWeight = FontWeight.Medium)
    }
}
