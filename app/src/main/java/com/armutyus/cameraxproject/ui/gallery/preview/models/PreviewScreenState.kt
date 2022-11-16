package com.armutyus.cameraxproject.ui.gallery.preview.models

data class PreviewScreenState(
    val showBars: Boolean = false,
    val filePath: String? = null,
    val playbackStatus: PlaybackStatus? = PlaybackStatus.Idle,
    val playbackPosition: Int = 0
)

sealed class PlaybackStatus {
    object Idle : PlaybackStatus()
    object InProgress : PlaybackStatus()
}
