package com.medina.juanantonio.watcher.data.models.settings

import com.medina.juanantonio.watcher.R

data class SettingsSelectionItem(
    val title: String,
    val description: String?,
    val isSelected: Boolean,
    val key: String,
    val type: Type
): SettingsItem {

    override val viewType: Int
        get() = R.layout.item_settings_selection

    enum class Type {
        QUALITY,
        CAPTIONS,
        PLAYBACK_SPEED
    }
}
