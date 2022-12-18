package com.medina.juanantonio.watcher.github

import com.medina.juanantonio.watcher.github.models.AccessTokenBean
import com.medina.juanantonio.watcher.github.models.GetAccessTokenRequest
import com.medina.juanantonio.watcher.github.models.InstallationBean
import com.medina.juanantonio.watcher.github.models.ReleaseBean
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Url

interface GithubApiService {

    @GET("app/installations")
    suspend fun getInstallations(): Response<List<InstallationBean>>

    @POST("app/installations/{installationId}/access_tokens")
    suspend fun getAccessToken(
        @Path("installationId") installationId: Int,
        @Body request: GetAccessTokenRequest
    ): Response<AccessTokenBean>

    @GET
    suspend fun getReleases(
        @Url repository: String
    ): Response<List<ReleaseBean>>
}