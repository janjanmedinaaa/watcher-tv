package com.medina.juanantonio.watcher.di

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.medina.juanantonio.watcher.BuildConfig
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.data.manager.DataStoreManager
import com.medina.juanantonio.watcher.data.manager.IDataStoreManager
import com.medina.juanantonio.watcher.data.manager.downloader.DownloadManager
import com.medina.juanantonio.watcher.data.manager.downloader.IDownloadManager
import com.medina.juanantonio.watcher.data.manager.downloader.MockDownloadManager
import com.medina.juanantonio.watcher.database.WatcherDb
import com.medina.juanantonio.watcher.github.GithubApiService
import com.medina.juanantonio.watcher.github.sources.*
import com.medina.juanantonio.watcher.network.ApiService
import com.medina.juanantonio.watcher.shared.utils.CoroutineDispatchers
import com.medina.juanantonio.watcher.sources.auth.*
import com.medina.juanantonio.watcher.sources.auth.IAuthRepository.Companion.AUTH_TOKEN
import com.medina.juanantonio.watcher.sources.content.*
import com.medina.juanantonio.watcher.sources.content.IContentRepository.Companion.API_HEADERS
import com.medina.juanantonio.watcher.sources.media.*
import com.medina.juanantonio.watcher.sources.user.IUserRemoteSource
import com.medina.juanantonio.watcher.sources.user.IUserRepository
import com.medina.juanantonio.watcher.sources.user.UserRemoteSource
import com.medina.juanantonio.watcher.sources.user.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class AppModule {

    @SuppressLint("HardwareIds")
    @Provides
    @Singleton
    fun provideApiService(
        @ApplicationContext context: Context,
        dataStoreManager: IDataStoreManager
    ): ApiService {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val deviceId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )

        val requiredHeaderInterceptor = Interceptor { chain ->
            val requestBuilder =
                chain.request()
                    .newBuilder()
                    .addHeader("deviceid", deviceId)
                    .also {
                        runBlocking {
                            val requiredHeaders = dataStoreManager.getString(API_HEADERS)
                            if (requiredHeaders.isNotBlank()) {
                                val headerType = object : TypeToken<Map<String, String>>() {}.type
                                val headersMap = Gson().fromJson<Map<String, String>>(
                                    requiredHeaders,
                                    headerType
                                )
                                headersMap.entries.forEach { (header, value) ->
                                    it.addHeader(header, value)
                                }
                            }

                            val authToken = dataStoreManager.getString(AUTH_TOKEN)
                            if (authToken.isNotBlank()) {
                                it.addHeader("token", authToken)
                            }
                        }
                    }

            chain.proceed(requestBuilder.build())
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.MINUTES)
            .writeTimeout(20, TimeUnit.MINUTES)
            .readTimeout(20, TimeUnit.MINUTES)
            .addInterceptor(requiredHeaderInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(context.getString(R.string.loklok_api_base_url))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideGithubApiService(
        @ApplicationContext context: Context
    ): GithubApiService {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val requiredHeaderInterceptor = Interceptor { chain ->
            val requestBuilder =
                chain.request()
                    .newBuilder()
                    .addHeader("Accept", "application/vnd.github+json")
                    .also {
                        val accessToken = UpdateRepository.temporaryAccessToken
                        it.addHeader("Authorization", "Bearer $accessToken")
                    }

            chain.proceed(requestBuilder.build())
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(requiredHeaderInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(context.getString(R.string.github_api_base_url))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GithubApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideWatcherDb(@ApplicationContext context: Context): WatcherDb {
        return Room.databaseBuilder(context, WatcherDb::class.java, "watcher.db")
            .fallbackToDestructiveMigration()
            .addMigrations(WatcherDb.MIGRATION_2_3)
            .build()
    }

    @Provides
    @Singleton
    fun provideVideoDatabase(watcherDb: WatcherDb): IVideoDatabase {
        return VideoDatabase(watcherDb)
    }

    @Provides
    @Singleton
    fun provideContentRemoteSource(
        @ApplicationContext context: Context,
        apiService: ApiService,
        dispatchers: CoroutineDispatchers
    ): IContentRemoteSource {
        return ContentRemoteSource(context, apiService, dispatchers)
    }

    @Provides
    @Singleton
    fun provideContentRepository(
        @ApplicationContext context: Context,
        remoteSource: IContentRemoteSource,
        dataStoreManager: IDataStoreManager,
        likedVideoUseCase: LikedVideoUseCase
    ): IContentRepository {
        return ContentRepository(context, remoteSource, dataStoreManager, likedVideoUseCase)
    }

    @Provides
    @Singleton
    fun provideMediaRemoteSource(
        @ApplicationContext context: Context,
        apiService: ApiService,
        dispatchers: CoroutineDispatchers
    ): IMediaRemoteSource {
        return MediaRemoteSource(context, apiService, dispatchers)
    }

    @Provides
    @Singleton
    fun provideMediaRepository(
        @ApplicationContext context: Context,
        remoteSource: IMediaRemoteSource,
        likedVideoUseCase: LikedVideoUseCase
    ): IMediaRepository {
        return MediaRepository(context, remoteSource, likedVideoUseCase)
    }

    @Provides
    @Singleton
    fun provideGithubRemoteSource(
        @ApplicationContext context: Context,
        apiService: GithubApiService,
        dispatchers: CoroutineDispatchers
    ): IGithubRemoteSource {
        return GithubRemoteSource(context, apiService, dispatchers)
    }

    @Provides
    @Singleton
    fun provideDataStoreManager(
        @ApplicationContext context: Context
    ): IDataStoreManager {
        return DataStoreManager(context)
    }

    @Provides
    @Singleton
    fun provideUpdateRepository(
        @ApplicationContext context: Context,
        remoteSource: IGithubRemoteSource,
        dataStoreManager: IDataStoreManager
    ): IUpdateRepository {
        return if (BuildConfig.DEBUG) MockUpdateRepository()
        else UpdateRepository(context, remoteSource, dataStoreManager)
    }

    @Provides
    @Singleton
    fun provideAuthRemoteSource(
        @ApplicationContext context: Context,
        apiService: ApiService,
        dispatchers: CoroutineDispatchers
    ): IAuthRemoteSource {
        return AuthRemoteSource(context, apiService, dispatchers)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        @ApplicationContext context: Context,
        remoteSource: IAuthRemoteSource,
        dataStoreManager: IDataStoreManager
    ): IAuthRepository {
        return AuthRepository(context, remoteSource, dataStoreManager)
    }

    @Provides
    @Singleton
    fun provideUserRemoteSource(
        @ApplicationContext context: Context,
        apiService: ApiService,
        dispatchers: CoroutineDispatchers
    ): IUserRemoteSource {
        return UserRemoteSource(context, apiService, dispatchers)
    }

    @Provides
    @Singleton
    fun provideUserRepository(remoteSource: IUserRemoteSource): IUserRepository {
        return UserRepository(remoteSource)
    }

    @Provides
    @Singleton
    fun provideUserDatabase(watcherDb: WatcherDb): IUserDatabase {
        return UserDatabase(watcherDb)
    }

    @Provides
    @Singleton
    fun provideDownloadManager(
        @ApplicationContext context: Context,
        @ApplicationScope coroutineScope: CoroutineScope
    ): IDownloadManager {
        return if (BuildConfig.DEBUG) MockDownloadManager(coroutineScope)
        else DownloadManager(context, coroutineScope)
    }

    @Provides
    @Singleton
    fun provideLikedVideoDatabase(watcherDb: WatcherDb): ILikedVideoDatabase {
        return LikedVideoDatabase(watcherDb)
    }
}