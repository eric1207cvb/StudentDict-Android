package com.yian.studentdict.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index // 👈 記得匯入這個
import androidx.room.PrimaryKey

@Entity(
    tableName = "dict_mini",
    // 👇 關鍵修改：補上這兩行索引設定，跟資料庫一模一樣
    indices = [
        Index(name = "idx_phonetic", value = ["phonetic"]),
        Index(name = "idx_word", value = ["word"])
    ]
)
data class DictEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int? = null,

    @ColumnInfo(name = "word")
    val word: String?,

    @ColumnInfo(name = "phonetic")
    val phonetic: String?,

    @ColumnInfo(name = "definition")
    val definition: String?,

    @ColumnInfo(name = "radical")
    val radical: String?,

    @ColumnInfo(name = "stroke_count")
    val strokeCount: Int?
)