package com.medina.juanantonio.watcher.data.models.settings

data class SettingsSelectionItem(
    val title: String,
    val description: String?,
    val isSelected: Boolean,
    val key: String,
    val type: Type
): SettingsItem {

    override val viewType: Int
        get() = 1

    enum class Type {
        QUALITY,
        CAPTIONS,
        PLAYBACK_SPEED
    }
}
