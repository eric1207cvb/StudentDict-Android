package com.yian.studentdict.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DictEntity::class], version = 1, exportSchema = false)
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
                    "student_dict_v7.db" // 👈 幸運的 v7！
                )
                    .createFromAsset("dictionary.sqlite")
                    .fallbackToDestructiveMigration() // 👈 確保這行有開著
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}