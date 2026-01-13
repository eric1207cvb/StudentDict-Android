package com.yian.studentdict.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// 🟢 修改 1：在 entities 陣列中加入 HistoryEntity::class
// 🟢 修改 2：將 version 從 1 改為 2 (通知系統資料庫結構有變動)
@Database(entities = [DictEntity::class, HistoryEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dictDao(): DictDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "student_dict_v7.db" // 您的資料庫檔案名稱
                )
                    .createFromAsset("dictionary.sqlite") // 您的來源檔案
                    // 🟢 重要：因為 version 升級了，這行會確保舊資料庫被清除重建，避免 App 閃退
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}