package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun observeUser(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: String)
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY subscriberCount DESC, createdAt DESC")
    fun observeAllChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE id = :channelId LIMIT 1")
    fun observeChannel(channelId: String): Flow<ChannelEntity?>

    @Query("SELECT * FROM channels WHERE ownerUserId = :userId LIMIT 1")
    fun observeUserChannel(userId: String): Flow<ChannelEntity?>

    @Query("SELECT * FROM channels WHERE id = :channelId LIMIT 1")
    suspend fun getChannelById(channelId: String): ChannelEntity?

    @Query("SELECT * FROM channels WHERE ownerUserId = :userId LIMIT 1")
    suspend fun getChannelByOwner(userId: String): ChannelEntity?

    @Query("SELECT * FROM channels WHERE LOWER(handle) = LOWER(:handle) LIMIT 1")
    suspend fun getChannelByHandle(handle: String): ChannelEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertChannel(channel: ChannelEntity)

    @Update
    suspend fun updateChannel(channel: ChannelEntity)

    @Query("UPDATE channels SET subscriberCount = MAX(0, subscriberCount + :delta) WHERE id = :channelId")
    suspend fun adjustSubscriberCount(channelId: String, delta: Long)

    @Query("UPDATE channels SET totalViews = totalViews + 1 WHERE id = :channelId")
    suspend fun incrementTotalViews(channelId: String)

    @Query("UPDATE channels SET videoCount = MAX(0, videoCount + :delta) WHERE id = :channelId")
    suspend fun adjustVideoCount(channelId: String, delta: Int)
}

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos WHERE privacy = 'PUBLIC' ORDER BY createdAt DESC")
    fun observePublicVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE channelId = :channelId ORDER BY createdAt DESC")
    fun observeChannelVideos(channelId: String): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE ownerUserId = :userId ORDER BY createdAt DESC")
    fun observeOwnerVideos(userId: String): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :videoId LIMIT 1")
    fun observeVideo(videoId: String): Flow<VideoEntity?>

    @Query("SELECT * FROM videos WHERE id = :videoId LIMIT 1")
    suspend fun getVideoById(videoId: String): VideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity)

    @Update
    suspend fun updateVideo(video: VideoEntity)

    @Query("DELETE FROM videos WHERE id = :videoId")
    suspend fun deleteVideo(videoId: String)

    @Query("UPDATE videos SET viewsCount = viewsCount + 1 WHERE id = :videoId")
    suspend fun incrementViews(videoId: String)

    @Query("UPDATE videos SET likesCount = MAX(0, likesCount + :likeDelta), dislikesCount = MAX(0, dislikesCount + :dislikeDelta) WHERE id = :videoId")
    suspend fun adjustReactionCounts(videoId: String, likeDelta: Long, dislikeDelta: Long)

    @Query("UPDATE videos SET commentsCount = MAX(0, commentsCount + :delta) WHERE id = :videoId")
    suspend fun adjustCommentsCount(videoId: String, delta: Long)
}

@Dao
interface SocialDao {
    @Query("SELECT * FROM subscriptions WHERE followerUserId = :userId AND isExternalYouTube = :isExternal ORDER BY createdAt DESC")
    fun observeSubscriptions(userId: String, isExternal: Boolean): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE followerUserId = :userId AND channelId = :channelId LIMIT 1")
    fun observeSubscription(userId: String, channelId: String): Flow<SubscriptionEntity?>

    @Query("SELECT * FROM subscriptions WHERE followerUserId = :userId AND channelId = :channelId LIMIT 1")
    suspend fun getSubscription(userId: String, channelId: String): SubscriptionEntity?

    @Query("SELECT * FROM subscriptions WHERE channelId = :channelId AND notificationsEnabled = 1")
    suspend fun getNotifiableFollowers(channelId: String): List<SubscriptionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(sub: SubscriptionEntity)

    @Query("DELETE FROM subscriptions WHERE followerUserId = :userId AND channelId = :channelId")
    suspend fun deleteSubscription(userId: String, channelId: String)

    @Query("UPDATE subscriptions SET notificationsEnabled = :enabled WHERE followerUserId = :userId AND channelId = :channelId")
    suspend fun updateNotificationSetting(userId: String, channelId: String, enabled: Boolean)

    @Query("SELECT * FROM video_reactions WHERE userId = :userId AND videoId = :videoId LIMIT 1")
    fun observeReaction(userId: String, videoId: String): Flow<VideoReactionEntity?>

    @Query("SELECT * FROM video_reactions WHERE userId = :userId AND videoId = :videoId LIMIT 1")
    suspend fun getReaction(userId: String, videoId: String): VideoReactionEntity?

    @Query("SELECT * FROM video_reactions WHERE userId = :userId AND reaction = 'LIKE' ORDER BY updatedAt DESC")
    fun observeLikedVideoIds(userId: String): Flow<List<VideoReactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReaction(reaction: VideoReactionEntity)

    @Query("DELETE FROM video_reactions WHERE userId = :userId AND videoId = :videoId")
    suspend fun deleteReaction(userId: String, videoId: String)

