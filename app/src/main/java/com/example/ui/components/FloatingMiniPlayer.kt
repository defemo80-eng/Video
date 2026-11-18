package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.example.player.DeiPlayPlayerManager
import com.example.player.PlayerUiState
import com.example.ui.theme.DeiBlack
import com.example.ui.theme.DeiRed
import com.example.ui.theme.DeiWhite
import kotlin.math.roundToInt

/**
 * Draggable & Resizable In-App Floating Window (Ventana Flotante) + System PiP trigger.
 */
@Composable
fun FloatingMiniPlayer(
    playerManager: DeiPlayPlayerManager,
    playerState: PlayerUiState,
    modifier: Modifier = Modifier
) {
    val video = playerState.activeVideo ?: return
    val context = LocalContext.current

    var offsetX by remember { mutableFloatStateOf(24f) }
    var offsetY by remember { mutableFloatStateOf(160f) }
    var sizePreset by remember { mutableIntStateOf(1) } // 0 = 200dp, 1 = 260dp, 2 = 320dp
    val windowWidth = when (sizePreset) {
        0 -> 200.dp
        1 -> 260.dp
        else -> 320.dp
    }

    Surface(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .width(windowWidth)
            .shadow(16.dp, RoundedCornerShape(16.dp))
            .border(1.5.dp, DeiRed.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .testTag("floating_mini_player"),
        shape = RoundedCornerShape(16.dp),
        color = DeiBlack
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(DeiBlack)
                    .clickable { playerManager.expandFromFloating() }
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

                // Top floating controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = { sizePreset = (sizePreset + 1) % 3 },
                            modifier = Modifier
                                .size(32.dp)
                                .background(DeiBlack.copy(alpha = 0.65f), CircleShape)
                        ) {
                            Icon(
                                Icons.Filled.AspectRatio,
                                contentDescription = "Cambiar tamaño de ventana flotante",
                                tint = DeiWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = { playerManager.enterSystemPip(context) },
                            modifier = Modifier
                                .size(32.dp)
                                .background(DeiBlack.copy(alpha = 0.65f), CircleShape)
                        ) {
                            Icon(
                                Icons.Filled.PictureInPictureAlt,
                                contentDescription = "Picture in Picture del sistema",
                                tint = DeiWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { playerManager.closePlayer() },
                        modifier = Modifier
                            .size(32.dp)
                            .background(DeiBlack.copy(alpha = 0.65f), CircleShape)
                            .testTag("close_floating_player")
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Cerrar ventana flotante",
                            tint = DeiWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Progress bar
            val progress = if (playerState.durationMs > 0) {
                (playerState.currentPositionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = DeiRed,
                trackColor = DeiWhite.copy(alpha = 0.2f)
            )

            // Bottom title + Play/Pause + Expand
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { playerManager.expandFromFloating() }
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = DeiWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = video.channelName,
                        style = MaterialTheme.typography.labelSmall,
                        color = DeiWhite.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = { playerManager.togglePlayPause() },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playerState.isPlaying) "Pausar" else "Reproducir",
                        tint = DeiWhite
                    )
                }
                IconButton(
                    onClick = { playerManager.expandFromFloating() },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.OpenInFull,
                        contentDescription = "Volver al reproductor completo",
                        tint = DeiWhite
                    )
                }
            }
        }
    }
}
