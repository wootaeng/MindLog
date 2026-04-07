package com.ws.skelton.remind.ai

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// Gemini REST API 요청 데이터 모델
data class GeminiRequest(
    val contents: List<Content>
) {
    data class Content(
        val parts: List<Part>
    ) {
        data class Part(
            val text: String
        )
    }
}

// Gemini REST API 응답 데이터 모델
data class GeminiResponse(
    val candidates: List<Candidate>?
) {
    data class Candidate(
        val content: Content?
    ) {
        data class Content(
            val parts: List<Part>?
        ) {
            data class Part(
                val text: String?
            )
        }
    }
}

interface GeminiApiService {
    @GET("v1beta/models")
    suspend fun listModels(
        @Query("key") apiKey: String
    ): GeminiModelsResponse

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

data class GeminiModelsResponse(
    val models: List<ModelInfo>
)

data class ModelInfo(
    val name: String,
    val displayName: String,
    val supportedGenerationMethods: List<String>
)
