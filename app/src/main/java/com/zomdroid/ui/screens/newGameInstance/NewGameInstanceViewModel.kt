package com.zomdroid.ui.screens.newGameInstance

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.zomdroid.InstallerService
import com.zomdroid.R
import com.zomdroid.game.GameInstance
import com.zomdroid.game.GameInstance.isUniqueName
import com.zomdroid.game.GameInstance.isValidName
import com.zomdroid.game.GameInstanceManager
import com.zomdroid.game.PresetManager

class NewGameInstanceViewModel(application: Application) : AndroidViewModel(application) {
    var instanceName by mutableStateOf("")
    var nameError by mutableStateOf<Int?>(null)

    val presets = PresetManager.getPresets()
    var selectedPreset by mutableStateOf(presets.firstOrNull())

    var gameFilesZipUri by mutableStateOf<Uri?>(null)
    var gameFilesZipName by mutableStateOf("")

    fun onInstanceNameChanged(newName: String) {
        instanceName = newName
        nameError = when {
            !isValidName(newName) -> R.string.game_instance_name_invalid
            !isUniqueName(newName) -> R.string.game_instance_name_already_exists
            else -> null
        }
    }

    fun onFileSelected(uri: Uri) {
        val contentResolver = getApplication<Application>().contentResolver
        if (contentResolver.getType(uri) == "application/zip") {
            gameFilesZipUri = uri
            val cursor = contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        gameFilesZipName = it.getString(nameIndex)
                    }
                }
            }
        }
    }

    fun install(onSuccess: () -> Unit) {
        val name = instanceName
        if (!isValidName(name) || !isUniqueName(name) || gameFilesZipUri == null) {
            return
        }

        val preset = selectedPreset ?: return
        try {
            val gameInstance = GameInstance(name, preset)
            GameInstanceManager.requireSingleton().registerInstance(gameInstance)

            val installerIntent = Intent(getApplication(), InstallerService::class.java).apply {
                putExtra(InstallerService.EXTRA_COMMAND, InstallerService.Task.CREATE_GAME_INSTANCE.ordinal)
                putExtra(InstallerService.EXTRA_GAME_INSTANCE_NAME, gameInstance.name)
                putExtra(InstallerService.EXTRA_ARCHIVE_URI, gameFilesZipUri)
            }
            getApplication<Application>().startForegroundService(installerIntent)
            onSuccess()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
