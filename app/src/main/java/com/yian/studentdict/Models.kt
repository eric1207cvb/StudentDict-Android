package com.yian.studentdict

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yian.studentdict.data.DictEntity
import java.util.UUID

// --- 1. 基礎設定與主題顏色 ---
object AppTheme {
    val Background = Color(0xFFF2F2F7)
    val CardBackground = Color.White
    val Primary = Color.Blue
    val Secondary = Color(0xFFFF9800)
}

object KeyboardColors {
    val Initials = Color(0xFFBBDEFB) // 藍
    val Medials = Color(0xFFC8E6C9)  // 綠
    val Finals = Color(0xFFFFE0B2)   // 橘
    val Tones = Color(0xFFF8BBD0)    // 粉
    val Function = Color(0xFFE0E0E0) // 灰
    val Text = Color(0xFF333333)
}

object BopomofoData {
    val initials = listOf("ㄅ", "ㄆ", "ㄇ", "ㄈ", "ㄉ", "ㄊ", "ㄋ", "ㄌ", "ㄍ", "ㄎ", "ㄏ", "ㄐ", "ㄑ", "ㄒ", "ㄓ", "ㄔ", "ㄕ", "ㄖ", "ㄗ", "ㄘ", "ㄙ")
    val medials = listOf("ㄧ", "ㄨ", "ㄩ")
    val finals = listOf("ㄚ", "ㄛ", "ㄜ", "ㄝ", "ㄞ", "ㄟ", "ㄠ", "ㄡ", "ㄢ", "ㄣ", "ㄤ", "ㄥ", "ㄦ")
    val tones = listOf("ˉ", "ˊ", "ˇ", "ˋ", "˙")
    val all: Set<String> = (initials + medials + finals + tones).toSet()
    fun isBopomofo(char: String): Boolean = all.contains(char)
}

data class DictItem(
    val id: String = UUID.randomUUID().toString(),
    val word: String,
    val phonetic: String,
    val definition: String,
    val radical: String = "",
    val strokeCount: Int = 0
)

object DatabaseManager {
    fun search(keyword: String): List<DictItem> = emptyList()
}

