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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yian.studentdict.data.AppDatabase
import com.yian.studentdict.data.DictEntity
import kotlinx.coroutines.launch

// 改名為 AppFont
private val AppFont = try {
    FontFamily(Font(R.font.edukai_std, FontWeight.Normal))
} catch (e: Exception) {
    FontFamily.Default
}

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
    val context = LocalContext.current
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
                    // 🟢 [Fix] 語音搜尋優化：
                    // 1. 只顯示以該字詞「開頭」的結果 (filter)
                    // 2. 字數短的排前面 (sortedBy)
                    results = dao.search(searchText)
                        .filter { it.word?.startsWith(searchText) == true }
                        .sortedBy { it.word?.length }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        scope.launch { allRadicals = dao.getAllRadicals() }
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
        DictWordDetailScreen(
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
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (selectedRadical != null) "部首：$selectedRadical" else "國語辭典簡編本",
                            style = MaterialTheme.typography.headlineSmall,
                            color = AppTheme.Primary,
                            fontWeight = FontWeight.Bold
                        )

                        Button(
                            onClick = {
                                isRadicalMode = !isRadicalMode
                                selectedRadical = null
                                searchText = ""
                                results = emptyList()
                                showCustomKeyboard = !isRadicalMode
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if(isRadicalMode) AppTheme.Secondary else Color.Gray)
                        ) {
                            Icon(Icons.Default.List, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(if (isRadicalMode) "關閉部首" else "部首表")
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
                    Box(modifier = Modifier.fillMaxWidth().height(50.dp).background(Color.LightGray), contentAlignment = Alignment.Center) {
                        Text("AdMob Banner Area", color = Color.DarkGray, fontSize = 12.sp)
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
                                    // 鍵盤輸入注音時，不強制 filter，因為注音還沒組成字
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
                                    // 🟢 [Fix] 關鍵修改：選字後，只搜尋「以此字開頭」的詞條
                                    results = dao.search(searchText)
                                        .filter { it.word?.startsWith(searchText) == true } // 只留開頭相符的
                                        .sortedBy { it.word?.length } // 短的排前面
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
                                VerticalSearchResultRow(
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

@Composable
fun VerticalSearchResultRow(
    item: DictItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.word,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AppFont,
                    color = AppTheme.TextWhite,
                    lineHeight = 30.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (item.phonetic.isNotEmpty()) {
                    Text(
                        text = item.phonetic,
                        fontSize = 18.sp,
                        color = AppTheme.Secondary,
                        fontFamily = AppFont,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.radical.isNotEmpty()) {
                    DictIndexTag(text = "部首:${item.radical}")
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.DarkGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Divider(color = Color(0xFF2C2C2E), thickness = 1.dp, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
fun DictIndexTag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF2C2C2E))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictWordDetailScreen(
    item: DictItem,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("詳細釋義", color = AppTheme.TextWhite) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AppTheme.Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.Background
                )
            )
        },
        containerColor = AppTheme.Background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            val wordChars = item.word.toCharArray().map { it.toString() }
                            val phoneticSounds = item.phonetic.trim().split("\\s+".toRegex())

                            wordChars.forEachIndexed { index, char ->
                                val sound = phoneticSounds.getOrElse(index) { "" }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(end = 12.dp)
                                ) {
                                    Text(
                                        text = char,
                                        fontSize = 48.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = AppFont,
                                        color = AppTheme.Primary
                                    )

                                    if (sound.isNotEmpty()) {
                                        Text(
                                            text = sound,
                                            fontSize = 20.sp,
                                            color = AppTheme.Secondary,
                                            fontFamily = AppFont,
                                            fontWeight = FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Divider(color = Color.DarkGray, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        DetailInfoBadge(label = "部首", value = item.radical.ifEmpty { "無" })
                        Spacer(modifier = Modifier.width(24.dp))
                        DetailInfoBadge(label = "總筆畫", value = "${item.strokeCount}")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "解釋：",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.Secondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val formattedDef = item.definition.replace("n", "\n\n")

                    Text(
                        text = formattedDef,
                        fontSize = 20.sp,
                        fontFamily = AppFont,
                        color = AppTheme.TextWhite,
                        lineHeight = 32.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DetailInfoBadge(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 20.sp, color = AppTheme.TextWhite, fontWeight = FontWeight.Medium)
    }
}