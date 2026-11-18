package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class YouTubeOEmbedResponse(
    val title: String = "",
    @Json(name = "author_name") val authorName: String = "",
    @Json(name = "author_url") val authorUrl: String = "",
    @Json(name = "thumbnail_url") val thumbnailUrl: String = ""
)

interface YouTubeOEmbedService {
    @GET("oembed")
    suspend fun getVideoInfo(
        @Query("url") videoUrl: String,
        @Query("format") format: String = "json"
    ): YouTubeOEmbedResponse
}

@JsonClass(generateAdapter = true)
data class YouTubeChannelListResponse(
    val items: List<YouTubeChannelItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class YouTubeChannelItem(
    val id: String = "",
    val snippet: YouTubeSnippet? = null,
    val statistics: YouTubeStatistics? = null,
    val contentDetails: YouTubeChannelContentDetails? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeChannelContentDetails(
    val relatedPlaylists: YouTubeRelatedPlaylists? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeRelatedPlaylists(
    val uploads: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubePlaylistItemListResponse(
    val items: List<YouTubePlaylistItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class YouTubePlaylistItem(
    val id: String = "",
    val snippet: YouTubePlaylistSnippet? = null,
    val contentDetails: YouTubePlaylistItemContentDetails? = null
)

@JsonClass(generateAdapter = true)
data class YouTubePlaylistSnippet(
    val title: String = "",
    val description: String = "",
    val channelId: String = "",
    val channelTitle: String = "",
    val publishedAt: String = "",
    val thumbnails: YouTubeThumbnails? = null,
    val resourceId: YouTubePlaylistResourceId? = null
)

@JsonClass(generateAdapter = true)
data class YouTubePlaylistResourceId(
    val videoId: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubePlaylistItemContentDetails(
    val videoId: String? = null,
    val videoPublishedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeSubscriptionListResponse(
    val items: List<YouTubeSubscriptionItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class YouTubeSubscriptionItem(
    val id: String = "",
    val snippet: YouTubeSubscriptionSnippet? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeSubscriptionSnippet(
    val title: String = "",
    val description: String = "",
    val resourceId: YouTubeResourceId? = null,
    val thumbnails: YouTubeThumbnails? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeResourceId(
    val channelId: String = ""
)

@JsonClass(generateAdapter = true)
data class YouTubeVideoListResponse(
    val items: List<YouTubeVideoItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class YouTubeVideoItem(
    val id: String = "",
    val snippet: YouTubeSnippet? = null,
    val statistics: YouTubeStatistics? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeSearchListResponse(
    val items: List<YouTubeSearchItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class YouTubeSearchItem(
    val id: YouTubeSearchId? = null,
    val snippet: YouTubeSnippet? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeSearchId(
    val videoId: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeSnippet(
    val title: String = "",
    val description: String = "",
    val channelId: String = "",
    val channelTitle: String = "",
    val publishedAt: String = "",
    val thumbnails: YouTubeThumbnails? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeThumbnails(
    val high: YouTubeThumbnail? = null,
    val medium: YouTubeThumbnail? = null,
    val default: YouTubeThumbnail? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeThumbnail(
    val url: String = ""
)

@JsonClass(generateAdapter = true)
data class YouTubeStatistics(
    val viewCount: String? = null,
    val subscriberCount: String? = null,
    val videoCount: String? = null,
    val likeCount: String? = null,
    val commentCount: String? = null
)

interface YouTubeApiService {
    @GET("channels")
    suspend fun getMyChannel(
        @Header("Authorization") bearerToken: String,
        @Query("part") part: String = "snippet,statistics,contentDetails",
        @Query("mine") mine: Boolean = true
    ): YouTubeChannelListResponse

    @GET("channels")
    suspend fun getChannelsByIds(
        @Header("Authorization") bearerToken: String? = null,
        @Query("key") apiKey: String? = null,
        @Query("part") part: String = "snippet,statistics,contentDetails",
        @Query("id") channelIds: String
    ): YouTubeChannelListResponse

    @GET("subscriptions")
    suspend fun getMySubscriptions(
        @Header("Authorization") bearerToken: String,
        @Query("part") part: String = "snippet",
        @Query("mine") mine: Boolean = true,
        @Query("maxResults") maxResults: Int = 50
    ): YouTubeSubscriptionListResponse

    @GET("playlistItems")
    suspend fun getPlaylistItems(
        @Header("Authorization") bearerToken: String? = null,
        @Query("key") apiKey: String? = null,
        @Query("part") part: String = "snippet,contentDetails",
        @Query("playlistId") playlistId: String,
        @Query("maxResults") maxResults: Int = 15
    ): YouTubePlaylistItemListResponse

    @GET("videos")
    suspend fun getVideosByIds(
        @Header("Authorization") bearerToken: String? = null,
        @Query("key") apiKey: String? = null,
        @Query("part") part: String = "snippet,statistics",
        @Query("id") videoIds: String
    ): YouTubeVideoListResponse

    @GET("videos")
    suspend fun getPopularVideos(
        @Query("key") apiKey: String,
        @Query("part") part: String = "snippet,statistics",
        @Query("chart") chart: String = "mostPopular",
        @Query("regionCode") regionCode: String = "US",
        @Query("maxResults") maxResults: Int = 20
    ): YouTubeVideoListResponse

    @GET("search")
    suspend fun searchVideos(
        @Query("key") apiKey: String,
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int = 20
    ): YouTubeSearchListResponse
}
