package com.ksupatcher.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File

class DownloadRepository(
    private val client: OkHttpClient = OkHttpClient()
) {
    suspend fun downloadText(url: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url).get().build()
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
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Download failed: ${response.code}")
                }
                val body = response.body ?: error("Empty response")
                val totalBytes = body.contentLength().takeIf { it > 0 } ?: -1L
                targetFile.parentFile?.mkdirs()
                targetFile.sink().buffer().use { sink ->
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
                onProgress(100)
                targetFile
            }
        }
    }
}
