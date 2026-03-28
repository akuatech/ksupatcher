package com.ksupatcher.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ksupatcher.viewmodel.UiState

@Composable
fun SettingsScreen(
    state: UiState,
    onRefreshVersion: () -> Unit,
    onVersionUrlChange: (String) -> Unit,
    onSaveVersionUrl: () -> Unit,
    onCheckLatestRelease: () -> Unit
) {
    val scrollState = rememberScrollState()
    var selectedTab by remember { mutableStateOf("update") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { selectedTab = "update" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == "update") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text("Update")
            }
            Button(
                onClick = { selectedTab = "ota" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == "ota") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text("OTA (coming soon)")
            }
        }

        if (selectedTab == "update") {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Update Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = state.versionUrl,
                        onValueChange = onVersionUrlChange,
                        label = { Text("Version JSON URL") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(onClick = onSaveVersionUrl) { Text("Save URL") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = onCheckLatestRelease) { Text("Check Latest Release") }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Version Info",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Button(
                                onClick = onRefreshVersion,
                                enabled = !state.isCheckingVersion
                            ) {
                                if (state.isCheckingVersion) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Checking...")
                                } else {
                                    Text("Check Now")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (!state.latestReleaseTag.isNullOrBlank()) {
                                Text("Latest tag: ${state.latestReleaseTag}")
                            }
                            val ts = state.updateManifest?.timestamp ?: "n/a"
                            val sha = state.updateManifest?.sha256 ?: "n/a"
                            if (state.updateManifest != null) {
                                Text("Manifest timestamp: $ts")
                                Text("Manifest sha256: $sha")
                            }
                            state.manifestError?.let { err ->
                                Text(err, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    HorizontalDivider()

                    if (state.versionError != null) {
                        Text(
                            text = state.versionError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        val info = state.versionInfo
                        if (info != null) {
                            InfoRow(label = "Version", value = info.versionName)
                            InfoRow(label = "Updated On", value = info.updatedOn)
                            InfoRow(label = "Min API", value = info.minApi.toString())
                            if (!info.notes.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Release Notes",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = info.notes,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        } else {
                            Text(
                                text = "No version information available.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )
    }
}
