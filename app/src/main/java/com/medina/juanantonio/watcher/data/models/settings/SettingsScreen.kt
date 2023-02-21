package com.medina.juanantonio.watcher.data.models.settings

import androidx.annotation.DrawableRes
import com.medina.juanantonio.watcher.R

data class SettingsScreen(
    val key: String,
    val title: String,
    val description: String?,
    @DrawableRes val icon: Int
): SettingsItem {

    override val viewType: Int
        get() = R.layout.item_settings_screen
}