package com.armutyus.cameraxproject.ui.video.models

import android.net.Uri
import androidx.camera.core.CameraInfo

sealed class VideoEvent {
    data class CameraInitialized(val cameraLensInfo: HashMap<Int, CameraInfo>) : VideoEvent()
    data class OnProgress(val progress: Int) : VideoEvent()
    object RecordingPaused : VideoEvent()
    data class RecordingEnded(val outputUri: Uri) : VideoEvent()
    data class Error(val throwable: Throwable?) : VideoEvent()
    data class SelectCameraExtension(val extension: Int) : VideoEvent()

    object FlashTapped : VideoEvent()
    object FlipTapped : VideoEvent()
    object ThumbnailTapped : VideoEvent()

    object RecordTapped : VideoEvent()
    object PauseTapped : VideoEvent()
    object ResumeTapped : VideoEvent()
    object StopTapped : VideoEvent()

}
