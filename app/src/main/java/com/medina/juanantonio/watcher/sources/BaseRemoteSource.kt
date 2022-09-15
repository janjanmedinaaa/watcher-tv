package com.medina.juanantonio.watcher.sources

import com.medina.juanantonio.watcher.network.Result
import android.content.Context
import com.medina.juanantonio.watcher.R

open class BaseRemoteSource(private val context: Context) {
    fun <T> getDefaultErrorResponse(internetError: Boolean = false): Result<T> {
        return if (internetError) {
            Result.Error(-1, context.getString(R.string.no_internet_connection))
        } else Result.Error(-2, context.getString(R.string.something_went_wrong))
    }
}