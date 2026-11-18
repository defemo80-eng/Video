package com.example.ui.screens.create

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.DeiPlayViewModel
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.DeiBlack
import com.example.ui.theme.DeiRed
import com.example.ui.theme.DeiWhite

private val UPLOAD_CATEGORIES = listOf(
    "Entretenimiento", "Música", "Deportes", "Noticias", "Videojuegos",
    "Educación", "Tecnología", "Películas y videos"
)

private val PRIVACY_OPTIONS = listOf("PUBLIC", "UNLISTED", "PRIVATE")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadVideoScreen(
    viewModel: DeiPlayViewModel,
    initialIsShort: Boolean,
    initialIsPost: Boolean,
    onBack: () -> Unit,
    onCreateChannelFirst: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val myChannel by viewModel.myChannel.collectAsStateWithLifecycle()
    val uploadProgress by viewModel.uploadProgress.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Entretenimiento") }
    var language by remember { mutableStateOf("es") }
    var country by remember { mutableStateOf(myChannel?.country ?: "México") }
    var tags by remember { mutableStateOf("") }
    var subtitlesText by remember { mutableStateOf("") }
    var privacy by remember { mutableStateOf("PUBLIC") }
    var allowDownload by remember { mutableStateOf(true) }
    var isShort by remember { mutableStateOf(initialIsShort) }
    var isPost by remember { mutableStateOf(initialIsPost) }

    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var thumbnailUri by remember { mutableStateOf<Uri?>(null) }

    var showCatMenu by remember { mutableStateOf(false) }
    var showPrivacyMenu by remember { mutableStateOf(false) }

    val videoPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) videoUri = uri
    }
    val documentVideoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) videoUri = uri
    }
    val thumbPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) thumbnailUri = uri
    }
    val subtitlePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()?.let { text ->
                subtitlesText = text.take(2500)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            isPost -> "Crear publicación"
                            isShort -> "Subir Short"
                            else -> "Subir video"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (myChannel == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyStateCard(
                    icon = Icons.Filled.CloudUpload,
                    title = "Crea tu canal antes de publicar",
                    subtitle = "Para subir videos, Shorts o publicaciones en DeiPlay necesitas configurar tu canal.",
                    actionLabel = "Crear mi canal",
                    onAction = onCreateChannelFirst
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!isPost) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            videoPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeiRed, contentColor = DeiWhite),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_video_phone_btn")
                    ) {
                        Icon(Icons.Filled.VideoFile, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (videoUri != null) "Video listo ✓" else "Desde teléfono")
                    }

                    OutlinedButton(
                        onClick = { documentVideoPicker.launch(arrayOf("video/*")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.FolderOpen, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Archivos / PC")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            thumbPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Image, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (thumbnailUri != null) "Miniatura ✓" else "Miniatura")
                    }
                    OutlinedButton(
                        onClick = { subtitlePicker.launch(arrayOf("text/*", "application/x-subrip")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Subtitles, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Importar .srt/.vtt")
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_title_input")
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showCatMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Categoría: $category", maxLines = 1)
                    }
                    DropdownMenu(expanded = showCatMenu, onDismissRequest = { showCatMenu = false }) {
                        UPLOAD_CATEGORIES.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    showCatMenu = false
                                }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showPrivacyMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Privacidad: $privacy", maxLines = 1)
                    }
                    DropdownMenu(expanded = showPrivacyMenu, onDismissRequest = { showPrivacyMenu = false }) {
                        PRIVACY_OPTIONS.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p) },
                                onClick = {
                                    privacy = p
                                    showPrivacyMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = language,
                    onValueChange = { language = it },
                    label = { Text("Idioma (es, en, pt...)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text("País") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("Etiquetas (separadas por coma)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (!isPost) {
                OutlinedTextField(
                    value = subtitlesText,
                    onValueChange = { subtitlesText = it },
                    label = { Text("Subtítulos / Transcripción (opcional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Publicar como Short vertical", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isShort, onCheckedChange = { isShort = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Permitir descarga sin conexión", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = allowDownload, onCheckedChange = { allowDownload = it })
                }
            }

            if (uploadProgress != null) {
                LinearProgressIndicator(
                    progress = { uploadProgress ?: 0f },
                    modifier = Modifier.fillMaxWidth(),
                    color = DeiRed
                )
                Text(
                    "Procesando y subiendo contenido... ${((uploadProgress ?: 0f) * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Cada publicación en modo PUBLIC se agrega al instante al catálogo de Inicio, Shorts (si está activado), tu Canal y el feed de tus suscriptores en este dispositivo. Para sincronizar subidas hacia otros teléfonos remotos se requiere activar Firebase Storage/Firestore o conectar un servidor externo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Button(
                onClick = {
                    viewModel.publishVideo(
                        title = title,
                        description = description,
                        videoUri = videoUri,
                        thumbnailUri = thumbnailUri,
                        isShort = isShort,
                        isPost = isPost,
                        category = category,
                        language = language,
                        country = country,
                        tags = tags,
                        subtitlesText = subtitlesText,
                        privacy = privacy,
                        allowDownload = allowDownload,
                        onSuccess = onBack
                    )
                },
                enabled = uploadProgress == null,
                colors = ButtonDefaults.buttonColors(containerColor = DeiRed, contentColor = DeiWhite),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("publish_video_btn")
            ) {
                Text("Publicar ahora")
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveBroadcastScreen(
    viewModel: DeiPlayViewModel,
    onBack: () -> Unit,
    onCreateChannelFirst: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val myChannel by viewModel.myChannel.collectAsStateWithLifecycle()

    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasCameraPerm = result[Manifest.permission.CAMERA] == true || hasCameraPerm
        hasAudioPerm = result[Manifest.permission.RECORD_AUDIO] == true || hasAudioPerm
    }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var activeSessionId by remember { mutableStateOf<String?>(null) }
    var chatDraft by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transmitir en directo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (myChannel == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyStateCard(
                    icon = Icons.Filled.LiveTv,
                    title = "Crea tu canal primero",
                    subtitle = "Necesitas un canal de DeiPlay para iniciar transmisiones en directo.",
                    actionLabel = "Crear mi canal",
                    onAction = onCreateChannelFirst
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // CameraX Live Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DeiBlack),
                contentAlignment = Alignment.Center
            ) {
                if (hasCameraPerm) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                runCatching {
                                    val provider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.surfaceProvider = previewView.surfaceProvider
                                    }
                                    provider.unbindAll()
                                    provider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_FRONT_CAMERA,
                                        preview
                                    )
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Se requiere permiso de Cámara y Micrófono para transmitir.",
                            color = DeiWhite,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                permLauncher.launch(
                                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DeiRed)
                        ) {
                            Text("Conceder permisos")
                        }
                    }
                }

                if (activeSessionId != null) {
                    Surface(
                        color = DeiRed,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    ) {
                        Text(
                            "EN DIRECTO",
                            color = DeiWhite,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (activeSessionId == null) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título de la transmisión") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        viewModel.startLiveSession(title, description) { session ->
                            activeSessionId = session.id
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeiRed, contentColor = DeiWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(Icons.Filled.LiveTv, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Iniciar transmisión en directo")
                }
            } else {
                val sid = activeSessionId!!
                val chatMessages by viewModel.observeLiveChat(sid).collectAsStateWithLifecycle(initialValue = emptyList())

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            viewModel.endLiveSession(sid)
                            activeSessionId = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeiRed)
                    ) {
                        Icon(Icons.Filled.StopCircle, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Finalizar")
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(chatMessages, key = { it.id }) { msg ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(msg.authorName, style = MaterialTheme.typography.labelMedium, color = DeiRed)
                                Text(msg.message, style = MaterialTheme.typography.bodyMedium)
                            }
                            IconButton(onClick = { viewModel.moderateLiveMessage(msg.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Moderar mensaje", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = chatDraft,
                        onValueChange = { chatDraft = it },
                        placeholder = { Text("Responder en el chat en directo...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (chatDraft.isNotBlank()) {
                                viewModel.sendLiveMessage(sid, chatDraft)
                                chatDraft = ""
                            }
                        },
                        modifier = Modifier.clip(CircleShape).background(DeiRed)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = DeiWhite)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveViewerScreen(
    viewModel: DeiPlayViewModel,
    sessionId: String,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val session by viewModel.observeLiveSession(sessionId).collectAsStateWithLifecycle(initialValue = null)
    val chatMessages by viewModel.observeLiveChat(sessionId).collectAsStateWithLifecycle(initialValue = emptyList())
    var chatDraft by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(session?.title ?: "Directo") },
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
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DeiBlack,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.LiveTv, null, tint = DeiRed, modifier = Modifier.size(44.dp))
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (session?.isActive == true) "Transmisión en directo de ${session?.channelName}" else "Transmisión finalizada",
                            color = DeiWhite,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            Text("Chat en directo", style = MaterialTheme.typography.titleMedium)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(chatMessages, key = { it.id }) { msg ->
                    Column {
                        Text(msg.authorName, style = MaterialTheme.typography.labelMedium, color = DeiRed)
                        Text(msg.message, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = chatDraft,
                    onValueChange = { chatDraft = it },
                    placeholder = { Text("Comentar en el directo...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (chatDraft.isNotBlank()) {
                            viewModel.sendLiveMessage(sessionId, chatDraft)
                            chatDraft = ""
                        }
                    },
                    modifier = Modifier.clip(CircleShape).background(DeiRed)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = DeiWhite)
                }
            }
        }
    }
}
