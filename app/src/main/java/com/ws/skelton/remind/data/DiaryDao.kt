package com.ws.skelton.remind.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary_table ORDER BY date DESC")
    fun getAllDiaries(): Flow<List<DiaryEntity>> // 실시간 목록 관찰

    @Query("SELECT * FROM diary_table ORDER BY date DESC")
    suspend fun getAllDiariesSync(): List<DiaryEntity> // 백업용 동기 조회

    @Query("SELECT * FROM diary_table WHERE date = :date AND content = :content LIMIT 1")
    suspend fun findDiaryByDateAndContent(date: Long, content: String): DiaryEntity? // 중복 체크용

    @Insert
    suspend fun insertDiary(diary: DiaryEntity): Long // 저장된 ID 반환

    @Update
    suspend fun updateDiary(diary: DiaryEntity) // 수정 (AI 결과 저장용)

    @androidx.room.Delete
    suspend fun deleteDiary(diary: DiaryEntity) // 삭제
}
