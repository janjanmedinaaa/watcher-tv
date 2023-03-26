package com.medina.juanantonio.watcher.sources.content

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.tv.TvContract.BaseTvColumns
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.tvprovider.media.tv.PreviewChannel
import androidx.tvprovider.media.tv.PreviewChannelHelper
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat
import androidx.tvprovider.media.tv.WatchNextProgram
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.medina.juanantonio.watcher.MainActivity
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.data.manager.IDataStoreManager
import com.medina.juanantonio.watcher.data.models.video.Video
import com.medina.juanantonio.watcher.di.ApplicationScope
import com.medina.juanantonio.watcher.shared.utils.CoroutineDispatchers
import com.medina.juanantonio.watcher.shared.helpers.TVProviderHelper.findFirstWatchNextProgram
import com.medina.juanantonio.watcher.shared.helpers.TVProviderHelper.getChannelByInternalProviderId
import com.medina.juanantonio.watcher.shared.helpers.TVProviderHelper.getProgramIdsInChannel
import com.medina.juanantonio.watcher.sources.media.IMediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@SuppressLint("RestrictedApi")
@RequiresApi(Build.VERSION_CODES.O)
class TVProviderUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val coroutineScope: CoroutineScope,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val mediaRepository: IMediaRepository,
    private val contentRepository: IContentRepository,
    private val dataStoreManager: IDataStoreManager
) {

    companion object {
        const val PROGRAM_INFO_EXTRA = "PROGRAM_INFO_EXTRA"
        const val DEFAULT_CHANNEL_KEY = "DEFAULT_CHANNEL_KEY"
        const val LAST_CHANNEL_UPDATE_KEY = "LAST_CHANNEL_UPDATE_KEY"
    }

    private val previewChannelHelper = PreviewChannelHelper(context)

    private val reminderInterval: Long
        get() = TimeUnit.DAYS.toMillis(7)

    suspend fun addVideoToWatchNextRow(
        video: Video,
        duration: Int,
        watchNextType: Int = TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE,
    ) {
        val existingWatchNextProgram = findFirstWatchNextProgram(context) { cursor ->
            val idColumnIndex = cursor.getColumnIndex(BaseTvColumns._ID)
            (cursor.getLong(idColumnIndex) == video.contentId.toLong())
        }

        if (existingWatchNextProgram != null) {
            updateVideoFromWatchNextRow(video, duration, watchNextType)
            return
        }

        try {
            val program = video.toWatchNextProgram(watchNextType, duration) ?: return
            previewChannelHelper.publishWatchNextProgram(program)
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    private fun updateVideoFromWatchNextRow(
        video: Video,
        duration: Int,
        watchNextType: Int
    ) {
        val existingWatchNextProgram = findFirstWatchNextProgram(context) { cursor ->
            val idColumnIndex = cursor.getColumnIndex(BaseTvColumns._ID)
            (cursor.getLong(idColumnIndex) == video.contentId.toLong())
        } ?: return

        val program = WatchNextProgram.Builder(existingWatchNextProgram)
            .setWatchNextType(watchNextType)
            .setLastEngagementTimeUtcMillis(video.lastWatchTime)
            .setDurationMillis(duration)
            .setLastPlaybackPositionMillis(video.videoProgress.toInt())
            .apply {
                if (!video.isMovie) setEpisodeNumber(video.episodeNumber)
            }
            .build()

        try {
            previewChannelHelper.updateWatchNextProgram(program, existingWatchNextProgram.id)
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    fun removeVideoFromWatchNextRow(video: Video) {
        val existingWatchNextProgram = findFirstWatchNextProgram(context) { cursor ->
            val idColumnIndex = cursor.getColumnIndex(BaseTvColumns._ID)
            (cursor.getLong(idColumnIndex) == video.contentId.toLong())
        } ?: return

        try {
            context.contentResolver.delete(
                TvContractCompat.buildWatchNextProgramUri(existingWatchNextProgram.id),
                null,
                null
            )
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    fun fillDefaultChannel() {
        coroutineScope.launch(coroutineDispatchers.io) {
            if (!shouldUpdateDefaultChannel()) return@launch

            val defaultChannel =
                getChannelByInternalProviderId(previewChannelHelper, DEFAULT_CHANNEL_KEY)
                    ?: createDefaultChannel()

            getProgramIdsInChannel(context, defaultChannel.id).forEach {
                removeVideoFromChannel(it)
            }

            contentRepository.getSearchLeaderboard()?.videoList?.forEach {
                try {
                    val program = it.toPreviewProgram(defaultChannel.id) ?: return@forEach
                    previewChannelHelper.publishPreviewProgram(program)
                } catch (e: Exception) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            }

            dataStoreManager.putString(LAST_CHANNEL_UPDATE_KEY, "${System.currentTimeMillis()}")
        }
    }

    private fun createDefaultChannel(): PreviewChannel {
        val channel = PreviewChannel.Builder()
            .setDisplayName(context.getString(R.string.default_channel_title))
            .setLogo(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            .setAppLinkIntentUri(Uri.parse(context.getString(R.string.view_intent)))
            .setInternalProviderId(DEFAULT_CHANNEL_KEY)
            .build()

        try {
            previewChannelHelper.publishDefaultChannel(channel)
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }

        return channel
    }

    private fun removeVideoFromChannel(programId: Long) {
        try {
            context.contentResolver.delete(
                TvContractCompat.buildPreviewProgramUri(programId),
                null,
                null
            )
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    private suspend fun shouldUpdateDefaultChannel(): Boolean {
        val lastUpdate = dataStoreManager.getString(LAST_CHANNEL_UPDATE_KEY)
        if (lastUpdate.isBlank()) return true

        return System.currentTimeMillis() > lastUpdate.toLong() + reminderInterval
    }

    private suspend fun Video.toWatchNextProgram(
        watchNextType: Int,
        duration: Int
    ): WatchNextProgram? {
        val videoInfo = mediaRepository.getVideoDetails(this) ?: return null
        val (seriesTitle, _) = getSeriesTitleDescription()
        val launchIntentExtra = "$category;$contentId;$imageUrl;$title"
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(PROGRAM_INFO_EXTRA, launchIntentExtra)
        }

        return WatchNextProgram.Builder()
            .setWatchNextType(watchNextType)
            .setId(contentId.toLong())
            .setContentId("$contentId")
            .setType(
                if (isMovie) TvContractCompat.PreviewPrograms.TYPE_MOVIE
                else TvContractCompat.PreviewPrograms.TYPE_TV_EPISODE
            )
            .setTitle(seriesTitle)
            .setDescription(videoInfo.introduction)
            .setPosterArtUri(Uri.parse(videoInfo.coverHorizontalUrl))
            .setDurationMillis(duration)
            .setLastEngagementTimeUtcMillis(lastWatchTime)
            .setLastPlaybackPositionMillis(videoProgress.toInt())
            .setIntent(launchIntent)
            .build()
    }

    private suspend fun Video.toPreviewProgram(channelId: Long): PreviewProgram? {
        val videoInfo = mediaRepository.getVideo(contentId, category ?: 0) ?: return null
        val (seriesTitle, seasonTitle) = getSeriesTitleDescription()
        val launchIntentExtra = "$category;$contentId;$imageUrl;$title"
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(PROGRAM_INFO_EXTRA, launchIntentExtra)
        }
        val totalDuration = TimeUnit.SECONDS.toMillis(videoInfo.totalDuration.toLong()).toInt()

        return PreviewProgram.Builder()
            .setChannelId(channelId)
            .setId(contentId.toLong())
            .setContentId("$contentId")
            .setType(
                if (isMovie) TvContractCompat.PreviewPrograms.TYPE_MOVIE
                else TvContractCompat.PreviewPrograms.TYPE_TV_EPISODE
            )
            .setTitle(seriesTitle)
            .setDescription(videoInfo.introduction)
            .setDurationMillis(totalDuration)
            .setPosterArtUri(Uri.parse(videoInfo.coverHorizontalUrl))
            .setIntent(launchIntent)
            .apply {
                if (!isMovie) {
                    val seasonNumber = seasonTitle.split(" ").last().toIntOrNull()
                    setSeasonNumber(seasonNumber ?: 1)
                    setEpisodeNumber(1)
                }
            }
            .build()
    }
}