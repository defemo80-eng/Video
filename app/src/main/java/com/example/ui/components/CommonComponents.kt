package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.VideoEntity
import com.example.ui.theme.DeiBlack
import com.example.ui.theme.DeiRed
import com.example.ui.theme.DeiWhite

fun formatCompactCount(count: Long): String {
    return when {
        count >= 1_000_000L -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000L -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

fun formatDuration(seconds: Long): String {
    if (seconds <= 0L) return ""
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) String.format("%d:%02d:%02d", hrs, mins, secs)
    else String.format("%02d:%02d", mins, secs)
}

@Composable
fun ChannelAvatar(
    avatarUri: String?,
    name: String,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "D"
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.5.dp, DeiRed.copy(alpha = 0.7f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!avatarUri.isNullOrBlank()) {
            AsyncImage(
                model = avatarUri,
                contentDescription = "Avatar de $name",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = DeiRed
            )
        }
    }
}

@Composable
fun VideoCard(
    video: VideoEntity,
    onClick: () -> Unit,
    onChannelClick: () -> Unit,
    onWatchLater: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("video_card_${video.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            if (!video.isPost) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(DeiBlack)
                ) {
                    if (!video.thumbnailUri.isNullOrBlank()) {
                        AsyncImage(
                            model = video.thumbnailUri,
                            contentDescription = "Miniatura de ${video.title}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(DeiBlack, Color(0xFF28090C))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = DeiWhite.copy(alpha = 0.85f),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    // Platform badge top-left
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = DeiBlack.copy(alpha = 0.78f)
                    ) {
                        Text(
                            text = when {
                                video.isLive -> "EN DIRECTO"
                                video.isShort -> "SHORT"
                                video.sourcePlatform == "PEERTUBE" -> "MUNDIAL"
                                video.sourcePlatform == "YOUTUBE" -> "YOUTUBE"
                                else -> "DEIPLAY"
                            },
                            color = if (video.isLive || video.sourcePlatform == "DEIPLAY") DeiRed else DeiWhite,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Duration or Live badge bottom-right
                    val durText = formatDuration(video.durationSeconds)
                    if (video.isLive) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp),
                            shape = RoundedCornerShape(6.dp),
                            color = DeiRed
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.LiveTv,
                                    contentDescription = null,
                                    tint = DeiWhite,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("LIVE", color = DeiWhite, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    } else if (durText.isNotBlank()) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp),
                            shape = RoundedCornerShape(6.dp),
                            color = DeiBlack.copy(alpha = 0.82f)
                        ) {
                            Text(
                                text = durText,
                                color = DeiWhite,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top
            ) {
                ChannelAvatar(
                    avatarUri = video.channelAvatarUri,
                    name = video.channelName,
                    size = 40.dp,
                    modifier = Modifier.clickable(onClick = onChannelClick)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = buildString {
                            append(video.channelName)
                            append(" · ")
                            append("${formatCompactCount(video.viewsCount)} vistas")
                            if (video.country.isNotBlank()) {
                                append(" · ${video.country}")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (video.isPost && video.description.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = video.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Opciones de video"
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ver más tarde") },
                            leadingIcon = { Icon(Icons.Filled.WatchLater, null) },
                            onClick = {
                                menuExpanded = false
                                onWatchLater()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Agregar a lista") },
                            leadingIcon = { Icon(Icons.Filled.PlaylistAdd, null) },
                            onClick = {
                                menuExpanded = false
                                onAddToPlaylist()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Denunciar contenido") },
                            leadingIcon = { Icon(Icons.Filled.Report, null) },
                            onClick = {
                                menuExpanded = false
                                onReport()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(DeiRed.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = DeiRed,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = DeiRed, contentColor = DeiWhite)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}
