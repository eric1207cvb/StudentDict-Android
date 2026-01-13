package com.yian.studentdict

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.revenuecat.purchases.*
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.yian.studentdict.data.AppDatabase
import com.yian.studentdict.data.DictEntity
import com.yian.studentdict.data.HistoryEntity
import kotlinx.coroutines.launch

// 全域狀態管理
object UserState {
    var isAdFree by mutableStateOf(false)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this) {}
        Purchases.configure(
            PurchasesConfiguration.Builder(this, "goog_JCoyQUudGrxMArsKUtXsNcEHicQ").build()
        )
        Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                UserState.isAdFree = customerInfo.entitlements["premium"]?.isActive == true
            }
            override fun onError(error: PurchasesError) {}
        })
        setContent { MaterialTheme { ContentView() } }
    }
}

@Composable
fun ContentView() {
    val context = LocalContext.current
    val activity = context as Activity
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.dictDao() }
    val scope = rememberCoroutineScope()

    var searchText by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<DictEntity>>(emptyList()) }
    var historyList by remember { mutableStateOf<List<HistoryEntity>>(emptyList()) }

    var showCustomKeyboard by remember { mutableStateOf(true) }
    var isRadicalMode by remember { mutableStateOf(false) }
    var allRadicals by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedRadical by remember { mutableStateOf<String?>(null) }
    var currentDetailItem by remember { mutableStateOf<DictEntity?>(null) }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp > 600

    fun isToneSymbol(char: String): Boolean = listOf("ˊ", "ˇ", "ˋ", "˙").contains(char)

    fun isZhuyin(char: Char): Boolean {
        // 包含標準注音符號與聲調
        return (char in '\u3105'..'\u3129') || listOf('ˉ', 'ˊ', 'ˇ', 'ˋ', '˙').contains(char)
    }

    // 🟢 修正後的混合搜尋邏輯
    suspend fun mixedSearch(input: String): List<DictEntity> {
        val hasZhuyin = input.any { isZhuyin(it) }
        val hasChinese = input.any { !isZhuyin(it) }

        // 1. 如果不是混合輸入 (純國字或純注音)，走原本的搜尋
        if (!hasZhuyin || !hasChinese) {
            var list = dao.search(input)
            // 如果注音搜尋沒結果，嘗試自動補一聲
            if (list.isEmpty() && hasZhuyin && !isToneSymbol(input.last().toString())) {
                list = dao.search(input + "ˉ")
            }
            return list.sortedBy { it.word?.length }
        }

        // 2. 混合模式 (例如：老ㄕ)
        // 找出最後一個國字的位置，切割字串
        val lastChineseIndex = input.indexOfLast { !isZhuyin(it) }
        val chinesePart = input.substring(0, lastChineseIndex + 1) // "老"
        val zhuyinPart = input.substring(lastChineseIndex + 1).trim() // "ㄕ"

        if (chinesePart.isEmpty()) return emptyList()

        // 🟢 關鍵修正：改用 getCandidates 抓取前 500 筆符合開頭的資料
        // 這樣可以避免 "老師" 因為排序問題被擠出前 100 名
        val candidates = dao.getCandidates(chinesePart)

        return candidates.filter { entity ->
            val word = entity.word ?: ""
            val phonetic = entity.phonetic ?: "" // 格式可能為 "ㄌㄠˇ　ㄕ" (全形空白)

            // 如果單字長度比輸入的國字部分還短，不可能匹配
            if (word.length <= chinesePart.length) return@filter false

            // 🟢 關鍵修正：使用 Regex("\\s+") 同時處理半形與全形空白
            val phoneticParts = phonetic
                .replace("　", " ") // 先把全形空白轉半形，以防萬一
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }

            // 我們要比對的是「輸入國字長度」位置的注音
            // 例如 "老(index 0) ㄕ"，我們要比對 phoneticParts[1]
            val targetIndex = chinesePart.length

            if (targetIndex < phoneticParts.size) {
                // 去除聲調後比對
                val targetPhonetic = phoneticParts[targetIndex]
                    .replace("ˉ", "").replace("ˊ", "").replace("ˇ", "").replace("ˋ", "").replace("˙", "")

                targetPhonetic.startsWith(zhuyinPart)
            } else {
                false
            }
        }.sortedBy { it.word?.length }
    }

    fun addToHistory(word: String) {
        scope.launch {
            dao.insertHistory(HistoryEntity(word = word, timestamp = System.currentTimeMillis()))
            historyList = dao.getHistory()
        }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            allRadicals = dao.getAllRadicals().filter { it.isNotBlank() }.sortedBy { RadicalOrder.getIndex(it) }
            historyList = dao.getHistory()
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrEmpty()) {
                searchText = spokenText
                isRadicalMode = false
                selectedRadical = null
                scope.launch {
                    results = mixedSearch(searchText)
                    if (results.isNotEmpty()) {
                        addToHistory(searchText)
                    }
                }
            }
        }
    }

    BackHandler(enabled = currentDetailItem != null) { currentDetailItem = null }

    if (currentDetailItem != null) {
        val item = currentDetailItem!!.let {
            DictItem(word = it.word ?: "", phonetic = it.phonetic ?: "", definition = it.definition ?: "", radical = it.radical ?: "", strokeCount = it.strokeCount ?: 0)
        }
        WordDetailScreen(item = item, onBack = { currentDetailItem = null })
    } else {
        Scaffold(
            topBar = {
                Column(modifier = Modifier.fillMaxWidth().background(AppTheme.Background).padding(top = 16.dp, bottom = 8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (selectedRadical != null) "部首：$selectedRadical" else "國語辭典簡編本",
                            style = MaterialTheme.typography.titleLarge,
                            color = AppTheme.Primary,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!UserState.isAdFree) {
                                Surface(modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { purchasePro(activity) }, color = AppTheme.Secondary.copy(alpha = 0.2f)) {
                                    Text("移除廣告", color = AppTheme.Secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                                }
                            }
                            Surface(modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                                isRadicalMode = !isRadicalMode
                                selectedRadical = null
                                searchText = ""
                                results = emptyList()
                                showCustomKeyboard = !isRadicalMode
                            }, color = if (isRadicalMode) AppTheme.Primary else AppTheme.KeyBackground) {
                                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.List, null, modifier = Modifier.size(16.dp), tint = if (isRadicalMode) Color.White else AppTheme.Primary)
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (isRadicalMode) "關閉" else "部首", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isRadicalMode) Color.White else AppTheme.Primary)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (!isRadicalMode || selectedRadical != null) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(if (isTablet) 52.dp else 44.dp).background(AppTheme.CardBackground, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, null, tint = Color.Gray)
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable {
                                if (isRadicalMode) {
                                    isRadicalMode = false
                                    selectedRadical = null
                                    results = emptyList()
                                }
                                showCustomKeyboard = true
                            }, contentAlignment = Alignment.CenterStart) {
                                if (searchText.isEmpty() && selectedRadical == null) {
                                    Text("輸入單字或注音...", color = Color.Gray)
                                } else if (selectedRadical != null) {
                                    Text("正在顯示「$selectedRadical」部首的字", color = AppTheme.Primary)
                                } else {
                                    Text(text = searchText, color = Color.White, fontSize = 18.sp)
                                }
                            }
                            if (searchText.isNotEmpty() || selectedRadical != null) {
                                IconButton(onClick = {
                                    searchText = ""
                                    selectedRadical = null
                                    results = emptyList()
                                    scope.launch { historyList = dao.getHistory() }
                                }) { Icon(Icons.Default.Close, null, tint = Color.Gray) }
                            }
                            IconButton(onClick = {
                                try {
                                    voiceLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-TW") })
                                } catch (e: Exception) { Toast.makeText(context, "不支援語音輸入", Toast.LENGTH_SHORT).show() }
                            }) { Icon(Icons.Default.Mic, null, tint = AppTheme.Secondary) }
                        }
                    }
                }
            },
            bottomBar = {
                Column {
                    if (!UserState.isAdFree) BannerAdView()
                    AnimatedVisibility(
                        visible = showCustomKeyboard,
                        enter = slideInVertically { it },
                        exit = slideOutVertically { it }
                    ) {
                        Column {
                            ZhuyinKeyboard(
                                results = results,
                                onKeyClick = { char ->
                                    isRadicalMode = false
                                    selectedRadical = null
                                    searchText += char
                                    scope.launch {
                                        results = mixedSearch(searchText) // 🟢 鍵盤輸入使用混合搜尋
                                    }
                                },
                                onDelete = {
                                    if (searchText.isNotEmpty()) {
                                        searchText = searchText.dropLast(1)
                                        scope.launch {
                                            results = if (searchText.isNotEmpty()) mixedSearch(searchText) else emptyList()
                                        }
                                    }
                                },
                                onCandidateSelect = { entity ->
                                    val word = entity.word ?: ""
                                    searchText = word
                                    isRadicalMode = false
                                    selectedRadical = null
                                    addToHistory(word)
                                    scope.launch {
                                        results = dao.search(searchText).filter { it.word?.startsWith(searchText) == true }.sortedBy { it.word?.length }
                                    }
                                    currentDetailItem = null
                                }
                            )
                            LegalFooter()
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize().background(AppTheme.Background)) {
                if (isRadicalMode && selectedRadical == null) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("請選擇部首：", modifier = Modifier.padding(bottom = 8.dp), color = Color.Gray)
                        LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 60.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(allRadicals) { radical ->
                                Box(modifier = Modifier.aspectRatio(1f).background(Color.White, RoundedCornerShape(8.dp)).clickable { selectedRadical = radical; scope.launch { results = dao.getWordsByRadical(radical) } }, contentAlignment = Alignment.Center) {
                                    Text(radical, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppTheme.Primary)
                                }
                            }
                        }
                    }
                } else {
                    if (searchText.isEmpty() && results.isEmpty()) {
                        if (historyList.isNotEmpty()) {
                            Column {
                                Text(
                                    text = "最近查詢 (100筆)",
                                    color = Color.Gray,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
                                )
                                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                                    items(historyList) { historyItem ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    searchText = historyItem.word
                                                    scope.launch {
                                                        results = dao.search(searchText).sortedBy { it.word?.length }
                                                        addToHistory(historyItem.word)
                                                    }
                                                }
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.History, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(16.dp))
                                            Text(historyItem.word, color = Color.White, fontSize = 18.sp)
                                        }
                                        Divider(color = Color.DarkGray, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                    }
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("👋 歡迎使用\n輸入單字或注音開始查詢", color = Color.Gray, textAlign = TextAlign.Center)
                            }
                        }
                    } else if (results.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("查無結果", color = Color.Gray) }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                            items(results) { entity ->
                                SearchResultRow(
                                    item = DictItem(word = entity.word ?: "", phonetic = entity.phonetic ?: "", definition = entity.definition ?: "", radical = entity.radical ?: "", strokeCount = entity.strokeCount ?: 0),
                                    onClick = {
                                        currentDetailItem = entity
                                        addToHistory(entity.word ?: "")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ... LegalFooter, BannerAdView, purchasePro 保持不變 ...
@Composable
fun LegalFooter() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "隱私權政策", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.clickable { uriHandler.openUri("https://eric1207cvb.github.io/StudentDict-Android/") }.padding(8.dp))
        Text(text = "  |  ", color = Color.Gray, fontSize = 11.sp)
        Text(text = if (UserState.isAdFree) "專業版已啟用" else "移除廣告", color = AppTheme.Secondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { if (!UserState.isAdFree) { val activity = context as? Activity; activity?.let { purchasePro(it) } } }.padding(8.dp))
    }
}

