package com.example

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DeiPlayDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.VideoEntity
import com.example.data.remote.PeerTubeAccount
import com.example.data.remote.PeerTubeApiService
import com.example.data.remote.PeerTubeChannel
import com.example.data.remote.PeerTubeNamedItem
import com.example.data.remote.PeerTubeResolution
import com.example.data.remote.PeerTubeSearchResponse
import com.example.data.remote.PeerTubeVideoDetail
import com.example.data.remote.PeerTubeVideoFile
import com.example.data.remote.PeerTubeVideoSummary
import com.example.data.remote.YouTubeApiService
import com.example.data.remote.YouTubeChannelContentDetails
import com.example.data.remote.YouTubeChannelItem
import com.example.data.remote.YouTubeChannelListResponse
import com.example.data.remote.YouTubeOEmbedResponse
import com.example.data.remote.YouTubeOEmbedService
import com.example.data.remote.YouTubePlaylistItem
import com.example.data.remote.YouTubePlaylistItemContentDetails
import com.example.data.remote.YouTubePlaylistItemListResponse
import com.example.data.remote.YouTubePlaylistSnippet
import com.example.data.remote.YouTubeRelatedPlaylists
import com.example.data.remote.YouTubeResourceId
import com.example.data.remote.YouTubeSearchListResponse
import com.example.data.remote.YouTubeSnippet
import com.example.data.remote.YouTubeStatistics
import com.example.data.remote.YouTubeSubscriptionItem
import com.example.data.remote.YouTubeSubscriptionListResponse
import com.example.data.remote.YouTubeSubscriptionSnippet
import com.example.data.remote.YouTubeThumbnail
import com.example.data.remote.YouTubeThumbnails
import com.example.data.remote.YouTubeVideoItem
import com.example.data.remote.YouTubeVideoListResponse
import com.example.data.repository.AuthRepository
import com.example.data.repository.ChannelAndVideoRepository
import com.example.data.repository.PlaceholderAiRecommendationRepository
import com.example.data.repository.SocialAndLibraryRepository
import com.example.data.repository.YouTubeConnectRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DeiPlayRepositoryTest {

    private lateinit var context: Context
    private lateinit var db: DeiPlayDatabase
    private lateinit var prefsRepo: UserPreferencesRepository
    private lateinit var authRepo: AuthRepository
    private lateinit var channelVideoRepo: ChannelAndVideoRepository
    private lateinit var socialLibraryRepo: SocialAndLibraryRepository
    private lateinit var ytRepo: YouTubeConnectRepository

    private class FakePeerTubeApi : PeerTubeApiService {
        override suspend fun searchVideos(
            search: String?,
            count: Int,
            sort: String,
            nsfw: String,
            isLive: Boolean?,
            categoryId: Int?,
            languageCode: String?,
            durationMin: Int?,
            durationMax: Int?
        ): PeerTubeSearchResponse {
            val all = listOf(
                PeerTubeVideoSummary(
                    id = 101L,
                    uuid = "uuid-music-1",
                    shortUUID = "short1",
                    name = "Global Acoustic Session",
                    description = "Live acoustic showcase from Madrid",
                    duration = 240L,
                    views = 1500L,
                    likes = 120L,
                    dislikes = 2L,
                    isLive = isLive ?: false,
                    thumbnailUrl = "https://example.org/thumb1.jpg",
                    url = "https://peertube.example.org/w/uuid-music-1",
                    category = PeerTubeNamedItem("1", "Música"),
                    language = PeerTubeNamedItem("es", "Español"),
                    channel = PeerTubeChannel(
                        id = 55L,
                        name = "madrid_sessions",
                        displayName = "Madrid Sessions",
                        host = "peertube.example.org"
                    ),
                    account = PeerTubeAccount(id = 10L, name = "madrid", displayName = "Madrid", host = "peertube.example.org"),
                    tags = listOf("musica", "acustico")
                ),
                PeerTubeVideoSummary(
                    id = 102L,
                    uuid = "uuid-short-2",
                    shortUUID = "short2",
                    name = "60s Tech Tip",
                    duration = 45L,
                    views = 3200L,
                    likes = 310L,
                    url = "https://peertube.example.org/w/uuid-short-2",
                    category = PeerTubeNamedItem("15", "Tecnología"),
                    language = PeerTubeNamedItem("en", "English"),
                    channel = PeerTubeChannel(
                        id = 56L,
                        name = "tech_world",
                        displayName = "Tech World",
                        host = "peertube.example.org"
                    )
                )
            ).filter { item ->
                (durationMin == null || item.duration >= durationMin) &&
                    (durationMax == null || item.duration <= durationMax)
            }
            return PeerTubeSearchResponse(
                total = all.size,
                data = all
            )
        }

        override suspend fun getChannelVideos(fullApiUrl: String): PeerTubeSearchResponse {
            return PeerTubeSearchResponse(
                total = 1,
                data = listOf(
                    PeerTubeVideoSummary(
                        id = 109L,
                        uuid = "uuid-channel-extra",
                        shortUUID = "chExtra",
                        name = "Extra Video from Creator Channel",
                        description = "Uploaded directly to creator channel",
                        duration = 180L,
                        views = 420L,
                        likes = 45L,
                        url = "https://peertube.example.org/w/uuid-channel-extra",
                        category = PeerTubeNamedItem("1", "Música"),
                        language = PeerTubeNamedItem("es", "Español"),
                        channel = PeerTubeChannel(
                            id = 55L,
                            name = "madrid_sessions",
                            displayName = "Madrid Sessions",
                            host = "peertube.example.org"
                        )
                    )
                )
            )
        }

        override suspend fun getVideoDetails(fullApiUrl: String): PeerTubeVideoDetail {
            return PeerTubeVideoDetail(
                id = 101L,
                uuid = "uuid-music-1",
                name = "Global Acoustic Session",
                downloadEnabled = true,
                url = "https://peertube.example.org/w/uuid-music-1",
                files = listOf(
                    PeerTubeVideoFile(
                        resolution = PeerTubeResolution(720, "720p"),
                        size = 4096L,
                        fileUrl = "https://peertube.example.org/static/web-videos/720p.mp4"
                    ),
                    PeerTubeVideoFile(
                        resolution = PeerTubeResolution(1080, "1080p"),
                        size = 8192L,
                        fileUrl = "https://peertube.example.org/static/web-videos/1080p.mp4"
                    )
                )
            )
        }

        override suspend fun downloadFile(fileUrl: String): Response<ResponseBody> {
            return Response.success("fake-mp4-bytes".toResponseBody("video/mp4".toMediaType()))
        }
    }

    private class FakeYouTubeApi : YouTubeApiService {
        override suspend fun getMyChannel(
            bearerToken: String,
            part: String,
            mine: Boolean
        ): YouTubeChannelListResponse {
            return YouTubeChannelListResponse(
                items = listOf(
                    YouTubeChannelItem(
                        id = "UC_TEST_123",
                        snippet = YouTubeSnippet(
                            title = "Dei Creator YT",
                            thumbnails = YouTubeThumbnails(high = YouTubeThumbnail("https://yt.example/avatar.jpg"))
                        ),
                        statistics = YouTubeStatistics(
                            viewCount = "98765",
                            subscriberCount = "4321",
                            videoCount = "42"
                        ),
                        contentDetails = YouTubeChannelContentDetails(
                            relatedPlaylists = YouTubeRelatedPlaylists(uploads = "UU_TEST_123")
                        )
                    )
                )
            )
        }

        override suspend fun getChannelsByIds(
            bearerToken: String?,
            apiKey: String?,
            part: String,
            channelIds: String
        ): YouTubeChannelListResponse {
            return YouTubeChannelListResponse(
                items = listOf(
                    YouTubeChannelItem(
                        id = "UC_EXTERNAL_999",
                        snippet = YouTubeSnippet(
                            title = "Canal Internacional YT",
                            thumbnails = YouTubeThumbnails(default = YouTubeThumbnail("https://yt.example/sub.jpg"))
                        ),
                        contentDetails = YouTubeChannelContentDetails(
                            relatedPlaylists = YouTubeRelatedPlaylists(uploads = "UU_EXTERNAL_999")
                        )
                    )
                )
            )
        }

        override suspend fun getMySubscriptions(
            bearerToken: String,
            part: String,
            mine: Boolean,
            maxResults: Int
        ): YouTubeSubscriptionListResponse {
            return YouTubeSubscriptionListResponse(
                items = listOf(
                    YouTubeSubscriptionItem(
                        id = "sub1",
                        snippet = YouTubeSubscriptionSnippet(
                            title = "Canal Internacional YT",
                            resourceId = YouTubeResourceId(channelId = "UC_EXTERNAL_999"),
                            thumbnails = YouTubeThumbnails(default = YouTubeThumbnail("https://yt.example/sub.jpg"))
                        )
                    )
                )
            )
        }

        override suspend fun getPlaylistItems(
            bearerToken: String?,
            apiKey: String?,
            part: String,
            playlistId: String,
            maxResults: Int
        ): YouTubePlaylistItemListResponse {
            val videoId = if (playlistId == "UU_TEST_123") "YT_OWN_VID_1" else "YT_SUB_VID_1"
            val chId = if (playlistId == "UU_TEST_123") "UC_TEST_123" else "UC_EXTERNAL_999"
            val chTitle = if (playlistId == "UU_TEST_123") "Dei Creator YT" else "Canal Internacional YT"
            val title = if (playlistId == "UU_TEST_123") "Mi Video Propio YT" else "Nuevo Video Suscripción YT"
            return YouTubePlaylistItemListResponse(
                items = listOf(
                    YouTubePlaylistItem(
                        id = "pl_$videoId",
                        snippet = YouTubePlaylistSnippet(
                            title = title,
                            channelId = chId,
                            channelTitle = chTitle,
                            thumbnails = YouTubeThumbnails(high = YouTubeThumbnail("https://yt.example/$videoId.jpg"))
                        ),
                        contentDetails = YouTubePlaylistItemContentDetails(videoId = videoId)
                    )
                )
            )
        }

        override suspend fun getVideosByIds(
            bearerToken: String?,
            apiKey: String?,
            part: String,
            videoIds: String
        ): YouTubeVideoListResponse {
            val ids = videoIds.split(",").filter { it.isNotBlank() }
            return YouTubeVideoListResponse(
                items = ids.map { vid ->
                    val isOwn = vid == "YT_OWN_VID_1"
                    YouTubeVideoItem(
                        id = vid,
                        snippet = YouTubeSnippet(
                            title = if (isOwn) "Mi Video Propio YT" else "Nuevo Video Suscripción YT",
                            channelId = if (isOwn) "UC_TEST_123" else "UC_EXTERNAL_999",
                            channelTitle = if (isOwn) "Dei Creator YT" else "Canal Internacional YT",
                            thumbnails = YouTubeThumbnails(high = YouTubeThumbnail("https://yt.example/$vid.jpg"))
                        ),
                        statistics = YouTubeStatistics(viewCount = "2500", likeCount = "300", commentCount = "18")
                    )
                }
            )
        }

        override suspend fun getPopularVideos(
            apiKey: String,
            part: String,
            chart: String,
            regionCode: String,
            maxResults: Int
        ): YouTubeVideoListResponse = YouTubeVideoListResponse()

        override suspend fun searchVideos(
            apiKey: String,
            part: String,
            query: String,
            type: String,
            maxResults: Int
        ): YouTubeSearchListResponse = YouTubeSearchListResponse()
    }

    private class FakeYouTubeOEmbed : YouTubeOEmbedService {
        override suspend fun getVideoInfo(videoUrl: String, format: String): YouTubeOEmbedResponse {
            return YouTubeOEmbedResponse(
                title = "Documental Planeta Tierra",
                authorName = "Canal Ciencia",
                authorUrl = "https://www.youtube.com/@ciencia",
                thumbnailUrl = "https://img.youtube.com/vi/abc/hqdefault.jpg"
            )
        }
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, DeiPlayDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prefsRepo = UserPreferencesRepository(context)
        authRepo = AuthRepository(context, db.userDao(), prefsRepo)
        channelVideoRepo = ChannelAndVideoRepository(
            context = context,
            channelDao = db.channelDao(),
            videoDao = db.videoDao(),
            socialDao = db.socialDao(),
            libraryDao = db.libraryDao(),
            peerTubeApi = FakePeerTubeApi()
        )
        socialLibraryRepo = SocialAndLibraryRepository(
            channelDao = db.channelDao(),
            videoDao = db.videoDao(),
            socialDao = db.socialDao(),
            libraryDao = db.libraryDao(),
            liveDao = db.liveDao()
        )
        ytRepo = YouTubeConnectRepository(
            youTubeApi = FakeYouTubeApi(),
            youTubeOEmbed = FakeYouTubeOEmbed(),
            userDao = db.userDao(),
            socialDao = db.socialDao(),
            prefsRepo = prefsRepo
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `auth lifecycle register login recovery update parental block and delete`() = runTest {
        val regResult = authRepo.registerWithEmail(
            email = "creator@deiplay.app",
            username = "CreatorOne",
            displayName = "Creator One",
            password = "secretPassword1"
        )
        assertTrue(regResult.isSuccess)
        val user = regResult.getOrThrow()
        assertEquals("creatorone", user.username)

        // Duplicate email rejected
        val dupEmail = authRepo.registerWithEmail("creator@deiplay.app", "other", "Other", "123456")
        assertTrue(dupEmail.isFailure)

        // Wrong password rejected
        val badLogin = authRepo.loginWithEmailOrUsername("creatorone", "wrongpass")
        assertTrue(badLogin.isFailure)

        // Login with username succeeds
        val okLogin = authRepo.loginWithEmailOrUsername("@creatorone", "secretPassword1")
        assertTrue(okLogin.isSuccess)

        // Recover password & verify new password works
        val rec = authRepo.recoverPassword("creator@deiplay.app", "newPass99")
        assertTrue(rec.isSuccess)
        assertTrue(authRepo.loginWithEmailOrUsername("creator@deiplay.app", "newPass99").isSuccess)

        // Edit profile, parental control, block user
        val updated = authRepo.updateProfile(user.id, "Creator Pro", "creatorpro", null).getOrThrow()
        assertEquals("Creator Pro", updated.displayName)
        assertEquals("creatorpro", updated.username)

        authRepo.setParentalControl(user.id, true)
        authRepo.toggleBlockUser(user.id, "spam_user_1")
        val reloaded = db.userDao().getUserById(user.id)!!
        assertTrue(reloaded.parentalControlEnabled)
        assertTrue(reloaded.blockedUserIds.contains("spam_user_1"))

        // Sign out and delete account
        authRepo.signOut()
        assertNull(prefsRepo.prefsFlow.first().currentUserId)
        authRepo.deleteAccount(user.id)
        assertNull(db.userDao().getUserById(user.id))
    }

    @Test
    fun `channel creation upload subscriptions counters likes comments and notifications`() = runTest {
        val creator = authRepo.registerWithEmail("c@dei.app", "creador1", "Creador", "123456").getOrThrow()
        val viewer = authRepo.registerWithEmail("v@dei.app", "viewer1", "Viewer", "123456").getOrThrow()

        // Create channel
        val channel = channelVideoRepo.createOrUpdateChannel(
            ownerUserId = creator.id,
            existingChannelId = null,
            name = "DeiPlay Studio Official",
            handle = "deistudio",
            description = "Canal mundial de prueba",
            country = "México",
            socialLinks = "https://deiplay.app",
            avatarUri = null,
            bannerUri = null
        ).getOrThrow()
        assertEquals(0L, channel.subscriberCount)

        // Viewer subscribes -> subscriberCount +1 & notification sent to creator
        val isSub = socialLibraryRepo.toggleSubscribe(viewer, channel.id, channel.name, null, true)
        assertTrue(isSub)
        assertEquals(1L, db.channelDao().getChannelById(channel.id)!!.subscriberCount)
        val creatorNotifs = socialLibraryRepo.observeNotifications(creator.id).first()
        assertTrue(creatorNotifs.any { it.type == "NEW_SUBSCRIBER" })

        // Create local dummy mp4 file and publish long video + Short
        val dummyVideo = File(context.cacheDir, "sample_upload.mp4").apply { writeText("mp4-content") }
        val videoUri = Uri.fromFile(dummyVideo)

        val published = channelVideoRepo.publishVideo(
            ownerUserId = creator.id,
            channel = channel,
            title = "Mi primer documental",
            description = "Grabado en 4K",
            videoSourceUri = videoUri,
            thumbnailSourceUri = null,
            isShort = false,
            isPost = false,
            category = "Educación",
            language = "Español",
            country = "México",
            tags = "documental, 4k",
            subtitlesText = "00:00 Hola mundo",
            privacy = "PUBLIC",
            allowDownload = true,
            onUploadProgress = {}
        ).getOrThrow()

        val publishedShort = channelVideoRepo.publishVideo(
            ownerUserId = creator.id,
            channel = channel,
            title = "Short rápido",
            description = "Vertical",
            videoSourceUri = videoUri,
            thumbnailSourceUri = null,
            isShort = true,
            isPost = false,
            category = "Entretenimiento",
            language = "Español",
            country = "México",
            tags = "short",
            subtitlesText = "",
            privacy = "PUBLIC",
            allowDownload = true,
            onUploadProgress = {}
        ).getOrThrow()
        assertTrue(publishedShort.isShort)

        // Follower receives NEW_VIDEO and NEW_SHORT notifications
        val viewerNotifs = socialLibraryRepo.observeNotifications(viewer.id).first()
        assertTrue(viewerNotifs.any { it.type == "NEW_VIDEO" })
        assertTrue(viewerNotifs.any { it.type == "NEW_SHORT" })
        assertEquals(2, db.channelDao().getChannelById(channel.id)!!.videoCount)

        // Like / Dislike without duplicates
        socialLibraryRepo.toggleVideoReaction(viewer.id, published, "LIKE")
        assertEquals(1L, db.videoDao().getVideoById(published.id)!!.likesCount)
        assertEquals(0L, db.videoDao().getVideoById(published.id)!!.dislikesCount)

        // Switch Like -> Dislike
        socialLibraryRepo.toggleVideoReaction(viewer.id, published, "DISLIKE")
        assertEquals(0L, db.videoDao().getVideoById(published.id)!!.likesCount)
        assertEquals(1L, db.videoDao().getVideoById(published.id)!!.dislikesCount)

        // Toggle Dislike off
        socialLibraryRepo.toggleVideoReaction(viewer.id, published, "DISLIKE")
        assertEquals(0L, db.videoDao().getVideoById(published.id)!!.dislikesCount)

        // Comments + Reply + Like + Pin + Delete
        socialLibraryRepo.addComment(viewer, published, "Excelente video!")
        val comments = socialLibraryRepo.observeComments(published.id).first()
        assertEquals(1, comments.size)
        val rootComment = comments.first()

        socialLibraryRepo.addComment(creator, published, "¡Muchas gracias!", parentCommentId = rootComment.id)
        val viewerNotifsAfterReply = socialLibraryRepo.observeNotifications(viewer.id).first()
        assertTrue(viewerNotifsAfterReply.any { it.type == "COMMENT_REPLY" })

        socialLibraryRepo.toggleCommentLike(creator.id, rootComment.id)
        assertEquals(1L, socialLibraryRepo.observeComments(published.id).first().first { it.id == rootComment.id }.likesCount)

        socialLibraryRepo.pinComment(published, rootComment, creator.id)
        assertTrue(socialLibraryRepo.observeComments(published.id).first().first { it.id == rootComment.id }.isPinned)

        // Unsubscribe reduces count back to 0
        val stillSub = socialLibraryRepo.toggleSubscribe(viewer, channel.id, channel.name, null, true)
        assertFalse(stillSub)
        assertEquals(0L, db.channelDao().getChannelById(channel.id)!!.subscriberCount)

        // Delete video updates count
        channelVideoRepo.deleteOwnedVideo(publishedShort.id, creator.id)
        assertEquals(1, db.channelDao().getChannelById(channel.id)!!.videoCount)
    }

    @Test
    fun `playlists history watch later favorites downloads and live streaming`() = runTest {
        val user = authRepo.registerWithEmail("lib@dei.app", "libuser", "Lib User", "123456").getOrThrow()
        val channel = channelVideoRepo.createOrUpdateChannel(
            ownerUserId = user.id,
            existingChannelId = null,
            name = "Lib Channel",
            handle = "libch",
            description = "",
            country = "España",
            socialLinks = "",
            avatarUri = null,
            bannerUri = null
        ).getOrThrow()

        val localFile = File(context.cacheDir, "offline_src.mp4").apply { writeText("offline-bytes") }
        val v1 = channelVideoRepo.publishVideo(
            ownerUserId = user.id, channel = channel, title = "Video Uno", description = "",
            videoSourceUri = Uri.fromFile(localFile), thumbnailSourceUri = null,
            isShort = false, isPost = false, category = "Música", language = "Español", country = "España",
            tags = "", subtitlesText = "", privacy = "PUBLIC", allowDownload = true, onUploadProgress = {}
        ).getOrThrow()
        val v2 = channelVideoRepo.publishVideo(
            ownerUserId = user.id, channel = channel, title = "Video Dos", description = "",
            videoSourceUri = Uri.fromFile(localFile), thumbnailSourceUri = null,
            isShort = false, isPost = false, category = "Tecnología", language = "Español", country = "España",
            tags = "", subtitlesText = "", privacy = "PUBLIC", allowDownload = true, onUploadProgress = {}
        ).getOrThrow()

        // History & Clear
        socialLibraryRepo.recordWatchHistory(user.id, v1)
        assertEquals(1, socialLibraryRepo.observeHistory(user.id).first().size)
        socialLibraryRepo.clearHistory(user.id)
        assertEquals(0, socialLibraryRepo.observeHistory(user.id).first().size)

        // Watch Later & Favorites
        socialLibraryRepo.toggleWatchLater(user.id, v1, false)
        assertTrue(socialLibraryRepo.observeIsWatchLater(user.id, v1.id).first())
        socialLibraryRepo.toggleSavedFavorite(user.id, v1, false)
        assertTrue(socialLibraryRepo.observeIsSavedFavorite(user.id, v1.id).first())

        // Playlist create + add + reorder up/down
        val pl = socialLibraryRepo.createPlaylist(user.id, "Favoritos Rock", "Lista", isPublic = true).getOrThrow()
        socialLibraryRepo.addVideoToPlaylist(pl.id, v1)
        socialLibraryRepo.addVideoToPlaylist(pl.id, v2)
        val initialOrder = socialLibraryRepo.observePlaylistItems(pl.id).first()
        assertEquals(listOf(v1.id, v2.id), initialOrder.map { it.videoId })

        socialLibraryRepo.movePlaylistItem(pl.id, initialOrder[1].id, moveUp = true)
        val reordered = socialLibraryRepo.observePlaylistItems(pl.id).first()
        assertEquals(listOf(v2.id, v1.id), reordered.map { it.videoId })

        // Offline download & deletion
        val dl = channelVideoRepo.downloadVideoOffline(user.id, v1, "Original", v1.mediaUri) {}.getOrThrow()
        assertTrue(File(dl.localFilePath).exists())
        assertEquals(1, socialLibraryRepo.observeDownloads(user.id).first().size)
        channelVideoRepo.deleteOfflineDownload(user.id, dl)
        assertEquals(0, socialLibraryRepo.observeDownloads(user.id).first().size)

        // Live session + chat + moderation + end
        val live = socialLibraryRepo.startLiveSession(user.id, channel.id, channel.name, "Directo Nocturno", "Q&A").getOrThrow()
        assertTrue(socialLibraryRepo.observeActiveLives().first().any { it.id == live.id })
        socialLibraryRepo.sendLiveChatMessage(live.id, user, "Hola a todos!")
        val chat = socialLibraryRepo.observeLiveChat(live.id).first()
        assertEquals(1, chat.size)
        socialLibraryRepo.deleteLiveChatMessage(chat.first().id)
        assertEquals(0, socialLibraryRepo.observeLiveChat(live.id).first().size)
        socialLibraryRepo.endLiveSession(live.id)
        assertFalse(socialLibraryRepo.observeActiveLives().first().any { it.id == live.id })
    }

    @Test
    fun `world discovery youtube oauth separation and placeholder ai recommendations`() = runTest {
        val user = authRepo.registerWithEmail("yt@dei.app", "ytuser", "YT User", "123456").getOrThrow()

        // PeerTube world fetch, creator channel video sync & stream quality resolution
        val worldVideos = channelVideoRepo.fetchWorldVideos(categoryLabel = "Tendencias").getOrThrow()
        assertEquals(2, worldVideos.size)
        assertEquals("pt_ch_peertube.example.org|madrid_sessions", worldVideos.first().channelId)
        val channelUploads = channelVideoRepo.fetchPeerTubeChannelVideos(worldVideos.first().channelId).getOrThrow()
        assertEquals(1, channelUploads.size)
        assertEquals("Extra Video from Creator Channel", channelUploads.first().title)

        val streams = channelVideoRepo.resolvePlayableStreams(worldVideos.first())
        assertEquals(listOf("1080p", "720p"), streams.streams.map { it.label })
        assertTrue(streams.downloadPermitted)

        val worldShorts = channelVideoRepo.fetchWorldShorts().getOrThrow()
        assertEquals(1, worldShorts.size)
        assertTrue(worldShorts.first().isShort)
        assertEquals("60s Tech Tip", worldShorts.first().title)

        // YouTube OAuth sync separates external YouTube subscriptions & imports own + subscribed creator uploads
        val syncResult = ytRepo.syncWithOAuthToken(user.id, "mock_oauth_token").getOrThrow()
        assertEquals("Dei Creator YT", syncResult.channelTitle)
        assertEquals(4321L, syncResult.subscribersCount)
        assertEquals(1, syncResult.importedSubscriptionsCount)
        assertTrue(syncResult.uploadedVideos.any { it.id == "yt_YT_OWN_VID_1" })
        assertTrue(syncResult.uploadedVideos.any { it.id == "yt_YT_SUB_VID_1" })

        val fetchedCreatorsUploads = ytRepo.fetchCreatorsUploads(
            bearerToken = "mock_oauth_token",
            apiKey = null,
            rawChannelIds = listOf("yt_UC_EXTERNAL_999")
        )
        assertEquals(1, fetchedCreatorsUploads.size)
        assertEquals("Nuevo Video Suscripción YT", fetchedCreatorsUploads.first().title)

        assertEquals(0, socialLibraryRepo.observeDeiPlaySubscriptions(user.id).first().size)
        assertEquals(1, socialLibraryRepo.observeYouTubeSubscriptions(user.id).first().size)

        // Disconnect YouTube
        ytRepo.disconnectYouTube(user.id)
        assertFalse(db.userDao().getUserById(user.id)!!.youtubeConnected)

        // Zero-key YouTube oEmbed lookup
        val oembedVideo = ytRepo.inspectYouTubeUrlViaOEmbed("https://www.youtube.com/watch?v=dQw4w9WgXcQ").getOrThrow()
        assertEquals("YOUTUBE", oembedVideo.sourcePlatform)
        assertEquals("Documental Planeta Tierra", oembedVideo.title)

        // Placeholder AI recommendation notice & ranking
        val aiRepo = PlaceholderAiRecommendationRepository()
        val rec = aiRepo.generateRecommendations(emptyList(), worldVideos)
        assertTrue(rec.isPlaceholder)
        assertTrue(rec.notice.contains("Firebase AI Logic"))
        assertNotNull(rec.rankedVideoIds.firstOrNull())
    }
}
