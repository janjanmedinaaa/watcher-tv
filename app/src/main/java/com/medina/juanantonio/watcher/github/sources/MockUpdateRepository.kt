package com.medina.juanantonio.watcher.github.sources

import com.medina.juanantonio.watcher.BuildConfig
import com.medina.juanantonio.watcher.github.models.ReleaseBean
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit

class MockUpdateRepository : IUpdateRepository {

    companion object {
        private var lastUpdateReminder = ""
        private var developerModeEnabled = false
    }

    override val reminderInterval: Long
        get() = TimeUnit.MINUTES.toMillis(5)

    override suspend fun getLatestRelease(): ReleaseBean {
        val currentVersion =
            BigDecimal(BuildConfig.VERSION_NAME)
                .setScale(2, RoundingMode.HALF_EVEN)
        val newVersion =
            currentVersion + BigDecimal(0.10).setScale(2, RoundingMode.HALF_EVEN)

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
            draft = false,
            prerelease = false,
            tag_name = "$newVersion"
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
        developerModeEnabled = !developerModeEnabled
    }

    override suspend fun isDeveloperMode(): Boolean {
        return developerModeEnabled
    }
}