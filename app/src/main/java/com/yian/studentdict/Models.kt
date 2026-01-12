package com.yian.studentdict

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yian.studentdict.data.DictEntity
import java.util.UUID

// --- 1. iOS 深色主題配色 ---
object AppTheme {
    val Background = Color(0xFF000000)
    val KeyboardBackground = Color(0xFF1C1C1E)
    val KeyBackground = Color(0xFF2C2C2E)
    val ToneBackground = Color(0xFF3A2556)
    val Primary = Color(0xFF0A84FF)
    val TextWhite = Color(0xFFE5E5E5)
    val Secondary = Color(0xFFFF9800)
    val CardBackground = Color(0xFF1C1C1E)
}

object KeyboardColors {
    val Consonants = Color(0xFFE5E5E5)
    val Medials = Color(0xFF4CAF50)
    val Finals = Color(0xFFFF9800)

    val ToneText = Color(0xFFD1C4E9)
    val ToneSubText = Color(0xFF9575CD)

    val LegalText = Color(0xFF636366)
}

object BopomofoData {
    val initials = listOf("ㄅ", "ㄆ", "ㄇ", "ㄈ", "ㄉ", "ㄊ", "ㄋ", "ㄌ", "ㄍ", "ㄎ", "ㄏ", "ㄐ", "ㄑ", "ㄒ", "ㄓ", "ㄔ", "ㄕ", "ㄖ", "ㄗ", "ㄘ", "ㄙ")
    val medials = listOf("ㄧ", "ㄨ", "ㄩ")
    val finals = listOf("ㄚ", "ㄛ", "ㄜ", "ㄝ", "ㄞ", "ㄟ", "ㄠ", "ㄡ", "ㄢ", "ㄣ", "ㄤ", "ㄥ", "ㄦ")
    val tones = listOf("ˉ", "ˊ", "ˇ", "ˋ", "˙")
    val all: Set<String> = (initials + medials + finals + tones).toSet()
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

// --- 5. 候選字列 (單字過濾 + 翻頁版) ---
@Composable
fun CandidateBar(
    candidates: List<DictEntity>,
    onCandidateClick: (DictEntity) -> Unit
) {
    // 🔥 1. 強制過濾：只顯示「單字 (長度=1)」，不顯示詞語
    // 使用 remember 避免每次重繪都重新計算
    val singleCharCandidates = remember(candidates) {
        candidates.filter { (it.word?.length ?: 0) == 1 }
    }

    // 🔥 2. 分頁設定
    val pageSize = 8 // 每頁顯示 8 個字 (配合寬度)
    var currentPage by remember { mutableIntStateOf(0) }

    // 當搜尋結果改變時，重置回第一頁
    LaunchedEffect(singleCharCandidates) {
        currentPage = 0
    }

    // 計算總頁數與當前頁面資料
    val totalPages = (singleCharCandidates.size + pageSize - 1) / pageSize
    val safePage = if (totalPages > 0) currentPage.coerceIn(0, totalPages - 1) else 0

    val currentPageItems = if (singleCharCandidates.isNotEmpty()) {
        singleCharCandidates.chunked(pageSize).getOrElse(safePage) { emptyList() }
    } else {
        emptyList()
    }

    if (singleCharCandidates.isNotEmpty()) {
        Column(modifier = Modifier.background(AppTheme.KeyboardBackground)) {
            // 提示文字
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = AppTheme.Secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                // 顯示頁碼提示，例如 (1/5)
                Text(
                    text = "點擊選字 (${safePage + 1}/$totalPages)",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            // 候選字列表容器
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // [上一頁] 按鈕
                IconButton(
                    onClick = { if (safePage > 0) currentPage-- },
                    enabled = safePage > 0, // 第一頁時停用
                    modifier = Modifier.width(32.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowLeft,
                        "Prev",
                        tint = if (safePage > 0) AppTheme.Primary else Color.DarkGray
                    )
                }

                // [候選字] 區域
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    currentPageItems.forEach { entity ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onCandidateClick(entity) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = entity.word ?: "",
                                fontSize = 22.sp,
                                color = AppTheme.TextWhite, // 白色字比較清楚
                                maxLines = 1,
                                softWrap = false,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // 如果這頁沒滿，補上空白格佔位，保持排版不變形
                    if (currentPageItems.size < pageSize) {
                        repeat(pageSize - currentPageItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // [下一頁] 按鈕
                IconButton(
                    onClick = { if (safePage < totalPages - 1) currentPage++ },
                    enabled = safePage < totalPages - 1, // 最後一頁時停用
                    modifier = Modifier.width(32.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        "Next",
                        tint = if (safePage < totalPages - 1) AppTheme.Primary else Color.DarkGray
                    )
                }
            }
        }
    }
}

// --- 6. iOS 風格注音鍵盤 ---
@Composable
fun ZhuyinKeyboard(
    results: List<DictEntity>,
    onKeyClick: (String) -> Unit,
    onDelete: () -> Unit,
    onCandidateSelect: (DictEntity) -> Unit
) {
    val keyHeight = 48.dp
    val spacing = 6.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.KeyboardBackground)
            .navigationBarsPadding()
    ) {
        CandidateBar(candidates = results, onCandidateClick = onCandidateSelect)

        Column(
            modifier = Modifier
                .padding(6.dp)
                .fillMaxWidth()
        ) {
            // 第 1 排：聲調 + 刪除鍵
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                val tones = listOf("ˉ" to "一聲", "ˊ" to "二聲", "ˇ" to "三聲", "ˋ" to "四聲", "˙" to "輕聲")
                tones.forEach { (symbol, label) ->
                    ToneButton(symbol, label, Modifier.weight(1f).height(keyHeight)) { onKeyClick(symbol) }
                }
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .height(keyHeight)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF48484A))
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Backspace, "Backspace", tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(spacing))

            // 第 2 排
            val row2 = listOf("ㄅ", "ㄆ", "ㄇ", "ㄈ", "ㄉ", "ㄊ", "ㄋ", "ㄌ", "ㄍ", "ㄎ")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                row2.forEach { char -> NormalKey(char, KeyboardColors.Consonants, Modifier.weight(1f).height(keyHeight), onKeyClick) }
            }
            Spacer(modifier = Modifier.height(spacing))

