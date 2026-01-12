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
import androidx.compose.ui.text.withStyle
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

data class DictItem(
    val id: String = UUID.randomUUID().toString(),
    val word: String,
    val phonetic: String,
    val definition: String,
    val radical: String = "",
    val strokeCount: Int = 0
)

/**
 * 🟢 [Fix] 康熙部首順序表 (繁體中文筆畫順序)
 */
object RadicalOrder {
    const val LIST = "一丨丶丿乙亅二亠人儿入八冂冖冫几凵刀力勹匕匚匸十卜卩厂厶又口囗土士夂夊夕大女子宀寸小尢尸屮山巛工己巾干幺广廴廾弋弓彐彡彳心戈戶手支攴文斗斤方无日曰月木欠止歹殳毋比毛氏气水火爪父爻爿片牙牛犬玄玉瓜瓦甘生用田疋疒癶白皮皿目矛矢石示禸禾穴立竹米糸缶网羊羽老而耒耳聿肉臣自至臼舌舛舟艮色艸虍虫血行衣襾見角言谷豆豕豸貝赤走足身車辛辰辵邑酉釆里金長門阜隶隹雨青非面革韋韭音頁風飛食首香馬骨高髟鬥鬯鬲鬼魚鳥鹵鹿麥麻黃黍黑黹黽鼎鼓鼠鼻齊齒龍龜龠"

    val VARIANTS = mapOf(
        "亻" to "人", "𠆢" to "人", "刂" to "刀", "⺈" to "刀",
        "忄" to "心", "⺗" to "心", "㣺" to "心", "扌" to "手",
        "氵" to "水", "氺" to "水", "犭" to "犬", "艹" to "艸",
        "䒑" to "艸", "辶" to "辵", "阝" to "阜", "礻" to "示",
        "衤" to "衣", "月" to "肉", "牜" to "牛", "攵" to "攴",
        "旡" to "无", "巜" to "川", "川" to "巛", "彑" to "彐",
        "旦" to "日", "母" to "毋", "灬" to "火", "王" to "玉"
    )

    fun getIndex(radical: String): Int {
        val canonical = VARIANTS[radical] ?: radical
        val index = LIST.indexOf(canonical)
        return if (index == -1) 999 else index
    }
}

// --- 5. 候選字列 ---
@Composable
fun CandidateBar(
    candidates: List<DictEntity>,
    onCandidateClick: (DictEntity) -> Unit
) {
    val singleCharCandidates = remember(candidates) {
        candidates.filter { (it.word?.length ?: 0) == 1 }
    }
    val pageSize = 8
    var currentPage by remember { mutableIntStateOf(0) }

    LaunchedEffect(singleCharCandidates) { currentPage = 0 }

    val totalPages = (singleCharCandidates.size + pageSize - 1) / pageSize
    val safePage = if (totalPages > 0) currentPage.coerceIn(0, totalPages - 1) else 0
    val currentPageItems = if (singleCharCandidates.isNotEmpty()) {
        singleCharCandidates.chunked(pageSize).getOrElse(safePage) { emptyList() }
    } else {
        emptyList()
    }

    if (singleCharCandidates.isNotEmpty()) {
        Column(modifier = Modifier.background(AppTheme.KeyboardBackground)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.KeyboardArrowRight, null, tint = AppTheme.Secondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "點擊選字 (${safePage + 1}/$totalPages)", color = Color.Gray, fontSize = 12.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (safePage > 0) currentPage-- }, enabled = safePage > 0, modifier = Modifier.width(32.dp)) {
                    Icon(Icons.Default.KeyboardArrowLeft, "Prev", tint = if (safePage > 0) AppTheme.Primary else Color.DarkGray)
                }
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                    currentPageItems.forEach { entity ->
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight().padding(2.dp).clip(RoundedCornerShape(8.dp)).clickable { onCandidateClick(entity) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = entity.word ?: "", fontSize = 22.sp, color = AppTheme.TextWhite)
                        }
                    }
                    if (currentPageItems.size < pageSize) {
                        repeat(pageSize - currentPageItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
                IconButton(onClick = { if (safePage < totalPages - 1) currentPage++ }, enabled = safePage < totalPages - 1, modifier = Modifier.width(32.dp)) {
                    Icon(Icons.Default.KeyboardArrowRight, "Next", tint = if (safePage < totalPages - 1) AppTheme.Primary else Color.DarkGray)
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
    Column(modifier = Modifier.fillMaxWidth().background(AppTheme.KeyboardBackground).navigationBarsPadding()) {
        CandidateBar(candidates = results, onCandidateClick = onCandidateSelect)
        Column(modifier = Modifier.padding(6.dp).fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                val tones = listOf("ˉ" to "一聲", "ˊ" to "二聲", "ˇ" to "三聲", "ˋ" to "四聲", "˙" to "輕聲")
                tones.forEach { (symbol, label) -> ToneButton(symbol, label, Modifier.weight(1f).height(keyHeight)) { onKeyClick(symbol) } }
                Box(modifier = Modifier.weight(1.2f).height(keyHeight).clip(RoundedCornerShape(6.dp)).background(Color(0xFF48484A)).clickable(onClick = onDelete), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Backspace, "Backspace", tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(spacing))
            val row2 = listOf("ㄅ", "ㄆ", "ㄇ", "ㄈ", "ㄉ", "ㄊ", "ㄋ", "ㄌ", "ㄍ", "ㄎ")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                row2.forEach { char -> NormalKey(char, KeyboardColors.Consonants, Modifier.weight(1f).height(keyHeight), onKeyClick) }
            }
            Spacer(modifier = Modifier.height(spacing))
            val row3 = listOf("ㄏ", "ㄐ", "ㄑ", "ㄒ", "ㄓ", "ㄔ", "ㄕ", "ㄖ", "ㄗ", "ㄘ")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                row3.forEach { char -> NormalKey(char, KeyboardColors.Consonants, Modifier.weight(1f).height(keyHeight), onKeyClick) }
            }
            Spacer(modifier = Modifier.height(spacing))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                NormalKey("ㄙ", KeyboardColors.Consonants, Modifier.weight(1f).height(keyHeight), onKeyClick)
                listOf("ㄧ", "ㄨ", "ㄩ").forEach { NormalKey(it, KeyboardColors.Medials, Modifier.weight(1f).height(keyHeight), onKeyClick) }
                listOf("ㄚ", "ㄛ", "ㄜ", "ㄝ", "ㄞ", "ㄟ").forEach { NormalKey(it, KeyboardColors.Finals, Modifier.weight(1f).height(keyHeight), onKeyClick) }
            }
            Spacer(modifier = Modifier.height(spacing))
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

@Composable
fun NormalKey(char: String, color: Color, modifier: Modifier, onClick: (String) -> Unit) {
    Box(modifier = modifier.clip(RoundedCornerShape(6.dp)).background(AppTheme.KeyBackground).clickable { onClick(char) }, contentAlignment = Alignment.Center) {
        Text(text = char, color = color, fontSize = 20.sp)
    }
}

@Composable
fun ToneButton(symbol: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier = modifier.clip(RoundedCornerShape(6.dp)).background(AppTheme.ToneBackground).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = symbol, color = KeyboardColors.ToneText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = label, color = KeyboardColors.ToneSubText, fontSize = 10.sp)
        }
    }
}

@Composable
fun LegalFooter() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        val textStyle = SpanStyle(color = KeyboardColors.LegalText, fontSize = 11.sp)
        val annotatedString = buildAnnotatedString {
            withStyle(textStyle) { append("隱私權政策   |   使用者授權合約 (EULA)") }
        }
        Text(text = annotatedString)
    }
}
