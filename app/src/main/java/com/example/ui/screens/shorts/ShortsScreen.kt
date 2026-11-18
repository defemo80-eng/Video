package com.example.ui.screens.shorts

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VideoCameraFront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.local.VideoEntity
import com.example.ui.DeiPlayViewModel
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.DeiBlack
import com.example.ui.theme.DeiRed
import com.example.ui.theme.DeiWhite

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsScreen(
    viewModel: DeiPlayViewModel,
    onUploadShort: () -> Unit,
    onOpenChannel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val shorts by viewModel.shortsVideos.collectAsStateWithLifecycle()
    val targetShortId by viewModel.selectedShortId.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (shorts.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DeiBlack),
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

    val initialPage = remember(shorts, targetShortId) {
        val idx = targetShortId?.let { id -> shorts.indexOfFirst { it.id == id } } ?: 0
        if (idx >= 0) idx else 0
    }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { shorts.size })

    LaunchedEffect(targetShortId, shorts) {
        val id = targetShortId ?: return@LaunchedEffect
        val idx = shorts.indexOfFirst { it.id == id }
        if (idx >= 0 && idx != pagerState.currentPage) {
            pagerState.scrollToPage(idx)
        }
        viewModel.selectShortToOpen(null)
    }

    val shortPlayer = remember {
        ExoPlayer.Builder(context.applicationContext).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    var firstFrameRendered by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var isPaused by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var isExternalWebShort by remember { mutableStateOf(false) }
    var reloadNonce by remember { mutableIntStateOf(0) }

    DisposableEffect(shortPlayer) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                firstFrameRendered = true
                isBuffering = false
                playbackError = null
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> isBuffering = true
                    Player.STATE_READY -> {
                        isBuffering = false
                        playbackError = null
                    }
                    Player.STATE_IDLE, Player.STATE_ENDED -> isBuffering = false
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isPaused = !isPlaying && shortPlayer.playbackState == Player.STATE_READY
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                firstFrameRendered = false
                playbackError = "No se pudo reproducir el video directamente aquí."
            }
        }
        shortPlayer.addListener(listener)
        onDispose {
            shortPlayer.removeListener(listener)
            shortPlayer.release()
        }
    }

    var activeShortComments by remember { mutableStateOf<VideoEntity?>(null) }

    LaunchedEffect(pagerState.currentPage, shorts, reloadNonce) {
        val current = shorts.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        firstFrameRendered = false
        isBuffering = true
        isPaused = false
        playbackError = null
        isExternalWebShort = false
        shortPlayer.stop()
        shortPlayer.clearMediaItems()

        val resolved = runCatching {
            viewModel.resolvePlayableStreamsForShort(current)
        }.getOrNull()

        val candidateUrls = resolved?.streams?.map { it.url }?.filter { it.isNotBlank() }.orEmpty()
        val chosenUrl = candidateUrls.firstOrNull { url ->
            url.startsWith("file://") ||
                url.startsWith("content://") ||
                url.contains(".mp4", ignoreCase = true) ||
                url.contains(".m3u8", ignoreCase = true) ||
                url.contains(".webm", ignoreCase = true) ||
                url.contains("/static/", ignoreCase = true) ||
                url.contains("/download/", ignoreCase = true)
        } ?: candidateUrls.firstOrNull() ?: current.mediaUri

        val looksLikeYouTubeWeb = chosenUrl.contains("youtube.com/watch", ignoreCase = true) ||
            chosenUrl.contains("youtu.be/", ignoreCase = true) ||
            chosenUrl.contains("youtube.com/shorts/", ignoreCase = true)
        val looksLikePeerTubeHtmlPage = current.sourcePlatform == "PEERTUBE" &&
            (chosenUrl == current.mediaUri ||
                chosenUrl.contains("/w/", ignoreCase = true) ||
                chosenUrl.contains("/videos/watch/", ignoreCase = true)) &&
            !chosenUrl.contains(".mp4", ignoreCase = true) &&
            !chosenUrl.contains(".m3u8", ignoreCase = true)

        if (looksLikeYouTubeWeb || looksLikePeerTubeHtmlPage || chosenUrl.isBlank()) {
            isBuffering = false
            isExternalWebShort = true
            playbackError = if (looksLikeYouTubeWeb) {
                "Short de YouTube: abre en YouTube o usa un Short nativo de DeiPlay."
            } else {
                "Este creador externo aún no expone stream directo en MP4/HLS."
            }
        } else {
            shortPlayer.setMediaItem(MediaItem.fromUri(chosenUrl))
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF140708), DeiBlack, Color(0xFF120608))
                        )
                    )
            ) {
                // Active ExoPlayer surface
                if (isCurrentPage && !isExternalWebShort && playbackError == null) {
                    AndroidView(
                        factory = { ctx ->
                            (LayoutInflater.from(ctx).inflate(
                                com.example.R.layout.view_short_player,
                                null,
                                false
                            ) as PlayerView).apply {
                                keepScreenOn = true
                                player = shortPlayer
                            }
                        },
                        update = { pv ->
                            if (pv.player !== shortPlayer) pv.player = shortPlayer
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                if (shortPlayer.isPlaying) {
                                    shortPlayer.pause()
                                    isPaused = true
                                } else {
                                    shortPlayer.play()
                                    isPaused = false
                                }
                            }
                    )
                }

                // Always show thumbnail/poster cleanly until first video frame actually renders (eliminates black screen!)
                val showPosterOverlay = !isCurrentPage || !firstFrameRendered || isExternalWebShort || playbackError != null
                AnimatedVisibility(
                    visible = showPosterOverlay,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (!shortVideo.thumbnailUri.isNullOrBlank()) {
                            AsyncImage(
                                model = shortVideo.thumbnailUri,
                                contentDescription = shortVideo.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.radialGradient(
                                            listOf(DeiRed.copy(alpha = 0.28f), DeiBlack)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = DeiRed.copy(alpha = 0.85f),
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DeiBlack.copy(alpha = 0.24f))
                        )
                    }
                }

                // Buffering / Pause / Fallback center controls
                if (isCurrentPage) {
                    when {
                        playbackError != null -> {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = DeiBlack.copy(alpha = 0.78f),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 28.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = playbackError!!,
                                        color = DeiWhite,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = { reloadNonce++ },
                                            colors = ButtonDefaults.buttonColors(containerColor = DeiRed, contentColor = DeiWhite)
                                        ) {
                                            Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Reintentar")
                                        }
                                        val extUrl = shortVideo.externalUrl ?: shortVideo.mediaUri
                                        if (extUrl.startsWith("http")) {
                                            Button(
                                                onClick = {
                                                    runCatching {
                                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(extUrl)))
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color.White.copy(alpha = 0.2f),
                                                    contentColor = DeiWhite
                                                )
                                            ) {
                                                Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Abrir origen")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        isBuffering && !firstFrameRendered -> {
                            CircularProgressIndicator(
                                color = DeiRed,
                                strokeWidth = 3.dp,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(44.dp)
                            )
                        }

                        isPaused -> {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(DeiBlack.copy(alpha = 0.6f))
                                    .clickable {
                                        shortPlayer.play()
                                        isPaused = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Reanudar Short",
                                    tint = DeiWhite,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }

                // Top & bottom gradients for readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(listOf(DeiBlack.copy(alpha = 0.72f), Color.Transparent))
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, DeiBlack.copy(alpha = 0.90f)))
                        )
                )

                // Top header: Shorts + Upload Short button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DeiRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Bolt, null, tint = DeiWhite, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Shorts",
                            style = MaterialTheme.typography.titleLarge,
                            color = DeiWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = onUploadShort,
                        shape = RoundedCornerShape(20.dp),
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
                                .background(DeiBlack.copy(alpha = 0.55f), CircleShape)
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
                                .background(DeiBlack.copy(alpha = 0.55f), CircleShape)
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
                            .background(DeiBlack.copy(alpha = 0.55f), CircleShape)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Compartir Short", tint = DeiWhite)
                    }

                    IconButton(
                        onClick = {
                            if (isCurrentPage && firstFrameRendered) {
                                if (shortPlayer.isPlaying) {
                                    shortPlayer.pause()
                                    isPaused = true
                                } else {
                                    shortPlayer.play()
                                    isPaused = false
                                }
                            } else {
                                viewModel.openVideo(shortVideo)
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(DeiRed, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isCurrentPage && firstFrameRendered && !isPaused) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Reproducir o pausar",
                            tint = DeiWhite
                        )
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
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(DeiRed)
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
