package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChannelEntity
import com.example.data.local.CommentEntity
import com.example.data.local.DeiPlayDatabase
import com.example.data.local.DownloadEntity
import com.example.data.local.LiveChatMessageEntity
import com.example.data.local.LiveSessionEntity
import com.example.data.local.NotificationEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistItemEntity
import com.example.data.local.SavedFavoriteEntity
import com.example.data.local.SubscriptionEntity
import com.example.data.local.ThemeMode
import com.example.data.local.UserEntity
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.UserPrefsState
import com.example.data.local.VideoEntity
import com.example.data.local.VideoReactionEntity
import com.example.data.local.WatchHistoryEntity
import com.example.data.local.WatchLaterEntity
import com.example.data.remote.NetworkModule
import com.example.data.repository.AiRecommendationRepository
import com.example.data.repository.AiRecommendationResult
import com.example.data.repository.AuthRepository
import com.example.data.repository.ChannelAndVideoRepository
import com.example.data.repository.PlaceholderAiRecommendationRepository
import com.example.data.repository.SocialAndLibraryRepository
import com.example.data.repository.YouTubeConnectRepository
import com.example.player.DeiPlayPlayerManager
import com.example.player.PlayerUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DeiPlayViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DeiPlayDatabase.getInstance(application)
    private val prefsRepo = UserPreferencesRepository(application)

    private val authRepo = AuthRepository(application, db.userDao(), prefsRepo)
    private val channelVideoRepo = ChannelAndVideoRepository(
        context = application,
        channelDao = db.channelDao(),
        videoDao = db.videoDao(),
        socialDao = db.socialDao(),
        libraryDao = db.libraryDao(),
        peerTubeApi = NetworkModule.peerTubeService
    )
    private val socialLibraryRepo = SocialAndLibraryRepository(
        channelDao = db.channelDao(),
        videoDao = db.videoDao(),
        socialDao = db.socialDao(),
        libraryDao = db.libraryDao(),
        liveDao = db.liveDao()
    )
    private val ytRepo = YouTubeConnectRepository(
        youTubeApi = NetworkModule.youTubeApiService,
        youTubeOEmbed = NetworkModule.youTubeOEmbedService,
        userDao = db.userDao(),
        socialDao = db.socialDao(),
        prefsRepo = prefsRepo
    )
    private val aiRepo: AiRecommendationRepository = PlaceholderAiRecommendationRepository()

    val playerManager = DeiPlayPlayerManager(
        context = application,
        scope = viewModelScope,
        onVideoCompleted = { playNextVideo() }
    )
    val playerState: StateFlow<PlayerUiState> = playerManager.state

    val prefsState: StateFlow<UserPrefsState> = prefsRepo.prefsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPrefsState())

    val currentUser: StateFlow<UserEntity?> = prefsRepo.prefsFlow
        .flatMapLatest { prefs ->
            val uid = prefs.currentUserId
            if (uid.isNullOrBlank()) flowOf(null) else authRepo.observeUser(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val myChannel: StateFlow<ChannelEntity?> = currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(null) else channelVideoRepo.observeUserChannel(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allChannels: StateFlow<List<ChannelEntity>> = channelVideoRepo.observeAllChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deiPlayPublicVideos: StateFlow<List<VideoEntity>> = channelVideoRepo.observePublicVideos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myOwnedVideos: StateFlow<List<VideoEntity>> = currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else channelVideoRepo.observeOwnerVideos(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deiPlaySubscriptions: StateFlow<List<SubscriptionEntity>> = currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else socialLibraryRepo.observeDeiPlaySubscriptions(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val youtubeSubscriptions: StateFlow<List<SubscriptionEntity>> = currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else socialLibraryRepo.observeYouTubeSubscriptions(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchHistory: StateFlow<List<WatchHistoryEntity>> = currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else socialLibraryRepo.observeHistory(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchLater: StateFlow<List<WatchLaterEntity>> = currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else socialLibraryRepo.observeWatchLater(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedFavorites: StateFlow<List<SavedFavoriteEntity>> = currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else socialLibraryRepo.observeSavedFavorites(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val likedVideoReactions: StateFlow<List<VideoReactionEntity>> = currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else socialLibraryRepo.observeLikedVideoIds(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloads: StateFlow<List<DownloadEntity>> = currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else socialLibraryRepo.observeDownloads(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> = currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else socialLibraryRepo.observePlaylists(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else socialLibraryRepo.observeNotifications(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationCount: StateFlow<Int> = currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(0) else socialLibraryRepo.observeUnreadNotificationCount(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeLives: StateFlow<List<LiveSessionEntity>> = socialLibraryRepo.observeActiveLives()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategory = MutableStateFlow("Inicio")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("Todos")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _worldVideos = MutableStateFlow<List<VideoEntity>>(emptyList())
    val worldVideos: StateFlow<List<VideoEntity>> = _worldVideos.asStateFlow()

    private val _syncedCreatorVideos = MutableStateFlow<List<VideoEntity>>(emptyList())
    val syncedCreatorVideos: StateFlow<List<VideoEntity>> = _syncedCreatorVideos.asStateFlow()

    private val _isWorldLoading = MutableStateFlow(false)
    val isWorldLoading: StateFlow<Boolean> = _isWorldLoading.asStateFlow()

    private val _worldError = MutableStateFlow<String?>(null)
    val worldError: StateFlow<String?> = _worldError.asStateFlow()

    private val _aiRecommendations = MutableStateFlow<AiRecommendationResult?>(null)
    val aiRecommendations: StateFlow<AiRecommendationResult?> = _aiRecommendations.asStateFlow()

    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress: StateFlow<Float?> = _uploadProgress.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress.asStateFlow()

    private val _authBusy = MutableStateFlow(false)
    val authBusy: StateFlow<Boolean> = _authBusy.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    val combinedFeedVideos: StateFlow<List<VideoEntity>> = combine(
        deiPlayPublicVideos,
        _worldVideos,
        _syncedCreatorVideos,
        currentUser,
        _selectedCategory,
        _searchQuery
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val local = values[0] as List<VideoEntity>
        @Suppress("UNCHECKED_CAST")
        val world = values[1] as List<VideoEntity>
        @Suppress("UNCHECKED_CAST")
        val creatorSynced = values[2] as List<VideoEntity>
        val user = values[3] as UserEntity?
        val category = values[4] as String
        val query = values[5] as String

        val blockedIds = user?.blockedUserIds
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()

        val all = (local + creatorSynced + world).distinctBy { it.id }.filter { v ->
            if (blockedIds.contains(v.ownerUserId)) return@filter false
            if (user?.parentalControlEnabled == true && v.category == "Películas y videos") return@filter false
            val matchesQuery = query.isBlank() ||
                v.title.contains(query, ignoreCase = true) ||
                v.channelName.contains(query, ignoreCase = true) ||
                v.tags.contains(query, ignoreCase = true)
            val matchesCategory = when (category) {
                "Inicio", "Para ti", "Tendencias" -> true
                "Shorts" -> v.isShort
                "Directos" -> v.isLive
                else -> v.category.equals(category, ignoreCase = true)
            }
            matchesQuery && matchesCategory
        }

        when (category) {
            "Tendencias" -> all.sortedByDescending { it.viewsCount + (it.likesCount * 2L) }
            "Para ti" -> {
                val ranked = _aiRecommendations.value?.rankedVideoIds.orEmpty()
                if (ranked.isEmpty()) all else all.sortedBy { ranked.indexOf(it.id).let { idx -> if (idx == -1) Int.MAX_VALUE else idx } }
            }
            else -> all
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shortsVideos: StateFlow<List<VideoEntity>> = combine(
        deiPlayPublicVideos,
        _worldVideos,
        _syncedCreatorVideos
    ) { local, world, synced ->
        (local + synced + world).distinctBy { it.id }.filter { it.isShort && !it.isPost }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshWorldVideos()
        viewModelScope.launch {
            prefsRepo.prefsFlow.collect { prefs ->
                playerManager.setAutoPlayNext(prefs.autoPlayNext)
            }
        }
        viewModelScope.launch {
            combine(watchHistory, combinedFeedVideos) { hist, videos ->
                hist to videos
            }.collect { (hist, videos) ->
                if (videos.isNotEmpty()) {
                    _aiRecommendations.value = aiRepo.generateRecommendations(hist, videos)
                }
            }
        }
    }

    fun emitMessage(message: String) {
        _snackbarEvents.tryEmit(message)
    }

    // --- AUTH ---
    fun register(email: String, username: String, displayName: String, password: String) {
        viewModelScope.launch {
            _authBusy.value = true
            val res = authRepo.registerWithEmail(email, username, displayName, password)
            _authBusy.value = false
            res.onSuccess {
                emitMessage("Bienvenido a DeiPlay, ${it.displayName}")
            }.onFailure {
                emitMessage(it.message ?: "Error al registrar cuenta")
            }
        }
    }

    fun login(identifier: String, password: String) {
        viewModelScope.launch {
            _authBusy.value = true
            val res = authRepo.loginWithEmailOrUsername(identifier, password)
            _authBusy.value = false
            res.onSuccess {
                emitMessage("Sesión iniciada como ${it.displayName}")
            }.onFailure {
                emitMessage(it.message ?: "Error al iniciar sesión")
            }
        }
    }

    fun recoverPassword(email: String, newPassword: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authBusy.value = true
            val res = authRepo.recoverPassword(email, newPassword)
            _authBusy.value = false
            res.onSuccess {
                emitMessage("Contraseña actualizada correctamente")
                onSuccess()
            }.onFailure {
                emitMessage(it.message ?: "No se pudo recuperar la contraseña")
            }
        }
    }

    fun updateProfile(displayName: String, username: String, photoUri: Uri?, onDone: () -> Unit) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            val res = authRepo.updateProfile(uid, displayName, username, photoUri)
            res.onSuccess {
                emitMessage("Perfil actualizado")
                onDone()
            }.onFailure {
                emitMessage(it.message ?: "Error al actualizar perfil")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            playerManager.closePlayer()
            authRepo.signOut()
            emitMessage("Sesión cerrada")
        }
    }

    fun deleteAccount() {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            playerManager.closePlayer()
            authRepo.deleteAccount(uid)
            emitMessage("Cuenta eliminada")
        }
    }

    // --- WORLD DISCOVERY ---
    fun selectCategory(category: String) {
        _selectedCategory.value = category
        refreshWorldVideos()
    }

    fun selectLanguage(language: String) {
        _selectedLanguage.value = language
        refreshWorldVideos()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refreshWorldVideos() {
        viewModelScope.launch {
            _isWorldLoading.value = true
            _worldError.value = null
            val res = channelVideoRepo.fetchWorldVideos(
                query = _searchQuery.value,
                categoryLabel = _selectedCategory.value,
                languageLabel = _selectedLanguage.value
            )
            val ytKey = prefsState.value.youtubeApiKey
            val ytToken = prefsState.value.youtubeAccessToken
            val ytPopular = if (ytKey.isNotBlank()) ytRepo.fetchYouTubePopularIfConfigured(ytKey) else emptyList()

            res.onSuccess { peerTubeList ->
                _worldVideos.value = (ytPopular + peerTubeList).distinctBy { it.id }
            }.onFailure { err ->
                if (ytPopular.isNotEmpty()) {
                    _worldVideos.value = ytPopular
                } else {
                    _worldError.value = "Sin conexión al índice mundial ahora mismo (${err.localizedMessage ?: "error de red"}). Tus videos y descargas locales siguen disponibles."
                }
            }

            // Also sync latest uploads from every creator the user is subscribed to (PeerTube + YouTube)
            syncSubscribedCreatorsUploads(ytToken, ytKey)
            _isWorldLoading.value = false
        }
    }

    private suspend fun syncSubscribedCreatorsUploads(ytToken: String, ytKey: String) {
        val synced = mutableListOf<VideoEntity>()

        // 1. Subscribed PeerTube creators
        val ptSubs = deiPlaySubscriptions.value.filter { it.channelId.startsWith("pt_ch_") }.take(8)
        ptSubs.forEach { sub ->
            channelVideoRepo.fetchPeerTubeChannelVideos(sub.channelId).onSuccess { list ->
                synced.addAll(list)
            }
        }

        // 2. Own connected YouTube channel + subscribed YouTube creators
        val ytChannelIds = buildList {
            currentUser.value?.youtubeChannelId?.takeIf { it.isNotBlank() }?.let { add(it) }
            youtubeSubscriptions.value.forEach { add(it.channelId) }
        }.distinct()

        if (ytChannelIds.isNotEmpty() && (ytToken.isNotBlank() || ytKey.isNotBlank())) {
            synced.addAll(
                ytRepo.fetchCreatorsUploads(
                    bearerToken = ytToken,
                    apiKey = ytKey,
                    rawChannelIds = ytChannelIds
                )
            )
        }

        if (synced.isNotEmpty()) {
            _syncedCreatorVideos.value = (synced + _syncedCreatorVideos.value).distinctBy { it.id }
        }
    }

    fun loadExternalChannelVideos(channelId: String) {
        viewModelScope.launch {
            val fetched = mutableListOf<VideoEntity>()
            if (channelId.startsWith("pt_ch_")) {
                channelVideoRepo.fetchPeerTubeChannelVideos(channelId).onSuccess { list ->
                    fetched.addAll(list)
                }
            } else if (channelId.startsWith("yt_")) {
                val prefs = prefsState.value
                fetched.addAll(
                    ytRepo.fetchCreatorsUploads(
                        bearerToken = prefs.youtubeAccessToken,
                        apiKey = prefs.youtubeApiKey,
                        rawChannelIds = listOf(channelId)
                    )
                )
            }
            if (fetched.isNotEmpty()) {
                _syncedCreatorVideos.value = (fetched + _syncedCreatorVideos.value).distinctBy { it.id }
            }
        }
    }

    fun importYouTubeUrl(url: String) {
        viewModelScope.launch {
            val res = ytRepo.inspectYouTubeUrlViaOEmbed(url)
            res.onSuccess { ytVideo ->
                _worldVideos.value = (listOf(ytVideo) + _worldVideos.value).distinctBy { it.id }
                openVideo(ytVideo)
            }.onFailure {
                emitMessage(it.message ?: "No se pudo consultar el enlace de YouTube")
            }
        }
    }

    // --- PLAYBACK & DOWNLOADS ---
    fun openVideo(video: VideoEntity) {
        val user = currentUser.value
        viewModelScope.launch {
            if (user != null) {
                socialLibraryRepo.recordWatchHistory(user.id, video)
            }
            channelVideoRepo.incrementVideoView(video)

            // Check if offline download already exists for offline playback!
            val offline = if (user != null) db.libraryDao().getDownload(user.id, video.id) else null
            if (offline != null && java.io.File(offline.localFilePath).exists()) {
                val localUri = Uri.fromFile(java.io.File(offline.localFilePath)).toString()
                playerManager.playVideo(
                    video = video,
                    streams = listOf(com.example.data.repository.StreamQualityOption("${offline.qualityLabel} (Offline)", localUri)),
                    downloadPermitted = true,
                    overrideStreamUrl = localUri
                )
                return@launch
            }

            val resolved = channelVideoRepo.resolvePlayableStreams(video)
            playerManager.playVideo(
                video = video,
                streams = resolved.streams,
                downloadPermitted = resolved.downloadPermitted
            )
        }
    }

    fun playNextVideo() {
        val current = playerState.value.activeVideo ?: return
        val feed = combinedFeedVideos.value.filter { !it.isPost }
        if (feed.isEmpty()) return
        val idx = feed.indexOfFirst { it.id == current.id }
        val next = if (idx >= 0 && idx + 1 < feed.size) feed[idx + 1] else feed.firstOrNull { it.id != current.id }
        if (next != null) {
            openVideo(next)
        }
    }

    fun downloadActiveVideo(qualityLabel: String, streamUrl: String) {
        val uid = currentUser.value?.id ?: return
        val video = playerState.value.activeVideo ?: return
        viewModelScope.launch {
            _downloadProgress.value = 0.01f
            val res = channelVideoRepo.downloadVideoOffline(uid, video, qualityLabel, streamUrl) { p ->
                _downloadProgress.value = p
            }
            _downloadProgress.value = null
            res.onSuccess {
                emitMessage("Descarga guardada sin conexión (${it.qualityLabel})")
            }.onFailure {
                emitMessage(it.message ?: "No se pudo descargar el video")
            }
        }
    }

    fun deleteDownload(download: DownloadEntity) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            channelVideoRepo.deleteOfflineDownload(uid, download)
            emitMessage("Descarga eliminada")
        }
    }

    // --- CHANNELS & PUBLISHING ---
    fun observeChannel(channelId: String): Flow<ChannelEntity?> = channelVideoRepo.observeChannel(channelId)
    fun observeChannelVideos(channelId: String): Flow<List<VideoEntity>> = channelVideoRepo.observeChannelVideos(channelId)
    fun observeSubscription(channelId: String): Flow<SubscriptionEntity?> {
        val uid = currentUser.value?.id ?: return flowOf(null)
        return socialLibraryRepo.observeSubscription(uid, channelId)
    }

    fun createOrUpdateChannel(
        name: String,
        handle: String,
        description: String,
        country: String,
        socialLinks: String,
        avatarUri: Uri?,
        bannerUri: Uri?,
        onSuccess: (ChannelEntity) -> Unit
    ) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            val res = channelVideoRepo.createOrUpdateChannel(
                ownerUserId = uid,
                existingChannelId = myChannel.value?.id,
                name = name,
                handle = handle,
                description = description,
                country = country,
                socialLinks = socialLinks,
                avatarUri = avatarUri,
                bannerUri = bannerUri
            )
            res.onSuccess {
                emitMessage("Canal guardado: ${it.name}")
                onSuccess(it)
            }.onFailure {
                emitMessage(it.message ?: "No se pudo guardar el canal")
            }
        }
    }

    fun publishVideo(
        title: String,
        description: String,
        videoUri: Uri?,
        thumbnailUri: Uri?,
        isShort: Boolean,
        isPost: Boolean,
        category: String,
        language: String,
        country: String,
        tags: String,
        subtitlesText: String,
        privacy: String,
        allowDownload: Boolean,
        onSuccess: () -> Unit
    ) {
        val uid = currentUser.value?.id ?: return
        val channel = myChannel.value
        if (channel == null) {
            emitMessage("Primero crea tu canal en DeiPlay para publicar contenido.")
            return
        }
        viewModelScope.launch {
            _uploadProgress.value = 0.01f
            val res = channelVideoRepo.publishVideo(
                ownerUserId = uid,
                channel = channel,
                title = title,
                description = description,
                videoSourceUri = videoUri,
                thumbnailSourceUri = thumbnailUri,
                isShort = isShort,
                isPost = isPost,
                category = category,
                language = language,
                country = country,
                tags = tags,
                subtitlesText = subtitlesText,
                privacy = privacy,
                allowDownload = allowDownload,
                onUploadProgress = { p -> _uploadProgress.value = p }
            )
            _uploadProgress.value = null
            res.onSuccess {
                emitMessage(if (isShort) "¡Short publicado!" else if (isPost) "¡Publicación creada!" else "¡Video publicado en DeiPlay!")
                onSuccess()
            }.onFailure {
                emitMessage(it.message ?: "Error al publicar")
            }
        }
    }

    fun updateVideoMetadata(
        videoId: String,
        title: String,
        description: String,
        category: String,
        tags: String,
        subtitlesText: String,
        privacy: String,
        allowDownload: Boolean
    ) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            val res = channelVideoRepo.updateVideoMetadata(
                videoId, uid, title, description, category, tags, subtitlesText, privacy, allowDownload
            )
            res.onSuccess { emitMessage("Video actualizado") }
                .onFailure { emitMessage(it.message ?: "Error al actualizar video") }
        }
    }

    fun deleteVideo(videoId: String) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            val res = channelVideoRepo.deleteOwnedVideo(videoId, uid)
            res.onSuccess { emitMessage("Video eliminado") }
                .onFailure { emitMessage(it.message ?: "No se pudo eliminar") }
        }
    }

    // --- SOCIAL & INTERACTIONS ---
    fun toggleSubscribe(channelId: String, channelName: String, channelAvatarUri: String?, isDeiPlayChannel: Boolean) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val subscribed = socialLibraryRepo.toggleSubscribe(user, channelId, channelName, channelAvatarUri, isDeiPlayChannel)
            emitMessage(if (subscribed) "Suscrito a $channelName" else "Suscripción cancelada")
        }
    }

    fun toggleBell(channelId: String, currentlyEnabled: Boolean) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            socialLibraryRepo.toggleBell(uid, channelId, currentlyEnabled)
            emitMessage(if (!currentlyEnabled) "Notificaciones activadas" else "Notificaciones desactivadas")
        }
    }

    fun observeReaction(videoId: String): Flow<VideoReactionEntity?> {
        val uid = currentUser.value?.id ?: return flowOf(null)
        return socialLibraryRepo.observeReaction(uid, videoId)
    }

    fun observeIsWatchLater(videoId: String): Flow<Boolean> {
        val uid = currentUser.value?.id ?: return flowOf(false)
        return socialLibraryRepo.observeIsWatchLater(uid, videoId)
    }

    fun observeIsSavedFavorite(videoId: String): Flow<Boolean> {
        val uid = currentUser.value?.id ?: return flowOf(false)
        return socialLibraryRepo.observeIsSavedFavorite(uid, videoId)
    }

    fun observeComments(videoId: String): Flow<List<CommentEntity>> = socialLibraryRepo.observeComments(videoId)

    fun observeLikedCommentIds(): Flow<List<String>> {
        val uid = currentUser.value?.id ?: return flowOf(emptyList())
        return socialLibraryRepo.observeLikedCommentIds(uid)
    }

    fun toggleReaction(video: VideoEntity, desired: String) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            socialLibraryRepo.toggleVideoReaction(uid, video, desired)
        }
    }

    fun addComment(video: VideoEntity, text: String, parentCommentId: String? = null) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = socialLibraryRepo.addComment(user, video, text, parentCommentId)
            res.onFailure { emitMessage(it.message ?: "No se pudo publicar el comentario") }
        }
    }

    fun toggleCommentLike(commentId: String) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch { socialLibraryRepo.toggleCommentLike(uid, commentId) }
    }

    fun pinComment(video: VideoEntity, comment: CommentEntity) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch { socialLibraryRepo.pinComment(video, comment, uid) }
    }

    fun deleteComment(video: VideoEntity, comment: CommentEntity) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch { socialLibraryRepo.deleteComment(video, comment, uid) }
    }

    fun toggleWatchLater(video: VideoEntity, currentlyAdded: Boolean) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            socialLibraryRepo.toggleWatchLater(uid, video, currentlyAdded)
            emitMessage(if (!currentlyAdded) "Guardado en Ver más tarde" else "Quitado de Ver más tarde")
        }
    }

    fun toggleSavedFavorite(video: VideoEntity, currentlySaved: Boolean) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            socialLibraryRepo.toggleSavedFavorite(uid, video, currentlySaved)
            emitMessage(if (!currentlySaved) "Guardado en Favoritos" else "Quitado de Favoritos")
        }
    }

    fun reportContent(targetType: String, targetId: String, reason: String) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            socialLibraryRepo.submitReport(uid, targetType, targetId, reason)
            emitMessage("Reporte enviado para moderación")
        }
    }

    fun blockUser(targetUserId: String) {
        val uid = currentUser.value?.id ?: return
        if (targetUserId == uid) return
        viewModelScope.launch {
            authRepo.toggleBlockUser(uid, targetUserId)
            emitMessage("Lista de usuarios bloqueados actualizada")
        }
    }

    // --- PLAYLISTS & HISTORY ---
    fun createPlaylist(name: String, description: String, isPublic: Boolean) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            socialLibraryRepo.createPlaylist(uid, name, description, isPublic)
                .onSuccess { emitMessage("Lista «${it.name}» creada") }
                .onFailure { emitMessage(it.message ?: "Error al crear lista") }
        }
    }

    fun observePlaylistItems(playlistId: String): Flow<List<PlaylistItemEntity>> =
        socialLibraryRepo.observePlaylistItems(playlistId)

    fun addVideoToPlaylist(playlistId: String, video: VideoEntity) {
        viewModelScope.launch {
            socialLibraryRepo.addVideoToPlaylist(playlistId, video)
            emitMessage("Video agregado a la lista")
        }
    }

    fun removeVideoFromPlaylist(playlistId: String, videoId: String) {
        viewModelScope.launch { socialLibraryRepo.removeVideoFromPlaylist(playlistId, videoId) }
    }

    fun movePlaylistItem(playlistId: String, itemId: String, moveUp: Boolean) {
        viewModelScope.launch { socialLibraryRepo.movePlaylistItem(playlistId, itemId, moveUp) }
    }

    fun togglePlaylistVisibility(playlist: PlaylistEntity) {
        viewModelScope.launch { socialLibraryRepo.togglePlaylistVisibility(playlist) }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch { socialLibraryRepo.deletePlaylist(playlistId) }
    }

    fun clearHistory() {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            socialLibraryRepo.clearHistory(uid)
            emitMessage("Historial borrado")
        }
    }

    fun markNotificationsRead() {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch { socialLibraryRepo.markAllNotificationsRead(uid) }
    }

    // --- LIVE STREAMING ---
    fun startLiveSession(title: String, description: String, onStarted: (LiveSessionEntity) -> Unit) {
        val uid = currentUser.value?.id ?: return
        val channel = myChannel.value
        if (channel == null) {
            emitMessage("Crea tu canal antes de transmitir en directo")
            return
        }
        viewModelScope.launch {
            socialLibraryRepo.startLiveSession(uid, channel.id, channel.name, title, description)
                .onSuccess {
                    emitMessage("Transmisión en directo iniciada")
                    onStarted(it)
                }
                .onFailure { emitMessage(it.message ?: "Error al iniciar directo") }
        }
    }

    fun endLiveSession(sessionId: String) {
        viewModelScope.launch {
            socialLibraryRepo.endLiveSession(sessionId)
            emitMessage("Transmisión finalizada")
        }
    }

    fun observeLiveSession(sessionId: String): Flow<LiveSessionEntity?> =
        db.liveDao().observeLiveSession(sessionId)

    fun observeLiveChat(sessionId: String): Flow<List<LiveChatMessageEntity>> =
        socialLibraryRepo.observeLiveChat(sessionId)

    fun sendLiveMessage(sessionId: String, message: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch { socialLibraryRepo.sendLiveChatMessage(sessionId, user, message) }
    }

    fun moderateLiveMessage(messageId: String) {
        viewModelScope.launch { socialLibraryRepo.deleteLiveChatMessage(messageId) }
    }

    // --- YOUTUBE & SETTINGS ---
    fun saveYouTubeConfig(apiKey: String, clientId: String, accessToken: String) {
        viewModelScope.launch {
            prefsRepo.saveYouTubeCredentials(apiKey, clientId, accessToken)
            emitMessage("Configuración de YouTube guardada")
            if (apiKey.isNotBlank()) refreshWorldVideos()
        }
    }

    fun syncYouTubeAccount(accessToken: String) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            val res = ytRepo.syncWithOAuthToken(uid, accessToken)
            res.onSuccess {
                prefsRepo.saveYouTubeCredentials(
                    prefsState.value.youtubeApiKey,
                    prefsState.value.youtubeOAuthClientId,
                    accessToken
                )
                if (it.uploadedVideos.isNotEmpty()) {
                    _syncedCreatorVideos.value = (it.uploadedVideos + _syncedCreatorVideos.value).distinctBy { v -> v.id }
                }
                emitMessage("YouTube conectado: ${it.channelTitle} (${it.importedSubscriptionsCount} canales, ${it.uploadedVideos.size} videos)")
            }.onFailure {
                emitMessage(it.message ?: "No se pudo conectar con YouTube Data API")
            }
        }
    }

    fun disconnectYouTube() {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            ytRepo.disconnectYouTube(uid)
            emitMessage("Cuenta de YouTube desconectada")
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    }

    fun setAutoPlayNext(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setAutoPlayNext(enabled) }
    }

    fun setParentalControl(enabled: Boolean) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            authRepo.setParentalControl(uid, enabled)
            emitMessage(if (enabled) "Control parental activado" else "Control parental desactivado")
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
