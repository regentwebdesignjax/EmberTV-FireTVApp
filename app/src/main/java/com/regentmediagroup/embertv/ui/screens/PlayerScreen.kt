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

    // Define the Prefs file name
    val PREFS_NAME = "EmberPlaybackState"

    // 1. Interceptor Workaround (Kept from previous step)
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
                Log.e("EmberInterceptor", "Failed to patch manifest", e)
                return@Interceptor response
            }
        }
        response
    }

    val okHttpClient = remember {
        OkHttpClient.Builder()
            .addInterceptor(manifestInterceptor)
            .build()
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

    // 2. Logic to Load and Play Media
    LaunchedEffect(videoUrl) {
        val mediaItem = MediaItem.Builder()
            .setUri(videoUrl)
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .build()

        // --- RESUME LOGIC STARTS HERE ---
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedPosition = prefs.getLong(videoUrl, 0L) // Retrieve saved time using URL as key

        exoPlayer.setMediaItem(mediaItem)

        // If we have a saved position (and it's not the very start), seek to it
        if (savedPosition > 0L) {
            exoPlayer.seekTo(savedPosition)
            Toast.makeText(context, "Resuming playback...", Toast.LENGTH_SHORT).show()
        }
        // --- RESUME LOGIC ENDS HERE ---

        exoPlayer.prepare()
    }

    // Helper function to save progress
    fun saveProgress() {
        val currentPos = exoPlayer.currentPosition
        val duration = exoPlayer.duration
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // If video ended (or is very close to end), reset to 0
        if (exoPlayer.playbackState == Player.STATE_ENDED || (duration > 0 && currentPos > duration - 10000)) {
            prefs.edit().remove(videoUrl).apply()
        } else {
            // Otherwise, save the exact milliseconds
            prefs.edit().putLong(videoUrl, currentPos).apply()
        }
    }

    // 3. Handle Back Button (Save before exit)
    BackHandler {
        saveProgress() // <--- Save
        exoPlayer.release()
        onBack()
    }

    // 4. Handle Lifecycle/App Closing (Save before exit)
    DisposableEffect(Unit) {
        onDispose {
            saveProgress() // <--- Save
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
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}