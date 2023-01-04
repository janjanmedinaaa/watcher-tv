package com.medina.juanantonio.watcher.sources.media

import android.content.Context
import com.medina.juanantonio.watcher.network.ApiService
import com.medina.juanantonio.watcher.network.wrapWithResult
import kotlinx.coroutines.CancellationException
import com.medina.juanantonio.watcher.network.Result
import com.medina.juanantonio.watcher.network.models.player.GetVideoDetailsResponse
import com.medina.juanantonio.watcher.network.models.player.GetVideoResourceResponse
import com.medina.juanantonio.watcher.shared.utils.CoroutineDispatchers
import com.medina.juanantonio.watcher.sources.BaseRemoteSource
import kotlinx.coroutines.withContext

class MediaRemoteSource(
    context: Context,
    private val apiService: ApiService,
    private val dispatchers: CoroutineDispatchers
) : BaseRemoteSource(context), IMediaRemoteSource {

    override suspend fun getVideoDetails(id: Int, category: Int): Result<GetVideoDetailsResponse> {
        return try {
            val response = withContext(dispatchers.io) {
                apiService.getVideoDetails(id, category)
            }
            response.wrapWithResult()
        } catch (exception: CancellationException) {
            Result.Cancelled()
        } catch (exception: Exception) {
            getDefaultErrorResponse()
        }
    }

    override suspend fun getVideoResource(
        category: Int,
        contentId: Int,
        episodeId: Int,
        definition: String
    ): Result<GetVideoResourceResponse> {
        return try {
            val response = withContext(dispatchers.io) {
                apiService.getVideoResource(
                    category,
                    contentId,
                    episodeId,
                    definition
                )
            }
            response.wrapWithResult()
        } catch (exception: CancellationException) {
            Result.Cancelled()
        } catch (exception: Exception) {
            getDefaultErrorResponse()
        }
    }
}

interface IMediaRemoteSource {
    suspend fun getVideoDetails(id: Int, category: Int): Result<GetVideoDetailsResponse>
    suspend fun getVideoResource(
        category: Int,
        contentId: Int,
        episodeId: Int,
        definition: String
    ): Result<GetVideoResourceResponse>
}