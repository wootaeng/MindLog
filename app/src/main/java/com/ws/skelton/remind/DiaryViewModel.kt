package com.ws.skelton.remind

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ws.skelton.remind.data.AppDatabase
import com.ws.skelton.remind.data.DiaryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.OutputStreamWriter
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DiaryViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).diaryDao()
    private val prefs = application.getSharedPreferences("mindlog_settings", Context.MODE_PRIVATE)

    // 알림 설정 상태
    private val _isReminderEnabled = MutableStateFlow(prefs.getBoolean("reminder_enabled", false))
    val isReminderEnabled = _isReminderEnabled.asStateFlow()

    private val _reminderHour = MutableStateFlow(prefs.getInt("reminder_hour", 21))
    val reminderHour = _reminderHour.asStateFlow()

    private val _reminderMinute = MutableStateFlow(prefs.getInt("reminder_minute", 0))
    val reminderMinute = _reminderMinute.asStateFlow()

    // 폰트 설정 상태 (0: 기본, 1: 명조, 2: 고운돋움/고딕, 3: 필기체)
    private val _selectedFontIndex = MutableStateFlow(prefs.getInt("selected_font_index", 0))
    val selectedFontIndex = _selectedFontIndex.asStateFlow()

    // AI 분석 횟수 추적 (10회마다 전면 광고)
    private val _aiAnalysisCount = MutableStateFlow(prefs.getInt("ai_analysis_count", 0))
    val aiAnalysisCount = _aiAnalysisCount.asStateFlow()

    // 글꼴 크기 설정 상태 (0: 작게, 1: 보통, 2: 크게)
    private val _selectedFontSize = MutableStateFlow(prefs.getInt("selected_font_size", 1))
    val selectedFontSize = _selectedFontSize.asStateFlow()

    private val _shouldShowInterstitial = MutableStateFlow(false)
    val shouldShowInterstitial = _shouldShowInterstitial.asStateFlow()

    private val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // 검색 및 필터 상태
    private val _searchQuery = kotlinx.coroutines.flow.MutableStateFlow("")
    private val _selectedMood = kotlinx.coroutines.flow.MutableStateFlow<Int?>(null) // null이면 필터 없음
    private val _selectedTag = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    val selectedMood = _selectedMood.asStateFlow()
    val selectedTag = _selectedTag.asStateFlow()

    // DB 원본 데이터와 검색/필터 상태를 결합하여 최종 리스트 산출
    val diaryList = kotlinx.coroutines.flow.combine(
        dao.getAllDiaries(),
        _searchQuery,
        _selectedMood,
        _selectedTag
    ) { diaries, query, mood, tag ->
        diaries.filter { diary ->
            val matchesQuery = query.isBlank() || 
                diary.content.contains(query, ignoreCase = true) ||
                (diary.summary?.contains(query, ignoreCase = true) == true) ||
                diary.moodTags.contains(query, ignoreCase = true)
            
            val matchesMood = mood == null || diary.moodScore == mood
            
            val matchesTag = tag == null || diary.moodTags.contains(tag, ignoreCase = true)

            matchesQuery && matchesMood && matchesTag
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 상태 업데이트 함수들
    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun updateSelectedMood(mood: Int?) { _selectedMood.value = mood }
    fun updateSelectedTag(tag: String?) { _selectedTag.value = tag }

    // 현재 사용 중인 모든 태그 목록 가져오기 (필터 칩 생성용)
    val allTags = dao.getAllDiaries().map { diaries ->
        diaries.flatMap { it.moodTags.cleanTags() }.distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 일기 저장 및 AI 분석 함수
    fun addDiary(content: String) {
        viewModelScope.launch {
            // 1. 일단 일기 저장 (AI 분석 전 상태)
            val newDiary = DiaryEntity(
                content = content,
                date = System.currentTimeMillis(),
                analysisStatus = "ANALYZING"
            )
            val id = dao.insertDiary(newDiary)
            
            // 2. AI 분석 요청 실행
            performAnalysis(id.toInt(), content)
        }
    }

    // 분석 로직 공통 분리
    private suspend fun performAnalysis(diaryId: Int, content: String) {
        // 비동기 AI 분석 요청
        val aiResult = com.ws.skelton.remind.ai.GeminiHelper.analyzeDiary(content)

        // 결과 반영
        if (aiResult != null) {
            // DB에서 최신 상태를 불러와서 업데이트 (중간에 내용이 바뀌었을 가능성 대비)
            val latestDiary = dao.getAllDiaries().stateIn(viewModelScope).value.find { it.id == diaryId }
            if (latestDiary != null) {
                val updatedDiary = latestDiary.copy(
                    moodScore = aiResult.moodScore,
                    moodTags = aiResult.moodTags,
                    summary = aiResult.summary,
                    aiComment = aiResult.comment,
                    analysisStatus = "DONE"
                )
                dao.updateDiary(updatedDiary)
                
                // 분석 성공 시 카운트 증가
                incrementAiAnalysisCount()
            }
        } else {
            // 실패 시 상태만 변경
            val latestDiary = dao.getAllDiaries().stateIn(viewModelScope).value.find { it.id == diaryId }
            if (latestDiary != null) {
                dao.updateDiary(latestDiary.copy(analysisStatus = "ERROR"))
            }
        }
    }

    // 실패한 분석 재시도 함수
    fun retryAnalysis(diary: DiaryEntity) {
        viewModelScope.launch {
            dao.updateDiary(diary.copy(analysisStatus = "ANALYZING"))
            performAnalysis(diary.id, diary.content)
        }
    }

    // 삭제
    fun deleteDiary(diary: DiaryEntity) {
        viewModelScope.launch {
            dao.deleteDiary(diary)
        }
    }

    // 수정 (내용 변경 시 AI 재분석)
    fun updateDiary(diary: DiaryEntity, newContent: String) {
        // 내용이 전혀 바뀌지 않았다면 아무것도 하지 않음
        if (diary.content == newContent && diary.analysisStatus == "DONE") return

        viewModelScope.launch {
            // 1. 내용 업데이트 및 분석 상태 초기화
            val updatingDiary = diary.copy(
                content = newContent,
                analysisStatus = "ANALYZING",
                moodScore = 3,
                moodTags = "",
                summary = null,
                aiComment = null
            )
            dao.updateDiary(updatingDiary)

            // 2. AI 재분석 실행
            performAnalysis(diary.id, newContent)
        }
    }

    /**
     * 특정 기간(days) 동안의 감정 태그 TOP 5를 가져오는 함수
     */
    fun getTopTags(days: Int): List<Pair<String, Int>> {
        val diaries = diaryList.value
        if (diaries.isEmpty()) return emptyList()

        val cutoff = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
        
        return diaries
            .filter { it.date >= cutoff && it.moodTags.isNotBlank() }
            .flatMap { it.moodTags.cleanTags() }
            .groupBy { it }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
    }

    // 태그 정화 확장 함수 (["감정", "태그"] 형식 및 특수문자 대응)
    private fun String.cleanTags(): List<String> {
        return this.replace("[", "")
            .replace("]", "")
            .replace("\"", "")
            .replace("'", "")
            .replace(",", " ")
            .split(" ", "#")
            .filter { it.isNotBlank() }
            .map { it.trim() }
    }

    // --- 리마인더 관련 로직 ---

    fun updateReminderSettings(enabled: Boolean, hour: Int, minute: Int) {
        _isReminderEnabled.value = enabled
        _reminderHour.value = hour
        _reminderMinute.value = minute

        prefs.edit().apply {
            putBoolean("reminder_enabled", enabled)
            putInt("reminder_hour", hour)
            putInt("reminder_minute", minute)
            apply()
        }

        if (enabled) {
            scheduleReminder(hour, minute)
        } else {
            cancelReminder()
        }
    }

    fun updateFont(index: Int) {
        _selectedFontIndex.value = index
        prefs.edit().putInt("selected_font_index", index).apply()
    }

    fun updateFontSize(index: Int) {
        _selectedFontSize.value = index
        prefs.edit().putInt("selected_font_size", index).apply()
    }

    private fun incrementAiAnalysisCount() {
        val newCount = _aiAnalysisCount.value + 1
        _aiAnalysisCount.value = newCount
        prefs.edit().putInt("ai_analysis_count", newCount).apply()

        if (newCount >= 10) {
            _shouldShowInterstitial.value = true
        }
    }

    fun resetInterstitialFlag() {
        _shouldShowInterstitial.value = false
        _aiAnalysisCount.value = 0
        prefs.edit().putInt("ai_analysis_count", 0).apply()
    }

    private fun scheduleReminder(hour: Int, minute: Int) {
        val intent = Intent(getApplication(), ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            getApplication(),
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            
            // 이미 지난 시간이라면 다음날로 예약
            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    // 권한 없으면 일반 알람으로 후퇴
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            // 실패 시 일반 설정 시도
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    private fun cancelReminder() {
        val intent = Intent(getApplication(), ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            getApplication(),
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    alarmManager.cancel(pendingIntent)
    }

    /**
     * 데이터를 JSON으로 내보내는 함수
     * Exports diary data as a JSON file.
     */
    fun exportData(context: Context, uri: android.net.Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val diaries = dao.getAllDiariesSync() // Flow가 아닌 리스트 직접 반환 필요
                val gson = Gson()
                val json = gson.toJson(diaries)

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(json)
                    }
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, context.getString(R.string.msg_toast_export_success), android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, context.getString(R.string.msg_toast_export_failure), android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * JSON 파일에서 데이터를 불러와 DB에 추가하는 함수
     * Imports diary data from a JSON file.
     */
    fun importData(context: Context, uri: android.net.Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val json = inputStream.bufferedReader().readText()
                    val gson = Gson()
                    val type = object : TypeToken<List<DiaryEntity>>() {}.type
                    val importedDiaries: List<DiaryEntity> = gson.fromJson(json, type)

                    // 중복 체크 및 삽입 (날짜와 내용 기준)
                    importedDiaries.forEach { imported ->
                        val existing = dao.findDiaryByDateAndContent(imported.date, imported.content)
                        if (existing == null) {
                            // ID는 자동 생성이므로 제거하고 새 엔티티로 저장
                            dao.insertDiary(imported.copy(id = 0))
                        }
                    }
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, context.getString(R.string.msg_toast_import_success), android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, context.getString(R.string.msg_toast_import_failure), android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun exportToTxt(context: Context, uri: android.net.Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val diaries = dao.getAllDiariesSync()
                val sb = java.lang.StringBuilder()
                val sdf = SimpleDateFormat("yyyy-MM-dd (E)", Locale.KOREAN)
                
                sb.append("🌿 MindLog 일기 백업\n")
                sb.append("생성일: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}\n")
                sb.append("========================\n\n")

                diaries.forEach { diary ->
                    sb.append("[${sdf.format(Date(diary.date))}]\n")
                    sb.append("내용: ${diary.content}\n")
                    diary.summary?.let { sb.append("요약: $it\n") }
                    if (diary.moodTags.isNotBlank()) sb.append("태그: ${diary.moodTags}\n")
                    sb.append("기분 점수: ${diary.moodScore}/5\n")
                    diary.aiComment?.let { sb.append("AI 코멘트: $it\n") }
                    sb.append("\n------------------------\n\n")
                }

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(sb.toString())
                    }
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, context.getString(R.string.msg_toast_export_success), android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, context.getString(R.string.msg_toast_export_failure), android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 데이터를 PDF 파일로 내보내는 함수 (간이 구현)
     */
    fun exportToPdf(context: Context, uri: android.net.Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val diaries = dao.getAllDiariesSync()
                val pdfDocument = PdfDocument()
                val paint = Paint()
                val titlePaint = Paint().apply {
                    isFakeBoldText = true
                    textSize = 18f
                }
                val contentPaint = Paint().apply {
                    textSize = 12f
                }
                
                // 페이지당 5개의 항목씩 배치 (간단 예시)
                var pageNumber = 1
                var yPos = 50f
                var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                var page = pdfDocument.startPage(pageInfo)
                var canvas = page.canvas

                canvas.drawText("MindLog Export (PDF)", 40f, yPos, titlePaint)
                yPos += 40f

                diaries.forEachIndexed { index, diary ->
                    if (yPos > 750f) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        yPos = 50f
                    }

                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(diary.date))
                    canvas.drawText("[$dateStr] Score: ${diary.moodScore}", 40f, yPos, contentPaint)
                    yPos += 20f
                    
                    // 멀티라인 처리는 복잡하므로 여기서는 한 줄씩 간단히 (실제 상용구현은 Layout 사용 권장)
                    val lines = diary.content.chunked(50)
                    lines.take(3).forEach { line ->
                        canvas.drawText(line, 50f, yPos, contentPaint)
                        yPos += 15f
                    }
                    yPos += 20f
                }

                pdfDocument.finishPage(page)

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
                pdfDocument.close()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, context.getString(R.string.msg_toast_export_success), android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, context.getString(R.string.msg_toast_export_failure), android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
