package com.medina.juanantonio.watcher.sources.settings

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.data.manager.IDataStoreManager
import com.medina.juanantonio.watcher.data.models.settings.SettingsItem
import com.medina.juanantonio.watcher.data.models.settings.SettingsNumberPickerItem
import com.medina.juanantonio.watcher.data.models.settings.SettingsScreen
import com.medina.juanantonio.watcher.data.models.settings.SettingsSelectionItem
import com.medina.juanantonio.watcher.data.models.video.VideoMedia
import com.medina.juanantonio.watcher.di.ApplicationScope
import com.medina.juanantonio.watcher.shared.utils.Event
import com.medina.juanantonio.watcher.sources.media.IMediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaRepository: IMediaRepository,
    private val dataStoreManager: IDataStoreManager,
    @ApplicationScope private val applicationScope: CoroutineScope
) {

    companion object {
        private const val SETTINGS_INITIAL_KEY = ""
        private const val SETTINGS_QUALITY_KEY = "settings_quality"
        private const val SETTINGS_CAPTIONS_KEY = "settings_captions"
        private const val SETTINGS_PLAYBACK_SPEED_KEY = "settings_playback_speed"
        private const val SETTINGS_CAPTIONS_LANGUAGE_KEY = "settings_captions_language"

        // DataStore Keys
        private const val SETTINGS_SELECTED_LANGUAGE_KEY = "SETTINGS_SELECTED_LANGUAGE_KEY"
        private const val SETTINGS_SELECTED_CAPTION_SIZE_KEY = "SETTINGS_SELECTED_CAPTION_SIZE_KEY"
    }

    init {
        applicationScope.launch {
            val savedLanguage = dataStoreManager.getString(SETTINGS_SELECTED_LANGUAGE_KEY)
            if (savedLanguage.isNotBlank()) selectedLanguage = savedLanguage
            val savedCaptionSize = dataStoreManager.getInt(SETTINGS_SELECTED_CAPTION_SIZE_KEY)
            if (savedCaptionSize != -1) selectedCaptionSize = savedCaptionSize
        }
    }

    val videoMedia: VideoMedia?
        get() = mediaRepository.currentlyPlayingVideoMedia

    private val _selectedSelectionItem = MutableLiveData<Event<SettingsSelectionItem>>()
    val selectedSelectionItem: LiveData<Event<SettingsSelectionItem>>
        get() = _selectedSelectionItem

    private val _selectedNumberPickerItem = MutableLiveData<Event<SettingsNumberPickerItem>>()
    val selectedNumberPickerItem: LiveData<Event<SettingsNumberPickerItem>>
        get() = _selectedNumberPickerItem

    var selectedLanguage = "en"
        private set
    var selectedPlaybackSpeed = "1.0"
        private set
    var selectedCaptionSize = 75
        private set

    fun selectedSelectionItem(item: SettingsSelectionItem) {
        when (item.type) {
            SettingsSelectionItem.Type.CAPTIONS -> {
                applicationScope.launch {
                    dataStoreManager.putString(SETTINGS_SELECTED_LANGUAGE_KEY, item.key)
                }
                selectedLanguage = item.key
            }
            SettingsSelectionItem.Type.PLAYBACK_SPEED -> {
                selectedPlaybackSpeed = item.key
            }
            else -> Unit
        }
        _selectedSelectionItem.value = Event(item)
    }

    fun selectedNumberPickerItem(item: SettingsNumberPickerItem) {
        when (item.type) {
            SettingsNumberPickerItem.Type.CAPTION_SIZE -> {
                applicationScope.launch {
                    dataStoreManager.putInt(SETTINGS_SELECTED_CAPTION_SIZE_KEY, item.value)
                }
                selectedCaptionSize = item.value
            }
        }
        _selectedNumberPickerItem.value = Event(item)
    }

    fun getSettingsList(key: String): Pair<String, List<SettingsItem>> {
        return when (key) {
            SETTINGS_INITIAL_KEY -> getInitialSettingsScreen()
            SETTINGS_QUALITY_KEY -> getQualitySettingsList()
            SETTINGS_CAPTIONS_KEY -> getCaptionsSettingsList()
            SETTINGS_CAPTIONS_LANGUAGE_KEY -> getCaptionsLanguageSettingsList()
            SETTINGS_PLAYBACK_SPEED_KEY -> getPlaybackSpeedSettingsList()
            else -> Pair("", emptyList())
        }
    }

    private fun getInitialSettingsScreen(): Pair<String, List<SettingsItem>> {
        val definitionDetails = videoMedia?.definitions?.firstOrNull {
            videoMedia?.currentDefinition == it.code
        }
        val captionDetails = videoMedia?.subtitles?.firstOrNull {
            selectedLanguage == it.languageAbbr
        }
        val settingsList = arrayListOf(
            SettingsScreen(
                key = SETTINGS_QUALITY_KEY,
                title = context.getString(R.string.settings_item_title_quality),
                description = definitionDetails?.description?.lowercase(),
                icon = R.drawable.ic_quality
            ),
            SettingsScreen(
                key = SETTINGS_CAPTIONS_KEY,
                title = context.getString(R.string.settings_item_title_captions),
                description = if (captionDetails?.language.isNullOrBlank()) "Off"
                else captionDetails?.language,
                icon = R.drawable.ic_closed_captions
            ),
            SettingsScreen(
                key = SETTINGS_PLAYBACK_SPEED_KEY,
                title = context.getString(R.string.settings_item_title_playback_speed),
                description = "${selectedPlaybackSpeed}x",
                icon = R.drawable.ic_speed_increase
            )
        )

        return Pair(context.getString(R.string.settings_title_initial), settingsList)
    }

    private fun getQualitySettingsList(): Pair<String, List<SettingsItem>> {
        val settingsList = arrayListOf<SettingsSelectionItem>().apply {
            val selectionItems = videoMedia?.definitions?.map {
                SettingsSelectionItem(
                    title = it.fullDescription.lowercase(),
                    description = null,
                    isSelected = it.code == videoMedia?.currentDefinition,
                    key = it.code.name,
                    type = SettingsSelectionItem.Type.QUALITY
                )
            } ?: emptyList()

            addAll(selectionItems)
        }

        return Pair(context.getString(R.string.settings_title_quality), settingsList)
    }

    private fun getCaptionsSettingsList(): Pair<String, List<SettingsItem>> {
        val captionDetails = videoMedia?.subtitles?.firstOrNull {
            selectedLanguage == it.languageAbbr
        }
        val settingsList = arrayListOf(
            SettingsNumberPickerItem(
                title = context.getString(R.string.settings_item_title_caption_size),
                description = context.getString(R.string.settings_item_description_caption_size),
                icon = R.drawable.ic_font_size,
                value = selectedCaptionSize,
                type = SettingsNumberPickerItem.Type.CAPTION_SIZE
            ),
            SettingsScreen(
                key = SETTINGS_CAPTIONS_LANGUAGE_KEY,
                title = context.getString(R.string.settings_item_title_language),
                description = if (captionDetails?.language.isNullOrBlank()) "Off"
                else captionDetails?.language,
                icon = R.drawable.ic_closed_captions
            ),
        )

        return Pair(context.getString(R.string.settings_title_captions_settings), settingsList)
    }

    private fun getCaptionsLanguageSettingsList(): Pair<String, List<SettingsItem>> {
        val settingsList = arrayListOf<SettingsSelectionItem>().apply {
            val captionsOffSelectionItem =
                SettingsSelectionItem(
                    title = "Off",
                    description = null,
                    isSelected = selectedLanguage.isEmpty() || videoMedia?.subtitles?.none {
                        selectedLanguage == it.languageAbbr
                    } == true,
                    key = "",
                    type = SettingsSelectionItem.Type.CAPTIONS
                )
            val selectionItems = videoMedia?.subtitles?.map {
                SettingsSelectionItem(
                    title = it.language,
                    description = null,
                    isSelected = selectedLanguage == it.languageAbbr,
                    key = it.languageAbbr,
                    type = SettingsSelectionItem.Type.CAPTIONS
                )
            } ?: emptyList()

            add(captionsOffSelectionItem)
            addAll(selectionItems)
        }

        return Pair(context.getString(R.string.settings_title_captions), settingsList)
    }

    private fun getPlaybackSpeedSettingsList(): Pair<String, List<SettingsItem>> {
        val settingsList = arrayListOf<SettingsSelectionItem>().apply {
            val selectionItems =
                context.resources.getStringArray(R.array.playback_speed).map {
                    SettingsSelectionItem(
                        title = it,
                        description = null,
                        isSelected = "${selectedPlaybackSpeed}x" == it,
                        key = it.dropLast(1),
                        type = SettingsSelectionItem.Type.PLAYBACK_SPEED
                    )
                }

            addAll(selectionItems)
        }

        return Pair(context.getString(R.string.settings_title_playback_speed), settingsList)
    }
}