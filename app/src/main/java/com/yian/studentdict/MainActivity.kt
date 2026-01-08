package com.yian.studentdict

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yian.studentdict.data.AppDatabase
import com.yian.studentdict.data.DictEntity
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ContentView()
            }
        }
    }
}

@Composable
fun ContentView() {
    // 1. 初始化資料庫
    val context = LocalContext.current
    // 注意：請確保 AppDatabase 裡的檔名已經改成 "student_dict_v2.db" 或更新的版本
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.dictDao() }
    val scope = rememberCoroutineScope()

    // 👇👇👇 【新增】自動檢查資料庫的小間諜 👇👇👇
    LaunchedEffect(Unit) {
        scope.launch {
            println("🕵️‍♂️ === 資料庫自我檢查開始 ===")
            try {
                // 故意搜尋空字串，這會列出資料庫裡的前 50 筆資料
                val testList = dao.search("")
                println("🕵️‍♂️ 資料庫回傳筆數: ${testList.size} 筆")

                if (testList.isNotEmpty()) {
                    val firstItem = testList.first()
                    println("✅ 資料庫讀取成功！")
                    println("🕵️‍♂️ 範例資料: ID=${firstItem.id}, Word=${firstItem.word}, Phone=${firstItem.phonetic}")
                } else {
                    println("❌ 資料庫是空的！(Size = 0)")
                    println("👉 請檢查：")
                    println("1. assets 資料夾裡是否有 dictionary.sqlite？")
                    println("2. 請嘗試去 AppDatabase.kt 把檔名改成 'student_dict_v3.db' 強迫重建。")
                }
            } catch (e: Exception) {
                println("❌ 資料庫查詢發生錯誤: ${e.message}")
                e.printStackTrace()
            }
            println("🕵️‍♂️ === 資料庫自我檢查結束 ===")
        }
    }
    // 👆👆👆 檢查結束 👆👆👆

    // 2. 狀態變數
    var searchText by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<DictEntity>>(emptyList()) }
    var showCustomKeyboard by remember { mutableStateOf(true) }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp > 600

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppTheme.Background)
                    .padding(top = 16.dp, bottom = 8.dp)
            ) {
                Text(
                    text = "國語辭典簡編本",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = AppTheme.Primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(if (isTablet) 50.dp else 40.dp)
                        .background(AppTheme.CardBackground, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { showCustomKeyboard = true },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchText.isEmpty()) {
                            Text("輸入單字...", color = Color.Gray)
                        } else {
                            Text(searchText, color = Color.Black)
                        }
                    }

                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = {
                            searchText = ""
                            results = emptyList()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }

                    IconButton(onClick = { showCustomKeyboard = !showCustomKeyboard }) {
                        Icon(
                            imageVector = if (showCustomKeyboard) Icons.Default.Close else Icons.Default.Edit,
                            contentDescription = "Toggle Keyboard",
                            tint = AppTheme.Secondary
                        )
                    }
                }
            }
        },
        bottomBar = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text("AdMob Banner Area", color = Color.DarkGray)
                }

                AnimatedVisibility(
                    visible = showCustomKeyboard,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    ZhuyinKeyboard(
                        onKeyClick = { char ->
                            searchText += char
                            scope.launch {
                                results = dao.search(searchText)
                            }
                        },
                        onDelete = {
                            if (searchText.isNotEmpty()) {
                                searchText = searchText.dropLast(1)
                                scope.launch {
                                    if (searchText.isNotEmpty()) {
                                        results = dao.search(searchText)
                                    } else {
                                        results = emptyList()
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(AppTheme.Background)
        ) {
            if (results.isEmpty() && searchText.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("搜尋中 / 查無結果", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(results) { entity ->
                        WordCardView(
                            item = DictItem(
                                // 👇 使用 ?: 運算子，如果是 null 就給它空字串 ""
                                word = entity.word ?: "",
                                phonetic = entity.phonetic ?: "",
                                definition = entity.definition ?: "",
                                radical = entity.radical ?: "",
                                strokeCount = entity.strokeCount ?: 0
                            )
                        )
                    }
                }
            }
        }
    }
}