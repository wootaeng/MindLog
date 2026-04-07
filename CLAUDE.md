# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

**MindLog (Re:Mind)** — 음성·텍스트 입력을 AI(Google Gemini)로 분석해 기분 점수, 태그, 조언을 제공하는 Android 다이어리 앱.
패키지: `com.ws.skelton.remind` | minSdk 25 (Android 7.0) | compileSdk/targetSdk 35

---

## 빌드 커맨드

```bash
./gradlew assembleDebug          # 디버그 APK 빌드
./gradlew assembleRelease        # 릴리즈 APK (R8 minify + 서명)
./gradlew bundleRelease          # Play Store용 AAB
./gradlew test                   # 유닛 테스트
./gradlew connectedAndroidTest   # 기기/에뮬레이터 인스트루멘테이션 테스트
./gradlew clean                  # 빌드 아티팩트 삭제
```

## 로컬 설정 (local.properties — 버전 관리 제외)

```
GEMINI_API_KEY=...
DEBUG_ADMOB_APP_ID=...
RELEASE_ADMOB_APP_ID=...
DEBUG_ADMOB_BANNER_ID=...   RELEASE_ADMOB_BANNER_ID=...
DEBUG_ADMOB_INTERSTITIAL_ID=...   RELEASE_ADMOB_INTERSTITIAL_ID=...
DEBUG_ADMOB_NATIVE_ID=...   RELEASE_ADMOB_NATIVE_ID=...
DEBUG_ADMOB_OPENING_ID=...   RELEASE_ADMOB_OPENING_ID=...
```

[app/build.gradle.kts](app/build.gradle.kts)의 `buildTypes` 블록이 빌드 타입별로 자동으로 올바른 광고 ID를 `BuildConfig`에 주입한다. 런타임 분기 불필요.

---

## 기술 스택

| 영역 | 기술 |
|---|---|
| 언어 | Kotlin 2.0.21 |
| UI | Jetpack Compose (Material3, BOM 2024.09.00) |
| 네비게이션 | Navigation Compose 2.8.2 (NavHost 단일 액티비티) |
| 상태 관리 | StateFlow + ViewModel (MVVM) |
| DB | Room 2.6.1 (KSP 코드 생성) |
| 네트워크 | Retrofit 2.11.0 + OkHttp 4.12.0 |
| AI | Google Gemini REST API (google-generativeai 0.9.0) |
| 광고 | AdMob (play-services-ads 23.6.0) |
| 음성 인식 | Android SpeechRecognizer (STT) |
| 직렬화 | Gson |
| 빌드 | AGP 8.11.2, Gradle Kotlin DSL, 버전 카탈로그 |

---

## 아키텍처 및 프로젝트 구조

### 레이어 구조 (Clean Architecture + MVVM)

```
Presentation  →  UI (Compose Screens / Components)
State         →  DiaryViewModel (StateFlow, Flow.combine)
Domain        →  ViewModel 내 비즈니스 로직 (별도 UseCase 계층 없음)
Data          →  Room DAO + GeminiHelper (AI) + SpeechUtils
```

### 패키지 구조

```
com.ws.skelton.remind/
├── MainActivity.kt          # NavHost, 단일 액티비티
├── MindLogApplication.kt    # App init, AdManager, ActivityLifecycleCallbacks
├── DiaryViewModel.kt        # 전체 앱 상태 관리 (단일 ViewModel)
├── ReminderReceiver.kt      # 알림 BroadcastReceiver
├── ai/
│   ├── GeminiApiService.kt  # Retrofit 인터페이스
│   └── GeminiHelper.kt      # AI 분석 오케스트레이션, 모델 폴백
├── data/
│   ├── AppDatabase.kt       # Room DB 싱글톤
│   ├── DiaryEntity.kt       # DB 엔티티 (analysisStatus 상태 머신)
│   └── DiaryDao.kt          # CRUD DAO (Flow 기반)
├── ui/
│   ├── home/HomeScreen.kt   # 메인 일기 작성 화면
│   ├── calendar/CalendarScreen.kt
│   ├── stats/StatsScreen.kt
│   ├── components/          # 재사용 컴포넌트
│   │   ├── DiaryItem.kt
│   │   ├── SettingsDialog.kt
│   │   ├── AdViews.kt
│   │   └── MoodGuideDialog.kt
│   └── theme/               # Color, Theme, Type
└── utils/
    ├── AdManager.kt         # AdMob 싱글톤
    └── SpeechUtils.kt       # STT 유틸
```

### 주요 데이터 흐름

```
사용자 입력
  → DiaryViewModel.addDiary()
  → Room INSERT (DiaryDao)
  → GeminiHelper.analyzeDiary() [IO 스레드, 모델 폴백 포함]
  → Room UPDATE (moodScore, moodTags, summary, aiComment, analysisStatus)
  → DiaryDao.getAllDiaries() Flow emit
  → UI 자동 갱신
```

