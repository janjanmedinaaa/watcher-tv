package com.medina.juanantonio.watcher.openai

import com.medina.juanantonio.watcher.openai.models.CompletionRequest
import com.medina.juanantonio.watcher.openai.models.CompletionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface OpenAIAPIService {

    @POST("v1/completions")
    suspend fun getCompletion(
        @Body request: CompletionRequest
    ): Response<CompletionResponse>
}