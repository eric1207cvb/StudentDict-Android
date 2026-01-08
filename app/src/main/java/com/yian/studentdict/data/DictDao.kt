package com.yian.studentdict.data

import androidx.room.Dao
import androidx.room.Query

@Dao
interface DictDao {
    /**
     * 超級搜尋功能 v3 (字典聲調排序版)：
     * 1. 精確度優先。
     * 2. 字數少的優先。
     * 3. 【關鍵】聲調排序：透過 REPLACE 把聲調符號換成數字，強制 1->2->3->4->輕聲 排列。
     */
    @Query("""
        SELECT * FROM dict_mini 
        WHERE word LIKE '%' || :keyword || '%' 
           OR REPLACE(REPLACE(phonetic, ' ', ''), '　', '') LIKE '%' || :keyword || '%' 
        ORDER BY 
           -- 1. 精確度：開頭符合的排前面
           CASE WHEN word LIKE :keyword || '%' THEN 0 
                WHEN REPLACE(REPLACE(phonetic, ' ', ''), '　', '') LIKE :keyword || '%' THEN 0 
                ELSE 1 END ASC,
                
           -- 2. 字數：短的排前面 (例如查「一」，「一」要在「一個」前面)
           length(word) ASC, 
           
           -- 3. ★ 字典聲調排序邏輯 ★
           -- 將聲調符號替換為數字以修正 Unicode 排序錯誤 (原本三聲ˇ會排在二聲ˊ前面)
           -- 順序：一聲(1) -> 二聲(2) -> 三聲(3) -> 四聲(4) -> 輕聲(5)
           REPLACE(
             REPLACE(
               REPLACE(
                 REPLACE(
                   REPLACE(phonetic, '˙', '5'), -- 輕聲變 5
                 'ˋ', '4'),                     -- 四聲變 4
               'ˇ', '3'),                       -- 三聲變 3
             'ˊ', '2'),                         -- 二聲變 2
           ' ', '1') ASC,                       -- 空白(一聲)變 1
           
           -- 4. 最後才是筆畫
           stroke_count ASC
        LIMIT 100
    """)
    suspend fun search(keyword: String): List<DictEntity>

    // 抓出所有部首
    @Query("SELECT DISTINCT radical FROM dict_mini WHERE radical IS NOT NULL AND radical != '' ORDER BY stroke_count ASC")
    suspend fun getAllRadicals(): List<String>

    // 部首檢索：也加上聲調排序邏輯
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