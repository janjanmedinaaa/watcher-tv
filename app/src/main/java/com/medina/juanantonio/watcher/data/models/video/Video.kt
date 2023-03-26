package com.medina.juanantonio.watcher.data.models.video

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.medina.juanantonio.watcher.network.models.home.AlbumItemBean
import com.medina.juanantonio.watcher.network.models.home.HomePageBean
import com.medina.juanantonio.watcher.network.models.home.WatchHistoryBean
import com.medina.juanantonio.watcher.network.models.player.EpisodeBean
import com.medina.juanantonio.watcher.network.models.player.VideoSuggestion
import com.medina.juanantonio.watcher.network.models.search.LeaderboardBean
import com.medina.juanantonio.watcher.network.models.search.SearchResultBean
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

    var episodeNumber: Int = 0

    var episodeCount: Int = 0

    var score: Double = 0.0

    var videoProgress: Long = 0L

    var lastWatchTime: Long = System.currentTimeMillis()

    @Ignore
    var isHomeDisplay = false

    @Ignore
    var resourceStatus: HomePageBean.ResourceStatus? = null

    @get:Ignore
    val isMovie: Boolean
        get() = categoryType == ItemCategory.MOVIE

    @get:Ignore
    val isAlbum: Boolean
        get() = categoryType == ItemCategory.ALBUM

    @get:Ignore
    val categoryType: ItemCategory
        get() = when (category) {
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

    @Ignore
    var videoResourceId = -1

    @Ignore
    var year: String = ""

    @Ignore
    var onlineTime: String? = null

    // Home Page item
    constructor(bean: HomePageBean.Content) : this(
        category = bean.contentType.category,
        contentId = when (bean.contentType) {
            HomePageBean.ContentType.APP_URL,
            HomePageBean.ContentType.ALBUM -> bean.getIdFromJumpAddress()
            else -> bean.id
        },
        imageUrl = bean.imageUrl,
        title = bean.title
    ) {
        episodeNumber = 0
        episodeCount = bean.resourceNum ?: 0
        resourceStatus = bean.resourceStatus
        score = bean.score
        isHomeDisplay = true
        onlineTime = bean.onlineTime
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
        videoResourceId = bean.id
        this.episodeCount = episodeCount
        this.score = score
    }

    // Search Result item
    constructor(bean: SearchResultBean) : this(
        category = bean.domainType,
        contentId = bean.id,
        imageUrl = bean.coverVerticalUrl,
        title = bean.name
    ) {
        year = bean.releaseTime
    }

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
        isHomeDisplay = true
    }

    // Watch History Items
    constructor(bean: WatchHistoryBean) : this(
        category = bean.category,
        contentId = bean.contentId.toInt(),
        imageUrl = bean.verticalUrl,
        title = bean.contentTitle
    ) {
        episodeNumber = bean.episodeNo
        videoProgress = bean.progress * 1000L
        isHomeDisplay = true
    }

    // Liked Video Items
    constructor(bean: LikedVideo) : this(
        category = bean.category,
        contentId = bean.contentId,
        imageUrl = bean.imageUrl,
        title = bean.title
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
    MOVIE,
    SERIES,
    ALBUM
}