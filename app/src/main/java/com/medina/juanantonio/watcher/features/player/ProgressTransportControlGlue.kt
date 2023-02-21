/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.medina.juanantonio.watcher.features.player

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import androidx.leanback.media.MediaPlayerAdapter
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.media.PlayerAdapter
import androidx.leanback.widget.Action
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.PlaybackControlsRow.*
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter
import com.medina.juanantonio.watcher.R
import java.util.concurrent.TimeUnit

/**
 * Custom [PlaybackTransportControlGlue] that exposes a callback when the progress is updated.
 *
 * The callback is triggered based on a progress interval defined in several ways depending on the
 * [PlayerAdapter].
 *
 * [LeanbackPlayerAdapter] example:
 * ```
 *     private val updateMillis = 16
 *     LeanbackPlayerAdapter(context, exoplayer, updateMillis)
 * ```
 *
 * [MediaPlayerAdapter] example:
 * ```
 *     object : MediaPlayerAdapter(context) {
 *         private val updateMillis = 16
 *         override fun getProgressUpdatingInterval(): Int {
 *             return updateMillis
 *         }
 *     }
 * ```
 */
class ProgressTransportControlGlue<T : PlayerAdapter>(
    context: Context,
    impl: T,
    private val updateProgress: (Long) -> Unit = {}
) : PlaybackTransportControlGlue<T>(context, impl) {

    val justStarted: Boolean
        get() = currentPosition < FIVE_SECONDS

    private var autoPlayedVideoCount = 0

    private var bedtimeModeEnabled = false

    val autoPlayVideos: Boolean
        get() = autoPlayedVideoCount <= MAX_VIDEO_AUTO_PLAYBACK || !bedtimeModeEnabled

    var skipNextAction = SkipNextAction(context)
        private set
    var skipPreviousAction = SkipPreviousAction(context)
        private set

    var settingsAction = CustomMultiAction(
        context,
        ACTION_SETTINGS,
        intArrayOf(R.drawable.ic_settings),
        intArrayOf(R.string.control_action_settings)
    )

    private var onActionListener: (Action) -> Unit = {}

    override fun onCreatePrimaryActions(primaryActionsAdapter: ArrayObjectAdapter) {
        // super.onCreatePrimaryActions() will create the play / pause action.
        super.onCreatePrimaryActions(primaryActionsAdapter)

        primaryActionsAdapter.apply {
            add(skipPreviousAction)
            add(skipNextAction)
            add(settingsAction)
        }
    }

    override fun onUpdateProgress() {
        super.onUpdateProgress()
        updateProgress(currentPosition)
    }

    override fun onActionClicked(action: Action) {
        // Primary actions are handled manually. The superclass handles default play/pause action.
        when (action) {
            skipPreviousAction,
            skipNextAction -> Unit
            else -> super.onActionClicked(action)
        }
        onActionListener.invoke(action)
        if (action is MultiAction) notifyActionChanged(action)

        // Reset auto play counter since the user is still watching
        resetAutoPlayedVideoCount()
    }

    fun setOnActionListener(listener: (Action) -> Unit) {
        onActionListener = listener
    }

    fun endVideo() {
        playerAdapter.seekTo(duration)
    }

    fun enableBedtimeMode(enable: Boolean) {
        bedtimeModeEnabled = enable
    }

    fun incrementAutoPlayedVideoCount() {
        autoPlayedVideoCount++
    }

    fun playAndResetVideoCount() {
        play()
        resetAutoPlayedVideoCount()
    }

    private fun resetAutoPlayedVideoCount() {
        autoPlayedVideoCount = 1
    }

    private fun notifyActionChanged(action: MultiAction) {
        var index: Int
        (controlsRow.secondaryActionsAdapter as? ArrayObjectAdapter)?.let {
            index = it.indexOf(action)
            if (index >= 0) {
                it.notifyArrayItemRangeChanged(index, 1)
            }
        }
    }

    companion object {
        // Custom Action IDs
        private const val ACTION_SETTINGS = 21

        private const val MAX_VIDEO_AUTO_PLAYBACK = 5

        private val FIVE_SECONDS = TimeUnit.SECONDS.toMillis(5)
    }

    class CustomMultiAction(
        context: Context,
        id: Int,
        icons: IntArray,
        labels: IntArray
    ) : MultiAction(id) {

        init {
            val res = context.resources
            val drawables = arrayOfNulls<Drawable>(icons.size)
            val labelStr = arrayOfNulls<String>(icons.size)
            for (i in icons.indices) {
                drawables[i] = ResourcesCompat.getDrawable(res, icons[i], null)
                labelStr[i] = res.getString(labels[i])
            }
            setDrawables(drawables)
            setLabels(labelStr)
        }
    }
}
