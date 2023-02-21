package com.medina.juanantonio.watcher.sources.settings

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.data.models.settings.SettingsItem
import com.medina.juanantonio.watcher.data.models.settings.SettingsScreen
import com.medina.juanantonio.watcher.data.models.settings.SettingsSelectionItem
import com.medina.juanantonio.watcher.data.models.video.VideoMedia
import com.medina.juanantonio.watcher.shared.utils.Event
import com.medina.juanantonio.watcher.sources.media.IMediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaRepository: IMediaRepository
) {

    companion object {
        private const val SETTINGS_INITIAL_KEY = ""
        private const val SETTINGS_QUALITY_KEY = "settings_quality"
        private const val SETTINGS_CAPTIONS_KEY = "settings_captions"
        private const val SETTINGS_PLAYBACK_SPEED_KEY = "settings_playback_speed"
    }

    val videoMedia: VideoMedia?
        get() = mediaRepository.currentlyPlayingVideoMedia

    private val _selectedSelectionItem = MutableLiveData<Event<SettingsSelectionItem>>()
    val selectedSelectionItem: LiveData<Event<SettingsSelectionItem>>
        get() = _selectedSelectionItem

    var selectedLanguage = "en"
        private set
    var selectedPlaybackSpeed = "1.0"
        private set

    fun selectedSelectionItem(item: SettingsSelectionItem) {
        when (item.type) {
            SettingsSelectionItem.Type.CAPTIONS -> {
                selectedLanguage = item.key
            }
            SettingsSelectionItem.Type.PLAYBACK_SPEED -> {
                selectedPlaybackSpeed = item.key
            }
            else -> Unit
        }
        _selectedSelectionItem.value = Event(item)
    }

    fun getSettingsList(key: String): Pair<String, List<SettingsItem>> {
        return when (key) {
            SETTINGS_INITIAL_KEY -> getInitialSettingsScreen()
            SETTINGS_QUALITY_KEY -> getQualitySettingsList()
            SETTINGS_CAPTIONS_KEY -> getCaptionsSettingsList()
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