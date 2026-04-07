package com.ws.skelton.remind.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ws.skelton.remind.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ws.skelton.remind.DiaryViewModel
import com.ws.skelton.remind.ui.ListeningDialog
import com.ws.skelton.remind.ui.components.DiaryItem
import com.ws.skelton.remind.ui.components.MoodGuideDialog
import com.ws.skelton.remind.ui.components.SettingsDialog
import com.ws.skelton.remind.ui.components.NativeAdListItem
import com.ws.skelton.remind.ui.components.ExitAdDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.itemsIndexed
import com.ws.skelton.remind.ui.theme.CardYellow
import com.ws.skelton.remind.ui.theme.SoftGray
import com.ws.skelton.remind.ui.theme.TextBrown
import com.ws.skelton.remind.utils.startListening
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(diaryViewModel: DiaryViewModel) {
    val diaryList by diaryViewModel.diaryList.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    // 날짜별 그룹화
    val groupedDiaries = remember(diaryList) {
        diaryList.groupBy { diary ->
            SimpleDateFormat("yyyy년 MM월 dd일 (E)", Locale.KOREAN).format(Date(diary.date))
        }
    }

    // 검색 및 필터 상태
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val allTags by diaryViewModel.allTags.collectAsState()
    val selectedMood: Int? by diaryViewModel.selectedMood.collectAsState() // 명시적 타입 지정
    val selectedTag: String? by diaryViewModel.selectedTag.collectAsState() // 명시적 타입 지정

    // 알림 설정 구독
    val isReminderEnabled by diaryViewModel.isReminderEnabled.collectAsState()
    val reminderHour by diaryViewModel.reminderHour.collectAsState()
    val reminderMinute by diaryViewModel.reminderMinute.collectAsState()

    // STT 상태 관리
    var isListening by remember { mutableStateOf(false) }
    var speechText by remember { mutableStateOf("") } 
    var showMoodGuide by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showExactAlarmRationale by remember { mutableStateOf(false) }
    var showNotificationDeniedDialog by remember { mutableStateOf(false) }
    
    // 뒤로가기 버튼 처리 (앱 종료 시 광고 팝업)
    BackHandler {
        showExitDialog = true
    }
    
    if (showExitDialog) {
        ExitAdDialog(
            onDismiss = { showExitDialog = false },
            onConfirm = { (context as? android.app.Activity)?.finish() }
        )
    }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    
    // 알림 권한 런처 (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 권한 허용 시 다음 단계로 (정확한 알람 체크)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
                if (!alarmManager.canScheduleExactAlarms()) {
                    showExactAlarmRationale = true
                    return@rememberLauncherForActivityResult
                }
            }
            diaryViewModel.updateReminderSettings(true, reminderHour, reminderMinute)
        } else {
            showNotificationDeniedDialog = true
            diaryViewModel.updateReminderSettings(false, reminderHour, reminderMinute)
        }
    }

    // 정확한 알람 권한 런처 (Android 14+)
    val exactAlarmLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
            if (alarmManager.canScheduleExactAlarms()) {
                diaryViewModel.updateReminderSettings(true, reminderHour, reminderMinute)
            } else {
                Toast.makeText(context, context.getString(R.string.msg_toast_exact_alarm_denied), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 정확한 알람 권한 안내 다이얼로그 (Android 14+)
    if (showExactAlarmRationale) {
        AlertDialog(
            onDismissRequest = { showExactAlarmRationale = false },
            title = { Text(stringResource(R.string.dialog_exact_alarm_title)) },
            text = { 
                Text(stringResource(R.string.dialog_exact_alarm_body)) 
            },
            confirmButton = {
                TextButton(onClick = {
                    showExactAlarmRationale = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        try {
                            val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                            }
                            exactAlarmLauncher.launch(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, context.getString(R.string.msg_toast_settings_error), Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Text(stringResource(R.string.action_go_to_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExactAlarmRationale = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            containerColor = Color.White
        )
    }

    // 데이터 내보내기 런처
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { diaryViewModel.exportData(context, it) }
    }

    // 데이터 불러오기 런처
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { diaryViewModel.importData(context, it) }
    }

    val exportTxtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let { diaryViewModel.exportToTxt(context, it) }
    }

    val exportPdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { diaryViewModel.exportToPdf(context, it) }
    }

    val currentFontIndex by diaryViewModel.selectedFontIndex.collectAsState()
    val shouldShowInterstitial by diaryViewModel.shouldShowInterstitial.collectAsState()

    // 전면 광고 트리거 관찰
    LaunchedEffect(shouldShowInterstitial) {
        if (shouldShowInterstitial) {
            val activity = context as? android.app.Activity
            activity?.let {
                com.ws.skelton.remind.utils.AdManager.showInterstitial(it) {
                    diaryViewModel.resetInterstitialFlag()
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            isListening = true
            speechText = "" 
            startListening(
                speechRecognizer = speechRecognizer, 
                context = context,
                onPartial = { partial -> speechText = partial },
                onResult = { result -> speechText = result }
            )
        } else {
            Toast.makeText(context, context.getString(R.string.msg_toast_voice_permission), Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try { speechRecognizer.destroy() } catch (e: Exception) {}
        }
    }

    // 듣기 팝업
    if (isListening) {
        ListeningDialog(
            currentText = speechText,
            onDismiss = { 
                speechRecognizer.stopListening()
                isListening = false
            },
            onRetry = { 
                speechRecognizer.stopListening()
                speechText = ""
                startListening(
                    speechRecognizer = speechRecognizer, 
                    context = context,
                    onPartial = { partial -> speechText = partial },
                    onResult = { result -> speechText = result }
                )
            },
            onSave = { 
                speechRecognizer.stopListening()
                isListening = false
                if (speechText.isNotBlank()) {
                    inputText += " $speechText"
                }
            }
        )
    }

    // 알림 권한 거부 안내 다이얼로그 (Settings 이동 유도)
    if (showNotificationDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationDeniedDialog = false },
            title = { Text(stringResource(R.string.dialog_notification_permission_title)) },
            text = { 
                Text(stringResource(R.string.dialog_notification_permission_body)) 
            },
            confirmButton = {
                TextButton(onClick = {
                    showNotificationDeniedDialog = false
                    try {
                        val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, context.getString(R.string.msg_toast_settings_error), Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text(stringResource(R.string.action_go_to_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationDeniedDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            containerColor = Color.White
        )
    }

    // 감정 가이드 팝업
    if (showMoodGuide) {
        MoodGuideDialog(onDismiss = { showMoodGuide = false })
    }

    // Settings Dialog
    if (showSettings) {
        SettingsDialog(
            onDismiss = { showSettings = false },
            onTimeChange = { h, m ->
                diaryViewModel.updateReminderSettings(isReminderEnabled, h, m)
            },
            onExportData = {
                val fileName = "mindlog_backup_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.json"
                exportLauncher.launch(fileName)
            },
            onImportData = {
                importLauncher.launch(arrayOf("application/json"))
            },
            onExportTxt = {
                val fileName = "mindlog_export_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.txt"
                exportTxtLauncher.launch(fileName)
            },
            onExportPdf = {
                val fileName = "mindlog_export_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.pdf"
                exportPdfLauncher.launch(fileName)
            },
            onReminderToggle = { enabled ->
                if (enabled) {
                    // 1. 알림 권한 체크 (Android 13+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val permission = Manifest.permission.POST_NOTIFICATIONS
                        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                            notificationPermissionLauncher.launch(permission)
                            return@SettingsDialog
                        }
                    }
                    
                    // 2. 정확한 알람 권한 체크 (Android 14+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
                        if (!alarmManager.canScheduleExactAlarms()) {
                            showExactAlarmRationale = true
                            return@SettingsDialog
                        }
                    }
                }
                diaryViewModel.updateReminderSettings(enabled, reminderHour, reminderMinute)
            },
            viewModel = diaryViewModel
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // imePadding() 제거 (adjustResize와 중복 방지)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // 1. 헤더 (검색 모드 대응)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp) // 상단 8->16, 하단 16->12
            ) {
            if (isSearching) {
                // 검색창 모드
                TextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        diaryViewModel.updateSearchQuery(it)
                    },
                    placeholder = { Text(stringResource(R.string.search_placeholder), fontSize = 16.sp) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = TextBrown
                    ),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = SoftGray) },
                    trailingIcon = {
                        IconButton(onClick = { 
                            isSearching = false
                            searchQuery = ""
                            diaryViewModel.updateSearchQuery("")
                        }) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.search_close), tint = SoftGray)
                        }
                    }
                )
            } else {
                // 일반 제목 모드
                Text(
                    text = stringResource(R.string.title_home),
                    color = TextBrown,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f)
                )

                // 검색 버튼
                IconButton(onClick = { isSearching = true }) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = stringResource(R.string.search_desc),
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // 가이드 버튼
                IconButton(onClick = { showMoodGuide = true }) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = stringResource(R.string.mood_guide_desc),
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 설정 버튼
                IconButton(onClick = { showSettings = true }) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = stringResource(R.string.settings_desc),
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 1-2. 필터 영역 (검색 모드일 때만 노출)
        if (isSearching) {
            val moodEmojis = mapOf(5 to "😀", 4 to "🙂", 3 to "😐", 2 to "🙁", 1 to "😫")
            
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                // 상단: 점수 필터 (가로 스크롤)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedMood == null && selectedTag == null,
                        onClick = {
                            diaryViewModel.updateSelectedMood(null)
                            diaryViewModel.updateSelectedTag(null)
                        },
                        label = { Text(stringResource(R.string.filter_all)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TextBrown,
                            selectedLabelColor = Color.White
                        )
                    )

                    moodEmojis.forEach { (score, emoji) ->
                        FilterChip(
                            selected = selectedMood == score,
                            onClick = { 
                                diaryViewModel.updateSelectedMood(if (selectedMood == score) null else score)
                                diaryViewModel.updateSelectedTag(null)
                            },
                            label = { Text("$emoji $score") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CardYellow,
                                selectedLabelColor = TextBrown
                            )
                        )
                    }
                }

                // 하단: 태그 필터 (태그가 있을 때만 가로 스크롤)
                if (allTags.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allTags.forEach { tag ->
                            FilterChip(
                                selected = selectedTag == tag,
                                onClick = { 
                                    diaryViewModel.updateSelectedTag(if (selectedTag == tag) null else tag)
                                    diaryViewModel.updateSelectedMood(null)
                                },
                                label = { Text("#$tag") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFE1F5FE),
                                    selectedLabelColor = Color(0xFF0288D1)
                                )
                            )
                        }
                    }
                }
            }
        }

        // 2. 일기 리스트 (날짜별 그룹화)
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp) // 80dp에서 16dp로 축소
        ) {
            if (diaryList.isEmpty()) {
                item {
                    EmptyStateView()
                }
            } else {
                groupedDiaries.forEach { (dateString, diaries) ->
                    // 날짜 헤더
                    item {
                        Text(
                            text = dateString,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }
                    // 해당 날짜의 일기들
                    itemsIndexed(diaries) { index, diary ->
                        DiaryItem(
                            diary = diary,
                            onDelete = { diaryViewModel.deleteDiary(diary) },
                            onEdit = { newContent -> diaryViewModel.updateDiary(diary, newContent) },
                            onRetry = { diaryViewModel.retryAnalysis(diary) },
                            onShare = { shareText ->
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Share Diary")
                                context.startActivity(shareIntent)
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 5번째 항목마다 네이티브 광고 삽입
                        if ((index + 1) % 5 == 0) {
                            NativeAdListItem()
                        }
                    }
                }
            }
        }

        // 구분선 추가
        HorizontalDivider(
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp), // 간격 축소
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )

        // 3. 입력창 (하단)
        Surface(
            shadowElevation = 6.dp,
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.92f) 
                .align(Alignment.CenterHorizontally)
                .imePadding() // 키보드 노출 시 입력창을 위로 밀어올림
                .padding(bottom = 4.dp) // 키보드와의 유격을 최소화
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 3.dp), // 세로 폭 더 축소 (8dp -> 4dp)
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 마이크 버튼
                IconButton(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            speechText = "" // 초기화
                            startListening(
                                speechRecognizer, context,
                                onPartial = { partial -> speechText = partial },
                                onResult = { result -> speechText = result }
                            )
                            isListening = true
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(if (isListening) Color(0xFFFFCDD2) else Color(0xFFE1F5FE), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice",
                        tint = if (isListening) Color(0xFFD32F2F) else Color(0xFF0288D1),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 입력 필드
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text(stringResource(R.string.input_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )

                // 전송 버튼
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            diaryViewModel.addDiary(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(CardYellow, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Create, 
                        contentDescription = "Save", 
                        tint = TextBrown,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

}

@Composable
fun EmptyStateView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp), // 간격 약간 조정
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.SentimentNeutral,
            contentDescription = null,
            tint = Color(0xFFE0E0E0),
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.empty_state_message),
            textAlign = TextAlign.Center,
            color = Color.Gray,
            lineHeight = 24.sp
        )
    }
}

// --- 프리뷰 영역 ---
@Preview(showBackground = true)
@Composable
fun PreviewEmptyState() {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
        EmptyStateView()
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewHomeScreenUI() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            ) {
                Text(
                    text = "나의 마음 기록 🌱",
                    color = com.ws.skelton.remind.ui.theme.TextBrown,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // 빈 상태 프리뷰 포함
            EmptyStateView()
            
            Spacer(modifier = Modifier.weight(1f))
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 0.5.dp,
                color = Color.LightGray.copy(alpha = 0.5f)
            )
            Surface(
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(44.dp).background(Color(0xFFE1F5FE), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFF0288D1), modifier = Modifier.size(20.dp)) }
                    Text(stringResource(R.string.input_placeholder), color = Color.LightGray, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp).weight(1f))
                    Box(modifier = Modifier.size(44.dp).background(com.ws.skelton.remind.ui.theme.CardYellow, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Create, contentDescription = null, tint = com.ws.skelton.remind.ui.theme.TextBrown, modifier = Modifier.size(20.dp)) }
                }
            }
        }
    }
}
