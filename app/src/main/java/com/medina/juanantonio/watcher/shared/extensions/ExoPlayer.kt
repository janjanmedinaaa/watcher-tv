package com.medina.juanantonio.watcher.shared.extensions

import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackParameters

var ExoPlayer.playbackSpeed: Float
    get() = playbackParameters.speed
    set(speed) {
        val pitch = playbackParameters.pitch
        playbackParameters = PlaybackParameters(speed, pitch)
    }