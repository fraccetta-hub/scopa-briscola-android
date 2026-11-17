package com.fraccetta.scopabriscola.data.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL

@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("html_url") val htmlUrl: String,
    val name: String = ""
)

data class UpdateInfo(val version: String, val downloadUrl: String)

sealed class UpdateResult {
    data class Available(val info: UpdateInfo) : UpdateResult()
    object UpToDate : UpdateResult()
    object NetworkError : UpdateResult()
}

object UpdateChecker {

    private const val GITHUB_OWNER = "fraccetta-hub"
    private const val GITHUB_REPO = "scopa-briscola-android"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun check(currentVersion: String): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest")
            val connection = url.openConnection()
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val response = connection.getInputStream().bufferedReader().readText()
            val release = json.decodeFromString<GithubRelease>(response)
            val remoteVersion = release.tagName.removePrefix("v")
            if (isNewer(remoteVersion, currentVersion)) {
                UpdateResult.Available(UpdateInfo(version = remoteVersion, downloadUrl = release.htmlUrl))
            } else UpdateResult.UpToDate
        } catch (_: Exception) {
            UpdateResult.NetworkError
        }
    }

    // Keep old function for HomeViewModel backward compat
    suspend fun checkForUpdate(currentVersion: String): UpdateInfo? =
        (check(currentVersion) as? UpdateResult.Available)?.info

    private fun isNewer(remote: String, current: String): Boolean {
        val r = remote.split(".").mapNotNull { it.toIntOrNull() }
        val c = current.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(r.size, c.size)) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv > cv) return true
            if (rv < cv) return false
        }
        return false
    }
}
