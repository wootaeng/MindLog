package com.ws.skelton.remind.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ws.skelton.remind.DiaryViewModel
import com.ws.skelton.remind.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    diaryViewModel: DiaryViewModel,
    onBack: () -> Unit
) {
    var selectedPeriod by remember { mutableIntStateOf(7) } // Default: 7 days
    val topTags = remember(selectedPeriod, diaryViewModel.diaryList.collectAsState().value) {
        diaryViewModel.getTopTags(selectedPeriod)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar( // 중앙 정렬 TopAppBar로 변경
                title = { Text("마음 통계 📊", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    // 우측 밸런스를 위한 더미 공간
                    Spacer(modifier = Modifier.width(48.dp))
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp) // 전체 패딩 축소
        ) {
            // 기간 선택 탭 (TabRow 사용)
            val periods = listOf(7 to "주간", 30 to "월간")
            val selectedIndex = periods.indexOfFirst { it.first == selectedPeriod }

            TabRow(
                selectedTabIndex = selectedIndex,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                divider = {},
                indicator = { tabPositions ->
                    if (selectedIndex < tabPositions.size) {
                        val tabPosition = tabPositions[selectedIndex]
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .wrapContentSize(Alignment.BottomStart)
                                .offset(x = tabPosition.left)
                                .width(tabPosition.width)
                                .height(3.dp)
                                .background(PrimaryGreen)
                        )
                    }
                }
            ) {
                periods.forEach { (days, label) ->
                    Tab(
                        selected = selectedPeriod == days,
                        onClick = { selectedPeriod = days },
                        text = { Text(label, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp)) // 간격 축소 (32 -> 24)

            Text(
                text = "가장 많이 느낀 감정 TOP 5",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp)) // 간격 축소 (16 -> 12)

            if (topTags.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("아직 분석된 감정이 없어요.. 🌱", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp), // 간격 축소 (12 -> 8)
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(topTags) { index, (tag, count) ->
                        TagStatItem(index + 1, tag, count)
                    }
                }
            }
        }
    }
}

@Composable
fun TagStatItem(rank: Int, tag: String, count: Int) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp), // 패딩 조정 (16 -> 12)
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 순위 숫자
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (rank <= 3) PrimaryGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rank.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (rank <= 3) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 태그명 (요청에 따라 # 제거)
            Text(
                text = tag,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // 빈도수
            Text(
                text = "${count}회",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
