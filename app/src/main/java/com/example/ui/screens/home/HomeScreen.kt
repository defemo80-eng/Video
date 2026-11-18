package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.VideoEntity
import com.example.ui.DeiPlayViewModel
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PlaylistPickerDialog
import com.example.ui.components.VideoCard
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.DeiPlayWordmark
import com.example.ui.theme.DeiRed
import com.example.ui.theme.DeiWhite

private val HOME_CATEGORIES = listOf(
    "Para ti",
    "Inicio",
    "Shorts",
    "Tendencias",
    "Suscripciones",
    "Música",
    "Deportes",
    "Noticias",
    "Videojuegos",
    "Entretenimiento",
    "Educación",
    "Tecnología",
    "Películas y videos",
    "Directos"
)

private val HOME_LANGUAGES = listOf(
    "Todos", "Español", "English", "Français", "Português", "Deutsch", "日本語"
)

@Composable
fun HomeScreen(
    viewModel: DeiPlayViewModel,
    onOpenChannel: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenShortsTab: () -> Unit,
    onOpenSubscriptionsTab: () -> Unit,
    onOpenLiveViewer: (String) -> Unit,
    onOpenUpload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val feedVideos by viewModel.combinedFeedVideos.collectAsStateWithLifecycle()
    val allChannels by viewModel.allChannels.collectAsStateWithLifecycle()
    val activeLives by viewModel.activeLives.collectAsStateWithLifecycle()
    val isLoading by viewModel.isWorldLoading.collectAsStateWithLifecycle()
    val worldError by viewModel.worldError.collectAsStateWithLifecycle()
    val aiRec by viewModel.aiRecommendations.collectAsStateWithLifecycle()
    val unreadNotifCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()
    val deiPlaySubs by viewModel.deiPlaySubscriptions.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    var showLanguageMenu by remember { mutableStateOf(false) }
    var showYouTubeLinkDialog by remember { mutableStateOf(false) }
    var playlistTargetVideo by remember { mutableStateOf<VideoEntity?>(null) }

    val displayedVideos = remember(feedVideos, selectedCategory, deiPlaySubs) {
        if (selectedCategory == "Suscripciones") {
            val subIds = deiPlaySubs.map { it.channelId }.toSet()
            feedVideos.filter { subIds.contains(it.channelId) }
        } else {
            feedVideos
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top Brand + Search Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DeiPlayWordmark(iconSize = 30.dp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            IconButton(onClick = { showLanguageMenu = true }) {
                                Icon(Icons.Filled.Language, contentDescription = "Filtrar idioma")
                            }
                            DropdownMenu(
                                expanded = showLanguageMenu,
                                onDismissRequest = { showLanguageMenu = false }
                            ) {
                                HOME_LANGUAGES.forEach { lang ->
                                    DropdownMenuItem(
                                        text = { Text(if (lang == selectedLanguage) "✓ $lang" else lang) },
                                        onClick = {
                                            viewModel.selectLanguage(lang)
                                            showLanguageMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        IconButton(onClick = { showYouTubeLinkDialog = true }) {
                            Icon(Icons.Filled.Link, contentDescription = "Importar video de YouTube por URL")
                        }

                        IconButton(onClick = { viewModel.refreshWorldVideos() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Actualizar feed mundial")
                        }

                        IconButton(onClick = onOpenNotifications) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotifCount > 0) {
                                        Badge(containerColor = DeiRed) {
                                            Text("$unreadNotifCount", color = DeiWhite)
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Notifications, contentDescription = "Notificaciones")
                            }
                        }
                    }
                }

                // Top Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Buscar videos, creadores o países en el mundo...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = {
                                viewModel.updateSearchQuery("")
                                viewModel.refreshWorldVideos()
                            }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.refreshWorldVideos() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_search_input")
                )

                // Category Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HOME_CATEGORIES.forEach { cat ->
                        val selected = selectedCategory == cat
                        FilterChip(
                            selected = selected,
                            onClick = {
                                when (cat) {
                                    "Shorts" -> onOpenShortsTab()
                                    "Suscripciones" -> onOpenSubscriptionsTab()
                                    else -> viewModel.selectCategory(cat)
                                }
                            },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DeiRed,
                                selectedLabelColor = DeiWhite
                            ),
                            modifier = Modifier.testTag("cat_chip_$cat")
                        )
                    }
                }
            }
        }

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = DeiRed
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!worldError.isNullOrBlank()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DeiRed.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = worldError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // AI Personalized Recommendations Card (with mandatory declined-Firebase notice)
            if (selectedCategory == "Para ti" && aiRec != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AutoAwesome, null, tint = DeiRed)
                                Spacer(Modifier.width(8.dp))
                                Text(aiRec!!.headline, style = MaterialTheme.typography.titleSmall)
                            }
                            Text(aiRec!!.summary, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = aiRec!!.notice,
                                style = MaterialTheme.typography.labelSmall,
                                color = DeiRed
                            )
                        }
                    }
                }
            }

            // Active Live Streams Banner
            if (activeLives.isNotEmpty()) {
                item {
                    Text("Directos en vivo ahora", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(activeLives, key = { it.id }) { live ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = DeiRed.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .width(230.dp)
                                    .clickable { onOpenLiveViewer(live.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.LiveTv, null, tint = DeiRed)
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(live.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${live.channelName} · EN VIVO", style = MaterialTheme.typography.labelSmall, color = DeiRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Popular Creators Strip (DeiPlay local channels + discovered world creators)
            val discoveredCreators = feedVideos
                .distinctBy { it.channelId }
                .take(10)
            if (allChannels.isNotEmpty() || discoveredCreators.isNotEmpty()) {
                item {
                    Text("Creadores populares", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(allChannels, key = { "ch_${it.id}" }) { ch ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(82.dp)
                                    .clickable { onOpenChannel(ch.id) }
                            ) {
                                ChannelAvatar(ch.avatarUri, ch.name, size = 54.dp)
                                Spacer(Modifier.height(4.dp))
                                Text(ch.name, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${formatCompactCount(ch.subscriberCount)} subs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        items(discoveredCreators.filter { ext -> allChannels.none { it.id == ext.channelId } }, key = { "ext_${it.channelId}" }) { vid ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(82.dp)
                                    .clickable { viewModel.openVideo(vid) }
                            ) {
                                ChannelAvatar(vid.channelAvatarUri, vid.channelName, size = 54.dp)
                                Spacer(Modifier.height(4.dp))
                                Text(vid.channelName, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(vid.country, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedCategory == "Inicio") "Videos recomendados y mundiales" else selectedCategory,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedLanguage != "Todos") {
                        Text(selectedLanguage, style = MaterialTheme.typography.labelMedium, color = DeiRed)
                    }
                }
            }

            if (displayedVideos.isEmpty() && !isLoading) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.VideoLibrary,
                        title = "Aún no hay videos en esta vista",
                        subtitle = "Sube tu primer video desde tu teléfono o actualiza el índice mundial para descubrir creadores.",
                        actionLabel = "Subir video",
                        onAction = onOpenUpload
                    )
                }
            } else {
                items(displayedVideos, key = { it.id }) { video ->
                    VideoCard(
                        video = video,
                        onClick = { viewModel.openVideo(video) },
                        onChannelClick = { onOpenChannel(video.channelId) },
                        onWatchLater = { viewModel.toggleWatchLater(video, false) },
                        onAddToPlaylist = { playlistTargetVideo = video },
                        onReport = { viewModel.reportContent("VIDEO", video.id, "Reporte desde Inicio") }
                    )
                }
            }
        }
    }

    if (showYouTubeLinkDialog) {
        var urlInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showYouTubeLinkDialog = false },
            title = { Text("Importar enlace oficial de YouTube") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Pega cualquier enlace de YouTube para consultar su título, creador y miniatura oficial sin necesidad de API Key (YouTube oEmbed).",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("https://youtube.com/watch?v=...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.importYouTubeUrl(urlInput)
                        showYouTubeLinkDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeiRed)
                ) { Text("Consultar") }
            },
            dismissButton = {
                TextButton(onClick = { showYouTubeLinkDialog = false }) { Text("Cancelar") }
            }
        )
    }

    playlistTargetVideo?.let { vid ->
        PlaylistPickerDialog(
            playlists = playlists,
            onDismiss = { playlistTargetVideo = null },
            onSelectPlaylist = { pl ->
                viewModel.addVideoToPlaylist(pl.id, vid)
                playlistTargetVideo = null
            },
            onCreatePlaylist = { name -> viewModel.createPlaylist(name, "", true) }
        )
    }
}
