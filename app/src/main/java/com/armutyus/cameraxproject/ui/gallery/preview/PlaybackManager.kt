package com.armutyus.cameraxproject.ui.gallery.preview

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlaybackManager private constructor(private val builder: Builder) {

    private val context = builder.context

    private val lifecycleOwner = builder.lifecycleOwner

    private lateinit var coroutineJob: Job

    val videoView: VideoView by lazy {
        VideoView(context).apply {
            val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            layoutParams = params
            setVideoURI(builder.uri)
            setOnPreparedListener {
                builder.listener?.onPrepared()
                this.seekTo(1)
            }
            setOnCompletionListener {
                builder.listener?.onCompleted()
            }
            requestFocus()
        }
    }

    fun start(currentPosition: Int) {
        videoView.seekTo(currentPosition)
        videoView.start()
        coroutineJob = lifecycleOwner!!.lifecycleScope.launch {
            while (videoView.isPlaying) {
                builder.listener?.onProgress(videoView.currentPosition)
                delay(1000)
            }
        }
    }

    fun pausePlayback() {
        videoView.pause()
        coroutineJob.cancel()
    }

    class Builder(val context: Context) {

        var listener: PlaybackListener? = null

        var lifecycleOwner: LifecycleOwner? = null

        var uri: Uri? = null

        fun build(): PlaybackManager {
            requireNotNull(uri)
            requireNotNull(lifecycleOwner)
            requireNotNull(listener)
            return PlaybackManager(this)
        }
    }

    interface PlaybackListener {

        fun onPrepared()

        fun onProgress(progress: Int)

        fun onCompleted()

    }
}

val LocalPlaybackManager =
    compositionLocalOf<PlaybackManager> { error("No playback manager found!") }