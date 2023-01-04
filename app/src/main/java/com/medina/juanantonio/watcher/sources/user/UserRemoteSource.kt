package com.medina.juanantonio.watcher.sources.user

import android.content.Context
import com.medina.juanantonio.watcher.network.ApiService
import com.medina.juanantonio.watcher.network.Result
import com.medina.juanantonio.watcher.network.models.auth.BasicResponse
import com.medina.juanantonio.watcher.network.models.auth.GetUserInfoResponse
import com.medina.juanantonio.watcher.network.models.home.GetWatchHistoryResponse
import com.medina.juanantonio.watcher.network.models.home.SaveWatchHistoryRequest
import com.medina.juanantonio.watcher.network.wrapWithResult
import com.medina.juanantonio.watcher.shared.utils.CoroutineDispatchers
import com.medina.juanantonio.watcher.sources.BaseRemoteSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

class UserRemoteSource(
    context: Context,
    private val apiService: ApiService,
    private val dispatchers: CoroutineDispatchers
) : BaseRemoteSource(context), IUserRemoteSource {

    override suspend fun getUserInfo(): Result<GetUserInfoResponse> {
        return try {
            val response = withContext(dispatchers.io) {
                apiService.getUserInfo()
            }
            response.wrapWithResult()
        } catch (exception: CancellationException) {
            Result.Cancelled()
        } catch (exception: Exception) {
            getDefaultErrorResponse()
        }
    }

    override suspend fun getWatchHistory(): Result<GetWatchHistoryResponse> {
        return try {
            val response = withContext(dispatchers.io) {
                apiService.getWatchHistory()
            }
            response.wrapWithResult()
        } catch (exception: CancellationException) {
            Result.Cancelled()
        } catch (exception: Exception) {
            getDefaultErrorResponse()
        }
    }

    override suspend fun saveWatchHistory(
        category: Int,
        contentId: Int,
        contentEpisodeId: Int,
        progress: Int,
        totalDuration: Int,
        timestamp: Long,
        seriesNo: Int?,
        episodeNo: Int,
        playTime: Int
    ): Result<BasicResponse> {
        val request = SaveWatchHistoryRequest(
            category,
            contentId,
            contentEpisodeId,
            progress,
            totalDuration,
            timestamp,
            playTime,
            seriesNo,
            episodeNo
        )

        return try {
            val response = withContext(dispatchers.io) {
                apiService.saveWatchHistory(listOf(request))
            }
            response.wrapWithResult()
        } catch (exception: CancellationException) {
            Result.Cancelled()
        } catch (exception: Exception) {
            getDefaultErrorResponse()
        }
    }
}

interface IUserRemoteSource {
    suspend fun getUserInfo(): Result<GetUserInfoResponse>
    suspend fun getWatchHistory(): Result<GetWatchHistoryResponse>
    suspend fun saveWatchHistory(
        category: Int,
        contentId: Int,
        contentEpisodeId: Int,
        progress: Int,
        totalDuration: Int,
        timestamp: Long,
        seriesNo: Int?,
        episodeNo: Int,
        playTime: Int = 1
    ): Result<BasicResponse>
}