package com.medina.juanantonio.watcher.sources.content

import android.content.Context
import com.medina.juanantonio.watcher.network.ApiService
import com.medina.juanantonio.watcher.network.models.home.GetHomePageResponse
import com.medina.juanantonio.watcher.network.wrapWithResult
import kotlinx.coroutines.CancellationException
import com.medina.juanantonio.watcher.network.Result
import com.medina.juanantonio.watcher.network.models.home.GetAlbumDetailsResponse
import com.medina.juanantonio.watcher.network.models.search.GetSearchLeaderboardResponse
import com.medina.juanantonio.watcher.network.models.search.SearchByKeywordRequest
import com.medina.juanantonio.watcher.network.models.search.SearchByKeywordResponse
import com.medina.juanantonio.watcher.sources.BaseRemoteSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContentRemoteSource(
    context: Context,
    private val apiService: ApiService
) : BaseRemoteSource(context), IContentRemoteSource {

    override suspend fun getHomePage(page: Int): Result<GetHomePageResponse> {
        return try {
            val response = withContext(Dispatchers.IO) {
                apiService.getHomePage(page)
            }
            response.wrapWithResult()
        } catch (exception: CancellationException) {
            Result.Cancelled()
        } catch (exception: Exception) {
            getDefaultErrorResponse()
        }
    }

    override suspend fun getAlbumDetails(
        page: Int,
        size: Int,
        id: Int
    ): Result<GetAlbumDetailsResponse> {
        return try {
            val response = withContext(Dispatchers.IO) {
                apiService.getAlbumDetails(page, size, id)
            }
            response.wrapWithResult()
        } catch (exception: CancellationException) {
            Result.Cancelled()
        } catch (exception: Exception) {
            getDefaultErrorResponse()
        }
    }

    override suspend fun searchByKeyword(
        searchKeyword: String,
        size: Int,
        sort: String,
        searchType: String
    ): Result<SearchByKeywordResponse> {
        return try {
            val response = withContext(Dispatchers.IO) {
                apiService.searchByKeyword(
                    SearchByKeywordRequest(
                        searchKeyword,
                        size,
                        sort,
                        searchType
                    )
                )
            }
            response.wrapWithResult()
        } catch (exception: CancellationException) {
            Result.Cancelled()
        } catch (exception: Exception) {
            getDefaultErrorResponse()
        }
    }

    override suspend fun getSearchLeaderboard(): Result<GetSearchLeaderboardResponse> {
        return try {
            val response = withContext(Dispatchers.IO) {
                apiService.getSearchLeaderboard()
            }
            response.wrapWithResult()
        } catch (exception: CancellationException) {
            Result.Cancelled()
        } catch (exception: Exception) {
            getDefaultErrorResponse()
        }
    }
}

interface IContentRemoteSource {
    suspend fun getHomePage(page: Int): Result<GetHomePageResponse>
    suspend fun getAlbumDetails(
        page: Int = 0,
        size: Int = 50,
        id: Int
    ): Result<GetAlbumDetailsResponse>

    suspend fun searchByKeyword(
        searchKeyword: String,
        size: Int = 50,
        sort: String = "",
        searchType: String = ""
    ): Result<SearchByKeywordResponse>

    suspend fun getSearchLeaderboard(): Result<GetSearchLeaderboardResponse>
}