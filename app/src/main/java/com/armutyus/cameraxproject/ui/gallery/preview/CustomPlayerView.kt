package com.armutyus.cameraxproject.ui.gallery.preview

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun CustomPlayerView(
    filePath: Uri?,
    modifier: Modifier = Modifier,
    videoPlayer: ExoPlayer,
    isFullScreen: Boolean,
    shouldShowController: Boolean,
    onFullScreenToggle: (isFullScreen: Boolean) -> Unit,
    hideController: (isPlaying: Boolean) -> Unit,
    onPlayerClick: () -> Unit,
    navigateBack: (() -> Unit)? = null
) {

    BackHandler {
        if (isFullScreen) {
            onFullScreenToggle.invoke(true)
        } else {
            navigateBack?.invoke()
        }
    }

    Box(modifier = modifier) {

        var isPlaying by remember { mutableStateOf(videoPlayer.isPlaying) }

        var playbackState by remember { mutableStateOf(videoPlayer.playbackState) }

        var videoTimer by remember { mutableStateOf(0f) }

        var totalDuration by remember { mutableStateOf(0L) }

        var bufferedPercentage by remember { mutableStateOf(0) }

        DisposableEffect(Unit) {
            val listener = object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    super.onEvents(player, events)
                    isPlaying = player.isPlaying
                    totalDuration = player.duration
                    videoTimer = player.contentPosition.toFloat()
                    bufferedPercentage = player.bufferedPercentage
                    playbackState = player.playbackState
                }
            }

            videoPlayer.addListener(listener)

            onDispose {
                videoPlayer.removeListener(listener)
            }
        }

        LaunchedEffect(true) {
            while (isActive) {
                videoTimer = videoPlayer.contentPosition.toFloat()
                delay(50)
            }
        }

        LaunchedEffect(true) {
            if (isPlaying) hideController(true)
        }

        VideoPlayer(
            filePath = filePath,
            modifier = Modifier.fillMaxSize(),
            videoPlayer = videoPlayer
        ) {
            onPlayerClick()
        }

        CustomMediaController(
            modifier = Modifier.fillMaxSize(),
            isVisible = { shouldShowController },
            isPlaying = { isPlaying },
            playbackState = { playbackState },
            totalDuration = { totalDuration },
            bufferedPercentage = { bufferedPercentage },
            isFullScreen = isFullScreen,
            onReplay = { videoPlayer.seekBack() },
            onForward = { videoPlayer.seekForward() },
            onPauseToggle = {
                when {
                    videoPlayer.isPlaying -> {
                        videoPlayer.pause()
                    }
                    videoPlayer.isPlaying.not() && playbackState == STATE_ENDED -> {
                        videoPlayer.seekTo(0, 0)
                        videoPlayer.playWhenReady = true
                    }
                    else -> {
                        videoPlayer.play()
                    }
                }
                isPlaying = isPlaying.not()
            },
            onSeekChanged = { position -> videoPlayer.seekTo(position.toLong()) },
            videoTimer = { videoTimer },
            onFullScreenToggle = onFullScreenToggle
        )
    }
}

@Composable
private fun VideoPlayer(
    filePath: Uri?,
    modifier: Modifier = Modifier,
    videoPlayer: ExoPlayer,
    onPlayerClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .clickable { onPlayerClick.invoke() }
    ) {
        AndroidView(
            modifier = modifier,
            factory = {
                PlayerView(it).apply {
                    player = videoPlayer
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    player?.setMediaItem(androidx.media3.common.MediaItem.fromUri(filePath!!))
                    player?.prepare()
                }
            },
            update = {
                it.apply {
                    player?.setMediaItem(androidx.media3.common.MediaItem.fromUri(filePath!!))
                }
            }
        )
    }
}