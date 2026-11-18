package com.example.ui.screens.subscriptions

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
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.DeiPlayViewModel
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.VideoCard
import com.example.ui.theme.DeiRed
import com.example.ui.theme.DeiWhite

@Composable
fun SubscriptionsScreen(
    viewModel: DeiPlayViewModel,
    onOpenChannel: (String) -> Unit,
    onOpenYouTubeConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val deiPlaySubs by viewModel.deiPlaySubscriptions.collectAsStateWithLifecycle()
    val ytSubs by viewModel.youtubeSubscriptions.collectAsStateWithLifecycle()
    val feedVideos by viewModel.combinedFeedVideos.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = DeiPlay, 1 = YouTube

    Column(modifier = modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Suscripciones",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("DeiPlay (${deiPlaySubs.size})") },
                        modifier = Modifier.testTag("subs_tab_deiplay")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("YouTube (${ytSubs.size})") },
                        modifier = Modifier.testTag("subs_tab_youtube")
                    )
                }
            }
        }

        if (selectedTab == 0) {
            val subscribedIds = remember(deiPlaySubs) { deiPlaySubs.map { it.channelId }.toSet() }
            val subVideos = remember(feedVideos, subscribedIds) {
                feedVideos.filter { subscribedIds.contains(it.channelId) }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (deiPlaySubs.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Filled.Subscriptions,
                            title = "Sin suscripciones en DeiPlay",
                            subtitle = "Suscríbete a canales de DeiPlay o creadores mundiales desde Inicio o Shorts."
                        )
                    }
                } else {
                    item {
                        Text("Canales suscritos en DeiPlay", style = MaterialTheme.typography.titleMedium)
                    }
                    items(deiPlaySubs, key = { it.id }) { sub ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenChannel(sub.channelId) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ChannelAvatar(sub.channelAvatarUri, sub.channelName, size = 44.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sub.channelName, style = MaterialTheme.typography.titleSmall)
                                    Text("Canal de DeiPlay", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { viewModel.toggleBell(sub.channelId, sub.notificationsEnabled) }) {
                                    Icon(
                                        if (sub.notificationsEnabled) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                                        contentDescription = "Notificaciones",
                                        tint = if (sub.notificationsEnabled) DeiRed else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.toggleSubscribe(sub.channelId, sub.channelName, sub.channelAvatarUri, !sub.channelId.startsWith("pt_"))
                                    }
                                ) {
                                    Text("Cancelar")
                                }
                            }
                        }
                    }

                    if (subVideos.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(6.dp))
                            Text("Últimos videos de tus canales", style = MaterialTheme.typography.titleMedium)
                        }
                        items(subVideos, key = { "subvid_${it.id}" }) { vid ->
                            VideoCard(
                                video = vid,
                                onClick = { viewModel.openVideo(vid) },
                                onChannelClick = { onOpenChannel(vid.channelId) },
                                onWatchLater = { viewModel.toggleWatchLater(vid, false) },
                                onAddToPlaylist = {},
                                onReport = { viewModel.reportContent("VIDEO", vid.id, "Reporte") }
                            )
                        }
                    }
                }
            }
        } else {
            // Strictly separated YouTube Subscriptions
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Sincronización oficial con YouTube", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (currentUser?.youtubeConnected == true) {
                                    "Canal conectado: ${currentUser?.youtubeChannelTitle}. Los videos nuevos de tu canal y creadores seguidos se muestran automáticamente aquí y en Inicio."
                                } else {
                                    "Conecta tu cuenta de YouTube con Google OAuth oficial para consultar tus suscripciones y mostrar aquí los videos que suban tus creadores."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = onOpenYouTubeConnect,
                                    colors = ButtonDefaults.buttonColors(containerColor = DeiRed, contentColor = DeiWhite)
                                ) {
                                    Text(if (currentUser?.youtubeConnected == true) "Administrar YouTube" else "Conectar mi YouTube")
                                }
                                if (currentUser?.youtubeConnected == true) {
                                    OutlinedButton(onClick = { viewModel.refreshWorldVideos() }) {
                                        Text("Sincronizar videos")
                                    }
                                }
                            }
                        }
                    }
                }

                val ytVideos = feedVideos.filter { it.sourcePlatform == "YOUTUBE" }
                if (ytVideos.isNotEmpty()) {
                    item {
                        Text("Videos recientes de creadores de YouTube (${ytVideos.size})", style = MaterialTheme.typography.titleMedium)
                    }
                    items(ytVideos, key = { "yt_feed_${it.id}" }) { vid ->
                        VideoCard(
                            video = vid,
                            onClick = { viewModel.openVideo(vid) },
                            onChannelClick = { onOpenChannel(vid.channelId) },
                            onWatchLater = { viewModel.toggleWatchLater(vid, false) },
                            onAddToPlaylist = {},
                            onReport = { viewModel.reportContent("VIDEO", vid.id, "Reporte") }
                        )
                    }
                }

                if (ytSubs.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Filled.Subscriptions,
                            title = "Sin suscripciones de YouTube importadas",
                            subtitle = "Conecta tu cuenta de YouTube usando OAuth oficial para ver tus canales seguidos y sus subidas recientes aquí."
                        )
                    }
                } else {
                    item {
                        Text("Canales suscritos en YouTube", style = MaterialTheme.typography.titleMedium)
                    }
                    items(ytSubs, key = { it.id }) { sub ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenChannel(sub.channelId) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ChannelAvatar(sub.channelAvatarUri, sub.channelName, size = 42.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sub.channelName, style = MaterialTheme.typography.titleSmall)
                                    Text("Suscripción oficial de YouTube", style = MaterialTheme.typography.labelSmall, color = DeiRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
