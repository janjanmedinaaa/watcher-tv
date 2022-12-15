package com.medina.juanantonio.watcher.data.adapters

import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.Presenter
import com.bumptech.glide.RequestManager
import com.medina.juanantonio.watcher.data.models.VideoGroup
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

        videoGroup.forEach {
            when (it.contentType) {
                VideoGroup.ContentType.VIDEOS -> {
                    add(getListRow(it, videoCardPresenter))
                }
                VideoGroup.ContentType.PERSONS -> {
                    add(getListRow(it, personCardPresenter))
                }
            }
        }
    }

    fun addVideoGroupOnStart(videoGroup: VideoGroup, replace: Boolean) {
        val cardPresenter = VideoCardPresenter(glide)
        val listRow = getListRow(videoGroup, cardPresenter)
        if (replace) replace(0, listRow) else add(0, listRow)
    }

    private fun getListRow(videoGroup: VideoGroup, presenter: Presenter): ListRow {
        val listRowAdapter = ArrayObjectAdapter(presenter)
        listRowAdapter.addAll(0, videoGroup.videoList)
        val headerItem = HeaderItem(videoGroup.category)
        return ListRow(headerItem, listRowAdapter)
    }
}