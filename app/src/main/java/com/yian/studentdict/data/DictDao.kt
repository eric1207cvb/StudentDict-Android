package com.yian.studentdict.data

import androidx.room.Dao
import androidx.room.Query

@Dao
interface DictDao {
    /**
     * 超級搜尋功能 v6 (注音嚴格首字匹配版)：
     * 1. 【關鍵修正】搜尋注音時，改為「開頭符合 (Prefix Match)」。
     * 例如：打「ㄨㄛ」，只會出現「我(ㄨㄛ)」，不會再出現「多(ㄉㄨㄛ)」。
     * 2. 依然保持單字優先。
     * 3. 依然保持字典聲調排序。
     */
    @Query("""
        SELECT * FROM dict_mini 
        WHERE word LIKE '%' || :keyword || '%' 
           -- 🔥 修改重點：注音搜尋改為「開頭符合」，拿掉前面的 '%'
           OR REPLACE(REPLACE(phonetic, ' ', ''), '　', '') LIKE :keyword || '%' 
        ORDER BY 
           -- 1. 【絕對優先】：搜尋字詞完全一樣
           CASE WHEN word = :keyword THEN 0 ELSE 1 END ASC,

           -- 2. 【單字優先】：單字排在詞前面
           CASE WHEN length(word) = 1 THEN 0 ELSE 1 END ASC,

           -- 3. 【匹配權重】(因為改成 Prefix Match 了，這裡主要影響 word 的排序)
           CASE 
                WHEN REPLACE(REPLACE(phonetic, ' ', ''), '　', '') LIKE :keyword || '%' THEN 0 
                ELSE 1 
           END ASC,
           
           -- 4. 【聲調排序】：1->2->3->4->5
           REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(phonetic, '˙', '5'), 'ˋ', '4'), 'ˇ', '3'), 'ˊ', '2'), ' ', '1') ASC,
           
           -- 5. 【部首與筆畫】
           radical ASC,
           stroke_count ASC
        LIMIT 100
    """)
    suspend fun search(keyword: String): List<DictEntity>

    // 抓出所有部首
    @Query("SELECT DISTINCT radical FROM dict_mini WHERE radical IS NOT NULL AND radical != '' ORDER BY stroke_count ASC")
    suspend fun getAllRadicals(): List<String>

    // 部首檢索
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