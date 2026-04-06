package com.ksupatcher.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okio.buffer
import okio.sink
import java.io.File

class DownloadRepository(
    private val client: OkHttpClient = OkHttpClient()
) {
    suspend fun downloadText(url: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val request = buildRequest(url)
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Download failed: ${response.code}")
                }
                response.body?.string() ?: error("Empty response")
            }
        }
    }

    suspend fun download(
        url: String,
        targetFile: File,
        onProgress: (Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val request = buildRequest(url)
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Download failed: ${response.code}")
                }
                val body = response.body ?: error("Empty response")
                val totalBytes = body.contentLength().takeIf { it > 0 } ?: -1L
                val parent = targetFile.parentFile
                parent?.mkdirs()
                val tempFile = File(parent ?: targetFile.absoluteFile.parentFile, "${targetFile.name}.part")
                tempFile.delete()

                try {
                    tempFile.sink().buffer().use { sink ->
                        val source = body.source()
                        var readBytes: Long
                        var written: Long = 0
                        val bufferSize = 8 * 1024L
                        while (true) {
                            readBytes = source.read(sink.buffer, bufferSize)
                            if (readBytes == -1L) break
                            written += readBytes
                            sink.emit()
                            if (totalBytes > 0) {
                                val percent = ((written * 100) / totalBytes).toInt()
                                onProgress(percent.coerceIn(0, 100))
                            }
                        }
                    }

                    if (targetFile.exists()) {
                        targetFile.delete()
                    }
                    if (!tempFile.renameTo(targetFile)) {
                        tempFile.copyTo(targetFile, overwrite = true)
                        tempFile.delete()
                    }
                } catch (error: Throwable) {
                    tempFile.delete()
                    throw error
                }
                onProgress(100)
                targetFile
            }
        }
    }

    private fun buildRequest(url: String): Request {
        val httpUrl = url.toHttpUrlOrNull() ?: error("Download URL is invalid")
        if (!httpUrl.isHttps) {
            error("Only HTTPS downloads are allowed")
        }
        return Request.Builder()
            .url(httpUrl)
            .get()
            .header("User-Agent", "ksupatcher")
            .build()
    }
}
