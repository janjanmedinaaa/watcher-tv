package com.medina.juanantonio.watcher.github.sources

import android.content.Context
import android.util.Base64
import com.medina.juanantonio.watcher.BuildConfig
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.github.models.GetAccessTokenRequest
import com.medina.juanantonio.watcher.github.models.ReleaseBean
import com.medina.juanantonio.watcher.network.Result
import io.jsonwebtoken.Jwts
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec

class GithubRepository(
    private val context: Context,
    private val remoteSource: IGithubRemoteSource
) : IGithubRepository {

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

        return releasesResult.data?.firstOrNull()
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
}

interface IGithubRepository {
    suspend fun getLatestRelease(): ReleaseBean?
}