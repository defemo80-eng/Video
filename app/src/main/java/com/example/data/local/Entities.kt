package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["username"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val username: String,
    val displayName: String,
    val passwordHash: String,
    val passwordSalt: String,
    val photoUri: String? = null,
    val googleConnected: Boolean = false,
    val googleAccountEmail: String? = null,
    val youtubeConnected: Boolean = false,
    val youtubeChannelId: String? = null,
    val youtubeChannelTitle: String? = null,
    val youtubeChannelAvatar: String? = null,
    val youtubeSubscriberCount: Long = 0L,
    val youtubeVideoCount: Long = 0L,
    val youtubeViewCount: Long = 0L,
    val parentalControlEnabled: Boolean = false,
    val blockedUserIds: String = "", // comma-separated
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "channels",
    indices = [
        Index(value = ["handle"], unique = true),
        Index(value = ["ownerUserId"])
    ]
)
data class ChannelEntity(
    @PrimaryKey val id: String,
    val ownerUserId: String,
    val name: String,
    val handle: String,
    val avatarUri: String? = null,
    val bannerUri: String? = null,
    val description: String = "",
    val country: String = "Global",
    val socialLinks: String = "",
    val subscriberCount: Long = 0L,
    val totalViews: Long = 0L,
    val videoCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "videos",
    indices = [
        Index(value = ["channelId"]),
        Index(value = ["ownerUserId"]),
        Index(value = ["category"]),
        Index(value = ["isShort"])
    ]
)
data class VideoEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    val ownerUserId: String,
    val channelName: String,
    val channelAvatarUri: String? = null,
    val title: String,
    val description: String = "",
    val mediaUri: String,
    val thumbnailUri: String? = null,
    val durationSeconds: Long = 0L,
    val isShort: Boolean = false,
    val isLive: Boolean = false,
    val isPost: Boolean = false,
    val category: String = "Entretenimiento",
    val language: String = "es",
    val country: String = "Global",
    val tags: String = "",
    val subtitlesText: String = "",
    val privacy: String = "PUBLIC", // PUBLIC, UNLISTED, PRIVATE
    val allowDownload: Boolean = true,
    val likesCount: Long = 0L,
    val dislikesCount: Long = 0L,
    val viewsCount: Long = 0L,
    val commentsCount: Long = 0L,
    val sourcePlatform: String = "DEIPLAY", // DEIPLAY, PEERTUBE, YOUTUBE
    val externalUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "subscriptions",
    indices = [Index(value = ["followerUserId", "channelId"], unique = true)]
)
data class SubscriptionEntity(
    @PrimaryKey val id: String,
    val followerUserId: String,
    val channelId: String,
    val channelName: String,
    val channelAvatarUri: String? = null,
    val notificationsEnabled: Boolean = true,
    val isExternalYouTube: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "video_reactions",
    primaryKeys = ["userId", "videoId"]
)
data class VideoReactionEntity(
    val userId: String,
    val videoId: String,
    val reaction: String, // LIKE, DISLIKE
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "comments",
    indices = [Index(value = ["videoId"]), Index(value = ["parentCommentId"])]
)
data class CommentEntity(
    @PrimaryKey val id: String,
    val videoId: String,
    val parentCommentId: String? = null,
    val authorUserId: String,
    val authorName: String,
    val authorAvatarUri: String? = null,
    val text: String,
    val likesCount: Long = 0L,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "comment_likes",
    primaryKeys = ["userId", "commentId"]
)
data class CommentLikeEntity(
    val userId: String,
    val commentId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val ownerUserId: String,
    val name: String,
    val description: String = "",
    val isPublic: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_items",
    indices = [Index(value = ["playlistId", "videoId"], unique = true)]
)
data class PlaylistItemEntity(
    @PrimaryKey val id: String,
    val playlistId: String,
    val videoId: String,
    val videoTitle: String,
    val videoThumbnailUri: String? = null,
    val channelName: String,
    val mediaUri: String,
    val position: Int,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "watch_history",
    primaryKeys = ["userId", "videoId"]
)
data class WatchHistoryEntity(
    val userId: String,
    val videoId: String,
    val title: String,
    val channelName: String,
    val thumbnailUri: String? = null,
    val mediaUri: String,
    val sourcePlatform: String,
    val watchedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "watch_later",
    primaryKeys = ["userId", "videoId"]
)
data class WatchLaterEntity(
    val userId: String,
    val videoId: String,
    val title: String,
    val channelName: String,
    val thumbnailUri: String? = null,
    val mediaUri: String,
    val sourcePlatform: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "saved_favorites",
    primaryKeys = ["userId", "videoId"]
)
data class SavedFavoriteEntity(
    val userId: String,
    val videoId: String,
    val title: String,
    val channelName: String,
    val thumbnailUri: String? = null,
    val mediaUri: String,
    val sourcePlatform: String,
    val savedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "downloads",
    primaryKeys = ["userId", "videoId"]
)
data class DownloadEntity(
    val userId: String,
    val videoId: String,
    val title: String,
    val channelName: String,
    val thumbnailUri: String? = null,
    val localFilePath: String,
    val qualityLabel: String = "HD",
    val sizeBytes: Long = 0L,
    val downloadedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "notifications",
    indices = [Index(value = ["userId"])]
)
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String, // NEW_VIDEO, NEW_SHORT, LIVE_START, COMMENT_REPLY, NEW_SUBSCRIBER
    val title: String,
    val message: String,
    val targetId: String? = null,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    val reporterUserId: String,
    val targetType: String, // VIDEO, COMMENT, CHANNEL
    val targetId: String,
    val reason: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "live_sessions")
data class LiveSessionEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    val ownerUserId: String,
    val channelName: String,
    val title: String,
    val description: String = "",
    val isActive: Boolean = true,
    val viewerCount: Int = 1,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null
)

@Entity(
    tableName = "live_chat_messages",
    indices = [Index(value = ["sessionId"])]
)
data class LiveChatMessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val authorUserId: String,
    val authorName: String,
    val message: String,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
