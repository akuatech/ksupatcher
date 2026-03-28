package com.ksupatcher.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

class GitHubReleaseRepository(
    private val client: OkHttpClient = OkHttpClient()
) {
    suspend fun fetchLatestTag(owner: String, repo: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://api.github.com/repos/${owner}/${repo}/releases"
            val request = Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", "ksupatcher")
                .header("Accept", "application/vnd.github+json")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Release check failed: ${response.code}")
                }
                val body = response.body?.string() ?: error("Empty response")
                val array = JSONArray(body)
                if (array.length() == 0) {
                    error("No releases found")
                }
                array.getJSONObject(0).optString("tag_name").ifBlank {
                    error("Missing tag name")
                }
            }
        }
    }
}
