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
import androidx.leanback.media.MediaPlayerAdapter
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.media.PlayerAdapter
import androidx.leanback.widget.Action
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.PlaybackControlsRow.*
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter
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

    var skipForwardAction = FastForwardAction(context)
        private set
    var skipBackwardAction = RewindAction(context)
        private set
    var skipNextAction = SkipNextAction(context)
        private set
    var skipPreviousAction = SkipPreviousAction(context)
        private set
    var closedCaptioningAction = ClosedCaptioningAction(context)
        private set

    private var onActionListener: (Action) -> Unit = {}

    override fun onCreatePrimaryActions(primaryActionsAdapter: ArrayObjectAdapter) {
        // super.onCreatePrimaryActions() will create the play / pause action.
        super.onCreatePrimaryActions(primaryActionsAdapter)

        // Add the rewind and fast forward actions following the play / pause action.
        primaryActionsAdapter.apply {
            add(skipPreviousAction)
            add(skipBackwardAction)
            add(skipForwardAction)
            add(skipNextAction)
            add(closedCaptioningAction)
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
    }

    fun setOnActionListener(listener: (Action) -> Unit) {
        onActionListener = listener
    }

    fun endVideo() {
        playerAdapter.seekTo(duration)
    }

    private fun notifyActionChanged(action: MultiAction) {
        var index: Int
        (controlsRow.primaryActionsAdapter as? ArrayObjectAdapter)?.let {
            index = it.indexOf(action)
            if (index >= 0) {
                it.notifyArrayItemRangeChanged(index, 1)
            }
        }
    }

    /** Skips backward 30 seconds.  */
    private fun skipBackward() {
        var newPosition: Long = currentPosition - THIRTY_SECONDS
        newPosition = newPosition.coerceAtLeast(0L)
        playerAdapter.seekTo(newPosition)
    }

    /** Skips forward 30 seconds.  */
    private fun skipForward() {
        var newPosition: Long = currentPosition + THIRTY_SECONDS
        newPosition = newPosition.coerceAtMost(duration)
        playerAdapter.seekTo(newPosition)
    }

    companion object {
        private val THIRTY_SECONDS = TimeUnit.SECONDS.toMillis(30)
    }
}