    @Query("SELECT * FROM comments WHERE videoId = :videoId ORDER BY isPinned DESC, createdAt DESC")
    fun observeCommentsForVideo(videoId: String): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comments WHERE id = :commentId LIMIT 1")
    suspend fun getCommentById(commentId: String): CommentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Query("DELETE FROM comments WHERE id = :commentId OR parentCommentId = :commentId")
    suspend fun deleteCommentAndReplies(commentId: String)

    @Query("UPDATE comments SET isPinned = CASE WHEN id = :commentId THEN :pinned ELSE 0 END WHERE videoId = :videoId")
    suspend fun setPinnedComment(videoId: String, commentId: String, pinned: Boolean)

    @Query("SELECT * FROM comment_likes WHERE userId = :userId AND commentId = :commentId LIMIT 1")
    suspend fun getCommentLike(userId: String, commentId: String): CommentLikeEntity?

    @Query("SELECT commentId FROM comment_likes WHERE userId = :userId")
    fun observeLikedCommentIds(userId: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommentLike(like: CommentLikeEntity)

    @Query("DELETE FROM comment_likes WHERE userId = :userId AND commentId = :commentId")
    suspend fun deleteCommentLike(userId: String, commentId: String)

    @Query("UPDATE comments SET likesCount = MAX(0, likesCount + :delta) WHERE id = :commentId")
    suspend fun adjustCommentLikes(commentId: String, delta: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)
}

@Dao
interface LibraryDao {
    @Query("SELECT * FROM playlists WHERE ownerUserId = :userId ORDER BY createdAt DESC")
    fun observePlaylists(userId: String): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId LIMIT 1")
    suspend fun getPlaylist(playlistId: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY position ASC")
    fun observePlaylistItems(playlistId: String): Flow<List<PlaylistItemEntity>>

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getPlaylistItemsSync(playlistId: String): List<PlaylistItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItemEntity)

    @Update
    suspend fun updatePlaylistItems(items: List<PlaylistItemEntity>)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun deletePlaylistItem(playlistId: String, videoId: String)

    @Query("SELECT * FROM watch_history WHERE userId = :userId ORDER BY watchedAt DESC")
    fun observeHistory(userId: String): Flow<List<WatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(entry: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE userId = :userId")
    suspend fun clearHistory(userId: String)

    @Query("SELECT * FROM watch_later WHERE userId = :userId ORDER BY addedAt DESC")
    fun observeWatchLater(userId: String): Flow<List<WatchLaterEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watch_later WHERE userId = :userId AND videoId = :videoId)")
    fun observeIsWatchLater(userId: String, videoId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchLater(entry: WatchLaterEntity)

    @Query("DELETE FROM watch_later WHERE userId = :userId AND videoId = :videoId")
    suspend fun deleteWatchLater(userId: String, videoId: String)

    @Query("SELECT * FROM saved_favorites WHERE userId = :userId ORDER BY savedAt DESC")
    fun observeSavedFavorites(userId: String): Flow<List<SavedFavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_favorites WHERE userId = :userId AND videoId = :videoId)")
    fun observeIsSavedFavorite(userId: String, videoId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedFavorite(entry: SavedFavoriteEntity)

    @Query("DELETE FROM saved_favorites WHERE userId = :userId AND videoId = :videoId")
    suspend fun deleteSavedFavorite(userId: String, videoId: String)

    @Query("SELECT * FROM downloads WHERE userId = :userId ORDER BY downloadedAt DESC")
    fun observeDownloads(userId: String): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE userId = :userId AND videoId = :videoId LIMIT 1")
    suspend fun getDownload(userId: String, videoId: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE userId = :userId AND videoId = :videoId")
    suspend fun deleteDownload(userId: String, videoId: String)

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeNotifications(userId: String): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0")
    fun observeUnreadNotificationCount(userId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllNotificationsRead(userId: String)
}

@Dao
interface LiveDao {
    @Query("SELECT * FROM live_sessions WHERE isActive = 1 ORDER BY startedAt DESC")
    fun observeActiveLives(): Flow<List<LiveSessionEntity>>

    @Query("SELECT * FROM live_sessions WHERE channelId = :channelId ORDER BY startedAt DESC")
    fun observeChannelLives(channelId: String): Flow<List<LiveSessionEntity>>

    @Query("SELECT * FROM live_sessions WHERE id = :sessionId LIMIT 1")
    fun observeLiveSession(sessionId: String): Flow<LiveSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLiveSession(session: LiveSessionEntity)

    @Query("UPDATE live_sessions SET isActive = 0, endedAt = :endedAt WHERE id = :sessionId")
    suspend fun endLiveSession(sessionId: String, endedAt: Long)

    @Query("SELECT * FROM live_chat_messages WHERE sessionId = :sessionId AND isDeleted = 0 ORDER BY createdAt ASC")
    fun observeLiveChat(sessionId: String): Flow<List<LiveChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLiveChatMessage(msg: LiveChatMessageEntity)

    @Query("UPDATE live_chat_messages SET isDeleted = 1 WHERE id = :messageId")
    suspend fun moderateDeleteMessage(messageId: String)
}
