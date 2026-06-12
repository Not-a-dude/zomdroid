package com.zomdroid.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.zomdroid.LauncherPreferences.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.File

val Context.dataStore by preferencesDataStore(name = "settings")

data class GameSettings(
    val renderer: Renderer = if (isKgsl) Renderer.ZINK_ZFA else Renderer.GL4ES,
    val vulkanDriver: VulkanDriver = if (isKgsl) VulkanDriver.FREEDRENO else VulkanDriver.SYSTEM_DEFAULT,
    val audioAPI: AudioAPI = AudioAPI.AAUDIO,
    val renderScale: Float = 1.0f,
    val isDebug: Boolean = false
) {
    companion object {
        private val isKgsl = File("/dev/kgsl-3d0").exists()
    }
}

class SettingsManager(private val context: Context) {
    private object Keys {
        val RENDERER = stringPreferencesKey("renderer")
        val VULKAN_DRIVER = stringPreferencesKey("vulkan_driver")
        val AUDIO_API = stringPreferencesKey("audio_api")
        val RENDER_SCALE = floatPreferencesKey("render_scale")
        val IS_DEBUG = booleanPreferencesKey("is_debug")
    }

    val settings: Flow<GameSettings> = context.dataStore.data.map { p ->
        val def = GameSettings()
        GameSettings(
            renderer = p.getEnum(Keys.RENDERER, def.renderer),
            vulkanDriver = p.getEnum(Keys.VULKAN_DRIVER, def.vulkanDriver),
            audioAPI = p.getEnum(Keys.AUDIO_API, def.audioAPI),
            renderScale = p[Keys.RENDER_SCALE] ?: def.renderScale,
            isDebug = p[Keys.IS_DEBUG] ?: def.isDebug
        )
    }

    fun getSettingsSync(): GameSettings = runBlocking { settings.first() }

    private suspend fun update(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    suspend fun setRenderer(v: Renderer) = update { it[Keys.RENDERER] = v.name }
    suspend fun setVulkanDriver(v: VulkanDriver) = update { it[Keys.VULKAN_DRIVER] = v.name }
    suspend fun setAudioAPI(v: AudioAPI) = update { it[Keys.AUDIO_API] = v.name }
    suspend fun setRenderScale(v: Float) = update { it[Keys.RENDER_SCALE] = v }
    suspend fun setIsDebug(v: Boolean) = update { it[Keys.IS_DEBUG] = v }

    private inline fun <reified T : Enum<T>> Preferences.getEnum(key: Preferences.Key<String>, default: T): T =
        this[key]?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
}
