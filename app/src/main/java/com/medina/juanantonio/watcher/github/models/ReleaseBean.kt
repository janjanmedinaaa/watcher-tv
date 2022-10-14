package com.medina.juanantonio.watcher.github.models

import com.medina.juanantonio.watcher.BuildConfig

data class ReleaseBean(
    val name: String,
    val assets: List<Asset>,
    private val draft: Boolean,
    private val prerelease: Boolean,
    private val tag_name: String
) {

    class Asset(
        val id: Int,
        private val content_type: String,
        private val browser_download_url: String, // This should work for public repositories
        private val url: String
    ) {

        var apiKey: String = ""
            private set

        val downloadUrl: String
            get() = url

        fun isAPK(): Boolean =
            content_type == "application/vnd.android.package-archive"

        fun setupApiKey(apiKey: String) {
            this.apiKey = apiKey
        }
    }

    fun isForDownload(): Boolean =
        !draft && !prerelease

    fun isNewerVersion(): Boolean =
        tag_name != BuildConfig.VERSION_NAME &&
            tag_name.toFloat() > BuildConfig.VERSION_NAME.toFloat()
}