package com.ws.skelton.remind.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ws.skelton.remind.DiaryViewModel
import com.ws.skelton.remind.data.DiaryEntity
import java.util.*

import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.ws.skelton.remind.ui.components.DiaryItem
import com.ws.skelton.remind.ui.components.SmallNativeAdView
import com.ws.skelton.remind.ui.components.NativeAdListItem // 추가
import androidx.compose.ui.res.stringResource
import com.ws.skelton.remind.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    diaryViewModel: DiaryViewModel,
    onNavigateToStats: () -> Unit // 추가
) {
    val context = androidx.compose.ui.platform.LocalContext.current // Add context capture

    val diaryList by diaryViewModel.diaryList.collectAsState()
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isSmallScreen = configuration.screenHeightDp < 600
    val gridItemHeight = if (isSmallScreen) 12.dp else 16.dp
    val verticalGap = if (isSmallScreen) 2.dp else 4.dp
    
    // ... (States unchanged) ...
    val tempCal = Calendar.getInstance()
    var currentYear by remember { mutableIntStateOf(tempCal.get(Calendar.YEAR)) }
    var currentMonth by remember { mutableIntStateOf(tempCal.get(Calendar.MONTH)) } // 0-based

    // BottomSheet 상태
    var showSheet by remember { mutableStateOf(false) }
    var selectedDiaries by remember { mutableStateOf<List<DiaryEntity>>(emptyList()) }
    val sheetState = rememberModalBottomSheetState()
    
    // 해당 월의 정보 계산
    val daysInMonth = remember(currentYear, currentMonth) {
        val cal = Calendar.getInstance()
        cal.set(currentYear, currentMonth, 1)
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val firstDayOfWeek = remember(currentYear, currentMonth) {
        val cal = Calendar.getInstance()
        cal.set(currentYear, currentMonth, 1)
        cal.get(Calendar.DAY_OF_WEEK) // 1=Sunday, 2=Monday, ...
    }

    // 일기 데이터 매핑: 날짜(Int) -> List<DiaryEntity>
    val diaryMap = remember(diaryList, currentYear, currentMonth) {
        val map = mutableMapOf<Int, MutableList<DiaryEntity>>()
        diaryList.forEach { diary ->
            val dCal = Calendar.getInstance()
            dCal.timeInMillis = diary.date
            if (dCal.get(Calendar.YEAR) == currentYear && dCal.get(Calendar.MONTH) == currentMonth) {
                val day = dCal.get(Calendar.DAY_OF_MONTH)
                if (!map.containsKey(day)) {
                    map[day] = mutableListOf()
                }
                map[day]?.add(diary)
            }
        }
        // 날짜별로 최신순 정렬 등 필요시 추가
        map
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp)
            ) {
                Text(
                    text = stringResource(R.string.calendar_records_title, currentMonth + 1),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 64.dp) // 하단 잘림 방지 여백 추가
                ) {
                    itemsIndexed(selectedDiaries) { index, diary ->
                        DiaryItem(
                            diary = diary,
                            onDelete = {
                                diaryViewModel.deleteDiary(diary)
                                showSheet = false // 삭제 후 닫기 (선택사항)
                            },
                            onEdit = { newContent ->
                                diaryViewModel.updateDiary(diary, newContent)
                                showSheet = false // 수정 후 닫기 (선택사항)
                            },
                            onRetry = {
                                diaryViewModel.retryAnalysis(diary)
                                showSheet = false
                            },
                            onShare = { shareText ->
                                val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                val shareIntent = android.content.Intent.createChooser(sendIntent, "Share Diary")
                                context.startActivity(shareIntent)
                            }
                        )
                        
                        // 5개마다 광고 노출 (4, 9, 14...)
                        if ((index + 1) % 5 == 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            NativeAdListItem()
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        // --- 1. 달력 헤더 ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { 
                if (currentMonth == 0) {
                    currentMonth = 11
                    currentYear--
                } else {
                    currentMonth--
                }
            }) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous Month",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Text(
                text = stringResource(R.string.calendar_month_label, currentYear, currentMonth + 1),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = if (isSmallScreen) 16.sp else 20.sp // 적응형 텍스트
            )
            
            IconButton(onClick = { 
                if (currentMonth == 11) {
                    currentMonth = 0
                    currentYear++
                } else {
                    currentMonth++
                }
            }) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next Month",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(verticalGap))

        // --- 2. 요일 헤더 ---
        Row(modifier = Modifier.fillMaxWidth()) {
            val days = listOf("일", "월", "화", "수", "목", "금", "토")
            days.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = if (day == "일") Color.Red else if (day == "토") Color.Blue else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isSmallScreen) 10.sp else 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(verticalGap))
        
        // --- 3. 날짜 그리드 ---
        val emptySlots = firstDayOfWeek - 1

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(bottom = 2.dp)
        ) {
            // 빈 칸
            items(emptySlots) {
                Spacer(modifier = Modifier.height(gridItemHeight))
            }

            // 날짜 칸
            items(daysInMonth) { index ->
                val day = index + 1
                val diariesOfDay = diaryMap[day] ?: emptyList()
                val topMoodScore = diariesOfDay.maxByOrNull { it.date }?.moodScore ?: 0
                val moodColor = getMoodColor(topMoodScore)

                CalendarDayItem(
                    day = day, 
                    moodColor = moodColor, 
                    moodScore = topMoodScore,
                    hasEntry = diariesOfDay.isNotEmpty(),
                    isSmallScreen = isSmallScreen,
                    onClick = {
                        if (diariesOfDay.isNotEmpty()) {
                            selectedDiaries = diariesOfDay
                            showSheet = true
                        }
                    }
                )
            }
        }

        // --- 4. 하단 광고 (네이티브 광고로 전환) ---
        Box(modifier = Modifier.fillMaxWidth().wrapContentHeight(), contentAlignment = Alignment.Center) {
            SmallNativeAdView()
        }
    }
}

