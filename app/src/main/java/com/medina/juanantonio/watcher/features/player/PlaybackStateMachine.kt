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

import com.medina.juanantonio.watcher.data.models.video.VideoMedia

/**
 * Notifies listeners when the state of playback changes. The new state is supplied but if a
 * listener needs to know the previous state, they must maintain that previous state themselves.
 *
 * Note: *Do not mock!* Use FakePlaybackStateMachine instead when writing tests.
 */
interface PlaybackStateMachine {
    fun onStateChange(state: VideoPlaybackState)
}

/** Represents different states a video can have when it comes to playback. */
sealed class VideoPlaybackState {
    /** Triggers the state to load the video. */
    data class Load(val videoMedia: VideoMedia) : VideoPlaybackState()

    /** Loading has completed and the player can be prepared. */
    data class Prepare(val videoMedia: VideoMedia, val startPosition: Long) : VideoPlaybackState()

    /** The video has started playback such as playing after Prepare or resuming after Pause. */
    data class Play(val videoMedia: VideoMedia) : VideoPlaybackState()

    /** The video is currently paused. */
    data class Pause(val videoMedia: VideoMedia, val position: Long) : VideoPlaybackState()

    /** The video has ended. */
    data class End(val videoMedia: VideoMedia) : VideoPlaybackState()

    /** Something terribly wrong occurred. */
    data class Error(val videoMedia: VideoMedia, val exception: Exception) : VideoPlaybackState()
}