@Composable
fun BannerAdView() {
    AndroidView(modifier = Modifier.fillMaxWidth().height(50.dp), factory = { context -> AdView(context).apply { setAdSize(AdSize.BANNER); adUnitId = "ca-app-pub-8563333250584395/2298788798"; loadAd(AdRequest.Builder().build()) } })
}

fun purchasePro(activity: Activity) {
    Purchases.sharedInstance.getOfferingsWith(
        onError = { Toast.makeText(activity, "無法取得購買項目: ${it.message}", Toast.LENGTH_SHORT).show() },
        onSuccess = { offerings ->
            val pkg = offerings["main"]?.lifetime ?: offerings.current?.availablePackages?.firstOrNull()
            pkg?.let { Purchases.sharedInstance.purchaseWith(PurchaseParams.Builder(activity, it).build(), onError = { e, c -> if(!c) Toast.makeText(activity, "購買失敗: ${e.message}", Toast.LENGTH_SHORT).show() }, onSuccess = { _, info -> if (info.entitlements["premium"]?.isActive == true) { UserState.isAdFree = true; Toast.makeText(activity, "感謝購買！廣告已移除", Toast.LENGTH_SHORT).show() } }) } ?: Toast.makeText(activity, "錯誤：找不到商品 (請確認測試權限)", Toast.LENGTH_LONG).show()
        }
    )
}