@Composable
fun CalendarDayItem(
    day: Int, 
    moodColor: Color, 
    moodScore: Int, 
    hasEntry: Boolean,
    isSmallScreen: Boolean, // 추가
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .aspectRatio(if (isSmallScreen) 0.9f else 0.8f) // 소형 화면에선 더 납작하게
            .clip(RoundedCornerShape(if (isSmallScreen) 8.dp else 12.dp))
            .background(if (moodScore > 0) moodColor.copy(alpha = 0.3f) else Color.Transparent)
            .border(
                width = if (moodScore > 0) 0.dp else 1.dp,
                color = if (moodScore > 0) Color.Transparent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(if (isSmallScreen) 8.dp else 12.dp)
            )
            .clickable(enabled = hasEntry) { onClick() }
            .padding(if (isSmallScreen) 2.dp else 4.dp)
    ) {
        Text(
            text = "$day",
            fontSize = if (isSmallScreen) 11.sp else 14.sp,
            color = if (moodScore > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (moodScore > 0) FontWeight.Bold else FontWeight.Normal
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // 감정 표시 (점)
        if (moodScore > 0) {
            Box(
                modifier = Modifier
                    .size(if (isSmallScreen) 6.dp else 10.dp)
                    .background(moodColor, CircleShape)
            )
        }
    }
}

// 감정 색상 (HomeScreen과 동일한 로직, 추후 공통화 가능)
fun getMoodColor(score: Int): Color {
    return when (score) {
        1 -> Color(0xFFE57373) // Very Dissatisfied
        2 -> Color(0xFFFFB74D) // Dissatisfied
        3 -> Color(0xFFFFD54F) // Neutral
        4 -> Color(0xFFAED581) // Satisfied
        5 -> Color(0xFF81C784) // Very Satisfied
        else -> Color.Transparent
    }
}
