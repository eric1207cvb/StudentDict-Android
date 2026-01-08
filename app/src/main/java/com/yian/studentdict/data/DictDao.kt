package com.yian.studentdict.data

import androidx.room.Dao
import androidx.room.Query

@Dao
interface DictDao {
    // 👇 關鍵修改：加上 OR phonetic LIKE ...，這樣打注音也能搜到！
    @Query("""
        SELECT * FROM dict_mini 
        WHERE word LIKE '%' || :keyword || '%' 
           OR phonetic LIKE '%' || :keyword || '%' 
        LIMIT 50
    """)
    suspend fun search(keyword: String): List<DictEntity>
}