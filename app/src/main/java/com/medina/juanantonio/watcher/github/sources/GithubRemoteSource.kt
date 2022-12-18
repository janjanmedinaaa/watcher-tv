package com.medina.juanantonio.watcher.github.sources

import android.content.Context
import com.medina.juanantonio.watcher.github.GithubApiService
import com.medina.juanantonio.watcher.github.models.AccessTokenBean
import com.medina.juanantonio.watcher.github.models.GetAccessTokenRequest
import com.medina.juanantonio.watcher.github.models.InstallationBean
import com.medina.juanantonio.watcher.github.models.ReleaseBean
import com.medina.juanantonio.watcher.network.Result
import com.medina.juanantonio.watcher.network.wrapWithResult
import com.medina.juanantonio.watcher.sources.BaseRemoteSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GithubRemoteSource(
    context: Context,
    private val apiService: GithubApiService
) : BaseRemoteSource(context), IGithubRemoteSource {

    override suspend fun getInstallations(): Result<List<InstallationBean>> {
        return try {
            val response = withContext(Dispatchers.IO) {
                apiService.getInstallations()
            }
            response.wrapWithResult()
        } catch (exception: CancellationException) {
            Result.Cancelled()
        } catch (exception: Exception) {
            getDefaultErrorResponse()
        }
    }

    override suspend fun getAccessToken(
        installationId: Int,
        request: GetAccessTokenRequest
    ): Result<AccessTokenBean> {
        return try {
            val response = withContext(Dispatchers.IO) {
                apiService.getAccessToken(installationId, request)
            }
            response.wrapWithResult()
        } catch (exception: CancellationException) {
            Result.Cancelled()
        } catch (exception: Exception) {
            getDefaultErrorResponse()
        }
    }

    override suspend fun getReleases(repositoryUrl: String): Result<List<ReleaseBean>> {
        return try {
            val response = withContext(Dispatchers.IO) {
                apiService.getReleases(repositoryUrl)
            }
            response.wrapWithResult()
        } catch (exception: CancellationException) {
            Result.Cancelled()
        } catch (exception: Exception) {
            getDefaultErrorResponse()
        }
    }
}

interface IGithubRemoteSource {
    suspend fun getInstallations(): Result<List<InstallationBean>>
    suspend fun getAccessToken(
        installationId: Int,
        request: GetAccessTokenRequest
    ): Result<AccessTokenBean>

    suspend fun getReleases(repositoryUrl: String): Result<List<ReleaseBean>>
}