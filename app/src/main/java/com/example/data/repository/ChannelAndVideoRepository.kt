package com.example.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.data.local.ChannelDao
import com.example.data.local.ChannelEntity
import com.example.data.local.DownloadEntity
import com.example.data.local.LibraryDao
import com.example.data.local.NotificationEntity
import com.example.data.local.SocialDao
import com.example.data.local.VideoDao
import com.example.data.local.VideoEntity
import com.example.data.remote.PeerTubeApiService
import com.example.data.remote.PeerTubeVideoSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.util.UUID

data class StreamQualityOption(
    val label: String,
    val url: String,
    val sizeBytes: Long = 0L
)

data class ResolvedPlaybackStreams(
    val streams: List<StreamQualityOption>,
    val downloadPermitted: Boolean,
    val sourceDetailUrl: String? = null
)

class ChannelAndVideoRepository(
    private val context: Context,
    private val channelDao: ChannelDao,
    private val videoDao: VideoDao,
    private val socialDao: SocialDao,
    private val libraryDao: LibraryDao,
    private val peerTubeApi: PeerTubeApiService
) {
    fun observeAllChannels(): Flow<List<ChannelEntity>> = channelDao.observeAllChannels()
    fun observeChannel(channelId: String): Flow<ChannelEntity?> = channelDao.observeChannel(channelId)
    fun observeUserChannel(userId: String): Flow<ChannelEntity?> = channelDao.observeUserChannel(userId)
    fun observePublicVideos(): Flow<List<VideoEntity>> = videoDao.observePublicVideos()
    fun observeChannelVideos(channelId: String): Flow<List<VideoEntity>> = videoDao.observeChannelVideos(channelId)
    fun observeOwnerVideos(userId: String): Flow<List<VideoEntity>> = videoDao.observeOwnerVideos(userId)
    fun observeVideo(videoId: String): Flow<VideoEntity?> = videoDao.observeVideo(videoId)

    suspend fun createOrUpdateChannel(
        ownerUserId: String,
        existingChannelId: String?,
        name: String,
        handle: String,
        description: String,
        country: String,
        socialLinks: String,
        avatarUri: Uri?,
        bannerUri: Uri?
    ): Result<ChannelEntity> = withContext(Dispatchers.IO) {
        try {
            val cleanName = name.trim()
            val cleanHandle = handle.trim().removePrefix("@").lowercase()
            require(cleanName.length >= 2) { "El nombre del canal debe tener al menos 2 caracteres." }
            require(cleanHandle.length >= 3 && cleanHandle.matches(Regex("^[a-zA-Z0-9_.-]+$"))) {
                "El @usuario del canal debe tener al menos 3 caracteres válidos."
            }

            val handleOwner = channelDao.getChannelByHandle(cleanHandle)
            if (handleOwner != null && handleOwner.id != existingChannelId) {
                return@withContext Result.failure(IllegalStateException("Ese @usuario de canal ya existe."))
            }

            val existing = existingChannelId?.let { channelDao.getChannelById(it) }
                ?: channelDao.getChannelByOwner(ownerUserId)

            val savedAvatar = if (avatarUri != null) {
                copyMediaFile(avatarUri, "avatars", "ch_avatar_${ownerUserId}.jpg") {}
            } else existing?.avatarUri

            val savedBanner = if (bannerUri != null) {
                copyMediaFile(bannerUri, "banners", "ch_banner_${ownerUserId}.jpg") {}
            } else existing?.bannerUri

            if (existing == null) {
                val created = ChannelEntity(
                    id = "ch_${UUID.randomUUID()}",
                    ownerUserId = ownerUserId,
                    name = cleanName,
                    handle = cleanHandle,
                    avatarUri = savedAvatar,
                    bannerUri = savedBanner,
                    description = description.trim(),
                    country = country.trim().ifBlank { "Global" },
                    socialLinks = socialLinks.trim()
                )
                channelDao.insertChannel(created)
                Result.success(created)
            } else {
                val updated = existing.copy(
                    name = cleanName,
                    handle = cleanHandle,
                    avatarUri = savedAvatar,
                    bannerUri = savedBanner,
                    description = description.trim(),
                    country = country.trim().ifBlank { existing.country },
                    socialLinks = socialLinks.trim()
                )
                channelDao.updateChannel(updated)
                Result.success(updated)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun publishVideo(
        ownerUserId: String,
        channel: ChannelEntity,
        title: String,
        description: String,
        videoSourceUri: Uri?,
        thumbnailSourceUri: Uri?,
        isShort: Boolean,
        isPost: Boolean,
        category: String,
        language: String,
        country: String,
        tags: String,
        subtitlesText: String,
        privacy: String,
        allowDownload: Boolean,
        onUploadProgress: (Float) -> Unit
    ): Result<VideoEntity> = withContext(Dispatchers.IO) {
        try {
            val cleanTitle = title.trim()
            require(cleanTitle.length >= 2) { "El título debe tener al menos 2 caracteres." }
            if (!isPost) {
                require(videoSourceUri != null) { "Selecciona un archivo de video desde tu dispositivo." }
            }

            onUploadProgress(0.05f)
            val videoId = "vid_${UUID.randomUUID()}"
            val storedMediaUri = if (videoSourceUri != null) {
                copyMediaFile(videoSourceUri, "videos", "${videoId}.mp4") { p ->
                    onUploadProgress(0.05f + (p * 0.80f))
                }
            } else {
                ""
            }

            val storedThumbUri = if (thumbnailSourceUri != null) {
                copyMediaFile(thumbnailSourceUri, "thumbnails", "${videoId}_thumb.jpg") {}
            } else {
                null
            }

            onUploadProgress(0.90f)
            val durationSec = if (storedMediaUri.isNotBlank()) {
                extractDurationSeconds(storedMediaUri)
            } else 0L

            val entity = VideoEntity(
                id = videoId,
                channelId = channel.id,
                ownerUserId = ownerUserId,
                channelName = channel.name,
                channelAvatarUri = channel.avatarUri,
                title = cleanTitle,
                description = description.trim(),
                mediaUri = storedMediaUri,
                thumbnailUri = storedThumbUri,
                durationSeconds = durationSec,
                isShort = isShort,
                isPost = isPost,
                category = category,
                language = language,
                country = country.ifBlank { channel.country },
                tags = tags.trim(),
                subtitlesText = subtitlesText.trim(),
                privacy = privacy,
                allowDownload = allowDownload,
                sourcePlatform = "DEIPLAY"
            )
            videoDao.insertVideo(entity)
            channelDao.adjustVideoCount(channel.id, 1)

            // Notify followers who enabled bell notifications
            if (privacy == "PUBLIC") {
                val followers = socialDao.getNotifiableFollowers(channel.id)
                val type = if (isShort) "NEW_SHORT" else "NEW_VIDEO"
                val label = if (isShort) "nuevo Short" else "nuevo video"
                followers.forEach { sub ->
                    if (sub.followerUserId != ownerUserId) {
                        libraryDao.insertNotification(
                            NotificationEntity(
                                id = UUID.randomUUID().toString(),
                                userId = sub.followerUserId,
                                type = type,
                                title = "${channel.name} publicó un $label",
                                message = cleanTitle,
                                targetId = videoId
                            )
                        )
                    }
                }
            }
            onUploadProgress(1.0f)
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateVideoMetadata(
        videoId: String,
        ownerUserId: String,
        title: String,
        description: String,
        category: String,
        tags: String,
        subtitlesText: String,
        privacy: String,
        allowDownload: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val existing = videoDao.getVideoById(videoId) ?: error("Video no encontrado")
            check(existing.ownerUserId == ownerUserId) { "No tienes permiso para editar este video." }
            require(title.trim().length >= 2) { "El título es demasiado corto." }
            videoDao.updateVideo(
                existing.copy(
                    title = title.trim(),
                    description = description.trim(),
                    category = category,
                    tags = tags.trim(),
                    subtitlesText = subtitlesText.trim(),
                    privacy = privacy,
                    allowDownload = allowDownload
                )
            )
        }
    }

    suspend fun deleteOwnedVideo(videoId: String, ownerUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val existing = videoDao.getVideoById(videoId) ?: error("Video no encontrado")
            check(existing.ownerUserId == ownerUserId) { "No puedes eliminar un video que no es tuyo." }
            videoDao.deleteVideo(videoId)
            channelDao.adjustVideoCount(existing.channelId, -1)
            runCatching {
                val parsed = Uri.parse(existing.mediaUri)
                if (parsed.scheme == "file" && parsed.path != null) {
                    File(parsed.path!!).delete()
                }
            }
            Unit
        }
    }

    suspend fun incrementVideoView(video: VideoEntity) = withContext(Dispatchers.IO) {
        if (video.sourcePlatform == "DEIPLAY") {
            videoDao.incrementViews(video.id)
            channelDao.incrementTotalViews(video.channelId)
        }
    }

    /**
     * Fetches real global creator videos from JoinPeerTube Federation (zero API key required).
     */
    suspend fun fetchWorldVideos(
        query: String? = null,
        categoryLabel: String = "Inicio",
        languageLabel: String = "Todos"
    ): Result<List<VideoEntity>> = withContext(Dispatchers.IO) {
        try {
            val categoryId = mapCategoryToPeerTubeId(categoryLabel)
            val sort = when (categoryLabel) {
                "Tendencias" -> "-views"
                "Para ti" -> "-likes"
                "Inicio" -> "-views"
                else -> "-publishedAt"
            }
            val isLive = when (categoryLabel) {
                "Directos" -> true
                "Inicio", "Para ti", "Tendencias" -> false
                else -> null
            }
            val durationMin = if (categoryLabel == "Inicio" && query.isNullOrBlank()) 61 else null
            val langCode = mapLanguageCode(languageLabel)

            val response = peerTubeApi.searchVideos(
                search = query?.takeIf { it.isNotBlank() },
                count = 24,
                sort = sort,
                isLive = isLive,
                categoryId = categoryId,
                languageCode = langCode,
                durationMin = durationMin
            )
            val mapped = response.data.map { summary -> summary.toVideoEntity(categoryLabel) }
            Result.success(mapped)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches short-form global creator videos (<= 60s) from JoinPeerTube Federation.
     */
    suspend fun fetchWorldShorts(
        query: String? = null,
        languageLabel: String = "Todos"
    ): Result<List<VideoEntity>> = withContext(Dispatchers.IO) {
        try {
            val langCode = mapLanguageCode(languageLabel)
            val response = peerTubeApi.searchVideos(
                search = query?.takeIf { it.isNotBlank() },
                count = 16,
                sort = "-views",
                isLive = false,
                languageCode = langCode,
                durationMax = 60
            )
            val mapped = response.data
                .map { summary -> summary.toVideoEntity("Shorts").copy(isShort = true) }
            Result.success(mapped)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches all public videos uploaded by a specific global PeerTube creator channel (zero API key required).
     */
    suspend fun fetchPeerTubeChannelVideos(channelId: String): Result<List<VideoEntity>> = withContext(Dispatchers.IO) {
        try {
            val raw = channelId.removePrefix("pt_ch_")
            val host = raw.substringBefore("|", "")
            val handle = raw.substringAfter("|", "")
            if (host.isBlank() || handle.isBlank() || handle.all { it.isDigit() }) {
                return@withContext Result.success(emptyList())
            }
            val url = "https://$host/api/v1/video-channels/$handle/videos?count=24&sort=-publishedAt&nsfw=false"
            val response = try {
                peerTubeApi.getChannelVideos(url)
            } catch (_: Exception) {
                val fallbackUrl = "https://$host/api/v1/accounts/$handle/videos?count=24&sort=-publishedAt&nsfw=false"
                peerTubeApi.getChannelVideos(fallbackUrl)
            }
            Result.success(response.data.map { it.toVideoEntity("Inicio") })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resolvePlayableStreams(video: VideoEntity): ResolvedPlaybackStreams = withContext(Dispatchers.IO) {
        if (video.sourcePlatform == "DEIPLAY" || video.mediaUri.startsWith("file://") || video.mediaUri.startsWith("content://")) {
            return@withContext ResolvedPlaybackStreams(
                streams = listOf(StreamQualityOption("Original", video.mediaUri)),
                downloadPermitted = video.allowDownload
            )
        }
        val extUrl = video.externalUrl ?: video.mediaUri
        if (video.sourcePlatform == "PEERTUBE" && extUrl.startsWith("http")) {
            try {
                val uri = URI(extUrl)
                val host = uri.host
                val uuid = video.id.removePrefix("pt_")
                if (!host.isNullOrBlank() && uuid.isNotBlank()) {
                    val detailUrl = "https://$host/api/v1/videos/$uuid"
                    val detail = peerTubeApi.getVideoDetails(detailUrl)
                    val options = mutableListOf<StreamQualityOption>()
                    detail.files.forEach { file ->
                        if (file.fileUrl.isNotBlank()) {
                            val label = file.resolution?.label?.ifBlank { "MP4" } ?: "MP4"
                            options.add(StreamQualityOption(label, file.fileUrl, file.size))
                        }
                    }
                    detail.streamingPlaylists.forEach { playlist ->
                        playlist.files.forEach { file ->
                            if (file.fileUrl.isNotBlank()) {
                                val label = file.resolution?.label?.ifBlank { "HLS" } ?: "HLS"
                                options.add(StreamQualityOption(label, file.fileUrl, file.size))
                            }
                        }
                        if (options.isEmpty() && playlist.playlistUrl.isNotBlank()) {
                            options.add(StreamQualityOption("Auto HLS", playlist.playlistUrl))
                        }
                    }
                    if (options.isNotEmpty()) {
                        return@withContext ResolvedPlaybackStreams(
                            streams = options.distinctBy { it.label }.sortedByDescending {
                                it.label.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0
                            },
                            downloadPermitted = detail.downloadEnabled,
                            sourceDetailUrl = detail.url
                        )
                    }
                }
            } catch (_: Exception) {
                // Fall back to direct URL if origin instance metadata fetch fails
            }
        }
        ResolvedPlaybackStreams(
            streams = listOf(StreamQualityOption("Auto", video.mediaUri)),
            downloadPermitted = video.allowDownload
        )
    }

    suspend fun downloadVideoOffline(
        userId: String,
        video: VideoEntity,
        qualityLabel: String,
        streamUrl: String,
        onProgress: (Float) -> Unit
    ): Result<DownloadEntity> = withContext(Dispatchers.IO) {
        try {
            require(streamUrl.isNotBlank()) { "No hay flujo de video disponible para descargar." }
            val dir = File(context.filesDir, "downloads/$userId").apply { mkdirs() }
            val outFile = File(dir, "${video.id}_${qualityLabel}.mp4")

            if (streamUrl.startsWith("file://")) {
                val srcFile = File(Uri.parse(streamUrl).path ?: error("Ruta local inválida"))
                val total = srcFile.length().coerceAtLeast(1L)
                var copied = 0L
                srcFile.inputStream().use { input ->
                    outFile.outputStream().use { output ->
                        val buf = ByteArray(8192)
                        var len = input.read(buf)
                        while (len >= 0) {
                            output.write(buf, 0, len)
                            copied += len
                            onProgress((copied.toFloat() / total.toFloat()).coerceIn(0f, 1f))
                            len = input.read(buf)
                        }
                    }
                }
            } else {
                val resp = peerTubeApi.downloadFile(streamUrl)
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("Error HTTP ${resp.code()} al descargar."))
                }
                val body = resp.body() ?: return@withContext Result.failure(IllegalStateException("Respuesta vacía"))
                val total = body.contentLength()
                var downloaded = 0L
                body.byteStream().use { input ->
                    outFile.outputStream().use { output ->
                        val buf = ByteArray(8192)
                        var len = input.read(buf)
                        while (len >= 0) {
                            output.write(buf, 0, len)
                            downloaded += len
                            if (total > 0) {
                                onProgress((downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f))
                            }
                            len = input.read(buf)
                        }
                    }
                }
            }

            onProgress(1f)
            val entity = DownloadEntity(
                userId = userId,
                videoId = video.id,
                title = video.title,
                channelName = video.channelName,
                thumbnailUri = video.thumbnailUri,
                localFilePath = outFile.absolutePath,
                qualityLabel = qualityLabel,
                sizeBytes = outFile.length()
            )
            libraryDao.insertDownload(entity)
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteOfflineDownload(userId: String, download: DownloadEntity) = withContext(Dispatchers.IO) {
        runCatching {
            File(download.localFilePath).delete()
            libraryDao.deleteDownload(userId, download.videoId)
        }
    }

    private fun copyMediaFile(
        uri: Uri,
        folder: String,
        fileName: String,
        onProgress: (Float) -> Unit
    ): String {
        val dir = File(context.filesDir, folder).apply { mkdirs() }
        val destFile = File(dir, fileName)
        val totalSize = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        var copied = 0L
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output ->
                val buffer = ByteArray(16 * 1024)
                var bytesRead = input.read(buffer)
                while (bytesRead >= 0) {
                    output.write(buffer, 0, bytesRead)
                    copied += bytesRead
                    if (totalSize > 0) {
                        onProgress((copied.toFloat() / totalSize.toFloat()).coerceIn(0f, 1f))
                    }
                    bytesRead = input.read(buffer)
                }
            }
        } ?: error("No se pudo abrir el archivo multimedia seleccionado.")
        return Uri.fromFile(destFile).toString()
    }

    private fun extractDurationSeconds(fileUriString: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            val path = Uri.parse(fileUriString).path ?: return 0L
            retriever.setDataSource(path)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            durationMs / 1000L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun PeerTubeVideoSummary.toVideoEntity(fallbackCategory: String): VideoEntity {
        val host = channel?.host?.ifBlank { account?.host.orEmpty() }.orEmpty()
        val avatarPath = channel?.avatar?.path
            ?: channel?.avatars?.firstOrNull()?.path
            ?: account?.avatar?.path
        val fullAvatar = if (!avatarPath.isNullOrBlank() && host.isNotBlank()) {
            if (avatarPath.startsWith("http")) avatarPath else "https://$host$avatarPath"
        } else null
        val channelName = channel?.displayName?.ifBlank { channel.name }
            ?: account?.displayName?.ifBlank { account.name }
            ?: "Creador Global"
        val channelHandle = channel?.name?.takeIf { it.isNotBlank() }
            ?: account?.name?.takeIf { it.isNotBlank() }
            ?: (channel?.id ?: account?.id ?: 0L).toString()
        val encodedChannelId = if (host.isNotBlank()) "pt_ch_${host}|${channelHandle}" else "pt_ch_${channel?.id ?: 0L}"
        val rawThumb = thumbnailUrl?.takeIf { it.isNotBlank() } ?: previewUrl?.takeIf { it.isNotBlank() }
        val resolvedThumb = when {
            rawThumb.isNullOrBlank() -> null
            rawThumb.startsWith("http") -> rawThumb
            host.isNotBlank() -> "https://$host$rawThumb"
            else -> rawThumb
        }
        return VideoEntity(
            id = "pt_${uuid.ifBlank { shortUUID }}",
            channelId = encodedChannelId,
            ownerUserId = "external_peertube",
            channelName = channelName,
            channelAvatarUri = fullAvatar,
            title = name.ifBlank { "Video sin título" },
            description = description.orEmpty(),
            mediaUri = url,
            thumbnailUri = resolvedThumb,
            durationSeconds = duration,
            isShort = duration in 1..60,
            isLive = isLive,
            category = category?.label ?: fallbackCategory,
            language = language?.id ?: "en",
            country = host.substringAfterLast('.').uppercase().ifBlank { "Global" },
            tags = tags.joinToString(", "),
            privacy = "PUBLIC",
            allowDownload = true,
            likesCount = likes,
            dislikesCount = dislikes,
            viewsCount = views,
            sourcePlatform = "PEERTUBE",
            externalUrl = url
        )
    }

    private fun mapCategoryToPeerTubeId(label: String): Int? = when (label) {
        "Música" -> 1
        "Películas y videos" -> 2
        "Videojuegos" -> 7
        "Deportes" -> 8
        "Noticias" -> 10
        "Educación" -> 13
        "Tecnología" -> 15
        "Entretenimiento" -> 16
        else -> null
    }

    private fun mapLanguageCode(label: String): String? = when (label) {
        "Español" -> "es"
        "English" -> "en"
        "Français" -> "fr"
        "Português" -> "pt"
        "Deutsch" -> "de"
        "日本語" -> "ja"
        else -> null
    }
}
