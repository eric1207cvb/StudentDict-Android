package com.yian.studentdict.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_table")
data class HistoryEntity(
    @PrimaryKey val word: String, // 單字本身當作 ID，避免重複
    val timestamp: Long // 用來排序時間
)