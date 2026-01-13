package com.yian.studentdict.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DictDao {
    /**
     * 超級搜尋功能 v7 (智慧注音模糊匹配版)：
     * 1. 解決「ㄔㄨ」搜尋不到一聲字的問題。
     * 2. 使用自定義 REPLACE 邏輯，將搜尋目標與資料庫內的注音都「去聲調化」進行比對。
     * 3. 保持單字優先與原始聲調排序。
     */
    @Query("""
        SELECT * FROM dict_mini 
        WHERE word LIKE :keyword || '%' 
           OR (
               -- 將資料庫中的注音去除所有聲調符號 (ˉˊˇˋ˙) 與空格後進行比對
               REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(phonetic, ' ', ''), '　', ''), 'ˉ', ''), 'ˊ', ''), 'ˇ', ''), 'ˋ', ''), '˙', '') 
               LIKE 
               -- 同時將使用者輸入的關鍵字也去除聲調符號，達到模糊匹配
               REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(:keyword, ' ', ''), '　', ''), 'ˉ', ''), 'ˊ', ''), 'ˇ', ''), 'ˋ', ''), '˙', '') || '%'
           )
        ORDER BY 
           -- 1. 搜尋字詞完全一樣最優先
           CASE WHEN word = :keyword THEN 0 ELSE 1 END ASC,

           -- 2. 單字優先
           CASE WHEN length(word) = 1 THEN 0 ELSE 1 END ASC,

           -- 3. 注音完全符合 (含聲調) 優先於模糊符合
           CASE 
                WHEN REPLACE(REPLACE(phonetic, ' ', ''), '　', '') LIKE :keyword || '%' THEN 0 
                ELSE 1 
           END ASC,
           
           -- 4. 原始聲調排序 (1->2->3->4->5)
           REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(phonetic, '˙', '5'), 'ˋ', '4'), 'ˇ', '3'), 'ˊ', '2'), ' ', '1') ASC,
           
           -- 5. 部首與筆畫
           radical ASC,
           stroke_count ASC
        LIMIT 100
    """)
    suspend fun search(keyword: String): List<DictEntity>

    // 🟢 新增：專門給混合搜尋 (Mixed Search) 使用的函式
    // 作用：撈出所有以該國字 (例如 "老") 開頭的詞，Limit 設大一點 (500)
    // 這樣可以確保 "老師" 即使排在後面，也能被抓出來讓 Kotlin 進行注音比對
    @Query("SELECT * FROM dict_mini WHERE word LIKE :prefix || '%' LIMIT 500")
    suspend fun getCandidates(prefix: String): List<DictEntity>

    @Query("SELECT DISTINCT radical FROM dict_mini WHERE radical IS NOT NULL AND radical != '' ORDER BY stroke_count ASC")
    suspend fun getAllRadicals(): List<String>

    @Query("""
        SELECT * FROM dict_mini 
        WHERE radical = :radical 
        ORDER BY 
          stroke_count ASC, 
          length(word) ASC,
          REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(phonetic, '˙', '5'), 'ˋ', '4'), 'ˇ', '3'), 'ˊ', '2'), ' ', '1') ASC
    """)
    suspend fun getWordsByRadical(radical: String): List<DictEntity>

    // --- 🟢 歷史紀錄功能區塊 ---

    /**
     * 新增或更新歷史紀錄
     * 使用 OnConflictStrategy.REPLACE：如果這個單字已經在歷史紀錄裡，
     * 就會覆蓋舊的資料（這樣 timestamp 就會更新成最新的時間，讓它排到最前面）。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)

    /**
     * 獲取歷史紀錄
     * 依照時間 (timestamp) 由新到舊排序 (DESC)
     * 只取最新的 100 筆
     */
    @Query("SELECT * FROM history_table ORDER BY timestamp DESC LIMIT 100")
    suspend fun getHistory(): List<HistoryEntity>

    /**
     * 刪除單筆歷史紀錄
     */
    @Query("DELETE FROM history_table WHERE word = :word")
    suspend fun deleteHistory(word: String)
}