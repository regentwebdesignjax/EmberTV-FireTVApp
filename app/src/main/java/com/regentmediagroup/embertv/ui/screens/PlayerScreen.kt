package com.regentmediagroup.embertv.ui.screens

import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.regentmediagroup.embertv.data.EmberApiClient
import com.regentmediagroup.embertv.data.Rental
import com.regentmediagroup.embertv.ui.theme.EmberTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody

/** How often to save the resume point while playing. */
private const val HEARTBEAT_MILLIS = 30_000L

/**
 * Plays a rental. Asks the server for a fresh signed stream URL (it checks the
 * rental), then reports the position every 30 seconds, on pause and on close
 * so the viewer can resume on any device.
 *
 * [onClose] receives the new resume point in seconds (0 when the film was
 * finished), or null when playback never started and nothing changed.
 */
@kotlin.OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerScreen(
    rental: Rental,
    startAtSeconds: Long?,
    onClose: (resumeSeconds: Long?) -> Unit
) {
    var url by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(rental.film.id) {
        try {
            url = EmberApiClient.startPlayback(rental.film.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "This film can't be played right now."
        }
    }

    val playbackUrl = url
    if (playbackUrl != null) {
        HlsPlayer(
            url = playbackUrl,
            filmId = rental.film.id,
            startAtSeconds = startPosition(startAtSeconds, rental.film.durationMinutes),
            onClose = onClose
        )
    } else {
        BackHandler { onClose(null) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val message = error
            if (message == null) {
                Text("Loading...", style = EmberTheme.bodyFont(22), color = EmberTheme.TextSecondary)
            } else {
                MessageBox(title = message)
            }
        }
    }
}

/** The resume point, unless it is too early to matter or within the last
 *  minute (then the film was essentially finished). */
private fun startPosition(seconds: Long?, durationMinutes: Int?): Long? {
    if (seconds == null || seconds <= 10) return null
    if (durationMinutes != null && seconds > durationMinutes * 60L - 60) return null
    return seconds
}

@kotlin.OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MessageBox(title: String) {
    Column(
        modifier = Modifier
            .background(EmberTheme.Background, RoundedCornerShape(16.dp))
            .padding(horizontal = 40.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = EmberTheme.bodySemibold(22), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Press Back to return.",
            style = EmberTheme.bodyFont(16),
            color = EmberTheme.TextSecondary
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun HlsPlayer(
    url: String,
    filmId: String,
    startAtSeconds: Long?,
    onClose: (resumeSeconds: Long?) -> Unit
) {
    val context = LocalContext.current

    // Captured so remote-control keys can be forwarded to it.
    var playerView: PlayerView? by remember { mutableStateOf(null) }
    var finished by remember { mutableStateOf(false) }
    var closed by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }

    // Adds the IV some players need for AES-128 HLS keys (Bunny MediaCage).
    val manifestInterceptor = remember {
        Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            if (request.url.encodedPath.endsWith(".m3u8")) {
                try {
                    val originalBody = response.body?.string() ?: ""
                    val fixedBody = originalBody.replace(
                        Regex("(#EXT-X-KEY:METHOD=AES-128,URI=\"[^\"]+\")(?!.*IV=)"),
                        "$1,IV=0x00000000000000000000000000000000"
                    )
                    val newBody = fixedBody.toResponseBody("application/vnd.apple.mpegurl".toMediaType())
                    return@Interceptor response.newBuilder().body(newBody).build()
                } catch (e: Exception) {
                    return@Interceptor response
                }
            }
            response
        }
    }

    val exoPlayer = remember {
        val okHttpClient = OkHttpClient.Builder().addInterceptor(manifestInterceptor).build()
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(OkHttpDataSource.Factory(okHttpClient)))
            .build()
            .apply { playWhenReady = true }
    }

    fun positionSeconds(): Long = if (finished) 0L else (exoPlayer.currentPosition / 1000L).coerceAtLeast(0L)

    fun close() {
        if (closed) return
        closed = true
        val position = positionSeconds()
        EmberApiClient.reportProgress(filmId, position)
        onClose(position)
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    finished = true
                    EmberApiClient.reportProgress(filmId, 0L)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // Paused by the viewer (not buffering): save the spot now.
                if (!isPlaying && !finished && exoPlayer.playbackState == Player.STATE_READY) {
                    EmberApiClient.reportProgress(filmId, positionSeconds())
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("EmberPlayer", "Playback error: ${error.errorCodeName}", error)
                playbackError = "Playback stopped. Please try again."
            }
        }
        exoPlayer.addListener(listener)

        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .build()
        if (startAtSeconds != null) {
            exoPlayer.setMediaItem(mediaItem, startAtSeconds * 1000L)
        } else {
            exoPlayer.setMediaItem(mediaItem)
        }
        exoPlayer.prepare()

        onDispose {
            // Leaving without Back (e.g. the app was closed): still save the spot.
            if (!closed) {
                closed = true
                EmberApiClient.reportProgress(filmId, positionSeconds())
            }
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(HEARTBEAT_MILLIS)
            if (exoPlayer.isPlaying) EmberApiClient.reportProgress(filmId, positionSeconds())
        }
    }

    BackHandler { close() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = true
                    keepScreenOn = true
                    isFocusable = true
                    playerView = this
                }
            },
            update = { view ->
                view.requestFocus()
                playerView = view
            },
            modifier = Modifier
                .fillMaxSize()
                .onKeyEvent { event ->
                    // Route remote keys (play/pause/seek) straight to the player view.
                    if (event.type == KeyEventType.KeyDown) {
                        return@onKeyEvent playerView?.dispatchKeyEvent(event.nativeKeyEvent) ?: false
                    }
                    false
                }
        )

        val message = playbackError
        if (message != null) {
            Box(modifier = Modifier.align(Alignment.Center)) {
                MessageBox(title = message)
            }
        }
    }
}
