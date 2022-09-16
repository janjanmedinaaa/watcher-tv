package com.medina.juanantonio.watcher.di

import android.content.Context
import android.net.TrafficStats
import androidx.room.Room
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.database.WatcherDb
import com.medina.juanantonio.watcher.network.ApiService
import com.medina.juanantonio.watcher.sources.home.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class AppModule {

    @Provides
    @Singleton
    fun provideApiService(
        @ApplicationContext context: Context
    ): ApiService {
        val requiredHeaderInterceptor = Interceptor { chain ->
            val requestBuilder =
                chain.request()
                    .newBuilder()
                    .addHeader("lang", "en")
                    .addHeader("versioncode", "11")
                    .addHeader("clienttype", "ios_jike_default")

            chain.proceed(requestBuilder.build())
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(0, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .addInterceptor(requiredHeaderInterceptor)
            .addInterceptor { chain ->
                TrafficStats.setThreadStatsTag(10000)
                chain.proceed(chain.request())
            }
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
    fun provideHomePageRemoteSource(
        @ApplicationContext context: Context,
        apiService: ApiService
    ): IHomePageRemoteSource {
        return HomePageRemoteSource(context, apiService)
    }

    @Provides
    @Singleton
    fun provideHomePageRepository(
        remoteSource: IHomePageRemoteSource,
        database: IHomePageDatabase
    ): IHomePageRepository {
        return HomePageRepository(remoteSource, database)
    }

    @Provides
    @Singleton
    fun provideWatcherDb(@ApplicationContext context: Context): WatcherDb {
        return Room.databaseBuilder(context, WatcherDb::class.java, "watcher.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideHomePageDatabase(
        watcherDb: WatcherDb
    ): IHomePageDatabase {
        return HomePageDatabase(watcherDb)
    }
}