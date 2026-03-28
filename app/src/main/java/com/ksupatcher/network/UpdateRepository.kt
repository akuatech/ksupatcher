package com.ksupatcher.network

import com.ksupatcher.data.VersionInfo
import com.ksupatcher.data.UpdateManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class UpdateRepository(
    private val client: OkHttpClient = OkHttpClient(),
    private val releaseRepository: GitHubReleaseRepository = GitHubReleaseRepository()
) {
    suspend fun fetchVersionInfo(url: String): Result<VersionInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Version check failed: ${response.code}")
                }
                val body = response.body?.string() ?: error("Empty response")
                val json = JSONObject(body)
                VersionInfo(
                    versionName = json.optString("versionName", "unknown"),
                    updatedOn = json.optString("updatedOn", "unknown"),
                    minApi = json.optInt("minApi", 28),
                    notes = json.optString("notes").ifBlank { null }
                )
            }
        }
    }

    suspend fun fetchUpdateManifestFromLatestRelease(owner: String, repo: String): Result<UpdateManifest> = withContext(Dispatchers.IO) {
        runCatching {
            val tag = releaseRepository.fetchLatestTag(owner, repo).getOrThrow()
            val url = "https://github.com/${owner}/${repo}/releases/download/${tag}/update.json"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Failed to fetch update.json: ${response.code}")
                val body = response.body?.string() ?: error("Empty update.json")
                val json = org.json.JSONObject(body)
                val timestamp = json.optString("timestamp", null)
                val sha256 = json.optString("sha256", null)
                UpdateManifest(tag = tag, timestamp = timestamp, sha256 = sha256)
            }
        }
    }
}
