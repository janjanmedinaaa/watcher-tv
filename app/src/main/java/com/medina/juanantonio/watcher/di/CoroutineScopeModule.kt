package com.medina.juanantonio.watcher.di

import com.medina.juanantonio.watcher.shared.utils.CoroutineDispatchers
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
class CoroutineScopeModule {

    @Singleton
    @Provides
    @ApplicationScope
    fun providesCoroutineScope(
        defaultDispatcher: CoroutineDispatchers
    ): CoroutineScope = CoroutineScope(SupervisorJob() + defaultDispatcher.default)

    @Provides
    @Singleton
    fun providesCoroutineDispatchers(): CoroutineDispatchers {
        return CoroutineDispatchers(
            main = Dispatchers.Main,
            io = Dispatchers.IO,
            default = Dispatchers.Default
        )
    }
}