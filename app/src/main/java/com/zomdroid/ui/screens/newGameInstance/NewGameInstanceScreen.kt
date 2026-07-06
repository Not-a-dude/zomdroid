package com.zomdroid.ui.screens.newGameInstance

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zomdroid.R
import com.zomdroid.ui.theme.ZomdroidTheme

@Composable
fun NewGameInstance(
    viewModel: NewGameInstanceViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToWiki: () -> Unit = {}
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.onFileSelected(it) }
    }

    NewGameInstanceContent(
        instanceName = viewModel.instanceName,
        nameError = viewModel.nameError,
        presets = viewModel.presets,
        selectedPreset = viewModel.selectedPreset,
        gameFilesZipName = viewModel.gameFilesZipName,
        onInstanceNameChanged = viewModel::onInstanceNameChanged,
        onPresetSelected = { viewModel.selectedPreset = it },
        onPickFile = { launcher.launch("application/zip") },
        onInstall = { viewModel.install(onNavigateBack) },
        onNavigateToWiki = onNavigateToWiki
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> NewGameInstanceContent(
    instanceName: String,
    nameError: Int?,
    presets: List<T>,
    selectedPreset: T?,
    gameFilesZipName: String,
    onInstanceNameChanged: (String) -> Unit,
    onPresetSelected: (T) -> Unit,
    onPickFile: () -> Unit,
    onInstall: () -> Unit,
    onNavigateToWiki: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Instance name
        OutlinedTextField(
            value = instanceName,
            onValueChange = onInstanceNameChanged,
            label = { Text(stringResource(R.string.game_instance_name)) },
            modifier = Modifier.fillMaxWidth(),
            isError = nameError != null,
            supportingText = {
                nameError?.let {
                    Text(stringResource(it))
                }
            }
        )

        // Preset (Spinner)
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedPreset?.toString() ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.game_instance_preset)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                colors = ExposedDropdownMenuDefaults.textFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                presets.forEach { preset ->
                    DropdownMenuItem(
                        text = { Text(preset.toString()) },
                        onClick = {
                            onPresetSelected(preset)
                            expanded = false
                        }
                    )
                }
            }
        }

        // File selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = gameFilesZipName,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.game_instance_files)) },
                placeholder = { Text(stringResource(R.string.game_instance_browse_files_hint)) },
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = { onNavigateToWiki() }) {
                Icon(
                    painter = painterResource(R.drawable.baseline_help_outline_24),
                    contentDescription = "Help"
                )
            }

            IconButton(onClick = onPickFile) {
                Icon(
                    painter = painterResource(R.drawable.outline_drive_folder_upload_24),
                    contentDescription = "Browse"
                )
            }
        }

        // Install button
        Button(
            onClick = onInstall,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.game_instance_install))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NewGameInstancePreview() {
    ZomdroidTheme {
        NewGameInstanceContent(
            instanceName = "My instance",
            nameError = null,
            presets = listOf("Build 41", "Build 42"),
            selectedPreset = "Build 41",
            gameFilesZipName = "",
            onInstanceNameChanged = {},
            onPresetSelected = {},
            onPickFile = {},
            onInstall = {},
            onNavigateToWiki = {}
        )
    }
}
