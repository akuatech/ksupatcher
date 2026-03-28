package com.ksupatcher.data

data class DownloadState(
    val isDownloading: Boolean = false,
    val progress: Int = 0,
    val filePath: String? = null,
    val error: String? = null
)
