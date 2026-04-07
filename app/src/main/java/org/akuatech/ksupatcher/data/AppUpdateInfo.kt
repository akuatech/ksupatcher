package org.akuatech.ksupatcher.data

data class AppUpdateInfo(
    val currentBuildHash: String,
    val latestReleaseHash: String,
    val isUpdateAvailable: Boolean,
    val apkAssetName: String?,
    val apkDownloadUrl: String?,
    val checksumDownloadUrl: String?,
    val releaseUrl: String?,
    val publishedAt: String?,
    val notes: String?
)
