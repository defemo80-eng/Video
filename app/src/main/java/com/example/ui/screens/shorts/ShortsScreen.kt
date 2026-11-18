package com.example.ui.screens.shorts

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VideoCameraFront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.local.VideoEntity
import com.example.data.remote.NetworkModule
import com.example.ui.DeiPlayViewModel
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.DeiBlack
import com.example.ui.theme.DeiRed
import com.example.ui.theme.DeiWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsScreen(
    viewModel: DeiPlayViewModel,
    onUploadShort: () -> Unit,
    onOpenChannel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val shorts by viewModel.shortsVideos.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (shorts.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().background(DeiBlack),
            contentAlignment = Alignment.Center
        ) {
            EmptyStateCard(
                icon = Icons.Filled.VideoCameraFront,
                title = "No hay Shorts disponibles aún",
                subtitle = "Graba o selecciona un video corto vertical de hasta 60 segundos para publicarlo en DeiPlay.",
                actionLabel = "Subir Short",
                onAction = onUploadShort
            )
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { shorts.size })
    val shortPlayer = remember {
        ExoPlayer.Builder(context.applicationContext).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(Unit) {
        onDispose { shortPlayer.release() }
    }

    var activeShortComments by remember { mutableStateOf<VideoEntity?>(null) }

    LaunchedEffect(pagerState.currentPage, shorts) {
        val current = shorts.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        val playableUrl = if (current.sourcePlatform == "DEIPLAY" || current.mediaUri.startsWith("file://")) {
            current.mediaUri
        } else if (current.sourcePlatform == "PEERTUBE") {
            // Resolve origin PeerTube MP4/HLS quickly
            runCatching {
                val uri = java.net.URI(current.externalUrl ?: current.mediaUri)
                val host = uri.host
                val uuid = current.id.removePrefix("pt_")
                val detail = NetworkModule.peerTubeService.getVideoDetails("https://$host/api/v1/videos/$uuid")
                detail.files.firstOrNull()?.fileUrl
                    ?: detail.streamingPlaylists.firstOrNull()?.files?.firstOrNull()?.fileUrl
                    ?: detail.streamingPlaylists.firstOrNull()?.playlistUrl
                    ?: current.mediaUri
            }.getOrDefault(current.mediaUri)
        } else current.mediaUri

        if (playableUrl.isNotBlank() && !playableUrl.contains("youtube.com/watch")) {
            shortPlayer.setMediaItem(MediaItem.fromUri(playableUrl))
            shortPlayer.prepare()
            shortPlayer.playWhenReady = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeiBlack)
            .testTag("shorts_pager")
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val shortVideo = shorts[page]
            val isCurrentPage = pagerState.currentPage == page
            val reaction by viewModel.observeReaction(shortVideo.id).collectAsStateWithLifecycle(initialValue = null)
            val sub by viewModel.observeSubscription(shortVideo.channelId).collectAsStateWithLifecycle(initialValue = null)

            Box(modifier = Modifier.fillMaxSize()) {
                if (isCurrentPage) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                player = shortPlayer
                            }
                        },
                        update = { pv ->
                            if (pv.player !== shortPlayer) pv.player = shortPlayer
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                if (shortPlayer.isPlaying) shortPlayer.pause() else shortPlayer.play()
                            }
                    )
                } else if (!shortVideo.thumbnailUri.isNullOrBlank()) {
                    AsyncImage(
                        model = shortVideo.thumbnailUri,
                        contentDescription = shortVideo.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Bottom gradient for readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, DeiBlack.copy(alpha = 0.88f)))
                        )
                )

                // Top header: Shorts + Upload Short button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DeiPlay Shorts",
                        style = MaterialTheme.typography.titleLarge,
                        color = DeiWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = onUploadShort,
                        colors = ButtonDefaults.buttonColors(containerColor = DeiRed, contentColor = DeiWhite)
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Subir Short")
                    }
                }

                // Right vertical action bar
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { viewModel.toggleReaction(shortVideo, "LIKE") },
                            modifier = Modifier
                                .size(48.dp)
                                .background(DeiBlack.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                Icons.Filled.ThumbUp,
                                contentDescription = "Me gusta",
                                tint = if (reaction?.reaction == "LIKE") DeiRed else DeiWhite
                            )
                        }
                        Text(formatCompactCount(shortVideo.likesCount), color = DeiWhite, style = MaterialTheme.typography.labelSmall)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { activeShortComments = shortVideo },
                            modifier = Modifier
                                .size(48.dp)
                                .background(DeiBlack.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "Comentarios", tint = DeiWhite)
                        }
                        Text(formatCompactCount(shortVideo.commentsCount), color = DeiWhite, style = MaterialTheme.typography.labelSmall)
                    }

                    IconButton(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Mira este Short en DeiPlay: ${shortVideo.title}")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Compartir Short"))
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(DeiBlack.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Compartir Short", tint = DeiWhite)
                    }

                    IconButton(
                        onClick = { viewModel.openVideo(shortVideo) },
                        modifier = Modifier
                            .size(48.dp)
                            .background(DeiRed, CircleShape)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Abrir en reproductor completo", tint = DeiWhite)
                    }
                }

                // Bottom-left creator & title info
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, end = 84.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            modifier = Modifier.clickable { onOpenChannel(shortVideo.channelId) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ChannelAvatar(shortVideo.channelAvatarUri, shortVideo.channelName, size = 38.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = shortVideo.channelName,
                                color = DeiWhite,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (sub != null) Color.White.copy(alpha = 0.25f) else DeiRed,
                            modifier = Modifier.clickable {
                                viewModel.toggleSubscribe(
                                    shortVideo.channelId,
                                    shortVideo.channelName,
                                    shortVideo.channelAvatarUri,
                                    shortVideo.sourcePlatform == "DEIPLAY"
                                )
                            }
                        ) {
                            Text(
                                text = if (sub != null) "Suscrito" else "Suscribirse",
                                color = DeiWhite,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Text(
                        text = shortVideo.title,
                        color = DeiWhite,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    activeShortComments?.let { target ->
        val comments by viewModel.observeComments(target.id).collectAsStateWithLifecycle(initialValue = emptyList())
        var draft by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { activeShortComments = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Comentarios (${comments.size})", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        placeholder = { Text("Comentar este Short...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (draft.isNotBlank()) {
                                viewModel.addComment(target, draft)
                                draft = ""
                            }
                        },
                        modifier = Modifier.clip(CircleShape).background(DeiRed)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = DeiWhite)
                    }
                }
                comments.take(15).forEach { c ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(c.authorName, style = MaterialTheme.typography.labelLarge)
                        Text(c.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
