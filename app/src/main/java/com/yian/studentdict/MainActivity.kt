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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.launch

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

    var searchText by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<DictEntity>>(emptyList()) }
    var showCustomKeyboard by remember { mutableStateOf(true) }
    var isRadicalMode by remember { mutableStateOf(false) }
    var allRadicals by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedRadical by remember { mutableStateOf<String?>(null) }
    var currentDetailItem by remember { mutableStateOf<DictEntity?>(null) }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp > 600

    // 判斷是否為聲調符號的輔助函式
    fun isToneSymbol(char: String): Boolean {
        return listOf("ˊ", "ˇ", "ˋ", "˙").contains(char)
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
                    results = dao.search(searchText)
                        .filter { it.word?.startsWith(searchText) == true }
                        .sortedBy { it.word?.length }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            val rawList = dao.getAllRadicals()
            allRadicals = rawList
                .filter { it.isNotBlank() }
                .sortedBy { RadicalOrder.getIndex(it) }
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
        WordDetailScreen(item = item, onBack = { currentDetailItem = null })
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
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedRadical != null) "部首：$selectedRadical" else "國語辭典簡編本",
                            style = MaterialTheme.typography.titleLarge,
                            color = AppTheme.Primary,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!UserState.isAdFree) {
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { purchasePro(activity) },
                                    color = AppTheme.Secondary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        "移除廣告",
                                        color = AppTheme.Secondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    isRadicalMode = !isRadicalMode
                                    selectedRadical = null
                                    searchText = ""
                                    results = emptyList()
                                    showCustomKeyboard = !isRadicalMode
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isRadicalMode) AppTheme.Primary else AppTheme.KeyBackground
                                )
                            ) {
                                Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (isRadicalMode) "關閉" else "部首", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!isRadicalMode || selectedRadical != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(if (isTablet) 52.dp else 44.dp)
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
                                    Text(text = searchText, color = Color.White, fontSize = 18.sp)
                                }
                            }

                            if (searchText.isNotEmpty() || selectedRadical != null) {
                                IconButton(onClick = {
                                    searchText = ""
                                    selectedRadical = null
                                    results = emptyList()
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }

                            IconButton(onClick = {
                                try {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-TW")
                                    }
                                    voiceLauncher.launch(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "不支援語音輸入", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice", tint = AppTheme.Secondary)
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Column {
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
                                    // 🟢 智慧搜尋優化
                                    var searchResults = dao.search(searchText)

                                    // 💡 如果目前搜尋不到結果，且使用者輸入的不是聲調（可能是隱藏的一聲）
                                    if (searchResults.isEmpty() && !isToneSymbol(char)) {
                                        // 嘗試加上「ˉ」一聲標記再搜一次
                                        searchResults = dao.search(searchText + "ˉ")
                                    }

                                    results = searchResults.sortedBy { it.word?.length }
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
                                searchText = entity.word ?: ""
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
                                            scope.launch { results = dao.getWordsByRadical(radical) }
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
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(if (searchText.isNotEmpty()) "查無結果" else "👋 歡迎使用", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                            items(results) { entity ->
                                SearchResultRow(
                                    item = DictItem(
                                        word = entity.word ?: "",
                                        phonetic = entity.phonetic ?: "",
                                        definition = entity.definition ?: "",
                                        radical = entity.radical ?: "",
                                        strokeCount = entity.strokeCount ?: 0
                                    ),
                                    onClick = { currentDetailItem = entity }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BannerAdView() {
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(50.dp),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = "ca-app-pub-8563333250584395/2298788798"
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

fun purchasePro(activity: Activity) {
    Purchases.sharedInstance.getOfferingsWith(
        onError = { error ->
            Toast.makeText(activity, "無法取得購買項目: ${error.message}", Toast.LENGTH_SHORT).show()
        },
        onSuccess = { offerings ->
            val packageToBuy = offerings["main"]?.lifetime
                ?: offerings.current?.availablePackages?.firstOrNull()

            packageToBuy?.let { pkg ->
                Purchases.sharedInstance.purchaseWith(
                    PurchaseParams.Builder(activity, pkg).build(),
                    onError = { error, userCancelled ->
                        if (!userCancelled) {
                            Toast.makeText(activity, "購買失敗: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onSuccess = { _, customerInfo ->
                        if (customerInfo.entitlements["premium"]?.isActive == true) {
                            UserState.isAdFree = true
                            Toast.makeText(activity, "感謝購買！廣告已移除", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    )
}