package com.medina.juanantonio.watcher.data.providers

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.medina.juanantonio.watcher.sources.content.SearchProviderUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking

class SearchResultsProvider : ContentProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SearchResultsProviderEntryPoint {
        fun searchProviderUseCase(): SearchProviderUseCase
    }

    override fun onCreate() = true

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        if (uri.pathSegments.firstOrNull() != "search") return null
        val appContext = context?.applicationContext ?: return null
        val hiltEntryPoint =
            EntryPointAccessors.fromApplication<SearchResultsProviderEntryPoint>(appContext)
        val useCase = hiltEntryPoint.searchProviderUseCase()

        val result = selectionArgs?.firstOrNull()?.let { selector ->
            val query = if (selector.endsWith("movies")) {
                val index = selector.lastIndexOf(" ")
                selector.substring(0, index)
            } else selector

            runBlocking {
                val limit = uri.getQueryParameter("limit")?.toIntOrNull() ?: 5
                useCase.getSearchResults(query, limit)
            }
        }

        return result
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri =
        throw UnsupportedOperationException("Requested operation not supported")

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int =
        throw UnsupportedOperationException("Requested operation not supported")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int =
        throw UnsupportedOperationException("Requested operation not supported")

    /** We do not implement resolution of MIME type given a URI */
    override fun getType(uri: Uri): String? = null
}