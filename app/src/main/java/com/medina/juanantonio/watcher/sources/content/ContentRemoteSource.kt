package com.medina.juanantonio.watcher.sources.content

import android.content.Context
import com.medina.juanantonio.watcher.network.ApiService
import com.medina.juanantonio.watcher.network.models.home.GetHomePageResponse
import com.medina.juanantonio.watcher.network.wrapWithResult
import kotlinx.coroutines.CancellationException
import com.medina.juanantonio.watcher.network.Result
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
}

interface IContentRemoteSource {
    suspend fun getHomePage(page: Int): Result<GetHomePageResponse>
    suspend fun searchByKeyword(
        searchKeyword: String,
        size: Int = 50,
        sort: String = "",
        searchType: String = ""
    ): Result<SearchByKeywordResponse>
}