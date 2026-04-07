package com.ws.skelton.remind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ws.skelton.remind.ui.calendar.CalendarScreen
import com.ws.skelton.remind.ui.home.HomeScreen
import com.ws.skelton.remind.ui.stats.StatsScreen
import com.ws.skelton.remind.ui.theme.BackgroundCream
import com.ws.skelton.remind.ui.theme.CardYellow
import com.ws.skelton.remind.ui.theme.TextBrown
import com.ws.skelton.remind.ui.theme.ReMindTheme
import kotlinx.coroutines.delay
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.ui.res.stringResource
import com.ws.skelton.remind.R

// --- 컬러 정의 (삭제됨 - ui/theme/Color.kt 사용) ---

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // SplashScreen API 적용 (시스템 스플래시는 즉시 종료되도록 설정)
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        // Edge-to-Edge 활성화
        enableEdgeToEdge()
        
        setContent {
            val diaryViewModel: com.ws.skelton.remind.DiaryViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val fontIndex by diaryViewModel.selectedFontIndex.collectAsState()
            val fontSizeIndex by diaryViewModel.selectedFontSize.collectAsState()

            ReMindTheme(fontIndex = fontIndex, fontSizeIndex = fontSizeIndex) {
                MainScreen(diaryViewModel)
            }
        }
    }
}

@Composable
fun MainScreen(diaryViewModel: DiaryViewModel) {
    // 수동 스플래시 가시성 제어 상태
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2000) // 2초간 노출
        showSplash = false
    }

    if (showSplash) {
        SplashScreen()
    } else {
        ReMindApp(diaryViewModel)
    }
}

// --- SplashScreen() 복구 (모든 OS 가시성 보장) ---
@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCream),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 앱 로고 아이콘
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "App Logo",
                modifier = Modifier.size(120.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 앱 이름
            Text(
                text = "Re:Mind",
                color = TextBrown,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

@Composable
fun ReMindApp(diaryViewModel: DiaryViewModel) {
    val navController = rememberNavController()
    val isKeyboardVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.systemBars,
        bottomBar = {
            if (!isKeyboardVisible) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp)
                        .zIndex(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 일기 버튼
                    NavigationPill(
                        screen = Screen.Home,
                        isSelected = navController.currentBackStackEntryAsState().value?.destination?.hierarchy?.any { it.route == Screen.Home.route } == true,
                        onClick = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(20.dp)) // 버튼 사이 간격

                    // 달력 버튼
                    NavigationPill(
                        screen = Screen.Calendar,
                        isSelected = navController.currentBackStackEntryAsState().value?.destination?.hierarchy?.any { it.route == Screen.Calendar.route } == true,
                        onClick = {
                            navController.navigate(Screen.Calendar.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(diaryViewModel = diaryViewModel)
            }
            composable(Screen.Calendar.route) {
                CalendarScreen(
                    diaryViewModel = diaryViewModel,
                    onNavigateToStats = { navController.navigate(Screen.Stats.route) }
                )
            }
            composable(Screen.Stats.route) {
                StatsScreen(
                    diaryViewModel = diaryViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

// 네비게이션 화면 정의
sealed class Screen(val route: String, val labelResId: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", R.string.nav_home, Icons.Default.Create)
    object Calendar : Screen("calendar", R.string.nav_calendar, Icons.Default.DateRange)
    object Stats : Screen("stats", R.string.nav_stats, Icons.Rounded.BarChart)
}

@Composable
fun NavigationPill(
    screen: Screen,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) CardYellow else Color.White
    val contentColor = if (isSelected) TextBrown else Color.LightGray
    val elevation = if (isSelected) 8.dp else 4.dp
    
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50), // 완전한 타원형
        color = backgroundColor,
        shadowElevation = elevation,
        modifier = Modifier
            .height(56.dp)
            .width(64.dp) // 아이콘만 보이므로 너비를 축소
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = screen.icon, 
                contentDescription = stringResource(screen.labelResId),
                tint = contentColor,
                modifier = Modifier.size(26.dp) // 아이콘을 조금 더 키움
            )
        }
    }
}