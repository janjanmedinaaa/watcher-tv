package com.medina.juanantonio.watcher.data.adapters

import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.Presenter
import com.bumptech.glide.RequestManager
import com.medina.juanantonio.watcher.data.models.video.VideoGroup
import com.medina.juanantonio.watcher.data.presenters.*

class ContentAdapter(private val glide: RequestManager) : ArrayObjectAdapter(
    ListRowPresenter().apply {
        shadowEnabled = false
        selectEffectEnabled = false
    }
) {

    fun addContent(videoGroup: List<VideoGroup>) {
        val videoCardPresenter = VideoCardPresenter(glide)
        val artistCardPresenter = ArtistCardPresenter(glide)
        val topContentCardPresenter = TopContentCardPresenter(glide)
        val collectionCardPresenter = CollectionCardPresenter(glide)
        val movieListCardPresenter = MovieListCardPresenter(glide)
        val comingSoonCardPresenter = ComingSoonCardPresenter(glide)
        val leaderboardCardPresenter = LeaderboardCardPresenter(glide)

        videoGroup.forEach {
            when (it.contentType) {
                VideoGroup.ContentType.VIDEOS -> {
                    add(getListRow(it, videoCardPresenter))
                }
                VideoGroup.ContentType.ARTISTS -> {
                    add(getListRow(it, artistCardPresenter))
                }
                VideoGroup.ContentType.TOP_CONTENT -> {
                    add(getListRow(it, topContentCardPresenter))
                }
                VideoGroup.ContentType.COLLECTION -> {
                    add(getListRow(it, collectionCardPresenter))
                }
                VideoGroup.ContentType.MOVIE_LIST -> {
                    add(getListRow(it, movieListCardPresenter))
                }
                VideoGroup.ContentType.COMING_SOON -> {
                    add(getListRow(it, comingSoonCardPresenter))
                }
                VideoGroup.ContentType.LEADERBOARD -> {
                    add(getListRow(it, leaderboardCardPresenter))
                }
            }
        }
    }

    fun addVideoGroup(videoGroup: VideoGroup, replace: Boolean, position: Int = 0) {
        val cardPresenter = when (videoGroup.contentType) {
            VideoGroup.ContentType.VIDEOS -> VideoCardPresenter(glide)
            VideoGroup.ContentType.ARTISTS -> ArtistCardPresenter(glide)
            VideoGroup.ContentType.TOP_CONTENT -> TopContentCardPresenter(glide)
            VideoGroup.ContentType.COLLECTION -> CollectionCardPresenter(glide)
            VideoGroup.ContentType.MOVIE_LIST -> MovieListCardPresenter(glide)
            VideoGroup.ContentType.COMING_SOON -> ComingSoonCardPresenter(glide)
            VideoGroup.ContentType.LEADERBOARD -> LeaderboardCardPresenter(glide)
        }

        val listRow = getListRow(videoGroup, cardPresenter)
        if (replace) replace(position, listRow) else add(position, listRow)
    }

    private fun getListRow(videoGroup: VideoGroup, presenter: Presenter): ListRow {
        val listRowAdapter = ArrayObjectAdapter(presenter)
        listRowAdapter.addAll(0, videoGroup.videoList)

        return if (videoGroup.category.isNotBlank()) {
            val headerItem = HeaderItem(videoGroup.category)
            ListRow(headerItem, listRowAdapter)
        } else ListRow(listRowAdapter)
    }
}