package com.ksupatcher.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ksupatcher.data.DownloadState
import com.ksupatcher.viewmodel.KsuVariant
import com.ksupatcher.viewmodel.UiState

@Composable
fun DownloadsScreen(
    state: UiState,
    onDownloadKsud: (KsuVariant) -> Unit,
    onDownloadMagiskboot: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Downloads", style = MaterialTheme.typography.headlineMedium)
        DownloadCard(
            title = "ksud (KernelSU)",
            state = state.ksuDownload,
            onDownload = { onDownloadKsud(KsuVariant.KSU) }
        )
        DownloadCard(
            title = "ksud (KernelSU-Next)",
            state = state.ksunDownload,
            onDownload = { onDownloadKsud(KsuVariant.KSUN) }
        )
        DownloadCard(
            title = "libmagiskboot.so",
            state = state.magiskbootDownload,
            onDownload = onDownloadMagiskboot
        )
    }
}

@Composable
private fun DownloadCard(
    title: String,
    state: DownloadState,
    onDownload: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (state.isDownloading) {
                CircularProgressIndicator()
                Text("Progress: ${state.progress}%")
            } else {
                state.filePath?.let { Text("Saved to: $it") }
                state.error?.let { Text("Error: $it") }
            }
            Button(onClick = onDownload) {
                Text(if (state.isDownloading) "Downloading" else "Download")
            }
        }
    }
}
