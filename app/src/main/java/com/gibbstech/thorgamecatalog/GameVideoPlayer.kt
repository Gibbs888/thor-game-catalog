package com.gibbstech.thorgamecatalog

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.delay

@Composable
fun GameVideoPlayer(game: Game, modifier: Modifier = Modifier) {
    var directVideoFailed by remember(game.id) { mutableStateOf(false) }
    when {
        !game.previewVideoUrl.isNullOrBlank() && !directVideoFailed -> DirectVideoPlayer(
            url = game.previewVideoUrl,
            modifier = modifier,
            onPlaybackError = {
                if (!game.youtubeVideoId.isNullOrBlank()) directVideoFailed = true
            },
        )

        !game.youtubeVideoId.isNullOrBlank() -> YouTubeVideoPlayer(
            videoId = game.youtubeVideoId,
            modifier = modifier,
        )
    }
}

@Composable
private fun DirectVideoPlayer(
    url: String,
    modifier: Modifier,
    onPlaybackError: () -> Unit,
) {
    val context = LocalContext.current
    var isBuffering by remember(url) { mutableStateOf(true) }
    var errorMessage by remember(url) { mutableStateOf<String?>(null) }
    val player = remember(url) {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("ThorGameCatalog/1.4.3")
            .setAllowCrossProtocolRedirects(true)
        val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(url))
                prepare()
                playWhenReady = true
            }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING ||
                    playbackState == Player.STATE_IDLE
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                errorMessage = videoPlaybackErrorMessage(error)
                onPlaybackError()
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    this.player = player
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    setBackgroundColor(android.graphics.Color.BLACK)
                    keepScreenOn = true
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize(),
        )

        if (isBuffering && errorMessage == null) {
            CircularProgressIndicator()
        }

        errorMessage?.let { message ->
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = message,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        errorMessage = null
                        isBuffering = true
                        player.prepare()
                        player.play()
                    },
                ) {
                    Text("Skúsiť znova")
                }
            }
        }
    }
}

private fun videoPlaybackErrorMessage(error: PlaybackException): String = when (error.errorCode) {
    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
        "ScreenScraper odmietol stiahnutie videa. Skús ho načítať znova."
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
        "Video sa nepodarilo načítať z internetu."
    PlaybackException.ERROR_CODE_DECODING_FAILED,
    PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ->
        "Formát tohto videa zariadenie nedokáže prehrať."
    else -> "Video sa nepodarilo prehrať (${error.errorCodeName})."
}

@Composable
private fun YouTubeVideoPlayer(videoId: String, modifier: Modifier) {
    val context = LocalContext.current
    var isReady by remember(videoId) { mutableStateOf(false) }
    var errorMessage by remember(videoId) { mutableStateOf<String?>(null) }
    val playerView = remember(videoId) {
        YouTubePlayerView(context).apply {
            enableAutomaticInitialization = false
            setBackgroundColor(android.graphics.Color.BLACK)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
    }

    DisposableEffect(playerView, videoId) {
        val listener = object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                isReady = true
                errorMessage = null
                youTubePlayer.loadVideo(videoId, 0f)
            }

            override fun onError(
                youTubePlayer: YouTubePlayer,
                error: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerError,
            ) {
                errorMessage = "YouTube video sa nepodarilo načítať ($error)."
            }
        }
        val options = IFramePlayerOptions.Builder(context)
            .controls(1)
            .fullscreen(1)
            .rel(0)
            .build()
        playerView.initialize(listener, true, options)
        onDispose {
            playerView.release()
        }
    }

    LaunchedEffect(videoId, isReady) {
        if (!isReady) {
            delay(12_000)
            if (!isReady) {
                errorMessage = "YouTube prehrávač sa nenačítal."
            }
        }
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { playerView },
            modifier = Modifier.fillMaxSize(),
        )

        if (!isReady && errorMessage == null) {
            CircularProgressIndicator()
        }

        errorMessage?.let { message ->
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = message,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { openYouTube(context, videoId) }) {
                    Text("Otvoriť v YouTube")
                }
            }
        }
    }
}

private fun openYouTube(context: Context, videoId: String) {
    context.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(youtubeWatchUrl(videoId))),
    )
}

internal fun youtubeWatchUrl(videoId: String): String =
    "https://www.youtube.com/watch?v=$videoId"
