package com.medina.juanantonio.watcher.network.models.player

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class VideoSuggestion(
    val category: Int,
    val coverHorizontalUrl: String,
    val coverVerticalUrl: String,
    val id: Int,
    val name: String
): Parcelable