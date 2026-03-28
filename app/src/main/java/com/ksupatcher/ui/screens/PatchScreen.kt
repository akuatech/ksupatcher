package com.ksupatcher.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ksupatcher.viewmodel.KsuVariant
import com.ksupatcher.viewmodel.UiState

@Composable
fun PatchScreen(
    state: UiState,
    onVariantSelected: (KsuVariant) -> Unit,
    onPickBoot: (Uri) -> Unit,
    onPickModule: (Uri) -> Unit,
    onRunPatch: () -> Unit
) {
    val bootPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let(onPickBoot) }
    )
    val modulePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let(onPickModule) }
    )

    val patch = state.patchState
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Patch Configuration",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "KernelSU Variant",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VariantButton(
                        text = "Official (KSU)",
                        selected = patch.variant == KsuVariant.KSU,
                        onClick = { onVariantSelected(KsuVariant.KSU) },
                        modifier = Modifier.weight(1f)
                    )
                    VariantButton(
                        text = "Next (KSUN)",
                        selected = patch.variant == KsuVariant.KSUN,
                        onClick = { onVariantSelected(KsuVariant.KSUN) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Text(
            text = "Files",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

        FileSelector(
            label = "Boot Image",
            fileName = patch.bootImageName,
            placeholder = "Select boot.img",
            onSelect = { bootPicker.launch(arrayOf("application/octet-stream", "image/*")) }
        )

        FileSelector(
            label = "Kernel Module (Auto-downloads if not selected)",
            fileName = patch.moduleName,
            placeholder = "Optional: Select custom kernelsu.ko",
            onSelect = { modulePicker.launch(arrayOf("application/octet-stream")) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (patch.isPatching) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = patch.status ?: "Processing...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        } else {
            Button(
                onClick = onRunPatch,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Start Patching")
            }

            if (!patch.status.isNullOrBlank()) {
                Text(
                    text = patch.status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (patch.status.contains("failed", ignoreCase = true))
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            }
        }

        if (!patch.lastCommand.isNullOrBlank() || !patch.lastOutput.isNullOrBlank()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0E111A),
                    contentColor = Color(0xFFE7EAF3)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Output Log", style = MaterialTheme.typography.labelMedium, color = Color(0xFFE7EAF3))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    SelectionContainer {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0E111A), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Column {
                                if (!patch.lastCommand.isNullOrBlank()) {
                                    Text(
                                        text = "Runtime:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFE7EAF3)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = patch.lastCommand ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = Color(0xFFE7EAF3)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                if (!patch.lastOutput.isNullOrBlank()) {
                                    Text(
                                        text = patch.lastOutput ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = Color(0xFFE7EAF3)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VariantButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Text(text)
    }
}

@Composable
fun FileSelector(
    label: String,
    fileName: String?,
    placeholder: String,
    onSelect: () -> Unit
) {
    OutlinedCard(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = fileName ?: placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = if (fileName != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
