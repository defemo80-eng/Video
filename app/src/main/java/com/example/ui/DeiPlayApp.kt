package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.player.PlayerDisplayMode
import com.example.ui.components.FloatingMiniPlayer
import com.example.ui.components.VideoPlayerOverlay
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.channel.ChannelDetailScreen
import com.example.ui.screens.channel.CreateOrEditChannelScreen
import com.example.ui.screens.create.LiveBroadcastScreen
import com.example.ui.screens.create.LiveViewerScreen
import com.example.ui.screens.create.UploadVideoScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.library.LibraryScreen
import com.example.ui.screens.profile.NotificationsScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.profile.SettingsSecurityScreen
import com.example.ui.screens.profile.YouTubeConnectScreen
import com.example.ui.screens.shorts.ShortsScreen
import com.example.ui.screens.studio.DeiPlayStudioScreen
import com.example.ui.screens.subscriptions.SubscriptionsScreen
import com.example.ui.theme.DeiPlayTheme
import com.example.ui.theme.DeiRed
import com.example.ui.theme.DeiWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeiPlayApp(viewModel: DeiPlayViewModel) {
    val prefs by viewModel.prefsState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val feedVideos by viewModel.combinedFeedVideos.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    DeiPlayTheme(themeMode = prefs.themeMode) {
        if (currentUser == null) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets.safeDrawing
            ) { padding ->
                AuthScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(padding)
                )
            }
            return@DeiPlayTheme
        }

        var selectedTab by remember { mutableStateOf(MainTab.HOME) }
        val backStack = remember { mutableStateListOf<AppSubScreen>() }
        var showCreateSheet by remember { mutableStateOf(false) }

        fun pushScreen(screen: AppSubScreen) {
            backStack.add(screen)
        }

        fun popScreen() {
            if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex)
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp && !playerState.isInSystemPip

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (!useRail && backStack.isEmpty() && playerState.displayMode != PlayerDisplayMode.FULL && !playerState.isInSystemPip) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.navigationBarsPadding()
                        ) {
                            MainTab.entries.forEach { tab ->
                                val icon = tabIcon(tab)
                                NavigationBarItem(
                                    selected = selectedTab == tab,
                                    onClick = {
                                        if (tab == MainTab.CREATE) {
                                            showCreateSheet = true
                                        } else {
                                            selectedTab = tab
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = tab.label,
                                            tint = if (tab == MainTab.CREATE) DeiRed else if (selectedTab == tab) DeiRed else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = DeiRed.copy(alpha = 0.16f)
                                    ),
                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail && backStack.isEmpty() && playerState.displayMode != PlayerDisplayMode.FULL) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            MainTab.entries.forEach { tab ->
                                NavigationRailItem(
                                    selected = selectedTab == tab,
                                    onClick = {
                                        if (tab == MainTab.CREATE) showCreateSheet = true
                                        else selectedTab = tab
                                    },
                                    icon = { Icon(tabIcon(tab), contentDescription = tab.label) },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        val topScreen = backStack.lastOrNull()
                        if (topScreen != null) {
                            when (topScreen) {
                                is AppSubScreen.ChannelDetail -> ChannelDetailScreen(
                                    viewModel = viewModel,
                                    channelId = topScreen.channelId,
                                    onBack = { popScreen() },
                                    onEditChannel = { pushScreen(AppSubScreen.CreateOrEditChannel) },
                                    onOpenLiveViewer = { sid -> pushScreen(AppSubScreen.LiveViewer(sid)) }
                                )

                                AppSubScreen.CreateOrEditChannel -> CreateOrEditChannelScreen(
                                    viewModel = viewModel,
                                    onBack = { popScreen() },
                                    onSaved = { channelId ->
                                        popScreen()
                                        pushScreen(AppSubScreen.ChannelDetail(channelId))
                                    }
                                )

                                is AppSubScreen.UploadVideo -> UploadVideoScreen(
                                    viewModel = viewModel,
                                    initialIsShort = topScreen.isShort,
                                    initialIsPost = topScreen.isPost,
                                    onBack = { popScreen() },
                                    onCreateChannelFirst = {
                                        popScreen()
                                        pushScreen(AppSubScreen.CreateOrEditChannel)
                                    }
                                )

                                AppSubScreen.LiveBroadcast -> LiveBroadcastScreen(
                                    viewModel = viewModel,
                                    onBack = { popScreen() },
                                    onCreateChannelFirst = {
                                        popScreen()
                                        pushScreen(AppSubScreen.CreateOrEditChannel)
                                    }
                                )

                                AppSubScreen.DeiPlayStudio -> DeiPlayStudioScreen(
                                    viewModel = viewModel,
                                    onBack = { popScreen() },
                                    onCreateChannel = {
                                        popScreen()
                                        pushScreen(AppSubScreen.CreateOrEditChannel)
                                    },
                                    onOpenNotifications = { pushScreen(AppSubScreen.Notifications) }
                                )

                                AppSubScreen.YouTubeConnect -> YouTubeConnectScreen(
                                    viewModel = viewModel,
                                    onBack = { popScreen() }
                                )

                                AppSubScreen.Notifications -> NotificationsScreen(
                                    viewModel = viewModel,
                                    onBack = { popScreen() }
                                )

                                AppSubScreen.SettingsSecurity -> SettingsSecurityScreen(
                                    viewModel = viewModel,
                                    onBack = { popScreen() }
                                )

                                is AppSubScreen.LibrarySection -> LibraryScreen(
                                    viewModel = viewModel,
                                    initialTab = topScreen.initialTab,
                                    onBack = { popScreen() }
                                )

                                is AppSubScreen.LiveViewer -> LiveViewerScreen(
                                    viewModel = viewModel,
                                    sessionId = topScreen.sessionId,
                                    onBack = { popScreen() }
                                )
                            }
                        } else {
                            when (selectedTab) {
                                MainTab.HOME -> HomeScreen(
                                    viewModel = viewModel,
                                    onOpenChannel = { cid -> pushScreen(AppSubScreen.ChannelDetail(cid)) },
                                    onOpenNotifications = { pushScreen(AppSubScreen.Notifications) },
                                    onOpenShortsTab = { selectedTab = MainTab.SHORTS },
                                    onOpenSubscriptionsTab = { selectedTab = MainTab.SUBSCRIPTIONS },
                                    onOpenLiveViewer = { sid -> pushScreen(AppSubScreen.LiveViewer(sid)) },
                                    onOpenUpload = { pushScreen(AppSubScreen.UploadVideo()) }
                                )

                                MainTab.SHORTS -> ShortsScreen(
                                    viewModel = viewModel,
                                    onUploadShort = { pushScreen(AppSubScreen.UploadVideo(isShort = true)) },
                                    onOpenChannel = { cid -> pushScreen(AppSubScreen.ChannelDetail(cid)) }
                                )

                                MainTab.SUBSCRIPTIONS -> SubscriptionsScreen(
                                    viewModel = viewModel,
                                    onOpenChannel = { cid -> pushScreen(AppSubScreen.ChannelDetail(cid)) },
                                    onOpenYouTubeConnect = { pushScreen(AppSubScreen.YouTubeConnect) }
                                )

                                MainTab.PROFILE -> ProfileScreen(
                                    viewModel = viewModel,
                                    onOpenChannel = { cid -> pushScreen(AppSubScreen.ChannelDetail(cid)) },
                                    onCreateOrEditChannel = { pushScreen(AppSubScreen.CreateOrEditChannel) },
                                    onOpenStudio = { pushScreen(AppSubScreen.DeiPlayStudio) },
                                    onOpenYouTubeConnect = { pushScreen(AppSubScreen.YouTubeConnect) },
                                    onOpenLibrary = { tabIdx -> pushScreen(AppSubScreen.LibrarySection(tabIdx)) },
                                    onOpenNotifications = { pushScreen(AppSubScreen.Notifications) },
                                    onOpenSettings = { pushScreen(AppSubScreen.SettingsSecurity) }
                                )

                                MainTab.CREATE -> Unit
                            }
                        }
                    }
                }
            }

            // Full Video Player Overlay
            if (playerState.displayMode == PlayerDisplayMode.FULL) {
                VideoPlayerOverlay(
                    viewModel = viewModel,
                    playerManager = viewModel.playerManager,
                    playerState = playerState,
                    recommendedVideos = feedVideos,
                    onOpenChannel = { cid -> pushScreen(AppSubScreen.ChannelDetail(cid)) }
                )
            }

            // Draggable & Resizable Floating Window (Ventana Flotante)
            if (playerState.displayMode == PlayerDisplayMode.FLOATING && !playerState.isInSystemPip) {
                FloatingMiniPlayer(
                    playerManager = viewModel.playerManager,
                    playerState = playerState
                )
            }

            // Create (+) Bottom Sheet: Subir video, Crear Short, Transmitir en directo, Crear publicación
            if (showCreateSheet) {
                ModalBottomSheet(onDismissRequest = { showCreateSheet = false }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Crear en DeiPlay", style = MaterialTheme.typography.titleLarge)
                        CreateSheetOption(Icons.Filled.VideoLibrary, "Subir video", "Publica videos largos desde tu teléfono o archivos") {
                            showCreateSheet = false
                            pushScreen(AppSubScreen.UploadVideo(isShort = false, isPost = false))
                        }
                        CreateSheetOption(Icons.Filled.VideoCall, "Crear Short", "Video vertical corto con reproducción automática") {
                            showCreateSheet = false
                            pushScreen(AppSubScreen.UploadVideo(isShort = true, isPost = false))
                        }
                        CreateSheetOption(Icons.Filled.LiveTv, "Transmitir en directo", "Emite en vivo con cámara y chat moderado") {
                            showCreateSheet = false
                            pushScreen(AppSubScreen.LiveBroadcast)
                        }
                        CreateSheetOption(Icons.Filled.Create, "Crear publicación", "Comparte novedades y texto con tu comunidad") {
                            showCreateSheet = false
                            pushScreen(AppSubScreen.UploadVideo(isShort = false, isPost = true))
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateSheetOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = DeiRed)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun tabIcon(tab: MainTab): ImageVector = when (tab) {
    MainTab.HOME -> Icons.Filled.Home
    MainTab.SHORTS -> Icons.Filled.Bolt
    MainTab.CREATE -> Icons.Filled.AddCircle
    MainTab.SUBSCRIPTIONS -> Icons.Filled.Subscriptions
    MainTab.PROFILE -> Icons.Filled.Person
}