**AI 모델 폴백 순서** (2026-04 기준): Gemini 3.1 Flash Lite Preview → Gemini 3 Flash Preview → Gemini 2.5 Flash → Gemini 2.5 Flash Lite → Gemma 3  
`analysisStatus` 상태 머신: `WAITING` → `ANALYZING` → `DONE` / `ERROR`

### 네비게이션

`MainActivity`의 `NavHost`에서 세 경로 관리: `"home"`, `"calendar"`, `"stats"`

---

## 코딩 컨벤션

### 네이밍

| 대상 | 규칙 | 예시 |
|---|---|---|
| 클래스 | PascalCase + 역할 접미사 | `DiaryViewModel`, `DiaryEntity`, `GeminiHelper` |
| Composable 함수 | PascalCase + Screen/Dialog | `HomeScreen`, `SettingsDialog` |
| private MutableStateFlow | `_` 접두사 | `_isReminderEnabled` |
| public StateFlow | clean name (asStateFlow) | `isReminderEnabled` |
| Boolean 상태 | `is` / `show` 접두사 | `isListening`, `showMoodGuide` |
| 콜백 파라미터 | `on` + 동작 | `onDelete`, `onEdit`, `onRetry` |
| 상수 | UPPER_SNAKE_CASE | `BASE_URL`, `INSTANCE` |
| DB 테이블/컬럼 | snake_case 소문자 | `"diary_table"` |

### StateFlow 패턴

```kotlin
// ViewModel 내 — private mutable / public immutable
private val _isReminderEnabled = MutableStateFlow(prefs.getBoolean("reminder_enabled", false))
val isReminderEnabled = _isReminderEnabled.asStateFlow()

// 파생 상태 — combine + stateIn
val diaryList = combine(dao.getAllDiaries(), _searchQuery, _selectedMood, _selectedTag) {
    diaries, query, mood, tag -> /* 필터링 */
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

### Composable 구조 원칙

- ViewModel은 최상위 Screen에서만 주입, 하위 컴포넌트에는 **데이터 + 콜백만** 전달
- 로컬 UI 전용 상태는 `remember { mutableStateOf(...) }` 사용
- Flow 수집: `collectAsState()`

```kotlin
@Composable
fun DiaryItem(
    diary: DiaryEntity,        // 불변 데이터
    onDelete: () -> Unit,      // 변경은 콜백으로
    onEdit: (String) -> Unit,
)
```

### 코루틴 / 비동기

- 모든 DB / 네트워크 작업은 `viewModelScope.launch` 또는 `suspend fun`
- 파일 I/O는 `Dispatchers.IO` 명시
- UI 업데이트가 필요한 백그라운드 작업 → `withContext(Dispatchers.Main)`

### 에러 처리

- 구체적인 예외 타입 명시 (`retrofit2.HttpException`)
- 로그 레벨 준수: `Log.d` (디버그) / `Log.i` (정보) / `Log.w` (경고) / `Log.e` (에러)
- 에러 로그 태그는 파일명 기준 (`"GeminiHelper"`)
- 복구 가능 오류(429, 404)는 폴백, 치명적 오류는 rethrow

### 주석 스타일

- 변수/함수 설명은 한국어 한 줄 주석
- KDoc에는 한/영 혼용 허용
- 디버그 로그는 `>>>` 마커 사용 (`Log.i("Tag", ">>> [Attempt] ...")`)

---

## 특별 주의사항

### 절대 금지

- **Mock 데이터 / 가짜 구현 사용 금지** — 실제 Room DB, 실제 Gemini API 호출 코드를 작성할 것
- **계정 정보 / API 키 커밋 금지** — `local.properties`에만 보관, `BuildConfig`를 통해 주입

### 권장사항

- **실제 API 호출** — GeminiApiService 인터페이스를 통한 실제 네트워크 호출
- **재사용 가능한 컴포넌트** — `ui/components/`에 신규 공통 컴포넌트 추가
- **성능 최적화** — Flow combine + `stateIn(WhileSubscribed(5000))` 패턴 유지

### 문제 해결 우선순위

1. **실제 동작하는 해결책** 우선 — 이론적 정합성보다 동작 여부
2. **기존 코드 패턴 분석** 후 일관성 유지 — 새 파일 작성 전 유사 기존 파일 먼저 참고
3. 새 의존성 추가 시 [gradle/libs.versions.toml](gradle/libs.versions.toml) 버전 카탈로그에 등록

---

## 의존성 관리

모든 버전은 [gradle/libs.versions.toml](gradle/libs.versions.toml)에서 중앙 관리. build.gradle.kts에 인라인 버전 기입 금지.  
Compose 관련 라이브러리는 BOM을 통해 버전 동기화.

## 다국어 지원

UI 문자열은 [res/values/strings.xml](app/src/main/res/values/strings.xml) (영어)와 [res/values-ko/strings.xml](app/src/main/res/values-ko/strings.xml) (한국어) 양쪽에 추가.

## ProGuard

릴리즈 빌드에 R8 minification 활성화. Gson 직렬화 대상 data class나 Retrofit 인터페이스 변경 시 [proguard-rules.pro](app/proguard-rules.pro) 업데이트 필요.
