package com.medina.juanantonio.watcher.github.models

import com.medina.juanantonio.watcher.BuildConfig
import org.apache.maven.artifact.versioning.DefaultArtifactVersion

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

        val downloadUrl: String
            get() = url

        fun isAPK(): Boolean =
            content_type == "application/vnd.android.package-archive"
    }

    fun isForDownload(): Boolean =
        !draft && !prerelease

    fun isNewerVersion(): Boolean {
        val differentVersion = tag_name != BuildConfig.VERSION_NAME
        val tagNameVersion = DefaultArtifactVersion(tag_name)
        val versionName = DefaultArtifactVersion(BuildConfig.VERSION_NAME)

        return differentVersion && (tagNameVersion > versionName)
    }
}