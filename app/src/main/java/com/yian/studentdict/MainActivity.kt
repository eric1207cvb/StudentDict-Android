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
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.purchaseWith
import com.yian.studentdict.data.AppDatabase
import com.yian.studentdict.data.DictEntity
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🟢 1. 初始化 AdMob
        MobileAds.initialize(this) {}

        // 🟢 2. 初始化 RevenueCat (請替換為您的 API Key)
        Purchases.configure(
            PurchasesConfiguration.Builder(this, "goog_your_revenuecat_api_key").build()
        )

        // 🟢 3. 檢查訂閱狀態
        Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                // 假設您的 Entitlement ID 叫 "pro"
                UserState.isAdFree = customerInfo.entitlements["pro"]?.isActive == true
            }
            override fun onError(error: com.revenuecat.purchases.PurchasesError) {
                // 處理錯誤
            }
        })

        setContent {
            MaterialTheme {
                ContentView()
            }
        }
    }
}

@Composable
fun ContentView() {
    val context = LocalContext.current
    val activity = context as Activity
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.dictDao() }
    val scope = rememberCoroutineScope()

    // --- 狀態變數 ---
    var searchText by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<DictEntity>>(emptyList()) }
    var showCustomKeyboard by remember { mutableStateOf(true) }

    // 部首模式
    var isRadicalMode by remember { mutableStateOf(false) }
    var allRadicals by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedRadical by remember { mutableStateOf<String?>(null) }

    // 詳情頁狀態
    var currentDetailItem by remember { mutableStateOf<DictEntity?>(null) }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp > 600

    // 語音輸入
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
                    results = dao.search(searchText)
                        .filter { it.word?.startsWith(searchText) == true }
                        .sortedBy { it.word?.length }
                }
            }
        }
    }

    // 載入部首並進行排序
    LaunchedEffect(Unit) {
        scope.launch {
            val rawList = dao.getAllRadicals()
            allRadicals = rawList.sortedBy { radical ->
                RadicalOrder.getIndex(radical)
            }
        }
    }

    BackHandler(enabled = currentDetailItem != null) {
        currentDetailItem = null
    }

    if (currentDetailItem != null) {
        val item = currentDetailItem!!.let {
            DictItem(
                word = it.word ?: "",
                phonetic = it.phonetic ?: "",
                definition = it.definition ?: "",
                radical = it.radical ?: "",
                strokeCount = it.strokeCount ?: 0
            )
        }
        WordDetailScreen(
            item = item,
            onBack = { currentDetailItem = null }
        )
    } else {
        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppTheme.Background)
                        .padding(top = 16.dp, bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. 左側標題：佔據剩餘空間，確保標題一定在最左邊
                        Text(
                            text = if (selectedRadical != null) "部首：$selectedRadical" else "國語辭典簡編本",
                            style = MaterialTheme.typography.titleLarge,
                            color = AppTheme.Primary,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.weight(1f)
                        )

                        // 2. 右側按鈕組
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // 🟢 移除廣告 (僅在未付費時顯示)
                            if (!UserState.isAdFree) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AppTheme.Secondary.copy(alpha = 0.1f)) // 輕微底色
                                        .clickable { purchasePro(activity) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "移除廣告",
                                        color = AppTheme.Secondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            // 🟢 部首表按鈕
                            Button(
                                onClick = {
                                    isRadicalMode = !isRadicalMode
                                    selectedRadical = null
                                    searchText = ""
                                    results = emptyList()
                                    showCustomKeyboard = !isRadicalMode
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(36.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isRadicalMode) AppTheme.Primary else AppTheme.KeyBackground
                                )
                            ) {
                                Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (isRadicalMode) "關閉" else "部首", fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!isRadicalMode || selectedRadical != null) {
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
                                modifier = Modifier.weight(1f).fillMaxHeight().clickable {
                                    if (isRadicalMode) {
                                        isRadicalMode = false
                                        selectedRadical = null
                                        results = emptyList()
                                    }
                                    showCustomKeyboard = true
                                },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (searchText.isEmpty() && selectedRadical == null) {
                                    Text("輸入單字或注音...", color = Color.Gray)
                                } else if (selectedRadical != null) {
                                    Text("正在顯示「$selectedRadical」部首的字", color = AppTheme.Primary)
                                } else {
                                    Text(
                                        text = searchText,
                                        color = Color.White,
                                        fontSize = 18.sp
                                    )
                                }
                            }

                            if (searchText.isNotEmpty() || selectedRadical != null) {
                                IconButton(onClick = {
                                    searchText = ""
                                    selectedRadical = null
                                    results = emptyList()
                                    if (isRadicalMode) results = emptyList()
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }

                            IconButton(onClick = {
                                try {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-TW")
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "請說出想查的字詞...")
                                    }
                                    voiceLauncher.launch(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "您的裝置不支援語音輸入", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Input",
                                    tint = AppTheme.Secondary
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Column {
                    // 🟢 廣告預留區：根據付費狀態決定是否顯示廣告
                    if (!UserState.isAdFree) {
                        BannerAdView()
                    }

                    AnimatedVisibility(
                        visible = showCustomKeyboard,
                        enter = slideInVertically { it },
                        exit = slideOutVertically { it }
                    ) {
                        ZhuyinKeyboard(
                            results = results,
                            onKeyClick = { char ->
                                isRadicalMode = false
                                selectedRadical = null
                                searchText += char
                                scope.launch {
                                    results = dao.search(searchText).sortedBy { it.word?.length }
                                }
                            },
                            onDelete = {
                                if (searchText.isNotEmpty()) {
                                    searchText = searchText.dropLast(1)
                                    scope.launch {
                                        results = if (searchText.isNotEmpty()) {
                                            dao.search(searchText).sortedBy { it.word?.length }
                                        } else {
                                            emptyList()
                                        }
                                    }
                                }
                            },
                            onCandidateSelect = { entity ->
                                val selectedWord = entity.word ?: ""
                                searchText = selectedWord
                                isRadicalMode = false
                                selectedRadical = null
                                scope.launch {
                                    results = dao.search(searchText)
                                        .filter { it.word?.startsWith(searchText) == true }
                                        .sortedBy { it.word?.length }
                                }
                                currentDetailItem = null
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
                if (isRadicalMode && selectedRadical == null) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("請選擇部首：", modifier = Modifier.padding(bottom = 8.dp), color = Color.Gray)
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 60.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(allRadicals) { radical ->
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .background(Color.White, RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedRadical = radical
                                            scope.launch {
                                                results = dao.getWordsByRadical(radical)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(radical, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppTheme.Primary)
                                }
                            }
                        }
                    }
                } else {
                    if (results.isEmpty()) {
                        if (searchText.isNotEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("查無結果", color = Color.Gray)
                            }
                        } else if (selectedRadical == null && !isRadicalMode) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("👋 歡迎使用", fontSize = 24.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("輸入單字、注音，或是點擊上方「部首表」查找", color = Color.LightGray)
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            if (selectedRadical != null) {
                                item {
                                    Text(
                                        "部首「$selectedRadical」共 ${results.size} 字",
                                        color = Color.Gray,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }

                            items(results) { entity ->
                                SearchResultRow(
                                    item = DictItem(
                                        word = entity.word ?: "",
                                        phonetic = entity.phonetic ?: "",
                                        definition = entity.definition ?: "",
                                        radical = entity.radical ?: "",
                                        strokeCount = entity.strokeCount ?: 0
                                    ),
                                    onClick = {
                                        currentDetailItem = entity
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

// 🟢 AdMob 橫幅廣告元件
@Composable
fun BannerAdView() {
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(50.dp),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                // 測試用 ID，正式發布請換成自己的 Ad Unit ID
                adUnitId = "ca-app-pub-3940256099942544/6300978111"
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

// 🟢 RevenueCat 購買邏輯
fun purchasePro(activity: Activity) {
    Purchases.sharedInstance.getOfferingsWith(
        onError = { error ->
            Toast.makeText(activity, "無法取得購買項目: ${error.message}", Toast.LENGTH_SHORT).show()
        },
        onSuccess = { offerings ->
            offerings.current?.let { offering ->
                val packageToBuy = offering.availablePackages.firstOrNull()
                packageToBuy?.let {
                    Purchases.sharedInstance.purchaseWith(
                        PurchaseParams.Builder(activity, it).build(),
                        onError = { error, userCancelled ->
                            // 修正點：確保參數中有 userCancelled
                            if (!userCancelled) {
                                Toast.makeText(activity, "購買失敗: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onSuccess = { _, customerInfo ->
                            if (customerInfo.entitlements["pro"]?.isActive == true) {
                                UserState.isAdFree = true
                                Toast.makeText(activity, "感謝購買！廣告已移除", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    )
}