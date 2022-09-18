package com.medina.juanantonio.watcher.data.adapters

import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import com.bumptech.glide.RequestManager
import com.medina.juanantonio.watcher.data.models.VideoGroup
import com.medina.juanantonio.watcher.data.presenters.VideoCardPresenter

class ContentAdapter(private val glide: RequestManager) : ArrayObjectAdapter(ListRowPresenter()) {

    fun addContent(videoGroup: List<VideoGroup>) {
        val cardPresenter = VideoCardPresenter(glide)
        videoGroup.forEach {
            val listRowAdapter = ArrayObjectAdapter(cardPresenter)
            listRowAdapter.addAll(0, it.videoList)
            val headerItem = HeaderItem(it.category)
            add(ListRow(headerItem, listRowAdapter))
        }
    }

    fun addVideoGroupOnStart(videoGroup: VideoGroup, replace: Boolean) {
        val cardPresenter = VideoCardPresenter(glide)
        val listRowAdapter = ArrayObjectAdapter(cardPresenter)
        listRowAdapter.addAll(0, videoGroup.videoList)
        val headerItem = HeaderItem(videoGroup.category)
        val listRow = ListRow(headerItem, listRowAdapter)
        if (replace) replace(0, listRow) else add(0, listRow)
    }
}