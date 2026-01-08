package com.yian.studentdict

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- 米字格 (MiZiGeView) ---
// [Version Update] Reason: Replaced SwiftUI Path with Compose Canvas
@Composable
fun MiZiGeView(char: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(100.dp) // 預設大小
            .background(Color.White)
            .border(3.dp, Color.Red),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

            // 繪製虛線
            drawLine(Color.Red.copy(alpha = 0.4f), Offset(0f, height/2), Offset(width, height/2), pathEffect = pathEffect)
            drawLine(Color.Red.copy(alpha = 0.4f), Offset(width/2, 0f), Offset(width/2, height), pathEffect = pathEffect)
            drawLine(Color.Red.copy(alpha = 0.4f), Offset(0f, 0f), Offset(width, height), pathEffect = pathEffect)
            drawLine(Color.Red.copy(alpha = 0.4f), Offset(width, 0f), Offset(0f, height), pathEffect = pathEffect)
        }
        Text(text = char, fontSize = 60.sp, color = Color.Black)
    }
}

// --- 列表卡片 (WordCardView) ---
@Composable
fun WordCardView(item: DictItem) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 左側單字顯示
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(AppTheme.Primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text(item.word, fontSize = 24.sp, color = AppTheme.Primary, fontWeight = FontWeight.Bold)
                Text(item.phonetic, fontSize = 12.sp, color = AppTheme.Secondary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            // 右側解釋
            Column {
                Text(item.definition, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// --- 注音鍵盤 (ZhuyinKeyboardView) ---
// [Version Update] Reason: Android implementation of custom keyboard
@Composable
fun ZhuyinKeyboard(
    onKeyClick: (String) -> Unit,
    onDelete: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp > 600
    val haptic = LocalHapticFeedback.current

    val bopomofoChars = BopomofoData.initials + BopomofoData.medials + BopomofoData.finals
    val tones = listOf("ˉ", "ˊ", "ˇ", "ˋ", "˙")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isTablet) 420.dp else 320.dp) // 模擬 iOS 高度
            .background(Color(0xFFF2F2F7)) // systemGray6
            .padding(8.dp)
    ) {
        // 1. 聲調列
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            tones.forEach { tone ->
                Button(
                    onClick = {
                        onKeyClick(tone)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    modifier = Modifier.weight(1f).padding(2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(tone, color = Color.Black)
                }
            }
            // 刪除鍵
            Button(
                onClick = {
                    onDelete()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                modifier = Modifier.weight(1f).padding(2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. 注音區
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = if (isTablet) 50.dp else 36.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(bopomofoChars) { char ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1.1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White)
                        .clickable {
                            onKeyClick(char)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // [Version Update] Reason: Color logic logic from iOS
                    val textColor = when {
                        BopomofoData.medials.contains(char) -> Color.Green
                        BopomofoData.finals.contains(char) -> AppTheme.Secondary
                        else -> Color.Black
                    }
                    Text(text = char, fontSize = 18.sp, color = textColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}