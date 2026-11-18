package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class PeerTubeSearchResponse(
    val total: Int = 0,
    val data: List<PeerTubeVideoSummary> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PeerTubeVideoSummary(
    val id: Long = 0L,
    val uuid: String = "",
    val shortUUID: String = "",
    val name: String = "",
    val description: String? = null,
    val duration: Long = 0L,
    val views: Long = 0L,
    val likes: Long = 0L,
    val dislikes: Long = 0L,
    val isLive: Boolean = false,
    val thumbnailUrl: String? = null,
    val previewUrl: String? = null,
    val url: String = "",
    val embedUrl: String? = null,
    val publishedAt: String? = null,
    val category: PeerTubeNamedItem? = null,
    val language: PeerTubeNamedItem? = null,
    val channel: PeerTubeChannel? = null,
    val account: PeerTubeAccount? = null,
    val tags: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PeerTubeNamedItem(
    val id: String? = null,
    val label: String? = null
)

@JsonClass(generateAdapter = true)
data class PeerTubeChannel(
    val id: Long = 0L,
    val name: String = "",
    val displayName: String = "",
    val url: String = "",
    val host: String = "",
    val avatar: PeerTubeAvatar? = null,
    val avatars: List<PeerTubeAvatar> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PeerTubeAccount(
    val id: Long = 0L,
    val name: String = "",
    val displayName: String = "",
    val host: String = "",
    val avatar: PeerTubeAvatar? = null,
    val avatars: List<PeerTubeAvatar> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PeerTubeAvatar(
    val path: String = "",
    val width: Int = 0
)

@JsonClass(generateAdapter = true)
data class PeerTubeVideoDetail(
    val id: Long = 0L,
    val uuid: String = "",
    val name: String = "",
    val description: String? = null,
    val duration: Long = 0L,
    val views: Long = 0L,
    val likes: Long = 0L,
    val dislikes: Long = 0L,
    val isLive: Boolean = false,
    val downloadEnabled: Boolean = false,
    val thumbnailPath: String? = null,
    val previewPath: String? = null,
    val url: String = "",
    val files: List<PeerTubeVideoFile> = emptyList(),
    val streamingPlaylists: List<PeerTubeStreamingPlaylist> = emptyList(),
    val channel: PeerTubeChannelDetail? = null
)

@JsonClass(generateAdapter = true)
data class PeerTubeVideoFile(
    val resolution: PeerTubeResolution? = null,
    val size: Long = 0L,
    val fileUrl: String = "",
    val fileDownloadUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class PeerTubeResolution(
    val id: Int = 0,
    val label: String = ""
)

@JsonClass(generateAdapter = true)
data class PeerTubeStreamingPlaylist(
    val id: Long = 0L,
    val type: Int = 0,
    val playlistUrl: String = "",
    val files: List<PeerTubeVideoFile> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PeerTubeChannelDetail(
    val id: Long = 0L,
    val name: String = "",
    val displayName: String = "",
    val description: String? = null,
    val followersCount: Long = 0L,
    val host: String = "",
    val url: String = ""
)

interface PeerTubeApiService {
    @GET("search/videos")
    suspend fun searchVideos(
        @Query("search") search: String? = null,
        @Query("count") count: Int = 20,
        @Query("sort") sort: String = "-views",
        @Query("nsfw") nsfw: String = "false",
        @Query("isLive") isLive: Boolean? = null,
        @Query("categoryOneOf") categoryId: Int? = null,
        @Query("languageOneOf") languageCode: String? = null
    ): PeerTubeSearchResponse

    @GET
    suspend fun getChannelVideos(@Url fullApiUrl: String): PeerTubeSearchResponse

    @GET
    suspend fun getVideoDetails(@Url fullApiUrl: String): PeerTubeVideoDetail

    @Streaming
    @GET
    suspend fun downloadFile(@Url fileUrl: String): Response<ResponseBody>
}

object NetworkModule {
    val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val peerTubeService: PeerTubeApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://search.joinpeertube.org/api/v1/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PeerTubeApiService::class.java)
    }

    val youTubeOEmbedService: YouTubeOEmbedService by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.youtube.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(YouTubeOEmbedService::class.java)
    }

    val youTubeApiService: YouTubeApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/youtube/v3/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(YouTubeApiService::class.java)
    }
}
