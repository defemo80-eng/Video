package com.example.ui.screens.studio

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.VideoEntity
import com.example.ui.DeiPlayViewModel
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.DeiRed
import com.example.ui.theme.DeiWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeiPlayStudioScreen(
    viewModel: DeiPlayViewModel,
    onBack: () -> Unit,
    onCreateChannel: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    BackHandler(onBack = onBack)

    val myChannel by viewModel.myChannel.collectAsStateWithLifecycle()
    val myVideos by viewModel.myOwnedVideos.collectAsStateWithLifecycle()
    var editingVideo by remember { mutableStateOf<VideoEntity?>(null) }

    val totalViews = remember(myVideos) { myVideos.sumOf { it.viewsCount } }
    val totalLikes = remember(myVideos) { myVideos.sumOf { it.likesCount } }
    val totalComments = remember(myVideos) { myVideos.sumOf { it.commentsCount } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DeiPlay Studio") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenNotifications) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notificaciones de Studio")
                    }
                }
            )
        }
    ) { padding ->
        if (myChannel == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyStateCard(
                    icon = Icons.Filled.Analytics,
                    title = "Activa tu DeiPlay Studio",
                    subtitle = "Crea tu canal de DeiPlay para consultar analíticas reales, rendimiento de videos y moderación.",
                    actionLabel = "Crear mi canal",
                    onAction = onCreateChannel
                )
            }
            return@Scaffold
        }

        val ch = myChannel!!

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Resumen de ${ch.name}", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StudioMetricCard("Suscriptores", formatCompactCount(ch.subscriberCount), Modifier.weight(1f))
                    StudioMetricCard("Vistas", formatCompactCount(totalViews), Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StudioMetricCard("Likes", formatCompactCount(totalLikes), Modifier.weight(1f))
                    StudioMetricCard("Comentarios", formatCompactCount(totalComments), Modifier.weight(1f))
                    StudioMetricCard("Publicaciones", "${myVideos.size}", Modifier.weight(1f))
                }
            }

            // Real Canvas Chart for Video Performance
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Rendimiento real por publicación (Vistas vs Likes)", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        if (myVideos.isEmpty()) {
                            Text(
                                "Publica tu primer video o Short para visualizar métricas reales.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val chartItems = myVideos.take(6)
                            val maxVal = (chartItems.maxOfOrNull { maxOf(it.viewsCount, it.likesCount) } ?: 1L).coerceAtLeast(1L)
                            val barBg = MaterialTheme.colorScheme.surfaceVariant
                            val likeBarColor = MaterialTheme.colorScheme.onSurface
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                            ) {
                                val slotW = size.width / chartItems.size.coerceAtLeast(1)
                                val barW = (slotW * 0.28f).coerceAtLeast(10f)
                                chartItems.forEachIndexed { idx, vid ->
                                    val centerX = slotW * idx + slotW / 2f
                                    val viewRatio = vid.viewsCount.toFloat() / maxVal.toFloat()
                                    val likeRatio = vid.likesCount.toFloat() / maxVal.toFloat()

                                    // Track background
                                    drawRoundRect(
                                        color = barBg,
                                        topLeft = Offset(centerX - barW - 3f, 0f),
                                        size = Size(barW, size.height),
                                        cornerRadius = CornerRadius(8f, 8f)
                                    )
                                    // Views bar (Red)
                                    val vH = (size.height * viewRatio).coerceAtLeast(6f)
                                    drawRoundRect(
                                        color = DeiRed,
                                        topLeft = Offset(centerX - barW - 3f, size.height - vH),
                                        size = Size(barW, vH),
                                        cornerRadius = CornerRadius(8f, 8f)
                                    )
                                    // Likes bar (White/OnSurface)
                                    val lH = (size.height * likeRatio).coerceAtLeast(6f)
                                    drawRoundRect(
                                        color = likeBarColor,
                                        topLeft = Offset(centerX + 3f, size.height - lH),
                                        size = Size(barW, lH),
                                        cornerRadius = CornerRadius(8f, 8f)
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("■ Vistas", color = DeiRed, style = MaterialTheme.typography.labelSmall)
                                Text("■ Likes", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            item {
                Text("Administración de publicaciones", style = MaterialTheme.typography.titleMedium)
            }

            if (myVideos.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.Analytics,
                        title = "Sin publicaciones todavía",
                        subtitle = "Usa el botón Crear (+) en la barra inferior para subir videos, Shorts o publicaciones."
                    )
                }
            } else {
                items(myVideos, key = { it.id }) { vid ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(vid.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${vid.privacy} · ${vid.viewsCount} vistas · ${vid.likesCount} likes · ${vid.commentsCount} comentarios",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { editingVideo = vid }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Editar video")
                            }
                            IconButton(onClick = { viewModel.deleteVideo(vid.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Eliminar video", tint = DeiRed)
                            }
                        }
                    }
                }
            }
        }
    }

    editingVideo?.let { target ->
        var title by remember(target) { mutableStateOf(target.title) }
        var desc by remember(target) { mutableStateOf(target.description) }
        var tags by remember(target) { mutableStateOf(target.tags) }
        var subtitles by remember(target) { mutableStateOf(target.subtitlesText) }
        var isPublic by remember(target) { mutableStateOf(target.privacy == "PUBLIC") }
        var allowDl by remember(target) { mutableStateOf(target.allowDownload) }

        AlertDialog(
            onDismissRequest = { editingVideo = null },
            title = { Text("Editar publicación") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Descripción") }, minLines = 2)
                    OutlinedTextField(value = tags, onValueChange = { tags = it }, label = { Text("Etiquetas") }, singleLine = true)
                    OutlinedTextField(value = subtitles, onValueChange = { subtitles = it }, label = { Text("Subtítulos") }, minLines = 2)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Público")
                        Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Permitir descarga")
                        Switch(checked = allowDl, onCheckedChange = { allowDl = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateVideoMetadata(
                            videoId = target.id,
                            title = title,
                            description = desc,
                            category = target.category,
                            tags = tags,
                            subtitlesText = subtitles,
                            privacy = if (isPublic) "PUBLIC" else "PRIVATE",
                            allowDownload = allowDl
                        )
                        editingVideo = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeiRed, contentColor = DeiWhite)
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { editingVideo = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun StudioMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = DeiRed, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
