package com.example.player

import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.local.VideoEntity
import com.example.data.repository.StreamQualityOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class PlayerDisplayMode {
    NONE,
    FULL,
    FLOATING
}

data class PlayerUiState(
    val activeVideo: VideoEntity? = null,
    val displayMode: PlayerDisplayMode = PlayerDisplayMode.NONE,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val subtitlesEnabled: Boolean = false,
    val isFullscreen: Boolean = false,
    val isInSystemPip: Boolean = false,
    val autoPlayNext: Boolean = true,
    val qualityOptions: List<StreamQualityOption> = emptyList(),
    val selectedQualityLabel: String = "Auto",
    val downloadPermitted: Boolean = false,
    val errorMessage: String? = null
)

class DeiPlayPlayerManager(
    context: Context,
    private val scope: CoroutineScope,
    private val onVideoCompleted: () -> Unit = {}
) {
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build()

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var positionTickerJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val buffering = playbackState == Player.STATE_BUFFERING
            _state.update {
                it.copy(
                    isBuffering = buffering,
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                )
            }
            if (playbackState == Player.STATE_ENDED && _state.value.autoPlayNext) {
                onVideoCompleted()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.update {
                it.copy(
                    isBuffering = false,
                    isPlaying = false,
                    errorMessage = error.localizedMessage ?: "No se pudo reproducir este flujo de video."
                )
            }
        }
    }

    init {
        exoPlayer.addListener(listener)
        startPositionTicker()
    }

    private fun startPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                if (_state.value.activeVideo != null) {
                    _state.update {
                        it.copy(
                            currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
                            durationMs = exoPlayer.duration.coerceAtLeast(0L)
                        )
                    }
                }
                delay(350L)
            }
        }
    }

    fun playVideo(
        video: VideoEntity,
        streams: List<StreamQualityOption>,
        downloadPermitted: Boolean,
        overrideStreamUrl: String? = null
    ) {
        val chosen = streams.firstOrNull()
        val playableUrl = overrideStreamUrl ?: chosen?.url ?: video.mediaUri

        _state.update {
            it.copy(
                activeVideo = video,
                displayMode = PlayerDisplayMode.FULL,
                qualityOptions = streams,
                selectedQualityLabel = chosen?.label ?: "Original",
                downloadPermitted = downloadPermitted,
                errorMessage = null,
                isBuffering = true
            )
        }

        if (playableUrl.isBlank() || playableUrl.contains("youtube.com/watch") || playableUrl.contains("youtu.be/")) {
            // External YouTube web links cannot be raw-streamed directly inside ExoPlayer without YouTube Player API/intent
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            _state.update {
                it.copy(
                    isBuffering = false,
                    isPlaying = false,
                    errorMessage = if (video.isPost) null else "Este enlace pertenece a YouTube externo. Ábrelo con el botón oficial de YouTube o reproduce videos de DeiPlay / PeerTube."
                )
            }
            return
        }

        exoPlayer.setMediaItem(MediaItem.fromUri(playableUrl))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    fun selectQuality(option: StreamQualityOption) {
        val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
        val wasPlaying = exoPlayer.isPlaying
        _state.update { it.copy(selectedQualityLabel = option.label, errorMessage = null, isBuffering = true) }
        exoPlayer.setMediaItem(MediaItem.fromUri(option.url))
        exoPlayer.prepare()
        exoPlayer.seekTo(pos)
        exoPlayer.playWhenReady = wasPlaying
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs.coerceAtLeast(0L))
        _state.update { it.copy(currentPositionMs = positionMs.coerceAtLeast(0L)) }
    }

    fun seekBy(deltaMs: Long) {
        val target = (exoPlayer.currentPosition + deltaMs).coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L))
        seekTo(target)
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer.setPlaybackSpeed(speed)
        _state.update { it.copy(playbackSpeed = speed) }
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        exoPlayer.volume = clamped
        _state.update { it.copy(volume = clamped, isMuted = clamped == 0f) }
    }

    fun toggleMute() {
        val currentlyMuted = _state.value.isMuted
        if (currentlyMuted) {
            val restored = _state.value.volume.takeIf { it > 0.05f } ?: 1f
            exoPlayer.volume = restored
            _state.update { it.copy(isMuted = false, volume = restored) }
        } else {
            exoPlayer.volume = 0f
            _state.update { it.copy(isMuted = true) }
        }
    }

    fun toggleSubtitles() {
        _state.update { it.copy(subtitlesEnabled = !it.subtitlesEnabled) }
    }

    fun setAutoPlayNext(enabled: Boolean) {
        _state.update { it.copy(autoPlayNext = enabled) }
    }

    fun setFullscreen(fullscreen: Boolean) {
        _state.update { it.copy(isFullscreen = fullscreen) }
    }

    fun minimizeToFloating() {
        if (_state.value.activeVideo != null) {
            _state.update { it.copy(displayMode = PlayerDisplayMode.FLOATING, isFullscreen = false) }
        }
    }

    fun expandFromFloating() {
        if (_state.value.activeVideo != null) {
            _state.update { it.copy(displayMode = PlayerDisplayMode.FULL) }
        }
    }

    fun closePlayer() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        _state.update {
            PlayerUiState(autoPlayNext = it.autoPlayNext)
        }
    }

    fun setSystemPipActive(inPip: Boolean) {
        _state.update { it.copy(isInSystemPip = inPip) }
    }

    fun enterSystemPip(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val activity = context.findActivityOrNull() ?: return false
        val builder = PictureInPictureParams.Builder()
        val videoSize = exoPlayer.videoSize
        if (videoSize != VideoSize.UNKNOWN && videoSize.width > 0 && videoSize.height > 0) {
            val rawRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
            val rational = when {
                rawRatio > 2.35f -> Rational(235, 100)
                rawRatio < 0.43f -> Rational(100, 235)
                else -> Rational(videoSize.width, videoSize.height)
            }
            builder.setAspectRatio(rational)
        } else {
            builder.setAspectRatio(Rational(16, 9))
        }
        return runCatching {
            activity.enterPictureInPictureMode(builder.build())
        }.getOrDefault(false)
    }

    fun release() {
        positionTickerJob?.cancel()
        exoPlayer.removeListener(listener)
        exoPlayer.release()
    }
}

tailrec fun Context.findActivityOrNull(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivityOrNull()
    else -> null
}
