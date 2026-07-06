package com.zomdroid.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zomdroid.LauncherPreferences
import com.zomdroid.R
import com.zomdroid.data.GameSettings
import com.zomdroid.ui.screens.settings.components.SettingsDropdown
import com.zomdroid.ui.screens.settings.components.SettingsSection
import com.zomdroid.ui.theme.ZomdroidTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterial3ExpressiveApi
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel<SettingsViewModel>()
) {
    val settings by viewModel.settings.collectAsState()

    SettingsScreenContent(
        settings = settings,
        onRendererSelected = viewModel::setRenderer,
        onVulkanDriverSelected = viewModel::setVulkanDriver,
        onRenderScaleChanged = viewModel::setRenderScale,
        onAudioApiSelected = viewModel::setAudioAPI
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterial3ExpressiveApi
@Composable
fun SettingsScreenContent(
    settings: GameSettings,
    onRendererSelected: (LauncherPreferences.Renderer) -> Unit,
    onVulkanDriverSelected: (LauncherPreferences.VulkanDriver) -> Unit,
    onRenderScaleChanged: (Float) -> Unit,
    onAudioApiSelected: (LauncherPreferences.AudioAPI) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Renderer
        SettingsSection(title = stringResource(R.string.settings_renderer)) {
            SettingsDropdown(
                options = LauncherPreferences.Renderer.entries,
                selectedOption = settings.renderer,
                onOptionSelected = onRendererSelected
            )
        }

        // Vulkan Driver
        if (settings.renderer != LauncherPreferences.Renderer.GL4ES) {
            SettingsSection(title = stringResource(R.string.settings_vulkan_driver)) {
                SettingsDropdown(
                    options = LauncherPreferences.VulkanDriver.entries,
                    selectedOption = settings.vulkanDriver,
                    onOptionSelected = onVulkanDriverSelected
                )
            }
        }

        // Resolution Scale
        SettingsSection(
            title = stringResource(R.string.settings_resolution_scale),
            headerContent = {
                Text(
                    text = stringResource(R.string.percentage_format, (settings.renderScale * 100).roundToInt()),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        ) {
            Slider(
                value = settings.renderScale,
                valueRange = 0.25f..1.0f,
                onValueChange = onRenderScaleChanged
            )
        }

        // Audio API
        SettingsSection(title = stringResource(R.string.settings_audio_api)) {
            SettingsDropdown(
                options = LauncherPreferences.AudioAPI.entries,
                selectedOption = settings.audioAPI,
                onOptionSelected = onAudioApiSelected
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    ZomdroidTheme {
        SettingsScreenContent(
            settings = GameSettings(),
            onRendererSelected = {},
            onVulkanDriverSelected = {},
            onRenderScaleChanged = {},
            onAudioApiSelected = {}
        )
    }
}
