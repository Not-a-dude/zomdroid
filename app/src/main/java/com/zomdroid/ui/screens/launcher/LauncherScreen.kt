package com.zomdroid.ui.screens.launcher

import android.content.Intent
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zomdroid.C
import com.zomdroid.GameActivity
import com.zomdroid.R
import com.zomdroid.game.GameInstance

@Composable
fun LauncherScreen(
    viewModel: LauncherViewModel = viewModel(),
    onNavigateToNewInstance: () -> Unit,
    onNavigateToWiki: () -> Unit,
    onGameStarted: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var gameFilesMissingInstance by remember { mutableStateOf<GameInstance?>(null) }
    var gameFilesNotForLinuxInstance by remember { mutableStateOf<GameInstance?>(null) }

    DisposableEffect(Unit) {
        viewModel.bindInstallerService()
        onDispose {
            viewModel.unbindInstallerService()
        }
    }

    LaunchedEffect(uiState.isLegalNoticeAccepted) {
        if (uiState.isLegalNoticeAccepted) {
            viewModel.checkAndInstallDependencies()
        }
    }

    // Legal Notice Dialog
    if (!uiState.isLegalNoticeAccepted) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.legal_notice_title)) },
            text = { Text(stringResource(R.string.legal_notice_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.acceptLegalNotice() }) {
                    Text(stringResource(R.string.dialog_button_accept))
                }
            },
            dismissButton = null
        )
    }

    // Task Progress/Finished Dialog
    uiState.taskState?.let { taskState ->
        AlertDialog(
            onDismissRequest = {
                if (taskState.isFinished || taskState.isFinishedWithError) {
                    viewModel.clearTaskState()
                }
            },
            title = { taskState.title?.let { Text(it) } },
            text = {
                Column {
                    taskState.message?.let { Text(it) }
                    if (!taskState.isFinished && !taskState.isFinishedWithError) {
                        Spacer(modifier = Modifier.height(16.dp))
                        if (taskState.progress < 0) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        } else {
                            LinearProgressIndicator(
                                progress = { taskState.progress.toFloat() / taskState.progressMax },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (taskState.isFinished || taskState.isFinishedWithError) {
                    TextButton(onClick = { viewModel.clearTaskState() }) {
                        Text(stringResource(R.string.dialog_button_ok))
                    }
                }
            }
        )
    }

    // Game Files Missing Dialog
    gameFilesMissingInstance?.let {
        AlertDialog(
            onDismissRequest = { gameFilesMissingInstance = null },
            title = { Text(stringResource(R.string.dialog_title_game_files_missing)) },
            text = { Text(stringResource(R.string.game_files_missing)) },
            confirmButton = {
                TextButton(onClick = {
                    onNavigateToWiki()
                    gameFilesMissingInstance = null
                }) {
                    Text(stringResource(R.string.dialog_button_view_guide))
                }
            },
            dismissButton = {
                TextButton(onClick = { gameFilesMissingInstance = null }) {
                    Text(stringResource(R.string.dialog_button_close))
                }
            }
        )
    }

    // Game Files Not For Linux Dialog
    gameFilesNotForLinuxInstance?.let {
        AlertDialog(
            onDismissRequest = { gameFilesNotForLinuxInstance = null },
            title = { Text(stringResource(R.string.dialog_title_game_files_not_for_linux)) },
            text = { Text(stringResource(R.string.game_files_not_for_linux)) },
            confirmButton = {
                TextButton(onClick = {
                    onNavigateToWiki()
                    gameFilesNotForLinuxInstance = null
                }) {
                    Text(stringResource(R.string.dialog_button_view_guide))
                }
            },
            dismissButton = {
                TextButton(onClick = { gameFilesNotForLinuxInstance = null }) {
                    Text(stringResource(R.string.dialog_button_close))
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToNewInstance) {
                Icon(
                    painter = painterResource(R.drawable.mt_icon_add),
                    contentDescription = stringResource(R.string.fragment_label_new_game_instance)
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.gameInstances) { instance ->
                GameInstanceItem(
                    instance = instance,
                    onLaunch = {
                        if (!instance.isInstallationFinished) {
                            Toast.makeText(context, R.string.installation_not_finished, Toast.LENGTH_SHORT).show()
                        } else if (!instance.hasGameFiles()) {
                            gameFilesMissingInstance = instance
                        } else if (!instance.hasFilesForLinux()) {
                            gameFilesNotForLinuxInstance = instance
                        } else if (!uiState.areDependenciesInstalled) {
                            Toast.makeText(context, R.string.dependencies_not_installed, Toast.LENGTH_SHORT).show()
                        } else {
                            val intent = Intent(context, GameActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                putExtra(GameActivity.EXTRA_GAME_INSTANCE_NAME, instance.name)
                            }
                            context.startActivity(intent)
                            onGameStarted()
                        }
                    },
                    onManageStorage = {
                        val folderUri = DocumentsContract.buildDocumentUri(C.STORAGE_PROVIDER_AUTHORITY, instance.homePath)
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(folderUri, DocumentsContract.Document.MIME_TYPE_DIR)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    },
                    onDelete = {
                        viewModel.deleteGameInstance(instance)
                    }
                )
            }
        }
    }
}

@Composable
fun GameInstanceItem(
    instance: GameInstance,
    onLaunch: () -> Unit,
    onManageStorage: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.dialog_title_delete_game_instance)) },
            text = { Text(stringResource(R.string.delete_game_instance)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text(stringResource(R.string.dialog_button_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.dialog_button_cancel))
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = instance.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = onLaunch) {
                Icon(painterResource(R.drawable.mt_icon_play), contentDescription = "Launch")
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(painterResource(R.drawable.mt_icon_settings), contentDescription = "Settings")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.game_instance_manage_storage)) },
                        onClick = {
                            onManageStorage()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.game_instance_delete)) },
                        onClick = {
                            showDeleteConfirm = true
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}
