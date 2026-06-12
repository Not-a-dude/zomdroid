package com.zomdroid.ui.screens.launcher

import android.app.Application
import android.content.*
import android.os.IBinder
import androidx.lifecycle.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.zomdroid.C
import com.zomdroid.InstallerService
import com.zomdroid.game.GameInstance
import com.zomdroid.game.GameInstanceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.core.content.edit

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferences = application.getSharedPreferences(C.shprefs.NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    private val taskStateObserver = Observer<InstallerService.TaskState?> { state ->
        _uiState.update { it.copy(taskState = state) }
        if (state?.isFinished == true || state?.isFinishedWithError == true) {
            refreshGameInstances()
        }
    }

    private val installerServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as InstallerService.LocalBinder
            val installerService = binder.service
            _uiState.update { it.copy(isInstallerServiceBound = true) }
            installerService.taskState.observeForever(taskStateObserver)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            _uiState.update { it.copy(isInstallerServiceBound = false, taskState = null) }
        }
    }

    private val taskProgressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == InstallerService.ACTION_STARTED) {
                bindInstallerService()
            }
        }
    }

    init {
        val isAccepted = sharedPreferences.getBoolean(C.shprefs.keys.IS_LEGAL_NOTICE_ACCEPTED, false)
        val areDepsInstalled = sharedPreferences.getBoolean(C.shprefs.keys.ARE_DEPENDENCIES_INSTALLED, false)
        _uiState.update { it.copy(
            isLegalNoticeAccepted = isAccepted,
            areDependenciesInstalled = areDepsInstalled
        ) }
        refreshGameInstances()

        val filter = IntentFilter(InstallerService.ACTION_STARTED)
        LocalBroadcastManager.getInstance(application).registerReceiver(taskProgressReceiver, filter)
    }

    fun refreshGameInstances() {
        _uiState.update { it.copy(gameInstances = GameInstanceManager.requireSingleton().instances.toList()) }
    }

    fun acceptLegalNotice() {
        sharedPreferences.edit { putBoolean(C.shprefs.keys.IS_LEGAL_NOTICE_ACCEPTED, true) }
        _uiState.update { it.copy(isLegalNoticeAccepted = true) }
        checkAndInstallDependencies()
    }

    fun checkAndInstallDependencies() {
        val installed = sharedPreferences.getBoolean(C.shprefs.keys.ARE_DEPENDENCIES_INSTALLED, false)
        if (!installed) {
            val intent = Intent(getApplication(), InstallerService::class.java).apply {
                putExtra(InstallerService.EXTRA_COMMAND, InstallerService.Task.INSTALL_DEPENDENCIES.ordinal)
            }
            getApplication<Application>().startForegroundService(intent)
        }
    }

    fun deleteGameInstance(gameInstance: GameInstance) {
        val intent = Intent(getApplication(), InstallerService::class.java).apply {
            putExtra(InstallerService.EXTRA_COMMAND, InstallerService.Task.DELETE_GAME_INSTANCE.ordinal)
            putExtra(InstallerService.EXTRA_GAME_INSTANCE_NAME, gameInstance.name)
        }
        getApplication<Application>().startForegroundService(intent)
    }

    fun bindInstallerService() {
        if (!_uiState.value.isInstallerServiceBound) {
            val intent = Intent(getApplication(), InstallerService::class.java)
            getApplication<Application>().bindService(intent, installerServiceConnection, 0)
        }
    }

    fun unbindInstallerService() {
        if (_uiState.value.isInstallerServiceBound) {
            getApplication<Application>().unbindService(installerServiceConnection)
            _uiState.update { it.copy(isInstallerServiceBound = false) }
        }
    }

    fun clearTaskState() {
        if (_uiState.value.taskState?.isFinished == true || _uiState.value.taskState?.isFinishedWithError == true) {
            _uiState.update { it.copy(taskState = null) }
            unbindInstallerService()
            getApplication<Application>().stopService(Intent(getApplication(), InstallerService::class.java))
        }
    }

    override fun onCleared() {
        super.onCleared()
        unbindInstallerService()
        LocalBroadcastManager.getInstance(getApplication()).unregisterReceiver(taskProgressReceiver)
    }
}
