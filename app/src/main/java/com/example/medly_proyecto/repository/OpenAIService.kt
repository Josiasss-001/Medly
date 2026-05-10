package com.example.medly_proyecto.repository

import com.example.medly_proyecto.model.ChatRequest
import com.example.medly_proyecto.model.ChatResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenAIService {
    @POST("v1/chat/completions")
    suspend fun getCompletion(
        @Header("Authorization") apiKey: String,
        @Body request: ChatRequest
    ): ChatResponse

    companion object {
        private const val BASE_URL = "https://api.openai.com/"

        fun create(): OpenAIService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(OpenAIService::class.java)
        }
    }
}
