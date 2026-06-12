package com.zomdroid

import android.content.Intent
import android.os.Bundle
import android.provider.DocumentsContract
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.TextView
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zomdroid.ui.screens.launcher.LauncherScreen
import com.zomdroid.ui.screens.newGameInstance.NewGameInstance
import com.zomdroid.ui.screens.settings.SettingsScreen
import com.zomdroid.ui.screens.wiki.WikiScreen
import com.zomdroid.ui.theme.ZomdroidTheme
import kotlinx.coroutines.launch

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ZomdroidApp() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var showDonateDialog by remember { mutableStateOf(false) }

    if (showDonateDialog) {
        AlertDialog(
            onDismissRequest = { showDonateDialog = false },
            title = { Text(stringResource(R.string.dialog_title_donate)) },
            text = {
                val donateMessage = stringResource(R.string.donate_message)
                AndroidView(factory = { ctx ->
                    TextView(ctx).apply {
                        text = SpannableString(donateMessage).apply {
                            Linkify.addLinks(this, Linkify.WEB_URLS)
                        }
                        movementMethod = LinkMovementMethod.getInstance()
                        textSize = 16f
                    }
                })
            },
            confirmButton = {
                TextButton(onClick = { showDonateDialog = false }) {
                    Text(stringResource(R.string.dialog_button_ok))
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                NavigationDrawerItem(
                    icon = { Icon(painterResource(R.drawable.mt_icon_settings), contentDescription = null) },
                    label = { Text(stringResource(R.string.fragment_label_settings)) },
                    selected = currentRoute == Screen.Settings.route,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Settings.route) {
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(painterResource(R.drawable.mt_icon_controller), contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_menu_controls_editor)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        context.startActivity(Intent(context, ControlsEditorActivity::class.java))
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(painterResource(R.drawable.mt_icon_menu_book), contentDescription = null) },
                    label = { Text(stringResource(R.string.fragment_label_wiki)) },
                    selected = currentRoute == Screen.Wiki.route,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Wiki.route) {
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(painterResource(R.drawable.mt_icon_folder), contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_menu_manage_storage)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
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
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 28.dp))
                NavigationDrawerItem(
                    icon = { Icon(painterResource(R.drawable.mt_icon_heart), contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_menu_donate)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showDonateDialog = true
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
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
                        if (currentRoute == Screen.Launcher.route) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(painterResource(R.drawable.mt_icon_menu), contentDescription = "Menu")
                            }
                        } else {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(painterResource(R.drawable.outline_arrow_back_24), contentDescription = "Back")
                            }
                        }
                    }
                )
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
}
