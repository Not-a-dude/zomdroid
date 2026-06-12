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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGameInstance(
    viewModel: NewGameInstanceViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToWiki: () -> Unit = {}
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.onFileSelected(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.fragment_label_new_game_instance)) },
                actions = {
                    IconButton(onClick = { viewModel.install(onNavigateBack) }) {
                        Icon(
                            painter = painterResource(R.drawable.outline_check_24),
                            contentDescription = "Install"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Instance name
            OutlinedTextField(
                value = viewModel.instanceName,
                onValueChange = viewModel::onInstanceNameChanged,
                label = { Text(stringResource(R.string.game_instance_name)) },
                modifier = Modifier.fillMaxWidth(),
                isError = viewModel.nameError != null,
                supportingText = {
                    viewModel.nameError?.let {
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
                    value = viewModel.selectedPreset?.toString() ?: "",
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
                    viewModel.presets.forEach { preset ->
                        DropdownMenuItem(
                            text = { Text(preset.toString()) },
                            onClick = {
                                viewModel.selectedPreset = preset
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
                    value = viewModel.gameFilesZipName,
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
                
                IconButton(onClick = { launcher.launch("application/zip") }) {
                    Icon(
                        painter = painterResource(R.drawable.outline_drive_folder_upload_24),
                        contentDescription = "Browse"
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun NewGameInstancePreview() {
    ZomdroidTheme {
        NewGameInstance()
    }
}
