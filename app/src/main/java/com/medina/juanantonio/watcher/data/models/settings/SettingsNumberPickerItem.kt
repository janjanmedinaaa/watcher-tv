package com.medina.juanantonio.watcher.data.models.settings

import androidx.annotation.DrawableRes
import com.medina.juanantonio.watcher.R

data class SettingsNumberPickerItem(
    val title: String,
    val description: String?,
    @DrawableRes val icon: Int,
    val value: Int,
    val type: Type
): SettingsItem {

    override val viewType: Int
        get() = R.layout.item_settings_number_picker

    enum class Type {
        CAPTION_SIZE
    }
}