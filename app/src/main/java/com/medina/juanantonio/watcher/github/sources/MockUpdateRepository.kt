package com.medina.juanantonio.watcher.github.sources

import com.medina.juanantonio.watcher.BuildConfig
import com.medina.juanantonio.watcher.github.models.ReleaseBean
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import java.util.concurrent.TimeUnit

class MockUpdateRepository : IUpdateRepository {

    companion object {
        private var lastUpdateReminder = ""
        private var alreadyDownloadedUpdate = false

        private var showUpdate = true
    }

    override val reminderInterval: Long
        get() = TimeUnit.MINUTES.toMillis(5)

    override suspend fun getLatestRelease(): ReleaseBean? {
        if (alreadyDownloadedUpdate) return null
        alreadyDownloadedUpdate = true

        val version = DefaultArtifactVersion(BuildConfig.VERSION_NAME)
        val newVersion = "${version.majorVersion}.${version.minorVersion + 1}"

        return ReleaseBean(
            name = "Watcher TV v${newVersion}",
            assets = listOf(
                ReleaseBean.Asset(
                    id = 1,
                    content_type = "application/vnd.android.package-archive",
                    browser_download_url = "",
                    url = ""
                )
            ),
            draft = !showUpdate,
            prerelease = false,
            tag_name = newVersion,
            body = ""
        )
    }

    override suspend fun shouldGetUpdate(): Boolean {
        return if (lastUpdateReminder.isBlank()) true
        else System.currentTimeMillis() > lastUpdateReminder.toLong() + reminderInterval
    }

    override suspend fun saveLastUpdateReminder() {
        lastUpdateReminder = "${System.currentTimeMillis()}"
    }

    override suspend fun enableDeveloperMode() {
        IUpdateRepository.isDeveloperMode = !IUpdateRepository.isDeveloperMode
    }

    override suspend fun isDeveloperMode(): Boolean {
        return IUpdateRepository.isDeveloperMode
    }
}