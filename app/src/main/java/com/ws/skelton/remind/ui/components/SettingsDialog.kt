package com.ws.skelton.remind.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ws.skelton.remind.ui.theme.TextBrown
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import com.ws.skelton.remind.ui.theme.CardYellow
import com.ws.skelton.remind.ui.theme.SoftGray
import androidx.compose.ui.res.stringResource
import com.ws.skelton.remind.R
import androidx.compose.material.icons.rounded.Language
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.rounded.Info
import androidx.compose.ui.text.style.TextDecoration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    onTimeChange: (Int, Int) -> Unit,
    onExportData: () -> Unit,
    onImportData: () -> Unit,
    onExportTxt: () -> Unit,
    onExportPdf: () -> Unit,
    onReminderToggle: (Boolean) -> Unit,
    viewModel: com.ws.skelton.remind.DiaryViewModel
) {
    val isReminderEnabled by viewModel.isReminderEnabled.collectAsState()
    val reminderHour by viewModel.reminderHour.collectAsState()
    val reminderMinute by viewModel.reminderMinute.collectAsState()
    val selectedFontIndex by viewModel.selectedFontIndex.collectAsState()
    val selectedFontSizeIndex by viewModel.selectedFontSize.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // 타임피커 상태 — Reflection 없이 직접 Int로 관리
    var selectedHour by remember { mutableIntStateOf(reminderHour) }
    var selectedMinute by remember { mutableIntStateOf(reminderMinute) }
    var isAfternoon by remember { mutableStateOf(reminderHour >= 12) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 24.dp, end = 24.dp, top = 24.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 알림 활성화 토글
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_reminder_daily),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = isReminderEnabled,
                        onCheckedChange = { checked ->
                            onReminderToggle(checked)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                if (isReminderEnabled) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = stringResource(R.string.settings_reminder_time),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    var isManualHourInput by remember { mutableStateOf(false) }
                    var isManualMinuteInput by remember { mutableStateOf(false) }
                    val hourFocusRequester = remember { FocusRequester() }
                    val minuteFocusRequester = remember { FocusRequester() }
                    val focusManager = LocalFocusManager.current

                    // 커스텀 휠 타임피커
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { 
                                isManualHourInput = false
                                isManualMinuteInput = false
                                focusManager.clearFocus()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // 오전/오후 휠
                            val periods = listOf(stringResource(R.string.settings_period_am), stringResource(R.string.settings_period_pm))
                            VerticalWheelPicker(
                                items = periods,
                                initialIndex = if (isAfternoon) 1 else 0,
                                onItemSelected = { index ->
                                    val newIsAfternoon = index == 1
                                    if (isAfternoon != newIsAfternoon) {
                                        isAfternoon = newIsAfternoon
                                        val current12Hour = selectedHour % 12
                                        selectedHour = if (newIsAfternoon) {
                                            if (current12Hour == 0) 12 else current12Hour + 12
                                        } else {
                                            if (current12Hour == 0) 0 else current12Hour
                                        }
                                    }
                                },
                                width = 60.dp,
                                isInfinite = false,
                                onClick = { 
                                    isManualHourInput = false
                                    isManualMinuteInput = false
                                    focusManager.clearFocus()
                                }
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            // 시간 영역
                            if (isManualHourInput) {
                                var hourText by remember { mutableStateOf((selectedHour % 12).let { if (it == 0) "12" else it.toString() }) }
                                LaunchedEffect(Unit) {
                                    hourFocusRequester.requestFocus()
                                }
                                Box(
                                    modifier = Modifier
                                        .width(60.dp)
                                        .height(60.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BasicTextField(
                                        value = hourText,
                                        onValueChange = {
                                            if (it.length <= 2) {
                                                hourText = it
                                                it.toIntOrNull()?.let { h ->
                                                    if (h in 1..12) {
                                                        selectedHour = if (isAfternoon) {
                                                            if (h == 12) 12 else h + 12
                                                        } else {
                                                            if (h == 12) 0 else h
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.focusRequester(hourFocusRequester),
                                        textStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true
                                    )
                                }
                            } else {
                                val hours = (1..12).toList().map { it.toString() }
                                // 페이지 상태를 직접 관찰하여 경계 감지
                                VerticalWheelPicker(
                                    items = hours,
                                    initialIndex = (selectedHour % 12).let { if (it == 0) 11 else it - 1 },
                                    onItemSelected = { index ->
                                        val hour1to12 = index + 1
                                        selectedHour = if (isAfternoon) {
                                            if (hour1to12 == 12) 12 else hour1to12 + 12
                                        } else {
                                            if (hour1to12 == 12) 0 else hour1to12
                                        }
                                    },
                                    width = 40.dp,
                                    isInfinite = true,
                                    onPageChanged = { oldPage, newPage ->
                                        val itemsSize = 12
                                        val oldIndex = oldPage % itemsSize
                                        val newIndex = newPage % itemsSize

                                        // 11시(인덱스 10) <-> 12시(인덱스 11) 경계에서 AM/PM 전환
                                        if ((oldIndex == 10 && newIndex == 11) || (oldIndex == 11 && newIndex == 10)) {
                                            isAfternoon = !isAfternoon
                                            selectedHour = if (selectedHour < 12) {
                                                (selectedHour + 12).coerceIn(0, 23)
                                            } else {
                                                (selectedHour - 12).coerceIn(0, 23)
                                            }
                                        }
                                    },
                                    onClick = { 
                                        isManualHourInput = true
                                        isManualMinuteInput = false 
                                    }
                                )
                            }

                            Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))

                            // 분 영역
                            if (isManualMinuteInput) {
                                var minuteText by remember { mutableStateOf(selectedMinute.toString().padStart(2, '0')) }
                                LaunchedEffect(Unit) {
                                    minuteFocusRequester.requestFocus()
                                }
                                Box(
                                    modifier = Modifier
                                        .width(60.dp)
                                        .height(60.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BasicTextField(
                                        value = minuteText,
                                        onValueChange = {
                                            if (it.length <= 2) {
                                                minuteText = it
                                                it.toIntOrNull()?.let { m ->
                                                    if (m in 0..59) {
                                                        selectedMinute = m
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.focusRequester(minuteFocusRequester),
                                        textStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true
                                    )
                                }
                            } else {
                                val minutes = (0..59).toList().map { it.toString().padStart(2, '0') }
                                VerticalWheelPicker(
                                    items = minutes,
                                    initialIndex = selectedMinute,
                                    onItemSelected = { index ->
                                        selectedMinute = index
                                    },
                                    width = 40.dp,
                                    isInfinite = true,
                                    onClick = { 
                                        isManualMinuteInput = true
                                        isManualHourInput = false 
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(24.dp))

                // Refactored Font Settings Section
                SettingsSectionTitle(stringResource(R.string.settings_font_title), Icons.Rounded.FontDownload)
                
                // 글꼴 종류 선택 (기존 유지)
                val fonts = listOf(
                    stringResource(R.string.settings_font_default), 
                    stringResource(R.string.settings_font_serif), 
                    stringResource(R.string.settings_font_gothic), 
                    stringResource(R.string.settings_font_handwriting), 
                    "카페24", "지마켓", "강부", "규리", "기쁨", "끄트", "느릿"
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(fonts.size) { index ->
                        FilterChip(
                            selected = selectedFontIndex == index,
                            onClick = { viewModel.updateFont(index) },
                            label = { Text(fonts[index]) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CardYellow,
                                selectedLabelColor = TextBrown
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 글꼴 크기 선택 추가
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FormatSize, contentDescription = null, tint = SoftGray, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_font_size), style = MaterialTheme.typography.titleSmall, color = SoftGray)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sizes = listOf(
                        stringResource(R.string.settings_font_size_small), 
                        stringResource(R.string.settings_font_size_medium), 
                        stringResource(R.string.settings_font_size_large)
                    )
                    sizes.forEachIndexed { index, size ->
                        FilterChip(
                            selected = selectedFontSizeIndex == index,
                            onClick = { viewModel.updateFontSize(index) },
                            label = { 
                                Text(
                                    text = size,
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) 
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CardYellow,
                                selectedLabelColor = TextBrown
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(24.dp))

                // Language Selector Section
                SettingsSectionTitle(stringResource(R.string.settings_language_title), Icons.Rounded.Language)
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val languageOptions = listOf(
                        stringResource(R.string.settings_language_system) to "",
                        stringResource(R.string.settings_language_korean) to "ko",
                        "English" to "en"
                    )
                    
                    val currentLocale = androidx.core.os.LocaleListCompat.getDefault()[0]?.language ?: ""
                    
                    languageOptions.forEach { (label, code) ->
                        FilterChip(
                            selected = when {
                                code.isEmpty() -> currentLocale.isEmpty() || 
                                    (currentLocale != "ko" && currentLocale != "en")
                                else -> currentLocale == code
                            },
                            onClick = {
                                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                                    androidx.core.os.LocaleListCompat.forLanguageTags(code.ifEmpty { null })
                                )
                            },
                            label = { 
                                Text(
                                    text = label,
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) 
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CardYellow,
                                selectedLabelColor = TextBrown
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.settings_data_title),
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onExportData,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text(stringResource(R.string.settings_backup_json))
                    }
                    OutlinedButton(
                        onClick = onImportData,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text(stringResource(R.string.settings_restore_json))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onExportTxt,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray.copy(alpha = 0.2f), contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text(stringResource(R.string.settings_export_txt))
                    }
                    Button(
                        onClick = onExportPdf,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray.copy(alpha = 0.2f), contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text(stringResource(R.string.settings_export_pdf))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(24.dp))

                // App Info Section
                SettingsSectionTitle("App Info / 앱 정보", Icons.Rounded.Info)
                
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val packageInfo = try {
                        context.packageManager.getPackageInfo(context.packageName, 0)
                    } catch (e: Exception) {
                        null
                    }
                    val versionName = packageInfo?.versionName ?: "1.0.0"
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Version / 버전", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(versionName, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Text(
                        text = "Privacy Policy / 개인정보 처리방침",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/wootaeng/MindLog/blob/main/privacy_policy.md"))
                                context.startActivity(intent)
                            }
                            .padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                } // End of scrollable Column

                // Fixed Action Buttons at the bottom
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_cancel), color = Color.Gray)
                    }
                    Button(
                        onClick = {
                            onTimeChange(selectedHour, selectedMinute)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA5D6A7)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.action_save), color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun VerticalWheelPicker(
    items: List<String>,
    initialIndex: Int,
    onItemSelected: (Int) -> Unit,
    width: androidx.compose.ui.unit.Dp,
    isInfinite: Boolean = false,
    onPageChanged: (Int, Int) -> Unit = { _, _ -> },
    onClick: () -> Unit = {}
) {
    val totalPageCount = if (isInfinite) Int.MAX_VALUE else items.size
    val initialPage = if (isInfinite) {
        // Int.MAX_VALUE의 절반 지점에서 시작하여 무한 스크롤 느낌을 줌
        (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % items.size) + initialIndex
    } else {
        initialIndex
    }

    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { totalPageCount })
    
    // 페이지 변경 시 콜백 호출
    LaunchedEffect(pagerState.currentPage) {
        onItemSelected(pagerState.currentPage % items.size)
    }

    // 외부에서 initialIndex가 변경되었을 때 (예: AM/PM 자동 전환) 페이지 동기화
    LaunchedEffect(initialIndex) {
        if (!isInfinite && pagerState.currentPage != initialIndex) {
             pagerState.animateScrollToPage(initialIndex)
        }
    }

    // 이전 페이지 추적을 위한 상태
    var lastPage by remember { mutableStateOf(initialPage) }
    LaunchedEffect(pagerState.currentPage) {
        if (lastPage != pagerState.currentPage) {
            onPageChanged(lastPage, pagerState.currentPage)
            lastPage = pagerState.currentPage
        }
    }

    VerticalPager(
        state = pagerState,
        modifier = Modifier
            .width(width)
            .height(120.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentPadding = PaddingValues(vertical = 40.dp) // 중앙 집중을 위한 패딩
    ) { page ->
        val actualIndex = page % items.size
        val isSelected = (pagerState.currentPage % items.size) == actualIndex
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = items[actualIndex],
                fontSize = if (isSelected) 24.sp else 18.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.LightGray,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun FontButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(label, fontSize = 14.sp)
    }
}
@Composable
fun SettingsSectionTitle(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SoftGray,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextBrown,
            fontWeight = FontWeight.Bold
        )
    }
}
