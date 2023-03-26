package com.medina.juanantonio.watcher.openai.sources

import android.content.Context
import com.medina.juanantonio.watcher.openai.models.CompletionRequest
import com.medina.juanantonio.watcher.network.Result
import com.medina.juanantonio.watcher.network.wrapWithResult
import com.medina.juanantonio.watcher.openai.OpenAIAPIService
import com.medina.juanantonio.watcher.openai.models.CompletionResponse
import com.medina.juanantonio.watcher.shared.utils.CoroutineDispatchers
import com.medina.juanantonio.watcher.sources.BaseRemoteSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

class OpenAIRemoteSource(
    context: Context,
    private val apiService: OpenAIAPIService,
    private val dispatchers: CoroutineDispatchers
) : BaseRemoteSource(context), IOpenAIRemoteSource {

    override suspend fun getCompletions(
        request: CompletionRequest
    ): Result<CompletionResponse> {
        return try {
            val response = withContext(dispatchers.io) {
                apiService.getCompletion(request)
            }
            response.wrapWithResult()
        } catch (exception: CancellationException) {
            Result.Cancelled()
        } catch (exception: Exception) {
            getDefaultErrorResponse()
        }
    }
}

interface IOpenAIRemoteSource {
    suspend fun getCompletions(request: CompletionRequest): Result<CompletionResponse>
}