package com.example.ui.components

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import com.example.data.local.CommentEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.VideoEntity
import com.example.player.DeiPlayPlayerManager
import com.example.player.PlayerUiState
import com.example.player.findActivityOrNull
import com.example.ui.DeiPlayViewModel
import com.example.ui.theme.DeiBlack
import com.example.ui.theme.DeiRed
import com.example.ui.theme.DeiWhite

@Composable
fun VideoPlayerOverlay(
    viewModel: DeiPlayViewModel,
    playerManager: DeiPlayPlayerManager,
    playerState: PlayerUiState,
    recommendedVideos: List<VideoEntity>,
    onOpenChannel: (String) -> Unit
) {
    val video = playerState.activeVideo ?: return
    val context = LocalContext.current
    val activity = context.findActivityOrNull()

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val reaction by viewModel.observeReaction(video.id).collectAsStateWithLifecycle(initialValue = null)
    val subscription by viewModel.observeSubscription(video.channelId).collectAsStateWithLifecycle(initialValue = null)
    val isWatchLater by viewModel.observeIsWatchLater(video.id).collectAsStateWithLifecycle(initialValue = false)
    val isFavorite by viewModel.observeIsSavedFavorite(video.id).collectAsStateWithLifecycle(initialValue = false)
    val comments by viewModel.observeComments(video.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val likedCommentIds by viewModel.observeLikedCommentIds().collectAsStateWithLifecycle(initialValue = emptyList())
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()

    var showControls by remember { mutableStateOf(true) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var commentDraft by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<CommentEntity?>(null) }

    BackHandler {
        if (playerState.isFullscreen) {
            playerManager.setFullscreen(false)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            playerManager.minimizeToFloating()
        }
    }

    DisposableEffect(playerState.isFullscreen) {
        if (playerState.isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("full_video_player"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Video Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (playerState.isFullscreen || playerState.isInSystemPip) Modifier.fillMaxSize()
                        else Modifier
                            .statusBarsPadding()
                            .aspectRatio(16f / 9f)
                    )
                    .background(DeiBlack)
                    .clickable { showControls = !showControls }
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            player = playerManager.exoPlayer
                        }
                    },
                    update = { view ->
                        if (view.player !== playerManager.exoPlayer) {
                            view.player = playerManager.exoPlayer
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (playerState.isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = DeiRed
                    )
                }

                // Subtitles overlay
                if (playerState.subtitlesEnabled && video.subtitlesText.isNotBlank() && !playerState.isInSystemPip) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = if (showControls) 56.dp else 14.dp, start = 16.dp, end = 16.dp),
                        color = DeiBlack.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = video.subtitlesText,
                            color = DeiWhite,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Error or external YouTube prompt
                if (!playerState.errorMessage.isNullOrBlank()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = playerState.errorMessage,
                            color = DeiWhite,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (video.sourcePlatform == "YOUTUBE" && !video.externalUrl.isNullOrBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(video.externalUrl))
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DeiRed)
                            ) {
                                Icon(Icons.Filled.OpenInBrowser, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Abrir en YouTube")
                            }
                        }
                    }
                }

                // Custom Player Controls Overlay (hidden automatically in System PiP)
                if (showControls && !playerState.isInSystemPip) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DeiBlack.copy(alpha = 0.42f))
                    ) {
                        // Top bar: Minimize to Floating Window, System PiP, CC, Quality, Speed, Close
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { playerManager.minimizeToFloating() },
                                    modifier = Modifier.testTag("minimize_to_floating_btn")
                                ) {
                                    Icon(
                                        Icons.Filled.KeyboardArrowDown,
                                        contentDescription = "Minimizar a ventana flotante",
                                        tint = DeiWhite
                                    )
                                }
                                IconButton(
                                    onClick = { playerManager.enterSystemPip(context) },
                                    modifier = Modifier.testTag("system_pip_btn")
                                ) {
                                    Icon(
                                        Icons.Filled.PictureInPictureAlt,
                                        contentDescription = "Modo Picture-in-Picture",
                                        tint = DeiWhite
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { playerManager.toggleSubtitles() }) {
                                    Icon(
                                        Icons.Filled.ClosedCaption,
                                        contentDescription = "Subtítulos",
                                        tint = if (playerState.subtitlesEnabled) DeiRed else DeiWhite
                                    )
                                }

                                // Speed selector
                                Box {
                                    TextButton(onClick = { showSpeedMenu = true }) {
                                        Icon(Icons.Filled.Speed, null, tint = DeiWhite, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(3.dp))
                                        Text("${playerState.playbackSpeed}x", color = DeiWhite)
                                    }
                                    DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { showSpeedMenu = false }) {
                                        listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                            DropdownMenuItem(
                                                text = { Text("${speed}x") },
                                                onClick = {
                                                    playerManager.setPlaybackSpeed(speed)
                                                    showSpeedMenu = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Quality selector
                                Box {
                                    TextButton(onClick = { showQualityMenu = true }) {
                                        Icon(Icons.Filled.Tune, null, tint = DeiWhite, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(3.dp))
                                        Text(playerState.selectedQualityLabel, color = DeiWhite)
                                    }
                                    DropdownMenu(expanded = showQualityMenu, onDismissRequest = { showQualityMenu = false }) {
                                        playerState.qualityOptions.forEach { opt ->
                                            DropdownMenuItem(
                                                text = { Text(opt.label) },
                                                onClick = {
                                                    playerManager.selectQuality(opt)
                                                    showQualityMenu = false
                                                }
                                            )
                                        }
                                    }
                                }

                                IconButton(onClick = { playerManager.closePlayer() }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Cerrar reproductor", tint = DeiWhite)
                                }
                            }
                        }

                        // Center transport buttons: -10s, Play/Pause, +10s, Next
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { playerManager.seekBy(-10_000L) }) {
                                Icon(Icons.Filled.Replay10, contentDescription = "Retroceder 10s", tint = DeiWhite, modifier = Modifier.size(34.dp))
                            }
                            Surface(
                                shape = CircleShape,
                                color = DeiRed,
                                modifier = Modifier.size(56.dp)
                            ) {
                                IconButton(
                                    onClick = { playerManager.togglePlayPause() },
                                    modifier = Modifier.testTag("player_play_pause_btn")
                                ) {
                                    Icon(
                                        imageVector = if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = if (playerState.isPlaying) "Pausar" else "Reproducir",
                                        tint = DeiWhite,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                            IconButton(onClick = { playerManager.seekBy(10_000L) }) {
                                Icon(Icons.Filled.Forward10, contentDescription = "Adelantar 10s", tint = DeiWhite, modifier = Modifier.size(34.dp))
                            }
                            IconButton(onClick = { viewModel.playNextVideo() }) {
                                Icon(Icons.Filled.SkipNext, contentDescription = "Siguiente video", tint = DeiWhite, modifier = Modifier.size(30.dp))
                            }
                        }

                        // Bottom seekbar + volume + fullscreen
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            val totalMs = playerState.durationMs.coerceAtLeast(1L)
                            val posMs = playerState.currentPositionMs.coerceIn(0L, totalMs)
                            Slider(
                                value = posMs.toFloat() / totalMs.toFloat(),
                                onValueChange = { frac -> playerManager.seekTo((frac * totalMs).toLong()) },
                                colors = SliderDefaults.colors(thumbColor = DeiRed, activeTrackColor = DeiRed),
                                modifier = Modifier.height(26.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${formatDuration(posMs / 1000L)} / ${formatDuration(totalMs / 1000L)}",
                                    color = DeiWhite,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { playerManager.toggleMute() },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            if (playerState.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Silenciar o activar audio",
                                            tint = DeiWhite,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Slider(
                                        value = if (playerState.isMuted) 0f else playerState.volume,
                                        onValueChange = { playerManager.setVolume(it) },
                                        modifier = Modifier.width(72.dp).height(22.dp),
                                        colors = SliderDefaults.colors(thumbColor = DeiWhite, activeTrackColor = DeiWhite)
                                    )
                                    IconButton(
                                        onClick = { playerManager.setFullscreen(!playerState.isFullscreen) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            if (playerState.isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                            contentDescription = "Pantalla completa",
                                            tint = DeiWhite
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!playerState.isFullscreen && !playerState.isInSystemPip) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${formatCompactCount(video.viewsCount)} vistas · ${video.category} · ${video.language.uppercase()} · ${video.sourcePlatform}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (video.description.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = video.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Channel & Subscribe bar
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            playerManager.minimizeToFloating()
                                            onOpenChannel(video.channelId)
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ChannelAvatar(video.channelAvatarUri, video.channelName, size = 42.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(video.channelName, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = if (video.sourcePlatform == "DEIPLAY") "Canal DeiPlay" else "Creador ${video.sourcePlatform}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (subscription != null) {
                                    IconButton(
                                        onClick = { viewModel.toggleBell(video.channelId, subscription!!.notificationsEnabled) }
                                    ) {
                                        Icon(
                                            if (subscription!!.notificationsEnabled) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                                            contentDescription = "Notificaciones del canal",
                                            tint = if (subscription!!.notificationsEnabled) DeiRed else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.toggleSubscribe(
                                                video.channelId,
                                                video.channelName,
                                                video.channelAvatarUri,
                                                video.sourcePlatform == "DEIPLAY"
                                            )
                                        },
                                        modifier = Modifier.testTag("subscribe_btn")
                                    ) {
                                        Text("Suscrito")
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            viewModel.toggleSubscribe(
                                                video.channelId,
                                                video.channelName,
                                                video.channelAvatarUri,
                                                video.sourcePlatform == "DEIPLAY"
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DeiRed, contentColor = DeiWhite),
                                        modifier = Modifier.testTag("subscribe_btn")
                                    ) {
                                        Text("Suscribirse")
                                    }
                                }
                            }
                        }
                    }

                    // Action Pills: Like, Dislike, Share, Download, Favorite, Watch Later, Playlist, Report
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isLiked = reaction?.reaction == "LIKE"
                            val isDisliked = reaction?.reaction == "DISLIKE"

                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = if (isLiked) DeiRed.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(
                                        onClick = { viewModel.toggleReaction(video, "LIKE") },
                                        modifier = Modifier.testTag("like_video_btn")
                                    ) {
                                        Icon(
                                            Icons.Filled.ThumbUp,
                                            contentDescription = "Me gusta",
                                            tint = if (isLiked) DeiRed else MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            formatCompactCount(video.likesCount),
                                            color = if (isLiked) DeiRed else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.toggleReaction(video, "DISLIKE") },
                                        modifier = Modifier.testTag("dislike_video_btn")
                                    ) {
                                        Icon(
                                            Icons.Filled.ThumbDown,
                                            contentDescription = "No me gusta",
                                            tint = if (isDisliked) DeiRed else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            OutlinedButton(onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Mira «${video.title}» en DeiPlay: ${video.externalUrl ?: video.mediaUri}")
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Compartir video"))
                            }) {
                                Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Compartir")
                            }

                            OutlinedButton(
                                onClick = {
                                    if (!playerState.downloadPermitted) {
                                        viewModel.emitMessage("El creador o la plataforma no autoriza la descarga directa de este video.")
                                    } else {
                                        showDownloadDialog = true
                                    }
                                },
                                modifier = Modifier.testTag("download_video_btn")
                            ) {
                                Icon(Icons.Filled.Download, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Descargar")
                            }

                            OutlinedButton(onClick = { viewModel.toggleSavedFavorite(video, isFavorite) }) {
                                Icon(
                                    if (isFavorite) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                    null,
                                    tint = if (isFavorite) DeiRed else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (isFavorite) "Guardado" else "Favoritos")
                            }

                            OutlinedButton(onClick = { viewModel.toggleWatchLater(video, isWatchLater) }) {
                                Icon(Icons.Filled.WatchLater, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (isWatchLater) "En Ver más tarde" else "Ver más tarde")
                            }

                            OutlinedButton(onClick = { showPlaylistDialog = true }) {
                                Icon(Icons.Filled.PlaylistAdd, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Lista")
                            }

                            OutlinedButton(onClick = { showReportDialog = true }) {
                                Icon(Icons.Filled.Report, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Denunciar")
                            }
                        }

                        if (downloadProgress != null) {
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { downloadProgress ?: 0f },
                                modifier = Modifier.fillMaxWidth(),
                                color = DeiRed
                            )
                            Text(
                                "Descargando para ver sin conexión... ${((downloadProgress ?: 0f) * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    // Auto-play next toggle
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Reproducción automática del siguiente video", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = playerState.autoPlayNext,
                                onCheckedChange = { viewModel.setAutoPlayNext(it) }
                            )
                        }
                    }

                    // Comments Section
                    item {
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Comentarios (${comments.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        if (replyingTo != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Respondiendo a ${replyingTo!!.authorName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DeiRed
                                )
                                TextButton(onClick = { replyingTo = null }) {
                                    Text("Cancelar")
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = commentDraft,
                                onValueChange = { commentDraft = it },
                                placeholder = { Text("Escribe un comentario...") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("comment_input"),
                                singleLine = true
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (commentDraft.isNotBlank()) {
                                        viewModel.addComment(video, commentDraft, replyingTo?.id)
                                        commentDraft = ""
                                        replyingTo = null
                                    }
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(DeiRed)
                                    .testTag("send_comment_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar comentario", tint = DeiWhite)
                            }
                        }
                    }

                    val topLevel = comments.filter { it.parentCommentId == null }
                    items(topLevel, key = { it.id }) { comment ->
                        val replies = comments.filter { it.parentCommentId == comment.id }
                        CommentThreadItem(
                            comment = comment,
                            replies = replies,
                            likedIds = likedCommentIds,
                            canPin = video.ownerUserId == currentUser?.id,
                            currentUserId = currentUser?.id,
                            videoOwnerId = video.ownerUserId,
                            onLike = { viewModel.toggleCommentLike(it) },
                            onReply = { replyingTo = comment },
                            onPin = { viewModel.pinComment(video, comment) },
                            onDelete = { target -> viewModel.deleteComment(video, target) }
                        )
                    }

                    // Up next / Recommended videos
                    item {
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        Text("Siguientes videos recomendados", style = MaterialTheme.typography.titleMedium)
                    }

                    items(recommendedVideos.filter { it.id != video.id && !it.isPost }.take(10), key = { "rec_${it.id}" }) { nextVid ->
                        VideoCard(
                            video = nextVid,
                            onClick = { viewModel.openVideo(nextVid) },
                            onChannelClick = {
                                playerManager.minimizeToFloating()
                                onOpenChannel(nextVid.channelId)
                            },
                            onWatchLater = { viewModel.toggleWatchLater(nextVid, false) },
                            onAddToPlaylist = {},
                            onReport = { viewModel.reportContent("VIDEO", nextVid.id, "Reporte de usuario") }
                        )
                    }
                    item { Spacer(Modifier.height(28.dp)) }
                }
            }
        }
    }

    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadDialog = false },
            title = { Text("Descargar video") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Elige la calidad para guardar en tu dispositivo y ver sin conexión:")
                    playerState.qualityOptions.forEach { opt ->
                        OutlinedButton(
                            onClick = {
                                showDownloadDialog = false
                                viewModel.downloadActiveVideo(opt.label, opt.url)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Calidad ${opt.label}")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDownloadDialog = false }) { Text("Cerrar") }
            }
        )
    }

    if (showPlaylistDialog) {
        PlaylistPickerDialog(
            playlists = playlists,
            onDismiss = { showPlaylistDialog = false },
            onSelectPlaylist = { pl ->
                viewModel.addVideoToPlaylist(pl.id, video)
                showPlaylistDialog = false
            },
            onCreatePlaylist = { name ->
                viewModel.createPlaylist(name, "", true)
            }
        )
    }

    if (showReportDialog) {
        var reason by remember { mutableStateOf("Contenido inapropiado o spam") }
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Denunciar video") },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motivo del reporte") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.reportContent("VIDEO", video.id, reason)
                        showReportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeiRed)
                ) { Text("Enviar denuncia") }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun CommentThreadItem(
    comment: CommentEntity,
    replies: List<CommentEntity>,
    likedIds: List<String>,
    canPin: Boolean,
    currentUserId: String?,
    videoOwnerId: String,
    onLike: (String) -> Unit,
    onReply: () -> Unit,
    onPin: () -> Unit,
    onDelete: (CommentEntity) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            if (comment.isPinned) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PushPin, null, tint = DeiRed, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Fijado por el creador", style = MaterialTheme.typography.labelSmall, color = DeiRed)
                }
                Spacer(Modifier.height(4.dp))
            }
            Row(verticalAlignment = Alignment.Top) {
                ChannelAvatar(comment.authorAvatarUri, comment.authorName, size = 30.dp)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(comment.authorName, style = MaterialTheme.typography.labelLarge)
                    Text(comment.text, style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val liked = likedIds.contains(comment.id)
                        TextButton(onClick = { onLike(comment.id) }) {
                            Icon(
                                Icons.Filled.ThumbUp,
                                null,
                                tint = if (liked) DeiRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("${comment.likesCount}")
                        }
                        TextButton(onClick = onReply) {
                            Icon(Icons.AutoMirrored.Filled.Reply, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Responder")
                        }
                        if (canPin) {
                            IconButton(onClick = onPin) {
                                Icon(
                                    Icons.Filled.PushPin,
                                    contentDescription = "Fijar comentario",
                                    tint = if (comment.isPinned) DeiRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        if (comment.authorUserId == currentUserId || videoOwnerId == currentUserId) {
                            IconButton(onClick = { onDelete(comment) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Eliminar comentario", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            if (replies.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .padding(start = 36.dp, top = 4.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    replies.forEach { reply ->
                        Row(verticalAlignment = Alignment.Top) {
                            ChannelAvatar(reply.authorAvatarUri, reply.authorName, size = 24.dp)
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(reply.authorName, style = MaterialTheme.typography.labelMedium)
                                Text(reply.text, style = MaterialTheme.typography.bodySmall)
                            }
                            if (reply.authorUserId == currentUserId || videoOwnerId == currentUserId) {
                                IconButton(onClick = { onDelete(reply) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar respuesta", modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistPickerDialog(
    playlists: List<PlaylistEntity>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (PlaylistEntity) -> Unit,
    onCreatePlaylist: (String) -> Unit
) {
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Guardar en lista de reproducción") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                playlists.forEach { pl ->
                    OutlinedButton(
                        onClick = { onSelectPlaylist(pl) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(pl.name)
                    }
                }
                HorizontalDivider()
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nueva lista") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onCreatePlaylist(newName)
                            newName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeiRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Crear nueva lista")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}
