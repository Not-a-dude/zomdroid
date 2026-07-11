package com.zomdroid

import android.content.Intent
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zomdroid.ui.screens.launcher.LauncherScreen
import com.zomdroid.ui.screens.newGameInstance.NewGameInstance
import com.zomdroid.ui.screens.settings.SettingsScreen
import com.zomdroid.ui.screens.wiki.WikiScreen
import com.zomdroid.ui.theme.ZomdroidTheme

class LauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ZomdroidTheme {
                ZomdroidApp()
            }
        }
    }
}

sealed class Screen(val route: String) {
    object Launcher : Screen("launcher")
    object NewGameInstance : Screen("new_game_instance")
    object Settings : Screen("settings")
    object Wiki : Screen("wiki")
}

private data class BottomBarItem(
    val screen: Screen,
    val iconRes: Int,
    val labelRes: Int
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ZomdroidApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarItems = listOf(
        BottomBarItem(Screen.Launcher, R.drawable.mt_icon_play, R.string.fragment_label_launcher),
        BottomBarItem(Screen.Wiki, R.drawable.mt_icon_menu_book, R.string.fragment_label_wiki),
        BottomBarItem(Screen.Settings, R.drawable.mt_icon_settings, R.string.fragment_label_settings)
    )
    val bottomBarRoutes = bottomBarItems.map { it.screen.route }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when (currentRoute) {
                        Screen.Launcher.route -> stringResource(R.string.fragment_label_launcher)
                        Screen.NewGameInstance.route -> stringResource(R.string.fragment_label_new_game_instance)
                        Screen.Settings.route -> stringResource(R.string.fragment_label_settings)
                        Screen.Wiki.route -> stringResource(R.string.fragment_label_wiki)
                        else -> ""
                    }
                    Text(title)
                },
                navigationIcon = {
                    if (currentRoute !in bottomBarRoutes) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(painterResource(R.drawable.outline_arrow_back_24), contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (currentRoute == Screen.Launcher.route) {
                        IconButton(onClick = {
                            context.startActivity(Intent(context, ControlsEditorActivity::class.java))
                        }) {
                            Icon(
                                painterResource(R.drawable.mt_icon_controller),
                                contentDescription = stringResource(R.string.action_controls_editor)
                            )
                        }
                        IconButton(onClick = {
                            val folderUri = DocumentsContract.buildDocumentUri(
                                C.STORAGE_PROVIDER_AUTHORITY,
                                AppStorage.requireSingleton().homePath
                            )
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(folderUri, DocumentsContract.Document.MIME_TYPE_DIR)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        }) {
                            Icon(
                                painterResource(R.drawable.mt_icon_folder),
                                contentDescription = stringResource(R.string.action_manage_storage)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (currentRoute in bottomBarRoutes) {
                NavigationBar {
                    bottomBarItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.screen.route,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(painterResource(item.iconRes), contentDescription = null) },
                            label = { Text(stringResource(item.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            NavHost(navController = navController, startDestination = Screen.Launcher.route) {
                composable(Screen.Launcher.route) {
                    LauncherScreen(
                        onNavigateToNewInstance = {
                            navController.navigate(Screen.NewGameInstance.route)
                        },
                        onNavigateToWiki = {
                            navController.navigate(Screen.Wiki.route)
                        },
                        onGameStarted = {
                            (context as? LauncherActivity)?.finish()
                        }
                    )
                }
                composable(Screen.NewGameInstance.route) {
                    NewGameInstance(
                        onNavigateBack = { navController.navigateUp() },
                        onNavigateToWiki = { navController.navigate(Screen.Wiki.route) }
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen()
                }
                composable(Screen.Wiki.route) {
                    WikiScreen()
                }
            }
        }
    }
}
