package com.medina.juanantonio.watcher.network.models.player

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Subtitle(
    val language: String,
    val languageAbbr: String,
    val subtitlingUrl: String,
    val translateType: Int
) : Parcelable