package com.medina.juanantonio.watcher.data.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.medina.juanantonio.watcher.network.models.home.AlbumItemBean
import com.medina.juanantonio.watcher.network.models.home.HomePageBean
import com.medina.juanantonio.watcher.network.models.home.NavigationItemBean
import com.medina.juanantonio.watcher.network.models.player.EpisodeBean
import com.medina.juanantonio.watcher.network.models.player.VideoSuggestion
import com.medina.juanantonio.watcher.network.models.search.LeaderboardBean
import com.medina.juanantonio.watcher.network.models.search.SearchResultBean
import com.medina.juanantonio.watcher.shared.Constants.ImageURL.NavigationBackgroundURL
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity
data class Video(
    // Category Ids are null for non-video contents (Album Groups, People, Banners)
    val category: Int?,
    @PrimaryKey val contentId: Int,
    val imageUrl: String,
    val title: String
) : Parcelable {

    companion object {
        const val NAVIGATION_CATEGORY_ID = -6969
    }

    var episodeNumber: Int = 0

    var episodeCount: Int = 0

    var score: Double = 0.0

    var videoProgress: Long = 0L

    var lastWatchTime: Long = System.currentTimeMillis()

    @get:Ignore
    val isMovie: Boolean
        get() = categoryType == ItemCategory.MOVIE

    @get:Ignore
    val isAlbum: Boolean
        get() = categoryType == ItemCategory.ALBUM

    @get:Ignore
    val categoryType: ItemCategory
        get() = when (category) {
            NAVIGATION_CATEGORY_ID -> ItemCategory.NAVIGATION
            0 -> ItemCategory.MOVIE
            1 -> ItemCategory.SERIES
            else -> ItemCategory.ALBUM
        }

    @Ignore
    var isAlbumItem = false

    @Ignore
    var showScore = true
        get() = score != 0.0 && field

    @Ignore
    var enableDeveloperMode = false

    // Home Page item
    constructor(bean: HomePageBean.Content) : this(
        category = bean.category,
        contentId = if (bean.category == null) bean.getIdFromJumpAddress() else bean.id,
        imageUrl = bean.imageUrl,
        title = bean.title
    ) {
        episodeNumber = 0
        episodeCount = bean.resourceNum ?: 0
        score = bean.score
    }

    // Home Page Episode display item
    constructor(video: Video, bean: EpisodeBean, episodeCount: Int, score: Double) : this(
        category = video.category,
        contentId = video.contentId,
        imageUrl = video.imageUrl,
        title = video.title
    ) {
        showScore = false
        episodeNumber = bean.seriesNo
        this.episodeCount = episodeCount
        this.score = score
    }

    // Search Result item
    constructor(bean: SearchResultBean) : this(
        category = bean.domainType,
        contentId = bean.id,
        imageUrl = bean.coverVerticalUrl,
        title = bean.name
    )

    // Video Suggestion item
    constructor(videoSuggestion: VideoSuggestion) : this(
        category = videoSuggestion.category,
        contentId = videoSuggestion.id,
        imageUrl = videoSuggestion.coverVerticalUrl,
        title = videoSuggestion.name
    ) {
        score = videoSuggestion.score
    }

    // Leaderboard item
    constructor(bean: LeaderboardBean) : this(
        category = bean.domainType,
        contentId = bean.id,
        imageUrl = bean.cover,
        title = bean.title
    )

    // Album item
    constructor(bean: AlbumItemBean) : this(
        category = bean.domainType,
        contentId = bean.contentId.toInt(),
        imageUrl = bean.image,
        title = bean.name
    ) {
        isAlbumItem = true
    }

    // Navigation Items
    constructor(bean: NavigationItemBean) : this(
        category = NAVIGATION_CATEGORY_ID,
        contentId = bean.id,
        imageUrl = NavigationBackgroundURL,
        title = bean.name
    )

    /**
     * Deep copy Video object
     */
    fun createNew(
        episodeNumber: Int? = null,
        episodeCount: Int? = null,
        score: Double? = null,
        videoProgress: Long? = null,
        lastWatchTime: Long? = null
    ): Video {
        return copy().apply {
            this.episodeNumber = episodeNumber ?: this@Video.episodeNumber
            this.episodeCount = episodeCount ?: this@Video.episodeCount
            this.score = score ?: this@Video.score
            this.videoProgress = videoProgress ?: this@Video.videoProgress
            this.lastWatchTime = lastWatchTime ?: this@Video.lastWatchTime
        }
    }

    fun getSeriesTitleDescription(): Pair<String, String> {
        val titleSplit = title.trim().split(" ")
        val lastTwoWords = titleSplit.takeLast(2).joinToString(" ")

        return if (lastTwoWords.startsWith("Season", ignoreCase = true)) {
            val firstWords = titleSplit.dropLast(2).joinToString(" ")
            Pair(firstWords, lastTwoWords)
        } else Pair(title, "")
    }
}

enum class ItemCategory {
    NAVIGATION,
    MOVIE,
    SERIES,
    ALBUM
}