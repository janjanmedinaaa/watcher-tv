package com.medina.juanantonio.watcher.network

import com.medina.juanantonio.watcher.network.models.home.GetAlbumDetailsResponse
import com.medina.juanantonio.watcher.network.models.home.GetHomePageResponse
import com.medina.juanantonio.watcher.network.models.home.GetNavigationBarResponse
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
}