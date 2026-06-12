package com.zomdroid.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zomdroid.LauncherPreferences
import com.zomdroid.data.GameSettings
import com.zomdroid.data.SettingsManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsManager = SettingsManager(application)

    val settings: StateFlow<GameSettings> = settingsManager.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GameSettings())

    fun setRenderer(renderer: LauncherPreferences.Renderer) {
        viewModelScope.launch {
            settingsManager.setRenderer(renderer)
        }
    }

    fun setVulkanDriver(vulkanDriver: LauncherPreferences.VulkanDriver) {
        viewModelScope.launch {
            settingsManager.setVulkanDriver(vulkanDriver)
        }
    }

    fun setRenderScale(scale: Float) {
        viewModelScope.launch {
            settingsManager.setRenderScale(scale)
        }
    }

    fun setAudioAPI(audioAPI: LauncherPreferences.AudioAPI) {
        viewModelScope.launch {
            settingsManager.setAudioAPI(audioAPI)
        }
    }
}
