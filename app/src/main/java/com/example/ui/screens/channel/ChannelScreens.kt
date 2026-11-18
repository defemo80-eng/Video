package com.example.ui.screens.channel

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.DeiPlayViewModel
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.VideoCard
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.DeiBlack
import com.example.ui.theme.DeiRed
import com.example.ui.theme.DeiWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CHANNEL_TABS = listOf(
    "Inicio", "Videos", "Shorts", "En directo", "Listas de reproducción", "Información"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelDetailScreen(
    viewModel: DeiPlayViewModel,
    channelId: String,
    onBack: () -> Unit,
    onEditChannel: () -> Unit,
    onOpenLiveViewer: (String) -> Unit
) {
    BackHandler(onBack = onBack)

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val localChannel by viewModel.observeChannel(channelId).collectAsStateWithLifecycle(initialValue = null)
    val localChannelVideos by viewModel.observeChannelVideos(channelId).collectAsStateWithLifecycle(initialValue = emptyList())
    val feedVideos by viewModel.combinedFeedVideos.collectAsStateWithLifecycle()
    val subscription by viewModel.observeSubscription(channelId).collectAsStateWithLifecycle(initialValue = null)
    val activeLives by viewModel.activeLives.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(channelId, localChannel) {
        if (localChannel == null) {
            viewModel.loadExternalChannelVideos(channelId)
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    val externalMatchingVideos = remember(feedVideos, channelId) {
        feedVideos.filter { it.channelId == channelId }
    }
    val ch = localChannel ?: externalMatchingVideos.firstOrNull()?.let { sample ->
        com.example.data.local.ChannelEntity(
            id = channelId,
            ownerUserId = sample.ownerUserId,
            name = sample.channelName,
            handle = channelId.substringAfter("|", sample.channelName.lowercase().replace(" ", "")),
            avatarUri = sample.channelAvatarUri,
            description = "Canal mundial de ${sample.sourcePlatform}. Todos los videos publicados por este creador se sincronizan aquí en DeiPlay.",
            country = sample.country,
            videoCount = externalMatchingVideos.size,
            totalViews = externalMatchingVideos.sumOf { it.viewsCount }
        )
    }
    val channelVideos = if (localChannel != null) localChannelVideos else externalMatchingVideos
    val isDeiPlayChannel = localChannel != null
    val isOwner = ch != null && ch.ownerUserId == currentUser?.id
    val channelLives = remember(activeLives, channelId) { activeLives.filter { it.channelId == channelId } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ch?.name ?: "Canal") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (isOwner) {
                        IconButton(onClick = onEditChannel) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar canal")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (ch == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyStateCard(
                    icon = Icons.Filled.VideoLibrary,
                    title = "Canal no disponible todavía",
                    subtitle = "No se encontraron videos públicos sincronizados para este creador en este momento."
                )
            }
            return@Scaffold
        }

        val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale("es", "ES")) }
        val createdStr = dateFormatter.format(Date(ch.createdAt))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            // Banner + Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(
                            Brush.horizontalGradient(listOf(DeiBlack, DeiRed.copy(alpha = 0.7f)))
                        )
                ) {
                    if (!ch.bannerUri.isNullOrBlank()) {
                        AsyncImage(
                            model = ch.bannerUri,
                            contentDescription = "Portada del canal",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChannelAvatar(ch.avatarUri, ch.name, size = 68.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ch.name, style = MaterialTheme.typography.headlineSmall)
                            Text(
                                "@${ch.handle} · ${ch.country}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${formatCompactCount(ch.subscriberCount)} suscriptores · ${ch.videoCount} videos · ${formatCompactCount(ch.totalViews)} visualizaciones",
                                style = MaterialTheme.typography.labelMedium,
                                color = DeiRed
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (subscription != null) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.toggleSubscribe(ch.id, ch.name, ch.avatarUri, isDeiPlayChannel)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("channel_unsubscribe_btn")
                            ) {
                                Text("Suscrito · Cancelar suscripción")
                            }
                            IconButton(
                                onClick = { viewModel.toggleBell(ch.id, subscription!!.notificationsEnabled) }
                            ) {
                                Icon(
                                    if (subscription!!.notificationsEnabled) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                                    contentDescription = "Campana de notificaciones",
                                    tint = if (subscription!!.notificationsEnabled) DeiRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    viewModel.toggleSubscribe(ch.id, ch.name, ch.avatarUri, isDeiPlayChannel)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DeiRed, contentColor = DeiWhite),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("channel_subscribe_btn")
                            ) {
                                Text("Suscribirse")
                            }
                        }
                    }
                }
            }

            // Channel Tabs
            item {
                ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 12.dp) {
                    CHANNEL_TABS.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            val publicOrOwned = channelVideos.filter { it.privacy == "PUBLIC" || isOwner }

            when (selectedTab) {
                0 -> { // Inicio
                    if (publicOrOwned.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.VideoLibrary,
                                title = "Sin contenido publicado aún",
                                subtitle = "Cuando este canal publique videos, Shorts o publicaciones aparecerán aquí."
                            )
                        }
                    } else {
                        items(publicOrOwned, key = { "ch_home_${it.id}" }) { vid ->
                            Box(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                                VideoCard(
                                    video = vid,
                                    onClick = { viewModel.openVideo(vid) },
                                    onChannelClick = {},
                                    onWatchLater = { viewModel.toggleWatchLater(vid, false) },
                                    onAddToPlaylist = {},
                                    onReport = { viewModel.reportContent("VIDEO", vid.id, "Reporte") }
                                )
                            }
                        }
                    }
                }

                1 -> { // Videos
                    val longVideos = publicOrOwned.filter { !it.isShort && !it.isPost }
                    if (longVideos.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.VideoLibrary,
                                title = "Sin videos largos",
                                subtitle = "Este canal aún no ha subido videos."
                            )
                        }
                    } else {
                        items(longVideos, key = { "ch_vid_${it.id}" }) { vid ->
                            Box(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                                VideoCard(
                                    video = vid,
                                    onClick = { viewModel.openVideo(vid) },
                                    onChannelClick = {},
                                    onWatchLater = { viewModel.toggleWatchLater(vid, false) },
                                    onAddToPlaylist = {},
                                    onReport = { viewModel.reportContent("VIDEO", vid.id, "Reporte") }
                                )
                            }
                        }
                    }
                }

                2 -> { // Shorts
                    val shortList = publicOrOwned.filter { it.isShort }
                    if (shortList.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.VideoLibrary,
                                title = "Sin Shorts",
                                subtitle = "Este canal aún no ha subido Shorts."
                            )
                        }
                    } else {
                        items(shortList, key = { "ch_short_${it.id}" }) { vid ->
                            Box(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                                VideoCard(
                                    video = vid,
                                    onClick = { viewModel.openVideo(vid) },
                                    onChannelClick = {},
                                    onWatchLater = { viewModel.toggleWatchLater(vid, false) },
                                    onAddToPlaylist = {},
                                    onReport = { viewModel.reportContent("VIDEO", vid.id, "Reporte") }
                                )
                            }
                        }
                    }
                }

                3 -> { // En directo
                    if (channelLives.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.VideoLibrary,
                                title = "Sin transmisiones en vivo activas",
                                subtitle = "Cuando el creador inicie un directo aparecerá aquí."
                            )
                        }
                    } else {
                        items(channelLives, key = { it.id }) { live ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = DeiRed.copy(alpha = 0.14f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                                    .clickable { onOpenLiveViewer(live.id) }
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Text("EN VIVO: ${live.title}", style = MaterialTheme.typography.titleMedium, color = DeiRed)
                                    Text(live.description, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                4 -> { // Listas
                    val visibleLists = playlists.filter { it.ownerUserId == ch.ownerUserId && (it.isPublic || isOwner) }
                    if (visibleLists.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.PlaylistPlay,
                                title = "Sin listas de reproducción públicas",
                                subtitle = "Las listas creadas por este canal aparecerán aquí."
                            )
                        }
                    } else {
                        items(visibleLists, key = { it.id }) { pl ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 5.dp)
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Text(pl.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        if (pl.isPublic) "Lista pública" else "Lista privada",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                5 -> { // Información
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Descripción", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(ch.description.ifBlank { "Sin descripción agregada." }, style = MaterialTheme.typography.bodyMedium)
                                Text("País: ${ch.country}", style = MaterialTheme.typography.bodyMedium)
                                Text("Fecha de creación: $createdStr", style = MaterialTheme.typography.bodyMedium)
                                Text("Suscriptores: ${ch.subscriberCount}", style = MaterialTheme.typography.bodyMedium)
                                Text("Total de visualizaciones: ${ch.totalViews}", style = MaterialTheme.typography.bodyMedium)
                                Text("Videos publicados: ${ch.videoCount}", style = MaterialTheme.typography.bodyMedium)
                                if (ch.socialLinks.isNotBlank()) {
                                    Text("Enlaces sociales: ${ch.socialLinks}", style = MaterialTheme.typography.bodyMedium, color = DeiRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrEditChannelScreen(
    viewModel: DeiPlayViewModel,
    onBack: () -> Unit,
    onSaved: (String) -> Unit
) {
    BackHandler(onBack = onBack)

    val existing by viewModel.myChannel.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var name by remember(existing) { mutableStateOf(existing?.name ?: currentUser?.displayName.orEmpty()) }
    var handle by remember(existing) { mutableStateOf(existing?.handle ?: currentUser?.username.orEmpty()) }
    var description by remember(existing) { mutableStateOf(existing?.description.orEmpty()) }
    var country by remember(existing) { mutableStateOf(existing?.country ?: "México") }
    var socialLinks by remember(existing) { mutableStateOf(existing?.socialLinks.orEmpty()) }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var bannerUri by remember { mutableStateOf<Uri?>(null) }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) avatarUri = uri
    }
    val bannerPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) bannerUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Crear mi canal" else "Personalizar mi canal") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Image, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (avatarUri != null) "Foto elegida ✓" else "Elegir foto")
                }
                OutlinedButton(
                    onClick = {
                        bannerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Image, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (bannerUri != null) "Portada elegida ✓" else "Elegir portada")
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre del canal") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("channel_name_input")
            )

            OutlinedTextField(
                value = handle,
                onValueChange = { handle = it },
                label = { Text("Nombre de usuario (@handle)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("channel_handle_input")
            )

            OutlinedTextField(
                value = country,
                onValueChange = { country = it },
                label = { Text("País") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción del canal") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = socialLinks,
                onValueChange = { socialLinks = it },
                label = { Text("Enlaces sociales (Instagram, TikTok, Web...)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    viewModel.createOrUpdateChannel(
                        name = name,
                        handle = handle,
                        description = description,
                        country = country,
                        socialLinks = socialLinks,
                        avatarUri = avatarUri,
                        bannerUri = bannerUri
                    ) { created ->
                        onSaved(created.id)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DeiRed, contentColor = DeiWhite),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_channel_btn")
            ) {
                Text(if (existing == null) "Crear mi canal ahora" else "Guardar cambios del canal")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
