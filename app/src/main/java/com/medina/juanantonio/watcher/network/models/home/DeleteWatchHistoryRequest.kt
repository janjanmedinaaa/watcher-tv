package com.medina.juanantonio.watcher.network.models.home

data class DeleteWatchHistoryRequest(
    val contentId: Int,
    val category: Int
)