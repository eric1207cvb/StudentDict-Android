package com.yian.studentdict

import androidx.compose.ui.graphics.Color
import java.util.UUID

object AppTheme {
    val Background = Color(0xFFF2F2F7)
    val CardBackground = Color.White
    val Primary = Color.Blue
    val Secondary = Color(0xFFFF9800) // Orange
}

object BopomofoData {
    val initials = listOf("ㄅ", "ㄆ", "ㄇ", "ㄈ", "ㄉ", "ㄊ", "ㄋ", "ㄌ", "ㄍ", "ㄎ", "ㄏ", "ㄐ", "ㄑ", "ㄒ", "ㄓ", "ㄔ", "ㄕ", "ㄖ", "ㄗ", "ㄘ", "ㄙ")
    val medials = listOf("ㄧ", "ㄨ", "ㄩ")
    val finals = listOf("ㄚ", "ㄛ", "ㄜ", "ㄝ", "ㄞ", "ㄟ", "ㄠ", "ㄡ", "ㄢ", "ㄣ", "ㄤ", "ㄥ", "ㄦ")
    val tones = listOf("ˉ", "ˊ", "ˇ", "ˋ", "˙")

    val all: Set<String> = (initials + medials + finals + tones).toSet()

    fun isBopomofo(char: String): Boolean {
        return all.contains(char)
    }
}

// UI 顯示用的資料結構
data class DictItem(
    val id: String = UUID.randomUUID().toString(),
    val word: String,
    val phonetic: String,
    val definition: String,
    val radical: String = "",
    val strokeCount: Int = 0
)

// 用於假資料的 Manager (目前實際上已改用 Room，保留此物件是為了相容舊程式碼不報錯)
object DatabaseManager {
    fun search(keyword: String): List<DictItem> = emptyList()
}