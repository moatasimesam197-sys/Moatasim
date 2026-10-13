package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.OscViewModel
import com.example.ui.components.DestructiveActionDialog
import com.example.ui.components.OscLiveLogDrawer
import com.example.ui.components.StageTopBar
import com.example.ui.components.WarningBanner
import com.example.ui.screens.CommandLineScreen
import com.example.ui.screens.FaderScreen
import com.example.ui.screens.GridScreen
import com.example.ui.screens.LayoutEditorScreen
import com.example.ui.screens.PlaybackScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StageBlack
import com.example.ui.theme.StageBorder
import com.example.ui.theme.StageCyan
import com.example.ui.theme.StageSurface
import com.example.ui.theme.StageTextMuted
import com.example.ui.theme.StageTextPrimary

enum class NavDestination(val label: String, val icon: ImageVector, val tag: String) {
    GRID("Grid", Icons.Default.GridView, "nav_grid"),
    FADERS("Faders", Icons.Default.Tune, "nav_faders"),
    PLAYBACK("Playback", Icons.Default.PlayCircle, "nav_playback"),
    CMD_LINE("Command", Icons.Default.Terminal, "nav_cmd"),
    LAYOUT("Layout", Icons.Default.DashboardCustomize, "nav_layout"),
    SETTINGS("Settings", Icons.Default.Settings, "nav_settings")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen(
    viewModel: OscViewModel = viewModel()
) {
    var currentDestination by remember { mutableStateOf(NavDestination.GRID) }
    var isLogPanelOpen by remember { mutableStateOf(false) }

    val connectionState by viewModel.connectionState.collectAsState()
    val config by viewModel.config.collectAsState()
    val blackoutActive by viewModel.blackoutActive.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val pendingDestructive by viewModel.pendingDestructiveAction.collectAsState()

    // Handle back button to return to GRID
    BackHandler(enabled = currentDestination != NavDestination.GRID) {
        currentDestination = NavDestination.GRID
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(StageBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val isLandscape = maxWidth > maxHeight && maxWidth > 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // Adaptive Navigation Rail in landscape
            if (isLandscape) {
                NavigationRail(
                    containerColor = StageSurface,
                    contentColor = StageCyan,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    NavDestination.values().forEach { dest ->
                        NavigationRailItem(
                            selected = currentDestination == dest,
                            onClick = { currentDestination = dest },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label, fontSize = 10.sp) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = StageBlack,
                                selectedTextColor = StageCyan,
                                indicatorColor = StageCyan,
                                unselectedIconColor = StageTextMuted,
                                unselectedTextColor = StageTextMuted
                            ),
                            modifier = Modifier.testTag(dest.tag)
                        )
                    }
                }
            }

            // Main Content Area
            Scaffold(
                containerColor = StageBlack,
                topBar = {
                    Column {
                        StageTopBar(
                            connectionState = connectionState,
                            config = config,
                            blackoutActive = blackoutActive,
                            currentPage = currentPage,
                            onBlackoutToggle = { viewModel.toggleBlackout() },
                            onStopAllClick = { viewModel.requestStopAll() },
                            onPrevPage = { viewModel.prevPage() },
                            onNextPage = { viewModel.nextPage() },
                            onToggleLogs = { isLogPanelOpen = !isLogPanelOpen },
                            onStatusClick = { currentDestination = NavDestination.SETTINGS }
                        )

                        WarningBanner(
                            connectionState = connectionState,
                            onReconnectClick = { viewModel.connect() }
                        )
                    }
                },
                bottomBar = {
                    if (!isLandscape) {
                        Column {
                            // Bottom Navigation Bar in Portrait
                            NavigationBar(
                                containerColor = StageSurface,
                                contentColor = StageCyan,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                            ) {
                                NavDestination.values().forEach { dest ->
                                    NavigationBarItem(
                                        selected = currentDestination == dest,
                                        onClick = { currentDestination = dest },
                                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                                        label = { Text(dest.label, fontSize = 10.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = StageBlack,
                                            selectedTextColor = StageCyan,
                                            indicatorColor = StageCyan,
                                            unselectedIconColor = StageTextMuted,
                                            unselectedTextColor = StageTextMuted
                                        ),
                                        modifier = Modifier.testTag(dest.tag)
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Active Destination View
                    when (currentDestination) {
                        NavDestination.GRID -> GridScreen(viewModel = viewModel)
                        NavDestination.FADERS -> FaderScreen(viewModel = viewModel)
                        NavDestination.PLAYBACK -> PlaybackScreen(viewModel = viewModel)
                        NavDestination.CMD_LINE -> CommandLineScreen(viewModel = viewModel)
                        NavDestination.LAYOUT -> LayoutEditorScreen(viewModel = viewModel)
                        NavDestination.SETTINGS -> SettingsScreen(viewModel = viewModel)
                    }

                    // Collapsible Live OSC Diagnostics Log
                    if (isLogPanelOpen) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            OscLiveLogDrawer(
                                logs = logs,
                                isOpen = isLogPanelOpen,
                                onClose = { isLogPanelOpen = false },
                                onClear = { viewModel.clearLogs() }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Confirmation Dialog for destructive operations
    DestructiveActionDialog(
        request = pendingDestructive,
        onDismiss = { viewModel.dismissDestructiveAction() }
    )
}
