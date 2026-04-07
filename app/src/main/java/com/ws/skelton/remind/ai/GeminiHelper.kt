package com.ws.skelton.remind.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class AiAnalysisResult(
    val summary: String,
    val moodTags: String,
    val moodScore: Int,
    val comment: String
)

object GeminiHelper {
    private val API_KEY = com.ws.skelton.remind.BuildConfig.GEMINI_API_KEY
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(GeminiApiService::class.java)

    suspend fun analyzeDiary(diaryContent: String): AiAnalysisResult? {
        if (API_KEY.isNullOrBlank() || API_KEY == "null") {
            Log.e("GeminiHelper", "API Key is missing! Check local.properties.")
            return null
        }
        
        return withContext(Dispatchers.IO) {
            try {
                // 1. 사용 가능한 모델 목록 조회 및 동적 필터링
                Log.d("GeminiHelper", "Checking available models...")
                val availableModels = try {
                    val response = apiService.listModels(API_KEY)
                    response.models.map { it.name to it.supportedGenerationMethods }.also { list ->
                        list.forEach { Log.d("GeminiHelper", "Available Model Found: ${it.first}") }
                    }
                } catch (e: Exception) {
                    Log.e("GeminiHelper", "Failed to list models dynamically", e)
                    null
                }

                // 2. 가용한 모델 후보군 (최신순/성능순, 2026-04 기준)
                val preferredModels = listOf(
                    "gemini-3.1-flash-lite-preview", // 최우선: Gemini 3.1 경량 최신 (Preview)
                    "gemini-3-flash-preview",        // Gemini 3.0 Flash (Preview)
                    "gemini-2.5-flash",              // 2.5 Stable — 짧은 지연 시간 대용량 작업
                    "gemini-2.5-flash-lite",         // 2.5 경량형 Stable — 가장 빠르고 저비용
                    "gemma-3-27b-it",                // [Fallback] 고성능 개방형 모델 (Gemma 3)
                    "gemma-3-12b-it"                 // [Fallback] 경량 고성능 모델
                )

                // 실제 API 리스트에 존재하는 모델만 순서대로 추출
                val modelsToTry = if (availableModels != null) {
                    preferredModels.filter { pref ->
                        availableModels.any { (name, methods) -> 
                            name.contains(pref) && methods.contains("generateContent") 
                        }
                    }
                } else {
                    preferredModels // API 조회 실패 시 하드코딩 리스트 사용
                }

                Log.d("GeminiHelper", "Final models to attempt: $modelsToTry")

                val prompt = """
                    You are a mental health assistant. Analyze the following diary entry.
                    
                    Diary: "$diaryContent"
                    
                    Response MUST be a raw JSON object (no markdown, no code blocks) with fields:
                    - summary: One sentence summary (Korean).
                    - moodTags: 2-3 hashtags (Korean).
                    - moodScore: 1 (Bad) to 5 (Good).
                    - comment: Warm, supportive comment (Korean, max 50 chars).
                """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(
                        GeminiRequest.Content(
                            parts = listOf(GeminiRequest.Content.Part(text = prompt))
                        )
                    )
                )

                var responseText: String? = null
                var successModel: String? = null

                for (modelName in modelsToTry) {
                    try {
                        Log.i("GeminiHelper", ">>> [Attempt] Trying model: $modelName")
                        val response = apiService.generateContent(modelName, API_KEY, request)
                        responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        
                        if (!responseText.isNullOrBlank()) {
                            successModel = modelName
                            Log.i("GeminiHelper", ">>> [Success] Done with model: $modelName")
                            break
                        } else {
                            Log.w("GeminiHelper", ">>> [Empty] Model $modelName returned empty")
                        }
                    } catch (e: Exception) {
                        if (e is retrofit2.HttpException) {
                            val code = e.code()
                            val errorBody = e.response()?.errorBody()?.string() ?: ""
                            
                            Log.e("GeminiHelper", ">>> [Error] $modelName failed ($code): $errorBody")
                            
                            if (code == 429 || code == 404) {
                                Log.i("GeminiHelper", ">>> [Retry] Quota or Not Found. Trying next model...")
                                continue 
                            } else {
                                Log.e("GeminiHelper", ">>> [Fatal] Stopping due to error $code")
                                throw e
                            }
                        } else {
                            Log.e("GeminiHelper", ">>> [Error] $modelName had generic fail", e)
                            continue
                        }
                    }
                }
                
                Log.d("GeminiHelper", "Final Selection: $successModel")
                
                if (responseText.isNullOrBlank()) {
                    Log.w("GeminiHelper", "All models failed or returned empty response")
                    return@withContext null
                }
                
                parseJson(responseText)
            } catch (e: Exception) {
                Log.e("GeminiHelper", "Critical error during Gemini analysis", e)
                null
            }
        }
    }

    private fun parseJson(text: String): AiAnalysisResult? {
        return try {
            // 마크다운 코드 블록 제거 (```json ... ```)
            val cleanJson = text.replace("```json", "").replace("```", "").trim()
            val jsonObject = JSONObject(cleanJson)

            AiAnalysisResult(
                summary = jsonObject.optString("summary", "분석 실패"),
                moodTags = jsonObject.optString("moodTags", "#모름"),
                moodScore = jsonObject.optInt("moodScore", 3),
                comment = jsonObject.optString("comment", "힘내세요!")
            )
        } catch (e: Exception) {
            Log.e("GeminiHelper", "JSON Parsing error: $text", e)
            null
        }
    }
}
