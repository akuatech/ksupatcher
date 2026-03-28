package com.ksupatcher.data

data class VersionInfo(
    val versionName: String,
    val updatedOn: String,
    val minApi: Int,
    val notes: String?
)
