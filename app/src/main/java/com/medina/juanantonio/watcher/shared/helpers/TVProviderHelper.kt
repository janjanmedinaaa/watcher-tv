package com.medina.juanantonio.watcher.shared.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.media.tv.TvContract
import androidx.tvprovider.media.tv.PreviewChannel
import androidx.tvprovider.media.tv.PreviewChannelHelper
import androidx.tvprovider.media.tv.TvContractCompat
import androidx.tvprovider.media.tv.WatchNextProgram
import javax.inject.Singleton

@Singleton
@SuppressLint("RestrictedApi")
object TVProviderHelper {

    fun getChannelByInternalProviderId(
        previewChannelHelper: PreviewChannelHelper,
        internalProviderId: String
    ): PreviewChannel? {
        return previewChannelHelper.allChannels.find {
            internalProviderId == it.internalProviderId
        }
    }

    fun getProgramIdsInChannel(context: Context, channelId: Long): List<Long> {
        return context.contentResolver.query(
            TvContractCompat.buildPreviewProgramsUriForChannel(channelId),
            null,
            null,
            null,
            null
        )?.use {
            return getProgramIdsFromCursor(it)
        } ?: listOf()
    }

    private fun getProgramIdsFromCursor(cursor: Cursor): List<Long> {
        val programIdsInChannel = arrayListOf<Long>()
        val idColumnIndex = cursor.getColumnIndex(TvContract.BaseTvColumns._ID)

        while (cursor.moveToNext()) {
            programIdsInChannel.add(cursor.getLong(idColumnIndex))
        }

        return programIdsInChannel
    }

    fun findFirstWatchNextProgram(
        context: Context,
        predicate: (Cursor) -> Boolean
    ): WatchNextProgram? {
        val cursor = context.contentResolver.query(
            TvContractCompat.WatchNextPrograms.CONTENT_URI,
            WatchNextProgram.PROJECTION,
            null,
            null,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    if (predicate(cursor)) {
                        return WatchNextProgram.fromCursor(cursor)
                    }
                } while (it.moveToNext())
            }
        }
        return null
    }
}