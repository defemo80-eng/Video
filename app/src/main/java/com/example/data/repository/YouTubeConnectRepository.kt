package com.example.data.repository

import com.example.data.local.SocialDao
import com.example.data.local.SubscriptionEntity
import com.example.data.local.UserDao
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.VideoEntity
import com.example.data.remote.YouTubeApiService
import com.example.data.remote.YouTubeOEmbedService
import com.example.data.remote.YouTubePlaylistItem
import com.example.data.remote.YouTubeVideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class YouTubeSyncResult(
    val channelTitle: String,
    val subscribersCount: Long,
    val videoCount: Long,
    val viewCount: Long,
    val importedSubscriptionsCount: Int,
    val uploadedVideos: List<VideoEntity> = emptyList()
)

class YouTubeConnectRepository(
    private val youTubeApi: YouTubeApiService,
    private val youTubeOEmbed: YouTubeOEmbedService,
    private val userDao: UserDao,
    private val socialDao: SocialDao,
    private val prefsRepo: UserPreferencesRepository
) {
    suspend fun syncWithOAuthToken(
        userId: String,
        accessToken: String
    ): Result<YouTubeSyncResult> = withContext(Dispatchers.IO) {
        try {
            val cleanToken = accessToken.trim().removePrefix("Bearer ").trim()
            require(cleanToken.isNotBlank()) {
                "Ingresa un token OAuth2 válido de Google/YouTube para sincronizar tu canal."
            }
            val authHeader = "Bearer $cleanToken"
            val channelResp = youTubeApi.getMyChannel(bearerToken = authHeader)
            val channel = channelResp.items.firstOrNull()
                ?: return@withContext Result.failure(
                    IllegalStateException("No se encontró un canal de YouTube asociado a ese token.")
                )

            val subsCount = channel.statistics?.subscriberCount?.toLongOrNull() ?: 0L
            val videoCount = channel.statistics?.videoCount?.toLongOrNull() ?: 0L
            val viewCount = channel.statistics?.viewCount?.toLongOrNull() ?: 0L
            val avatar = channel.snippet?.thumbnails?.high?.url
                ?: channel.snippet?.thumbnails?.medium?.url
                ?: channel.snippet?.thumbnails?.default?.url

            val user = userDao.getUserById(userId)
                ?: return@withContext Result.failure(IllegalStateException("Usuario no encontrado."))

            userDao.updateUser(
                user.copy(
                    youtubeConnected = true,
                    youtubeChannelId = channel.id,
                    youtubeChannelTitle = channel.snippet?.title ?: "Mi Canal YouTube",
                    youtubeChannelAvatar = avatar,
                    youtubeSubscriberCount = subsCount,
                    youtubeVideoCount = videoCount,
                    youtubeViewCount = viewCount
                )
            )

            val syncedVideos = mutableListOf<VideoEntity>()

            // 1. Fetch uploads from the creator's own YouTube channel
            val ownUploadsPlaylist = channel.contentDetails?.relatedPlaylists?.uploads
            if (!ownUploadsPlaylist.isNullOrBlank()) {
                runCatching {
                    val items = youTubeApi.getPlaylistItems(
                        bearerToken = authHeader,
                        playlistId = ownUploadsPlaylist,
                        maxResults = 15
                    ).items
                    syncedVideos.addAll(
                        resolveYouTubeItemsWithStats(items, bearerToken = authHeader, apiKey = null, fallbackAvatar = avatar)
                    )
                }
            }

            // 2. Fetch authorized YouTube subscriptions & each creator's recent uploads
            var importedSubs = 0
            val subscribedChannelIds = mutableListOf<String>()
            try {
                val subsResp = youTubeApi.getMySubscriptions(bearerToken = authHeader)
                subsResp.items.forEach { item ->
                    val targetChannelId = item.snippet?.resourceId?.channelId
                    if (!targetChannelId.isNullOrBlank()) {
                        val subAvatar = item.snippet.thumbnails?.medium?.url
                            ?: item.snippet.thumbnails?.default?.url
                        socialDao.insertSubscription(
                            SubscriptionEntity(
                                id = "yt_${userId}_$targetChannelId",
                                followerUserId = userId,
                                channelId = "yt_$targetChannelId",
                                channelName = item.snippet.title.ifBlank { "Canal de YouTube" },
                                channelAvatarUri = subAvatar,
                                notificationsEnabled = false,
                                isExternalYouTube = true
                            )
                        )
                        subscribedChannelIds.add(targetChannelId)
                        importedSubs++
                    }
                }
            } catch (_: Exception) {
                // Scope may allow channel read without subscriptions read; still succeed channel sync
            }

            if (subscribedChannelIds.isNotEmpty()) {
                syncedVideos.addAll(
                    fetchCreatorsUploads(
                        bearerToken = cleanToken,
                        apiKey = null,
                        rawChannelIds = subscribedChannelIds.take(8)
                    )
                )
            }

            Result.success(
                YouTubeSyncResult(
                    channelTitle = channel.snippet?.title ?: "Canal conectado",
                    subscribersCount = subsCount,
                    videoCount = videoCount,
                    viewCount = viewCount,
                    importedSubscriptionsCount = importedSubs,
                    uploadedVideos = syncedVideos.distinctBy { it.id }
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches the latest videos uploaded by a list of YouTube creators (using OAuth token or API key).
     */
    suspend fun fetchCreatorsUploads(
        bearerToken: String?,
        apiKey: String?,
        rawChannelIds: List<String>
    ): List<VideoEntity> = withContext(Dispatchers.IO) {
        val cleanToken = bearerToken?.trim()?.removePrefix("Bearer ")?.takeIf { it.isNotBlank() }?.let { "Bearer $it" }
        val cleanKey = apiKey?.trim()?.takeIf { it.isNotBlank() }
        if (cleanToken == null && cleanKey == null) return@withContext emptyList()
        if (rawChannelIds.isEmpty()) return@withContext emptyList()

        val collected = mutableListOf<VideoEntity>()
        try {
            val idsParam = rawChannelIds
                .map { it.removePrefix("yt_") }
                .filter { it.isNotBlank() }
                .distinct()
                .take(10)
                .joinToString(",")
            if (idsParam.isBlank()) return@withContext emptyList()

            val channelsResp = youTubeApi.getChannelsByIds(
                bearerToken = cleanToken,
                apiKey = cleanKey,
                channelIds = idsParam
            )
            channelsResp.items.forEach { ch ->
                val uploadsId = ch.contentDetails?.relatedPlaylists?.uploads ?: return@forEach
                val chAvatar = ch.snippet?.thumbnails?.medium?.url ?: ch.snippet?.thumbnails?.default?.url
                runCatching {
                    val playlistResp = youTubeApi.getPlaylistItems(
                        bearerToken = cleanToken,
                        apiKey = cleanKey,
                        playlistId = uploadsId,
                        maxResults = 6
                    )
                    collected.addAll(
                        resolveYouTubeItemsWithStats(
                            items = playlistResp.items,
                            bearerToken = cleanToken,
                            apiKey = cleanKey,
                            fallbackAvatar = chAvatar
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Return whatever was fetched before failure
        }
        collected.distinctBy { it.id }
    }

    private suspend fun resolveYouTubeItemsWithStats(
        items: List<YouTubePlaylistItem>,
        bearerToken: String?,
        apiKey: String?,
        fallbackAvatar: String?
    ): List<VideoEntity> {
        if (items.isEmpty()) return emptyList()
        val videoIds = items.mapNotNull {
            it.contentDetails?.videoId ?: it.snippet?.resourceId?.videoId
        }.distinct().take(25)
        if (videoIds.isEmpty()) return emptyList()

        val detailedMap: Map<String, YouTubeVideoItem> = runCatching {
            youTubeApi.getVideosByIds(
                bearerToken = bearerToken,
                apiKey = apiKey,
                videoIds = videoIds.joinToString(",")
            ).items.associateBy { it.id }
        }.getOrDefault(emptyMap())

        return items.mapNotNull { item ->
            val vid = item.contentDetails?.videoId ?: item.snippet?.resourceId?.videoId ?: return@mapNotNull null
            val detail = detailedMap[vid]
            val snippet = detail?.snippet ?: item.snippet
            val thumb = detail?.snippet?.thumbnails?.high?.url
                ?: detail?.snippet?.thumbnails?.medium?.url
                ?: item.snippet?.thumbnails?.high?.url
                ?: item.snippet?.thumbnails?.medium?.url
                ?: item.snippet?.thumbnails?.default?.url
            val chId = detail?.snippet?.channelId?.ifBlank { item.snippet?.channelId.orEmpty() }
                ?: item.snippet?.channelId.orEmpty()
            val title = detail?.snippet?.title ?: item.snippet?.title ?: "Video de YouTube"
            val isShort = title.contains("#shorts", ignoreCase = true)

            VideoEntity(
                id = "yt_$vid",
                channelId = "yt_$chId",
                ownerUserId = "external_youtube",
                channelName = detail?.snippet?.channelTitle ?: item.snippet?.channelTitle ?: "Creador YouTube",
                channelAvatarUri = fallbackAvatar,
                title = title,
                description = detail?.snippet?.description ?: item.snippet?.description.orEmpty(),
                mediaUri = "https://www.youtube.com/watch?v=$vid",
                thumbnailUri = thumb,
                isShort = isShort,
                category = "Entretenimiento",
                viewsCount = detail?.statistics?.viewCount?.toLongOrNull() ?: 0L,
                likesCount = detail?.statistics?.likeCount?.toLongOrNull() ?: 0L,
                commentsCount = detail?.statistics?.commentCount?.toLongOrNull() ?: 0L,
                sourcePlatform = "YOUTUBE",
                externalUrl = "https://www.youtube.com/watch?v=$vid",
                allowDownload = false
            )
        }
    }

    suspend fun disconnectYouTube(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val user = userDao.getUserById(userId) ?: error("Usuario no encontrado")
            userDao.updateUser(
                user.copy(
                    youtubeConnected = false,
                    youtubeChannelId = null,
                    youtubeChannelTitle = null,
                    youtubeChannelAvatar = null,
                    youtubeSubscriberCount = 0L,
                    youtubeVideoCount = 0L,
                    youtubeViewCount = 0L
                )
            )
            prefsRepo.clearYouTubeToken()
        }
    }

    /**
     * Zero-API-key YouTube oEmbed metadata lookup for importing any valid YouTube link reference.
     */
    suspend fun inspectYouTubeUrlViaOEmbed(url: String): Result<VideoEntity> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = url.trim()
            require(cleanUrl.contains("youtube.com") || cleanUrl.contains("youtu.be")) {
                "Ingresa una URL válida de YouTube."
            }
            val info = youTubeOEmbed.getVideoInfo(cleanUrl)
            val id = "yt_oembed_${cleanUrl.hashCode()}"
            Result.success(
                VideoEntity(
                    id = id,
                    channelId = "yt_ext_${info.authorName.hashCode()}",
                    ownerUserId = "external_youtube",
                    channelName = info.authorName.ifBlank { "Creador YouTube" },
                    title = info.title.ifBlank { "Video de YouTube" },
                    description = "Video verificado mediante YouTube oEmbed oficial. URL original: $cleanUrl",
                    mediaUri = cleanUrl,
                    thumbnailUri = info.thumbnailUrl,
                    category = "Entretenimiento",
                    sourcePlatform = "YOUTUBE",
                    externalUrl = cleanUrl,
                    allowDownload = false
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchYouTubePopularIfConfigured(apiKey: String): List<VideoEntity> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext emptyList()
        try {
            val resp = youTubeApi.getPopularVideos(apiKey = apiKey)
            resp.items.map { item ->
                val thumb = item.snippet?.thumbnails?.high?.url
                    ?: item.snippet?.thumbnails?.medium?.url
                VideoEntity(
                    id = "yt_${item.id}",
                    channelId = "yt_${item.snippet?.channelId.orEmpty()}",
                    ownerUserId = "external_youtube",
                    channelName = item.snippet?.channelTitle ?: "YouTube",
                    title = item.snippet?.title ?: "Video de YouTube",
                    description = item.snippet?.description.orEmpty(),
                    mediaUri = "https://www.youtube.com/watch?v=${item.id}",
                    thumbnailUri = thumb,
                    viewsCount = item.statistics?.viewCount?.toLongOrNull() ?: 0L,
                    likesCount = item.statistics?.likeCount?.toLongOrNull() ?: 0L,
                    commentsCount = item.statistics?.commentCount?.toLongOrNull() ?: 0L,
                    category = "Tendencias",
                    sourcePlatform = "YOUTUBE",
                    externalUrl = "https://www.youtube.com/watch?v=${item.id}",
                    allowDownload = false
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
