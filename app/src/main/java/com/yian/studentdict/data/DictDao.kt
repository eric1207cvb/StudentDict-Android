package com.yian.studentdict.data

import androidx.room.Dao
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
}