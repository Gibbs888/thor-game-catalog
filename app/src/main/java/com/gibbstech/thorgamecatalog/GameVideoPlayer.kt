package com.gibbstech.thorgamecatalog

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun GameVideoPlayer(game: Game, modifier: Modifier = Modifier) {
    when {
        !game.previewVideoUrl.isNullOrBlank() -> DirectVideoPlayer(
            url = game.previewVideoUrl,
            modifier = modifier,
        )

        !game.youtubeVideoId.isNullOrBlank() -> YouTubeVideoPlayer(
            videoId = game.youtubeVideoId,
            modifier = modifier,
        )
    }
}

@Composable
private fun DirectVideoPlayer(url: String, modifier: Modifier) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                this.player = player
                useController = true
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
        },
        update = { it.player = player },
        modifier = modifier,
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YouTubeVideoPlayer(videoId: String, modifier: Modifier) {
    val context = LocalContext.current
    val webView = remember(videoId) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            webViewClient = WebViewClient()
            loadUrl(
                "https://www.youtube.com/embed/$videoId" +
                    "?autoplay=1&playsinline=1&rel=0",
            )
        }
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier,
    )
}
