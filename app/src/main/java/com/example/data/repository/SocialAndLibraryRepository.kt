package com.example.data.repository

import com.example.data.local.ChannelDao
import com.example.data.local.CommentEntity
import com.example.data.local.CommentLikeEntity
import com.example.data.local.DownloadEntity
import com.example.data.local.LibraryDao
import com.example.data.local.LiveChatMessageEntity
import com.example.data.local.LiveDao
import com.example.data.local.LiveSessionEntity
import com.example.data.local.NotificationEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistItemEntity
import com.example.data.local.ReportEntity
import com.example.data.local.SavedFavoriteEntity
import com.example.data.local.SocialDao
import com.example.data.local.SubscriptionEntity
import com.example.data.local.UserEntity
import com.example.data.local.VideoDao
import com.example.data.local.VideoEntity
import com.example.data.local.VideoReactionEntity
import com.example.data.local.WatchHistoryEntity
import com.example.data.local.WatchLaterEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class SocialAndLibraryRepository(
    private val channelDao: ChannelDao,
    private val videoDao: VideoDao,
    private val socialDao: SocialDao,
    private val libraryDao: LibraryDao,
    private val liveDao: LiveDao
) {
    fun observeDeiPlaySubscriptions(userId: String): Flow<List<SubscriptionEntity>> =
        socialDao.observeSubscriptions(userId, isExternal = false)

    fun observeYouTubeSubscriptions(userId: String): Flow<List<SubscriptionEntity>> =
        socialDao.observeSubscriptions(userId, isExternal = true)

    fun observeSubscription(userId: String, channelId: String): Flow<SubscriptionEntity?> =
        socialDao.observeSubscription(userId, channelId)

    fun observeReaction(userId: String, videoId: String): Flow<VideoReactionEntity?> =
        socialDao.observeReaction(userId, videoId)

    fun observeLikedVideoIds(userId: String): Flow<List<VideoReactionEntity>> =
        socialDao.observeLikedVideoIds(userId)

    fun observeComments(videoId: String): Flow<List<CommentEntity>> =
        socialDao.observeCommentsForVideo(videoId)

    fun observeLikedCommentIds(userId: String): Flow<List<String>> =
        socialDao.observeLikedCommentIds(userId)

    fun observePlaylists(userId: String): Flow<List<PlaylistEntity>> =
        libraryDao.observePlaylists(userId)

    fun observePlaylistItems(playlistId: String): Flow<List<PlaylistItemEntity>> =
        libraryDao.observePlaylistItems(playlistId)

    fun observeHistory(userId: String): Flow<List<WatchHistoryEntity>> =
        libraryDao.observeHistory(userId)

    fun observeWatchLater(userId: String): Flow<List<WatchLaterEntity>> =
        libraryDao.observeWatchLater(userId)

    fun observeIsWatchLater(userId: String, videoId: String): Flow<Boolean> =
        libraryDao.observeIsWatchLater(userId, videoId)

    fun observeSavedFavorites(userId: String): Flow<List<SavedFavoriteEntity>> =
        libraryDao.observeSavedFavorites(userId)

    fun observeIsSavedFavorite(userId: String, videoId: String): Flow<Boolean> =
        libraryDao.observeIsSavedFavorite(userId, videoId)

    fun observeDownloads(userId: String): Flow<List<DownloadEntity>> =
        libraryDao.observeDownloads(userId)

    fun observeNotifications(userId: String): Flow<List<NotificationEntity>> =
        libraryDao.observeNotifications(userId)

    fun observeUnreadNotificationCount(userId: String): Flow<Int> =
        libraryDao.observeUnreadNotificationCount(userId)

    fun observeActiveLives(): Flow<List<LiveSessionEntity>> =
        liveDao.observeActiveLives()

    fun observeChannelLives(channelId: String): Flow<List<LiveSessionEntity>> =
        liveDao.observeChannelLives(channelId)

    fun observeLiveChat(sessionId: String): Flow<List<LiveChatMessageEntity>> =
        liveDao.observeLiveChat(sessionId)

    suspend fun toggleSubscribe(
        follower: UserEntity,
        channelId: String,
        channelName: String,
        channelAvatarUri: String?,
        isDeiPlayChannel: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val existing = socialDao.getSubscription(follower.id, channelId)
        if (existing != null) {
            socialDao.deleteSubscription(follower.id, channelId)
            if (isDeiPlayChannel) {
                channelDao.adjustSubscriberCount(channelId, -1L)
            }
            false
        } else {
            socialDao.insertSubscription(
                SubscriptionEntity(
                    id = "${follower.id}_$channelId",
                    followerUserId = follower.id,
                    channelId = channelId,
                    channelName = channelName,
                    channelAvatarUri = channelAvatarUri,
                    notificationsEnabled = true,
                    isExternalYouTube = false
                )
            )
            if (isDeiPlayChannel) {
                channelDao.adjustSubscriberCount(channelId, 1L)
                val targetChannel = channelDao.getChannelById(channelId)
                if (targetChannel != null && targetChannel.ownerUserId != follower.id) {
                    libraryDao.insertNotification(
                        NotificationEntity(
                            id = UUID.randomUUID().toString(),
                            userId = targetChannel.ownerUserId,
                            type = "NEW_SUBSCRIBER",
                            title = "¡Nuevo suscriptor en ${targetChannel.name}!",
                            message = "${follower.displayName} (@${follower.username}) se suscribió a tu canal.",
                            targetId = channelId
                        )
                    )
                }
            }
            true
        }
    }

    suspend fun toggleBell(userId: String, channelId: String, currentEnabled: Boolean) = withContext(Dispatchers.IO) {
        socialDao.updateNotificationSetting(userId, channelId, !currentEnabled)
    }

    suspend fun toggleVideoReaction(
        userId: String,
        video: VideoEntity,
        desired: String // "LIKE" or "DISLIKE"
    ) = withContext(Dispatchers.IO) {
        val current = socialDao.getReaction(userId, video.id)
        var likeDelta = 0L
        var dislikeDelta = 0L

        if (current == null) {
            socialDao.insertReaction(VideoReactionEntity(userId, video.id, desired))
            if (desired == "LIKE") likeDelta = 1L else dislikeDelta = 1L
        } else if (current.reaction == desired) {
            socialDao.deleteReaction(userId, video.id)
            if (desired == "LIKE") likeDelta = -1L else dislikeDelta = -1L
        } else {
            socialDao.insertReaction(VideoReactionEntity(userId, video.id, desired))
            if (desired == "LIKE") {
                likeDelta = 1L
                dislikeDelta = -1L
            } else {
                likeDelta = -1L
                dislikeDelta = 1L
            }
        }
        if (video.sourcePlatform == "DEIPLAY") {
            videoDao.adjustReactionCounts(video.id, likeDelta, dislikeDelta)
        }
    }

    suspend fun addComment(
        author: UserEntity,
        video: VideoEntity,
        text: String,
        parentCommentId: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val clean = text.trim()
            require(clean.isNotBlank()) { "El comentario no puede estar vacío." }
            val comment = CommentEntity(
                id = UUID.randomUUID().toString(),
                videoId = video.id,
                parentCommentId = parentCommentId,
                authorUserId = author.id,
                authorName = author.displayName,
                authorAvatarUri = author.photoUri,
                text = clean
            )
            socialDao.insertComment(comment)
            if (video.sourcePlatform == "DEIPLAY") {
                videoDao.adjustCommentsCount(video.id, 1L)
            }
            if (parentCommentId != null) {
                val parent = socialDao.getCommentById(parentCommentId)
                if (parent != null && parent.authorUserId != author.id) {
                    libraryDao.insertNotification(
                        NotificationEntity(
                            id = UUID.randomUUID().toString(),
                            userId = parent.authorUserId,
                            type = "COMMENT_REPLY",
                            title = "${author.displayName} respondió a tu comentario",
                            message = clean,
                            targetId = video.id
                        )
                    )
                }
            }
            Unit
        }
    }

    suspend fun toggleCommentLike(userId: String, commentId: String) = withContext(Dispatchers.IO) {
        val existing = socialDao.getCommentLike(userId, commentId)
        if (existing != null) {
            socialDao.deleteCommentLike(userId, commentId)
            socialDao.adjustCommentLikes(commentId, -1L)
        } else {
            socialDao.insertCommentLike(CommentLikeEntity(userId, commentId))
            socialDao.adjustCommentLikes(commentId, 1L)
        }
    }

    suspend fun pinComment(video: VideoEntity, comment: CommentEntity, actorUserId: String) = withContext(Dispatchers.IO) {
        if (video.ownerUserId == actorUserId) {
            socialDao.setPinnedComment(video.id, comment.id, !comment.isPinned)
        }
    }

    suspend fun deleteComment(video: VideoEntity, comment: CommentEntity, actorUserId: String) = withContext(Dispatchers.IO) {
        if (comment.authorUserId == actorUserId || video.ownerUserId == actorUserId) {
            socialDao.deleteCommentAndReplies(comment.id)
            if (video.sourcePlatform == "DEIPLAY") {
                videoDao.adjustCommentsCount(video.id, -1L)
            }
        }
    }

    suspend fun submitReport(
        reporterUserId: String,
        targetType: String,
        targetId: String,
        reason: String
    ) = withContext(Dispatchers.IO) {
        socialDao.insertReport(
            ReportEntity(
                id = UUID.randomUUID().toString(),
                reporterUserId = reporterUserId,
                targetType = targetType,
                targetId = targetId,
                reason = reason.trim().ifBlank { "Contenido inapropiado" }
            )
        )
    }

    suspend fun recordWatchHistory(userId: String, video: VideoEntity) = withContext(Dispatchers.IO) {
        libraryDao.upsertHistory(
            WatchHistoryEntity(
                userId = userId,
                videoId = video.id,
                title = video.title,
                channelName = video.channelName,
                thumbnailUri = video.thumbnailUri,
                mediaUri = video.mediaUri,
                sourcePlatform = video.sourcePlatform
            )
        )
    }

    suspend fun clearHistory(userId: String) = withContext(Dispatchers.IO) {
        libraryDao.clearHistory(userId)
    }

    suspend fun toggleWatchLater(userId: String, video: VideoEntity, currentlyAdded: Boolean) = withContext(Dispatchers.IO) {
        if (currentlyAdded) {
            libraryDao.deleteWatchLater(userId, video.id)
        } else {
            libraryDao.insertWatchLater(
                WatchLaterEntity(
                    userId = userId,
                    videoId = video.id,
                    title = video.title,
                    channelName = video.channelName,
                    thumbnailUri = video.thumbnailUri,
                    mediaUri = video.mediaUri,
                    sourcePlatform = video.sourcePlatform
                )
            )
        }
    }

    suspend fun toggleSavedFavorite(userId: String, video: VideoEntity, currentlySaved: Boolean) = withContext(Dispatchers.IO) {
        if (currentlySaved) {
            libraryDao.deleteSavedFavorite(userId, video.id)
        } else {
            libraryDao.insertSavedFavorite(
                SavedFavoriteEntity(
                    userId = userId,
                    videoId = video.id,
                    title = video.title,
                    channelName = video.channelName,
                    thumbnailUri = video.thumbnailUri,
                    mediaUri = video.mediaUri,
                    sourcePlatform = video.sourcePlatform
                )
            )
        }
    }

    suspend fun createPlaylist(
        ownerUserId: String,
        name: String,
        description: String,
        isPublic: Boolean
    ): Result<PlaylistEntity> = withContext(Dispatchers.IO) {
        runCatching {
            require(name.trim().isNotBlank()) { "Ingresa un nombre para la lista." }
            val pl = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                ownerUserId = ownerUserId,
                name = name.trim(),
                description = description.trim(),
                isPublic = isPublic
            )
            libraryDao.insertPlaylist(pl)
            pl
        }
    }

    suspend fun togglePlaylistVisibility(playlist: PlaylistEntity) = withContext(Dispatchers.IO) {
        libraryDao.updatePlaylist(playlist.copy(isPublic = !playlist.isPublic))
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        libraryDao.deletePlaylist(playlistId)
    }

    suspend fun addVideoToPlaylist(playlistId: String, video: VideoEntity) = withContext(Dispatchers.IO) {
        val existing = libraryDao.getPlaylistItemsSync(playlistId)
        val nextPos = (existing.maxOfOrNull { it.position } ?: -1) + 1
        libraryDao.insertPlaylistItem(
            PlaylistItemEntity(
                id = "${playlistId}_${video.id}",
                playlistId = playlistId,
                videoId = video.id,
                videoTitle = video.title,
                videoThumbnailUri = video.thumbnailUri,
                channelName = video.channelName,
                mediaUri = video.mediaUri,
                position = nextPos
            )
        )
    }

    suspend fun removeVideoFromPlaylist(playlistId: String, videoId: String) = withContext(Dispatchers.IO) {
        libraryDao.deletePlaylistItem(playlistId, videoId)
    }

    suspend fun movePlaylistItem(playlistId: String, itemId: String, moveUp: Boolean) = withContext(Dispatchers.IO) {
        val items = libraryDao.getPlaylistItemsSync(playlistId).toMutableList()
        val index = items.indexOfFirst { it.id == itemId }
        if (index == -1) return@withContext
        val targetIndex = if (moveUp) index - 1 else index + 1
        if (targetIndex !in items.indices) return@withContext
        val current = items[index]
        items[index] = items[targetIndex]
        items[targetIndex] = current
        val reindexed = items.mapIndexed { idx, item -> item.copy(position = idx) }
        libraryDao.updatePlaylistItems(reindexed)
    }

    suspend fun markAllNotificationsRead(userId: String) = withContext(Dispatchers.IO) {
        libraryDao.markAllNotificationsRead(userId)
    }

    suspend fun startLiveSession(
        ownerUserId: String,
        channelId: String,
        channelName: String,
        title: String,
        description: String
    ): Result<LiveSessionEntity> = withContext(Dispatchers.IO) {
        runCatching {
            require(title.trim().length >= 2) { "Ingresa un título para tu transmisión." }
            val session = LiveSessionEntity(
                id = "live_${UUID.randomUUID()}",
                channelId = channelId,
                ownerUserId = ownerUserId,
                channelName = channelName,
                title = title.trim(),
                description = description.trim()
            )
            liveDao.insertLiveSession(session)

            val followers = socialDao.getNotifiableFollowers(channelId)
            followers.forEach { sub ->
                if (sub.followerUserId != ownerUserId) {
                    libraryDao.insertNotification(
                        NotificationEntity(
                            id = UUID.randomUUID().toString(),
                            userId = sub.followerUserId,
                            type = "LIVE_START",
                            title = "$channelName está EN DIRECTO",
                            message = title.trim(),
                            targetId = session.id
                        )
                    )
                }
            }
            session
        }
    }

    suspend fun endLiveSession(sessionId: String) = withContext(Dispatchers.IO) {
        liveDao.endLiveSession(sessionId, System.currentTimeMillis())
    }

    suspend fun sendLiveChatMessage(
        sessionId: String,
        author: UserEntity,
        message: String
    ) = withContext(Dispatchers.IO) {
        val clean = message.trim()
        if (clean.isNotBlank()) {
            liveDao.insertLiveChatMessage(
                LiveChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    authorUserId = author.id,
                    authorName = author.displayName,
                    message = clean
                )
            )
        }
    }

    suspend fun deleteLiveChatMessage(messageId: String) = withContext(Dispatchers.IO) {
        liveDao.moderateDeleteMessage(messageId)
    }
}