            // 第 3 排
            val row3 = listOf("ㄏ", "ㄐ", "ㄑ", "ㄒ", "ㄓ", "ㄔ", "ㄕ", "ㄖ", "ㄗ", "ㄘ")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                row3.forEach { char -> NormalKey(char, KeyboardColors.Consonants, Modifier.weight(1f).height(keyHeight), onKeyClick) }
            }
            Spacer(modifier = Modifier.height(spacing))

            // 第 4 排
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                NormalKey("ㄙ", KeyboardColors.Consonants, Modifier.weight(1f).height(keyHeight), onKeyClick)
                listOf("ㄧ", "ㄨ", "ㄩ").forEach { NormalKey(it, KeyboardColors.Medials, Modifier.weight(1f).height(keyHeight), onKeyClick) }
                listOf("ㄚ", "ㄛ", "ㄜ", "ㄝ", "ㄞ", "ㄟ").forEach { NormalKey(it, KeyboardColors.Finals, Modifier.weight(1f).height(keyHeight), onKeyClick) }
            }
            Spacer(modifier = Modifier.height(spacing))

            // 第 5 排
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                val lastRow = listOf("ㄠ", "ㄡ", "ㄢ", "ㄣ", "ㄤ", "ㄥ", "ㄦ")
                lastRow.forEach { NormalKey(it, KeyboardColors.Finals, Modifier.weight(1f).height(keyHeight), onKeyClick) }
                Spacer(modifier = Modifier.weight(3f))
            }

            Spacer(modifier = Modifier.height(16.dp))
            LegalFooter()
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// --- 元件 ---
@Composable
fun NormalKey(char: String, color: Color, modifier: Modifier, onClick: (String) -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AppTheme.KeyBackground)
            .clickable { onClick(char) },
        contentAlignment = Alignment.Center
    ) {
        Text(text = char, color = color, fontSize = 20.sp, fontWeight = FontWeight.Normal)
    }
}

@Composable
fun ToneButton(symbol: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AppTheme.ToneBackground)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = symbol, color = KeyboardColors.ToneText, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = 2.dp))
            Text(text = label, color = KeyboardColors.ToneSubText, fontSize = 10.sp, lineHeight = 10.sp)
        }
    }
}

@Composable
fun LegalFooter() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val textStyle = SpanStyle(color = KeyboardColors.LegalText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        val annotatedString = buildAnnotatedString {
            pushStringAnnotation(tag = "PRIVACY", annotation = "privacy")
            withStyle(textStyle) { append("隱私權政策") }
            pop()
            withStyle(textStyle) { append("   |   ") }
            pushStringAnnotation(tag = "EULA", annotation = "eula")
            withStyle(textStyle) { append("使用者授權合約 (EULA)") }
            pop()
        }
        Text(text = annotatedString, modifier = Modifier.clickable { })
    }
}