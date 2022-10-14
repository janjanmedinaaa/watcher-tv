package com.medina.juanantonio.watcher.github.sources

import android.content.Context
import android.util.Base64
import com.medina.juanantonio.watcher.BuildConfig
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.data.manager.IDataStoreManager
import com.medina.juanantonio.watcher.github.models.GetAccessTokenRequest
import com.medina.juanantonio.watcher.github.models.ReleaseBean
import com.medina.juanantonio.watcher.github.sources.IUpdateRepository.Companion.DEVELOPER_MODE_KEY
import com.medina.juanantonio.watcher.github.sources.IUpdateRepository.Companion.LAST_UPDATE_REMINDER_KEY
import com.medina.juanantonio.watcher.network.Result
import io.jsonwebtoken.Jwts
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.concurrent.TimeUnit

class UpdateRepository(
    private val context: Context,
    private val remoteSource: IGithubRemoteSource,
    private val dataStoreManager: IDataStoreManager
) : IUpdateRepository {

    override val reminderInterval: Long
        get() = TimeUnit.HOURS.toMillis(6)

    override suspend fun getLatestRelease(): ReleaseBean? {
        val apiKey = generateAPIKey()
        val installationsResult = remoteSource.getInstallations(apiKey)
        if (installationsResult !is Result.Success) return null

        val firstInstallationId = installationsResult.data?.firstOrNull()?.id ?: return null
        val requestModel = GetAccessTokenRequest(
            repository = "watcher-tv",
            permissions = GetAccessTokenRequest.Permission("read")
        )
        val accessTokenResult = remoteSource.getAccessToken(
            installationId = firstInstallationId,
            request = requestModel,
            apiKey = apiKey
        )

        if (accessTokenResult !is Result.Success) return null
        val releasesResult = remoteSource.getReleases(
            repositoryUrl = context.getString(R.string.repository_releases_url),
            apiKey = accessTokenResult.data?.token ?: return null
        )

        return releasesResult.data?.firstOrNull()?.apply {
            assets.map { it.setupApiKey(accessTokenResult.data.token) }
        }
    }

    private fun generateAPIKey(): String {
        val currentTime = System.currentTimeMillis() / 1000
        val claims = Jwts.claims().apply {
            put("iat", currentTime - 60)
            put("iss", BuildConfig.APP_ID)
            put("exp", currentTime + 100)
        }

        val encodedKey = Base64.decode(BuildConfig.PRIVATE_KEY, Base64.DEFAULT)
        val keySpec = PKCS8EncodedKeySpec(encodedKey)

        val keyFactory = KeyFactory.getInstance("RSA")
        val privateKey = keyFactory.generatePrivate(keySpec)

        return Jwts.builder().setClaims(claims).signWith(privateKey).compact()
    }

    override suspend fun shouldGetUpdate(): Boolean {
        val lastUpdateReminder = dataStoreManager.getString(LAST_UPDATE_REMINDER_KEY)
        if (lastUpdateReminder.isBlank()) return true

        return System.currentTimeMillis() > lastUpdateReminder.toLong() + reminderInterval
    }

    override suspend fun saveLastUpdateReminder() {
        val currentTime = "${System.currentTimeMillis()}"
        dataStoreManager.putString(LAST_UPDATE_REMINDER_KEY, currentTime)
    }

    override suspend fun enableDeveloperMode() {
        dataStoreManager.putBoolean(DEVELOPER_MODE_KEY, true)
    }

    override suspend fun isDeveloperMode(): Boolean {
        return dataStoreManager.getBoolean(DEVELOPER_MODE_KEY)
    }
}

interface IUpdateRepository {
    val reminderInterval: Long

    suspend fun getLatestRelease(): ReleaseBean?
    suspend fun shouldGetUpdate(): Boolean

    suspend fun saveLastUpdateReminder()
    suspend fun enableDeveloperMode()
    suspend fun isDeveloperMode(): Boolean

    companion object {
        const val LAST_UPDATE_REMINDER_KEY = "LAST_UPDATE_REMINDER_KEY"
        const val DEVELOPER_MODE_KEY = "DEVELOPER_MODE_KEY"
        const val DEVELOPER_KEYWORD = "Make me a Developer"
    }
}