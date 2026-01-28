package com.regentmediagroup.embertv.ui.screens

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
// REMOVED BAD IMPORT: import androidx.compose.ui.input.key.nativeKeyEvent
import androidx.compose.ui.platform.LocalContext
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
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    videoUrl: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val PREFS_NAME = "EmberPlaybackState"

    // We need to capture the View to send remote clicks to it
    var playerView: PlayerView? by remember { mutableStateOf(null) }

    // --- INTERCEPTOR (For DRM Workaround) ---
    val manifestInterceptor = Interceptor { chain ->
        val request = chain.request()
        val response = chain.proceed(request)
        if (request.url.toString().endsWith(".m3u8")) {
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

    val okHttpClient = remember {
        OkHttpClient.Builder().addInterceptor(manifestInterceptor).build()
    }

    val exoPlayer = remember {
        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("EmberPlayer", "Playback Error: ${error.message}", error)
                    }
                })
            }
    }

    // Load & Resume Logic
    LaunchedEffect(videoUrl) {
        val mediaItem = MediaItem.Builder()
            .setUri(videoUrl)
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .build()

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedPosition = prefs.getLong(videoUrl, 0L)

        exoPlayer.setMediaItem(mediaItem)
        if (savedPosition > 0L) {
            exoPlayer.seekTo(savedPosition)
            Toast.makeText(context, "Resuming playback...", Toast.LENGTH_SHORT).show()
        }
        exoPlayer.prepare()
    }

    fun saveProgress() {
        val currentPos = exoPlayer.currentPosition
        val duration = exoPlayer.duration
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (exoPlayer.playbackState == Player.STATE_ENDED || (duration > 0 && currentPos > duration - 10000)) {
            prefs.edit().remove(videoUrl).apply()
        } else {
            prefs.edit().putLong(videoUrl, currentPos).apply()
        }
    }

    BackHandler {
        saveProgress()
        exoPlayer.release()
        onBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            saveProgress()
            exoPlayer.release()
        }
    }

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

                    // Capture the view reference so we can send keys to it later
                    playerView = this
                }
            },
            update = { view ->
                // Ensure focus remains on the player view
                view.requestFocus()
                playerView = view
            },
            modifier = Modifier
                .fillMaxSize()
                .onKeyEvent { event ->
                    // Route the remote control keys (Play/Pause/Rewind) directly to the Android View
                    if (event.type == KeyEventType.KeyDown) {
                        return@onKeyEvent playerView?.dispatchKeyEvent(event.nativeKeyEvent) ?: false
                    }
                    false
                }
        )
    }
}