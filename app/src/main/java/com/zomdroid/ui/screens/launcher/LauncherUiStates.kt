package com.zomdroid.ui.screens.launcher

import com.zomdroid.InstallerService
import com.zomdroid.game.GameInstance

data class LauncherUiState(
    val gameInstances: List<GameInstance> = emptyList(),
    val isLegalNoticeAccepted: Boolean = true,
    val areDependenciesInstalled: Boolean = true,
    val taskState: InstallerService.TaskState? = null,
    val isInstallerServiceBound: Boolean = false
)