package com.example.ui.screens.profile

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.ThemeMode
import com.example.ui.DeiPlayViewModel
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.DeiRed
import com.example.ui.theme.DeiWhite
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope

@Composable
fun ProfileScreen(
    viewModel: DeiPlayViewModel,
    onOpenChannel: (String) -> Unit,
    onCreateOrEditChannel: () -> Unit,
    onOpenStudio: () -> Unit,
    onOpenYouTubeConnect: () -> Unit,
    onOpenLibrary: (Int) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val myChannel by viewModel.myChannel.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()

    var showEditProfileDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // User Header Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChannelAvatar(user?.photoUri, user?.displayName ?: "DeiPlay", size = 64.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user?.displayName ?: "Usuario", style = MaterialTheme.typography.titleLarge)
                        Text("@${user?.username.orEmpty()} · ${user?.email.orEmpty()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { showEditProfileDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar perfil")
                    }
                }
            }
        }

        // Visible CREAR MI CANAL / MI CANAL + DEIPLAY STUDIO + CONECTAR MI YOUTUBE
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (myChannel != null) onOpenChannel(myChannel!!.id) else onCreateOrEditChannel()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeiRed, contentColor = DeiWhite),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("create_or_view_channel_btn")
                ) {
                    Icon(if (myChannel == null) Icons.Filled.AddCircle else Icons.Filled.Tv, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (myChannel == null) "CREAR MI CANAL" else "MI CANAL")
                }

                OutlinedButton(
                    onClick = onOpenStudio,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("deiplay_studio_btn")
                ) {
                    Icon(Icons.Filled.Analytics, null, tint = DeiRed)
                    Spacer(Modifier.width(6.dp))
                    Text("DEIPLAY STUDIO")
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onOpenYouTubeConnect,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("connect_youtube_btn")
            ) {
                Icon(Icons.Filled.Link, null, tint = DeiRed)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (user?.youtubeConnected == true) "YOUTUBE CONECTADO: ${user?.youtubeChannelTitle}"
                    else "CONECTAR MI YOUTUBE"
                )
            }
        }

        // Library & Account Options
        item {
            Text("Tu biblioteca y actividad", style = MaterialTheme.typography.titleMedium)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileMenuRow(Icons.Filled.History, "Historial de reproducción", "Consulta y limpia tu historial") { onOpenLibrary(0) }
                ProfileMenuRow(Icons.Filled.ThumbUp, "Videos que me gustan & Favoritos", "Ver más tarde, Likes y Guardados") { onOpenLibrary(2) }
                ProfileMenuRow(Icons.Filled.Download, "Descargas sin conexión", "Reproduce tus videos offline y revisa espacio") { onOpenLibrary(4) }
                ProfileMenuRow(Icons.Filled.PlaylistPlay, "Listas de reproducción", "Crea, ordena y comparte tus listas") { onOpenLibrary(5) }
                ProfileMenuRow(Icons.Filled.Notifications, "Notificaciones ($unreadCount sin leer)", "Nuevos videos, Shorts, directos y respuestas") { onOpenNotifications() }
                ProfileMenuRow(Icons.Filled.Settings, "Configuración, Privacidad y Seguridad", "Modo oscuro/claro, control parental y cuenta") { onOpenSettings() }
            }
        }

        item {
            OutlinedButton(
                onClick = { viewModel.signOut() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, null, tint = DeiRed)
                Spacer(Modifier.width(8.dp))
                Text("Cerrar sesión")
            }
        }
    }

    if (showEditProfileDialog) {
        var displayName by remember { mutableStateOf(user?.displayName.orEmpty()) }
        var username by remember { mutableStateOf(user?.username.orEmpty()) }
        var pickedPhotoUri by remember { mutableStateOf<Uri?>(null) }
        val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) pickedPhotoUri = uri
        }
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Editar perfil") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (pickedPhotoUri != null) "Foto seleccionada ✓" else "Cambiar foto de perfil")
                    }
                    OutlinedTextField(value = displayName, onValueChange = { displayName = it }, label = { Text("Nombre visible") }, singleLine = true)
                    OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Nombre de usuario (@)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProfile(displayName, username, pickedPhotoUri) {
                            showEditProfileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeiRed)
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = DeiRed)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeConnectScreen(
    viewModel: DeiPlayViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val prefs by viewModel.prefsState.collectAsStateWithLifecycle()
    val feedVideos by viewModel.combinedFeedVideos.collectAsStateWithLifecycle()

    var isConnectingGoogle by remember { mutableStateOf(false) }
    var googleErrorMessage by remember { mutableStateOf<String?>(null) }
    var showAdvancedOptions by remember { mutableStateOf(false) }

    var apiKey by remember(prefs.youtubeApiKey) { mutableStateOf(prefs.youtubeApiKey) }
    var clientId by remember(prefs.youtubeOAuthClientId) { mutableStateOf(prefs.youtubeOAuthClientId) }
    var accessToken by remember(prefs.youtubeAccessToken) { mutableStateOf(prefs.youtubeAccessToken) }

    val authLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        isConnectingGoogle = false
        try {
            val authResult = Identity.getAuthorizationClient(context.findActivity() ?: context)
                .getAuthorizationResultFromIntent(result.data)
            val token = authResult.accessToken
            if (!token.isNullOrBlank()) {
                googleErrorMessage = null
                accessToken = token
                viewModel.syncYouTubeAccount(token)
            } else {
                googleErrorMessage = "No se recibió autorización de Google para leer tu canal de YouTube."
            }
        } catch (e: ApiException) {
            googleErrorMessage = when (e.statusCode) {
                16 -> "Autorización cancelada."
                10 -> "Este APK requiere registrar su huella SHA-1 y YouTube Data API v3 en Google Cloud Console para autorizar cuentas automáticamente."
                else -> "Error de Google (${e.statusCode}): ${e.localizedMessage ?: "no se pudo completar el inicio"}"
            }
        } catch (e: Exception) {
            googleErrorMessage = e.localizedMessage ?: "No se pudo conectar con la cuenta de Google."
        }
    }

    fun startGoogleYouTubeSignIn() {
        isConnectingGoogle = true
        googleErrorMessage = null
        val requestedScopes = listOf(
            Scope("https://www.googleapis.com/auth/youtube.readonly"),
            Scope("https://www.googleapis.com/auth/userinfo.profile"),
            Scope("https://www.googleapis.com/auth/userinfo.email")
        )
        val authRequest = AuthorizationRequest.builder()
            .setRequestedScopes(requestedScopes)
            .build()

        Identity.getAuthorizationClient(context.findActivity() ?: context)
            .authorize(authRequest)
            .addOnSuccessListener { authorizationResult ->
                if (authorizationResult.hasResolution() && authorizationResult.pendingIntent != null) {
                    isConnectingGoogle = false
                    runCatching {
                        authLauncher.launch(
                            IntentSenderRequest.Builder(authorizationResult.pendingIntent!!.intentSender).build()
                        )
                    }.onFailure {
                        googleErrorMessage = it.localizedMessage ?: "No se pudo abrir el selector de cuentas de Google."
                    }
                } else {
                    isConnectingGoogle = false
                    val token = authorizationResult.accessToken
                    if (!token.isNullOrBlank()) {
                        accessToken = token
                        viewModel.syncYouTubeAccount(token)
                    } else {
                        googleErrorMessage = "Google no devolvió un token de acceso para YouTube."
                    }
                }
            }
            .addOnFailureListener { e ->
                isConnectingGoogle = false
                val apiEx = e as? ApiException
                googleErrorMessage = when (apiEx?.statusCode) {
                    10 -> "Google Play Services requiere que el paquete de la app y su SHA-1 estén autorizados en Google Cloud con YouTube Data API v3 habilitada."
                    else -> e.localizedMessage ?: "No se pudo iniciar la conexión con tu cuenta de Google."
                }
            }
    }

    val syncedOwnChannelVideos = remember(feedVideos, user?.youtubeChannelId) {
        val ytChId = user?.youtubeChannelId
        if (ytChId.isNullOrBlank()) emptyList()
        else feedVideos.filter { it.sourcePlatform == "YOUTUBE" && it.channelId == "yt_$ytChId" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conectar mi YouTube") },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (user?.youtubeConnected == true) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ChannelAvatar(
                                avatarUri = user?.youtubeChannelAvatar,
                                name = user?.youtubeChannelTitle ?: "YT",
                                size = 64.dp,
                                accentBorder = true
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = user?.youtubeChannelTitle.orEmpty(),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Icon(
                                        Icons.Filled.Verified,
                                        contentDescription = "Canal conectado",
                                        tint = DeiRed,
                                        modifier = Modifier.height(18.dp)
                                    )
                                }
                                Text(
                                    text = "Canal oficial vinculado con Google",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    formatCompactCount(user?.youtubeSubscriberCount ?: 0L),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Suscriptores", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${user?.youtubeVideoCount ?: 0L}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Videos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    formatCompactCount(user?.youtubeViewCount ?: 0L),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Vistas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { startGoogleYouTubeSignIn() },
                                enabled = !isConnectingGoogle,
                                colors = ButtonDefaults.buttonColors(containerColor = DeiRed, contentColor = DeiWhite),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Refresh, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Actualizar")
                            }
                            OutlinedButton(
                                onClick = { viewModel.disconnectYouTube() },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Desconectar")
                            }
                        }
                    }
                }

                if (syncedOwnChannelVideos.isNotEmpty()) {
                    Text(
                        text = "Videos de tu canal (${syncedOwnChannelVideos.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    syncedOwnChannelVideos.forEach { vid ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.openVideo(vid) }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = vid.thumbnailUri,
                                    contentDescription = vid.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .width(116.dp)
                                        .height(66.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = vid.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${formatCompactCount(vid.viewsCount)} vistas",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = DeiRed)
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Vincula tu canal con Google",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Inicia sesión únicamente con tu cuenta de Google para mostrar automáticamente tu canal de YouTube, suscriptores, videos subidos y suscripciones dentro de DeiPlay.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { startGoogleYouTubeSignIn() },
                            enabled = !isConnectingGoogle,
                            colors = ButtonDefaults.buttonColors(containerColor = DeiRed, contentColor = DeiWhite),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("youtube_google_connect_btn")
                        ) {
                            if (isConnectingGoogle) {
                                CircularProgressIndicator(color = DeiWhite, strokeWidth = 2.dp, modifier = Modifier.height(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("Conectando con Google...")
                            } else {
                                Icon(Icons.Filled.Tv, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Conectar con mi cuenta de Google", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (!googleErrorMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DeiRed.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = googleErrorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            // Collapsed advanced options (hidden by default so standard users only see Google Account Sign-In)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAdvancedOptions = !showAdvancedOptions }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Opciones avanzadas de desarrollador (opcional)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = if (showAdvancedOptions) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = showAdvancedOptions) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("YouTube Data API v3 Key (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = clientId,
                        onValueChange = { clientId = it },
                        label = { Text("Google OAuth Client ID (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = accessToken,
                        onValueChange = { accessToken = it },
                        label = { Text("Token OAuth2 manual (Bearer)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.saveYouTubeConfig(apiKey, clientId, accessToken) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Guardar") }
                        Button(
                            onClick = { viewModel.syncYouTubeAccount(accessToken) },
                            colors = ButtonDefaults.buttonColors(containerColor = DeiRed, contentColor = DeiWhite),
                            modifier = Modifier.weight(1f)
                        ) { Text("Sincronizar") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: DeiPlayViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.markNotificationsRead()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (notifications.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center
            ) {
                EmptyStateCard(
                    icon = Icons.Filled.Notifications,
                    title = "Sin notificaciones nuevas",
                    subtitle = "Te avisaremos cuando canales que sigues publiquen videos, Shorts, directos o respondan tus comentarios."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notifications, key = { it.id }) { notif ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(notif.title, style = MaterialTheme.typography.titleSmall, color = DeiRed)
                            Spacer(Modifier.height(2.dp))
                            Text(notif.message, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSecurityScreen(
    viewModel: DeiPlayViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val prefs by viewModel.prefsState.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.emitMessage(if (granted) "Permiso de notificaciones concedido" else "Permiso de notificaciones denegado")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración y Seguridad") },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Theme Selector
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.DarkMode, null, tint = DeiRed)
                        Spacer(Modifier.width(8.dp))
                        Text("Apariencia (Modo oscuro y claro)", style = MaterialTheme.typography.titleSmall)
                    }
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ThemeMode.entries.forEachIndexed { idx, mode ->
                            SegmentedButton(
                                selected = prefs.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(idx, ThemeMode.entries.size)
                            ) {
                                Text(
                                    when (mode) {
                                        ThemeMode.SYSTEM -> "Sistema"
                                        ThemeMode.DARK -> "Oscuro"
                                        ThemeMode.LIGHT -> "Claro"
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Parental Control & Security
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Shield, null, tint = DeiRed)
                            Spacer(Modifier.width(8.dp))
                            Text("Control parental", style = MaterialTheme.typography.titleSmall)
                        }
                        Switch(
                            checked = user?.parentalControlEnabled == true,
                            onCheckedChange = { viewModel.setParentalControl(it) }
                        )
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.VerifiedUser, null, tint = DeiRed)
                            Spacer(Modifier.width(8.dp))
                            Text("Reproducción automática", style = MaterialTheme.typography.titleSmall)
                        }
                        Switch(
                            checked = prefs.autoPlayNext,
                            onCheckedChange = { viewModel.setAutoPlayNext(it) }
                        )
                    }
                }
            }

            // Blocked Users
            val blocked = user?.blockedUserIds.orEmpty().split(",").filter { it.isNotBlank() }
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Block, null, tint = DeiRed)
                        Spacer(Modifier.width(8.dp))
                        Text("Usuarios bloqueados (${blocked.size})", style = MaterialTheme.typography.titleSmall)
                    }
                    if (blocked.isEmpty()) {
                        Text("No has bloqueado ningún usuario.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        blocked.forEach { targetId ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(targetId, style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { viewModel.blockUser(targetId) }) { Text("Desbloquear") }
                            }
                        }
                    }
                }
            }

            // Permissions & Zero Ads Guarantee
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Security, null, tint = DeiRed)
                        Spacer(Modifier.width(8.dp))
                        Text("Permisos, Seguridad y Cero Anuncios", style = MaterialTheme.typography.titleSmall)
                    }
                    Text(
                        "• 100% Libre de Publicidad: DeiPlay no integra redes de anuncios ni banners.\n• Almacenamiento seguro con Photo Picker sin permisos invasivos.\n• Contraseñas protegidas con SHA-256 + salt único.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val notifGranted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!notifGranted) {
                            OutlinedButton(onClick = { notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                                Text("Activar permiso de notificaciones")
                            }
                        }
                    }
                }
            }

            // Delete Account
            Button(
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = DeiRed, contentColor = DeiWhite),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.DeleteForever, null)
                Spacer(Modifier.width(8.dp))
                Text("Eliminar mi cuenta permanentemente")
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Eliminar cuenta?") },
            text = { Text("Esta acción eliminará tu perfil de DeiPlay en este dispositivo y cerrará la sesión.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeiRed)
                ) { Text("Sí, eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
            }
        )
    }
}
