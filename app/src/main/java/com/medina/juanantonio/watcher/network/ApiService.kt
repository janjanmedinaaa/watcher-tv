package com.medina.juanantonio.watcher.network

import com.medina.juanantonio.watcher.network.models.auth.*
import com.medina.juanantonio.watcher.network.models.home.*
import com.medina.juanantonio.watcher.network.models.player.GetVideoDetailsResponse
import com.medina.juanantonio.watcher.network.models.player.GetVideoResourceResponse
import com.medina.juanantonio.watcher.network.models.search.GetSearchLeaderboardResponse
import com.medina.juanantonio.watcher.network.models.search.SearchByKeywordRequest
import com.medina.juanantonio.watcher.network.models.search.SearchByKeywordResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @GET("cms/app/homePage/navigationBar")
    suspend fun getNavigationBar(): Response<GetNavigationBarResponse>

    @GET("cms/app/homePage/getHome")
    suspend fun getHomePage(
        @Query("page") page: Int,
        @Query("navigationId") navigationId: Int?
    ): Response<GetHomePageResponse>

    @GET("cms/app/album/detail")
    suspend fun getAlbumDetails(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("id") id: Int,
    ): Response<GetAlbumDetailsResponse>

    @POST("cms/app/search/v1/searchWithKeyWord")
    suspend fun searchByKeyword(
        @Body request: SearchByKeywordRequest
    ): Response<SearchByKeywordResponse>

    @GET("cms/app/search/v1/searchLeaderboard")
    suspend fun getSearchLeaderboard(): Response<GetSearchLeaderboardResponse>

    @GET("cms/app/movieDrama/get")
    suspend fun getVideoDetails(
        @Query("id") id: Int,
        @Query("category") category: Int,
    ): Response<GetVideoDetailsResponse>

    @GET("cms/app/media/previewInfo")
    suspend fun getVideoResource(
        @Query("category") category: Int,
        @Query("contentId") contentId: Int,
        @Query("episodeId") episodeId: Int,
        @Query("definition") definition: String
    ): Response<GetVideoResourceResponse>

    @POST("auth/sendCaptcha")
    suspend fun getOTPForLogin(
        @Body request: GetOTPRequest
    ): Response<BasicResponse>

    @POST("auth/mobile/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("auth/logoutWithFcmToken")
    suspend fun logout(
        @Body request: LogoutRequest
    ): Response<BasicResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(): Response<RefreshTokenResponse>

    @POST("auth/userInfo")
    suspend fun getUserInfo(): Response<GetUserInfoResponse>

    @GET("user/behavior/app/findBatchWatchHistory")
    suspend fun getWatchHistory(): Response<GetWatchHistoryResponse>

    @POST("user/behavior/app/addWatchHistory")
    suspend fun saveWatchHistory(
        @Body request: List<SaveWatchHistoryRequest>
    ): Response<BasicResponse>

    @POST("user/behavior/app/delBatchWatchHistory")
    suspend fun deleteWatchHistory(
        @Body request: List<DeleteWatchHistoryRequest>
    ): Response<BasicResponse>
}