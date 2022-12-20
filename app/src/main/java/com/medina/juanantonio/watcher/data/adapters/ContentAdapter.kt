package com.medina.juanantonio.watcher.data.adapters

import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.Presenter
import com.bumptech.glide.RequestManager
import com.medina.juanantonio.watcher.data.models.VideoGroup
import com.medina.juanantonio.watcher.data.presenters.NavigationCardPresenter
import com.medina.juanantonio.watcher.data.presenters.PersonCardPresenter
import com.medina.juanantonio.watcher.data.presenters.VideoCardPresenter

class ContentAdapter(private val glide: RequestManager) : ArrayObjectAdapter(
    ListRowPresenter().apply {
        shadowEnabled = false
        selectEffectEnabled = false
    }
) {

    fun addContent(videoGroup: List<VideoGroup>) {
        val videoCardPresenter = VideoCardPresenter(glide)
        val personCardPresenter = PersonCardPresenter(glide)
        val navigationCardPresenter = NavigationCardPresenter()

        videoGroup.forEach {
            when (it.contentType) {
                VideoGroup.ContentType.VIDEOS -> {
                    add(getListRow(it, videoCardPresenter))
                }
                VideoGroup.ContentType.ARTISTS -> {
                    add(getListRow(it, personCardPresenter))
                }
                VideoGroup.ContentType.NAVIGATION -> {
                    add(getListRow(it, navigationCardPresenter))
                }
            }
        }
    }

    fun addVideoGroup(videoGroup: VideoGroup, replace: Boolean, position: Int = 0) {
        val cardPresenter = when (videoGroup.contentType) {
            VideoGroup.ContentType.VIDEOS -> VideoCardPresenter(glide)
            VideoGroup.ContentType.ARTISTS -> PersonCardPresenter(glide)
            VideoGroup.ContentType.NAVIGATION -> NavigationCardPresenter()
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