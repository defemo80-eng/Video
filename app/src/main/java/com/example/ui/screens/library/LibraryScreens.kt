package com.example.ui.screens.library

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.PlaylistEntity
import com.example.data.local.VideoEntity
import com.example.ui.DeiPlayViewModel
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.DeiRed
import com.example.ui.theme.DeiWhite
import java.io.File

private val LIBRARY_TABS = listOf(
    "Historial", "Ver más tarde", "Me gustan", "Guardados", "Descargas", "Listas"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: DeiPlayViewModel,
    initialTab: Int = 0,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, LIBRARY_TABS.lastIndex)) }

    val history by viewModel.watchHistory.collectAsStateWithLifecycle()
    val watchLater by viewModel.watchLater.collectAsStateWithLifecycle()
    val likedReactions by viewModel.likedVideoReactions.collectAsStateWithLifecycle()
    val savedFavorites by viewModel.savedFavorites.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val allFeed by viewModel.combinedFeedVideos.collectAsStateWithLifecycle()

    var showCreatePlaylist by remember { mutableStateOf(false) }
    var activePlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi biblioteca") },
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
        ) {
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 12.dp) {
                LIBRARY_TABS.forEachIndexed { idx, label ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(label) }
                    )
                }
            }

            when (selectedTab) {
                0 -> { // Historial
                    LazyColumn(
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (history.isNotEmpty()) {
                            item {
                                OutlinedButton(onClick = { viewModel.clearHistory() }) {
                                    Text("Limpiar historial")
                                }
                            }
                            items(history, key = { it.videoId }) { h ->
                                SimpleLibraryRow(
                                    title = h.title,
                                    subtitle = "${h.channelName} · ${h.sourcePlatform}",
                                    onPlay = {
                                        viewModel.openVideo(
                                            VideoEntity(
                                                id = h.videoId,
                                                channelId = "hist_${h.videoId}",
                                                ownerUserId = "",
                                                channelName = h.channelName,
                                                title = h.title,
                                                mediaUri = h.mediaUri,
                                                thumbnailUri = h.thumbnailUri,
                                                sourcePlatform = h.sourcePlatform
                                            )
                                        )
                                    }
                                )
                            }
                        } else {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Filled.History,
                                    title = "Historial vacío",
                                    subtitle = "Los videos que reproduzcas aparecerán aquí automáticamente."
                                )
                            }
                        }
                    }
                }

                1 -> { // Ver más tarde
                    LazyColumn(
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (watchLater.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Filled.WatchLater,
                                    title = "Sin videos en Ver más tarde",
                                    subtitle = "Agrega videos desde cualquier tarjeta o reproductor."
                                )
                            }
                        } else {
                            items(watchLater, key = { it.videoId }) { item ->
                                SimpleLibraryRow(
                                    title = item.title,
                                    subtitle = item.channelName,
                                    onPlay = {
                                        viewModel.openVideo(
                                            VideoEntity(
                                                id = item.videoId,
                                                channelId = "wl_${item.videoId}",
                                                ownerUserId = "",
                                                channelName = item.channelName,
                                                title = item.title,
                                                mediaUri = item.mediaUri,
                                                thumbnailUri = item.thumbnailUri,
                                                sourcePlatform = item.sourcePlatform
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                2 -> { // Me gustan
                    val likedIds = likedReactions.map { it.videoId }.toSet()
                    val likedVideos = allFeed.filter { likedIds.contains(it.id) }
                    LazyColumn(
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (likedVideos.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Filled.ThumbUp,
                                    title = "Aún no tienes videos con Me gusta",
                                    subtitle = "Los videos que marques con Me gusta aparecerán aquí."
                                )
                            }
                        } else {
                            items(likedVideos, key = { it.id }) { vid ->
                                SimpleLibraryRow(
                                    title = vid.title,
                                    subtitle = "${vid.channelName} · ${vid.likesCount} likes",
                                    onPlay = { viewModel.openVideo(vid) }
                                )
                            }
                        }
                    }
                }

                3 -> { // Guardados / Favoritos
                    LazyColumn(
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (savedFavorites.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Filled.Bookmark,
                                    title = "Sin videos guardados",
                                    subtitle = "Usa el botón Favoritos en el reproductor para guardar tus videos clave."
                                )
                            }
                        } else {
                            items(savedFavorites, key = { it.videoId }) { fav ->
                                SimpleLibraryRow(
                                    title = fav.title,
                                    subtitle = fav.channelName,
                                    onPlay = {
                                        viewModel.openVideo(
                                            VideoEntity(
                                                id = fav.videoId,
                                                channelId = "fav_${fav.videoId}",
                                                ownerUserId = "",
                                                channelName = fav.channelName,
                                                title = fav.title,
                                                mediaUri = fav.mediaUri,
                                                thumbnailUri = fav.thumbnailUri,
                                                sourcePlatform = fav.sourcePlatform
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                4 -> { // Descargas sin conexión + Espacio ocupado
                    val totalBytes = downloads.sumOf { File(it.localFilePath).length().takeIf { len -> len > 0 } ?: it.sizeBytes }
                    val mbUsed = String.format("%.2f MB", totalBytes / (1024.0 * 1024.0))

                    LazyColumn(
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Espacio ocupado sin conexión", style = MaterialTheme.typography.titleSmall)
                                    Text(mbUsed, style = MaterialTheme.typography.titleMedium, color = DeiRed)
                                }
                            }
                        }

                        if (downloads.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Filled.DownloadDone,
                                    title = "Sin descargas guardadas",
                                    subtitle = "Descarga videos autorizados desde el reproductor para verlos sin conexión a internet."
                                )
                            }
                        } else {
                            items(downloads, key = { it.videoId }) { dl ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.openVideo(
                                                VideoEntity(
                                                    id = dl.videoId,
                                                    channelId = "dl_${dl.videoId}",
                                                    ownerUserId = "",
                                                    channelName = dl.channelName,
                                                    title = dl.title,
                                                    mediaUri = Uri.fromFile(File(dl.localFilePath)).toString(),
                                                    thumbnailUri = dl.thumbnailUri,
                                                    sourcePlatform = "DEIPLAY"
                                                )
                                            )
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(dl.title, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                "${dl.channelName} · ${dl.qualityLabel} · ${String.format("%.1f MB", dl.sizeBytes / (1024.0 * 1024.0))}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { viewModel.deleteDownload(dl) }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar descarga", tint = DeiRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                5 -> { // Listas de reproducción
                    LazyColumn(
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Button(
                                onClick = { showCreatePlaylist = true },
                                colors = ButtonDefaults.buttonColors(containerColor = DeiRed, contentColor = DeiWhite),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Add, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Crear nueva lista de reproducción")
                            }
                        }

                        if (playlists.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Filled.PlaylistPlay,
                                    title = "Sin listas de reproducción",
                                    subtitle = "Crea listas públicas o privadas para organizar y reordenar tus videos."
                                )
                            }
                        } else {
                            items(playlists, key = { it.id }) { pl ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activePlaylist = pl }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (pl.isPublic) Icons.Filled.Public else Icons.Filled.Lock,
                                            contentDescription = null,
                                            tint = DeiRed
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(pl.name, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                if (pl.isPublic) "Lista pública · Toca para administrar" else "Lista privada · Toca para administrar",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = {
                                            val send = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, "Lista de reproducción en DeiPlay: ${pl.name}")
                                            }
                                            context.startActivity(Intent.createChooser(send, "Compartir lista"))
                                        }) {
                                            Icon(Icons.Filled.Share, contentDescription = "Compartir lista")
                                        }
                                        IconButton(onClick = { viewModel.deletePlaylist(pl.id) }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar lista")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePlaylist) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var isPublic by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { showCreatePlaylist = false },
            title = { Text("Nueva lista de reproducción") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Descripción") })
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Lista pública")
                        Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createPlaylist(name, desc, isPublic)
                        showCreatePlaylist = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeiRed)
                ) { Text("Crear") }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylist = false }) { Text("Cancelar") }
            }
        )
    }

    activePlaylist?.let { pl ->
        val items by viewModel.observePlaylistItems(pl.id).collectAsStateWithLifecycle(initialValue = emptyList())
        AlertDialog(
            onDismissRequest = { activePlaylist = null },
            title = { Text(pl.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (pl.isPublic) "Visibilidad: Pública" else "Visibilidad: Privada")
                        TextButton(onClick = { viewModel.togglePlaylistVisibility(pl) }) {
                            Text("Cambiar")
                        }
                    }
                    if (items.isEmpty()) {
                        Text("Aún no has agregado videos a esta lista.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        items.forEachIndexed { idx, item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${idx + 1}. ${item.videoTitle}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            activePlaylist = null
                                            viewModel.openVideo(
                                                VideoEntity(
                                                    id = item.videoId,
                                                    channelId = "pl_${item.videoId}",
                                                    ownerUserId = "",
                                                    channelName = item.channelName,
                                                    title = item.videoTitle,
                                                    mediaUri = item.mediaUri,
                                                    thumbnailUri = item.videoThumbnailUri
                                                )
                                            )
                                        }
                                )
                                IconButton(
                                    onClick = { viewModel.movePlaylistItem(pl.id, item.id, moveUp = true) },
                                    enabled = idx > 0
                                ) {
                                    Icon(Icons.Filled.ArrowUpward, contentDescription = "Subir")
                                }
                                IconButton(
                                    onClick = { viewModel.movePlaylistItem(pl.id, item.id, moveUp = false) },
                                    enabled = idx < items.lastIndex
                                ) {
                                    Icon(Icons.Filled.ArrowDownward, contentDescription = "Bajar")
                                }
                                IconButton(onClick = { viewModel.removeVideoFromPlaylist(pl.id, item.videoId) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Quitar")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activePlaylist = null }) { Text("Cerrar") }
            }
        )
    }
}

@Composable
private fun SimpleLibraryRow(
    title: String,
    subtitle: String,
    onPlay: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = DeiRed)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
