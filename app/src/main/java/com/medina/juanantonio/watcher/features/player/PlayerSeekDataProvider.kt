package com.medina.juanantonio.watcher.features.player

import android.graphics.Bitmap
import androidx.leanback.widget.PlaybackSeekDataProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import wseemann.media.FFmpegMediaMetadataRetriever

// TODO: Not displaying Thumbnails
class PlayerSeekDataProvider() : PlaybackSeekDataProvider() {

    private var retriever = FFmpegMediaMetadataRetriever()
    private lateinit var mSeekPositions: LongArray
    private lateinit var viewModel: ViewModel
    private var videoUrl = ""

    constructor(
        videoUrl: String,
        interval: Long,
        viewModel: ViewModel
    ) : this() {
        retriever.setDataSource(videoUrl)
        val duration =
            retriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
                ?: 0L
        val size = ((duration / interval) + 1).toInt()
        val tempList = mutableListOf<Long>()
        for (i in 0 until size) {
            tempList.add(i * interval)
        }
        mSeekPositions = tempList.toLongArray()

        this.videoUrl = videoUrl
        this.viewModel = viewModel
    }

    override fun getSeekPositions(): LongArray {
        return mSeekPositions
    }

    override fun getThumbnail(index: Int, callback: ResultCallback?) {
        val position = seekPositions[index]

        viewModel.viewModelScope.launch {
            val thumbnail = getThumbnail(position)
            withContext(Dispatchers.Main) {
                callback?.onThumbnailLoaded(thumbnail, index)
            }
        }
    }

    private suspend fun getThumbnail(position: Long): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                retriever.setDataSource(videoUrl)
                val bitmap =
                    retriever.getFrameAtTime(
                        position * 1000,
                        FFmpegMediaMetadataRetriever.OPTION_CLOSEST
                    )
                bitmap
            } catch (e: Exception) {
                null
            }
        }
    }
}