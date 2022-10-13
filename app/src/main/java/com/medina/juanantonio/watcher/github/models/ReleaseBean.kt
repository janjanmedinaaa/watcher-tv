package com.medina.juanantonio.watcher.github.models

import com.medina.juanantonio.watcher.BuildConfig

data class ReleaseBean(
    val name: String,
    val assets: List<Assets>,
    private val draft: Boolean,
    private val prerelease: Boolean,
    private val tag_name: String
) {

    class Assets(
        val id: Int,
        private val content_type: String,
        private val browser_download_url: String
    ) {

        val downloadUrl: String
            get() = browser_download_url

        fun isAPK(): Boolean =
            content_type == "application/vnd.android.package-archive"
    }

    fun isForDownload(): Boolean =
        !draft && !prerelease

    fun isNewerVersion(): Boolean =
        tag_name != BuildConfig.VERSION_NAME &&
            tag_name.toFloat() > BuildConfig.VERSION_NAME.toFloat()
}