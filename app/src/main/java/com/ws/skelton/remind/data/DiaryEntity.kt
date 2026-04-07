package com.ws.skelton.remind.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_table")
data class DiaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,           // 일기 내용
    val date: Long,                // 날짜
    val moodScore: Int = 3,        // 감정 점수 (1~5)
    val moodTags: String = "",     // 감정 태그 (#행복 #뿌듯)
    val summary: String? = null,   // 한 줄 요약
    val aiComment: String? = null, // AI 조언
    val analysisStatus: String = "WAITING" // 분석 상태: WAITING, ANALYZING, DONE, ERROR
)