// --- 5. 橫式候選字列 ---
@Composable
fun CandidateBar(
    candidates: List<DictEntity>,
    onCandidateClick: (DictEntity) -> Unit
) {
    val pageSize = 7
    var currentPage by remember { mutableIntStateOf(0) }

    LaunchedEffect(candidates) { currentPage = 0 }

    val totalPages = (candidates.size + pageSize - 1) / pageSize
    val safePage = if (totalPages > 0) currentPage.coerceIn(0, totalPages - 1) else 0
    val currentCandidates = if (candidates.isNotEmpty()) {
        candidates.chunked(pageSize).getOrElse(safePage) { emptyList() }
    } else {
        emptyList()
    }

    if (candidates.isNotEmpty()) {
        Column {
            Divider(color = Color.LightGray, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(Color(0xFFFAFAFA)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (safePage > 0) currentPage-- },
                    enabled = safePage > 0,
                    modifier = Modifier.width(40.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowLeft, "Prev", tint = if (safePage > 0) Color.Black else Color.LightGray)
                }

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    currentCandidates.forEach { entity ->
                        TextButton(
                            onClick = { onCandidateClick(entity) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = entity.word ?: "",
                                fontSize = 22.sp,
                                color = Color.Black,
                                maxLines = 1,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { if (safePage < totalPages - 1) currentPage++ },
                    enabled = safePage < totalPages - 1,
                    modifier = Modifier.width(40.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowRight, "Next", tint = if (safePage < totalPages - 1) Color.Black else Color.LightGray)
                }
            }
            Divider(color = Color.LightGray, thickness = 1.dp)
        }
    }
}

// --- 6. 兒童友善版注音鍵盤 (最終乾淨版) ---
@Composable
fun ZhuyinKeyboard(
    results: List<DictEntity>,
    onKeyClick: (String) -> Unit,
    onDelete: () -> Unit,
    onCandidateSelect: (DictEntity) -> Unit
) {
    val keyHeight = 46.dp
    val rowSpacing = 6.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFECEFF1))
            .navigationBarsPadding() // 避開底部手勢區
    ) {
        CandidateBar(candidates = results, onCandidateClick = onCandidateSelect)

        Column(
            modifier = Modifier
                .padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 8.dp) // 底部空間留一點就好
                .fillMaxWidth()
        ) {
            // 第 0 排：聲調 + 刪除鍵 (向右切齊)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // 左側補白 (3格)
                Spacer(modifier = Modifier.weight(3f))

                // 聲調 (5格)
                listOf("ˉ", "ˊ", "ˇ", "ˋ", "˙").forEach { key ->
                    KeyButton(
                        text = key,
                        modifier = Modifier.weight(1f).height(keyHeight),
                        backgroundColor = KeyboardColors.Tones,
                        fontSize = 24.sp,
                        onClick = { onKeyClick(key) }
                    )
                }

                // 刪除鍵 (2格)
                Button(
                    onClick = onDelete,
                    modifier = Modifier.weight(2f).height(keyHeight),
                    colors = ButtonDefaults.buttonColors(containerColor = KeyboardColors.Function),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Delete", tint = Color.Black)
                }
            }
            Spacer(modifier = Modifier.height(rowSpacing))

            // 第 1 排：聲母
            UniformRow(
                keys = listOf("ㄅ", "ㄆ", "ㄇ", "ㄈ", "ㄉ", "ㄊ", "ㄋ", "ㄌ"),
                bgColor = KeyboardColors.Initials,
                totalSlots = 10,
                keyHeight = keyHeight,
                onKeyClick = onKeyClick
            )
            Spacer(modifier = Modifier.height(rowSpacing))

            // 第 2 排：聲母
            UniformRow(
                keys = listOf("ㄍ", "ㄎ", "ㄏ", "ㄐ", "ㄑ", "ㄒ", "ㄓ", "ㄔ"),
                bgColor = KeyboardColors.Initials,
                totalSlots = 10,
                keyHeight = keyHeight,
                onKeyClick = onKeyClick
            )
            Spacer(modifier = Modifier.height(rowSpacing))

            // 第 3 排：聲母 + 介音
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Spacer(modifier = Modifier.weight(1f))
                listOf("ㄕ", "ㄖ", "ㄗ", "ㄘ", "ㄙ").forEach { key ->
                    KeyButton(key, Modifier.weight(1f).height(keyHeight), KeyboardColors.Initials, onClick = { onKeyClick(key) })
                }
                listOf("ㄧ", "ㄨ", "ㄩ").forEach { key ->
                    KeyButton(key, Modifier.weight(1f).height(keyHeight), KeyboardColors.Medials, onClick = { onKeyClick(key) })
                }
                Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(rowSpacing))

            // 第 4 排：韻母
            UniformRow(
                keys = listOf("ㄚ", "ㄛ", "ㄜ", "ㄝ", "ㄞ", "ㄟ", "ㄠ", "ㄡ"),
                bgColor = KeyboardColors.Finals,
                totalSlots = 10,
                keyHeight = keyHeight,
                onKeyClick = onKeyClick
            )
            Spacer(modifier = Modifier.height(rowSpacing))

            // 第 5 排：韻母結尾 (置中)
            UniformRow(
                keys = listOf("ㄢ", "ㄣ", "ㄤ", "ㄥ", "ㄦ"),
                bgColor = KeyboardColors.Finals,
                totalSlots = 10,
                keyHeight = keyHeight,
                onKeyClick = onKeyClick
            )
        }
    }
}

// --- 輔助元件 ---
@Composable
fun UniformRow(
    keys: List<String>,
    bgColor: Color,
    totalSlots: Int,
    keyHeight: androidx.compose.ui.unit.Dp,
    fontSize: TextUnit = 22.sp,
    onKeyClick: (String) -> Unit
) {
    val keyCount = keys.size
    val spacerWeight = (totalSlots - keyCount) / 2f

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (spacerWeight > 0) Spacer(modifier = Modifier.weight(spacerWeight))
        keys.forEach { key ->
            KeyButton(
                text = key,
                modifier = Modifier.weight(1f).height(keyHeight),
                backgroundColor = bgColor,
                fontSize = fontSize,
                onClick = { onKeyClick(key) }
            )
        }
        if (spacerWeight > 0) Spacer(modifier = Modifier.weight(spacerWeight))
    }
}

@Composable
fun KeyButton(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    fontSize: TextUnit = 22.sp,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(0.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 1.dp)
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            color = KeyboardColors.Text,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.wrapContentSize(Alignment.Center)
        )
    }
}