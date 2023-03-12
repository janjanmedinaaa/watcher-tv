package com.medina.juanantonio.watcher.features.settings.display

import androidx.lifecycle.ViewModel
import com.medina.juanantonio.watcher.data.models.settings.SettingsItem
import com.medina.juanantonio.watcher.data.models.settings.SettingsNumberPickerItem
import com.medina.juanantonio.watcher.data.models.settings.SettingsSelectionItem
import com.medina.juanantonio.watcher.sources.settings.SettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsDisplayViewModel @Inject constructor(
    private val settingsUseCase: SettingsUseCase
): ViewModel() {

    fun getSettingsList(key: String = ""): Pair<String, List<SettingsItem>> {
        return settingsUseCase.getSettingsList(key)
    }

    fun selectedSelectionItem(item: SettingsSelectionItem) {
        if (item.isSelected) return
        settingsUseCase.selectedSelectionItem(item)
    }

    fun selectedNumberPickerItem(item: SettingsNumberPickerItem) {
        settingsUseCase.selectedNumberPickerItem(item)
    }
}