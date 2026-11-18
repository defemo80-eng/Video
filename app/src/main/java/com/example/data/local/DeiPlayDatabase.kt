package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        ChannelEntity::class,
        VideoEntity::class,
        SubscriptionEntity::class,
        VideoReactionEntity::class,
        CommentEntity::class,
        CommentLikeEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        WatchHistoryEntity::class,
        WatchLaterEntity::class,
        SavedFavoriteEntity::class,
        DownloadEntity::class,
        NotificationEntity::class,
        ReportEntity::class,
        LiveSessionEntity::class,
        LiveChatMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DeiPlayDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun channelDao(): ChannelDao
    abstract fun videoDao(): VideoDao
    abstract fun socialDao(): SocialDao
    abstract fun libraryDao(): LibraryDao
    abstract fun liveDao(): LiveDao

    companion object {
        @Volatile
        private var INSTANCE: DeiPlayDatabase? = null

        fun getInstance(context: Context): DeiPlayDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DeiPlayDatabase::class.java,
                    "deiplay_database.db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
