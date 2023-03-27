package com.medina.juanantonio.watcher.openai.sources

import com.medina.juanantonio.watcher.openai.models.CompletionRequest
import com.medina.juanantonio.watcher.openai.models.CompletionResponse

class OpenAIRepository(
    private val remoteSource: IOpenAIRemoteSource
) : IOpenAIRepository {

    override suspend fun getCompletion(prompt: String, model: String): CompletionResponse? {
        val request = CompletionRequest(
            model = model,
            prompt = prompt,
            temperature = 0.8F,
            max_tokens = 256,
            top_p = 1F,
            frequency_penalty = 1F,
            presence_penalty = 1F
        )
        val result = remoteSource.getCompletions(request)

        return result.data
    }
}

interface IOpenAIRepository {
    suspend fun getCompletion(
        prompt: String,
        model: String = "text-davinci-003"
    ): CompletionResponse?
}