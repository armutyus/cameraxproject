package com.armutyus.cameraxproject.models

import androidx.camera.core.CameraInfo
import androidx.camera.core.ImageCapture
import androidx.camera.extensions.ExtensionMode

sealed class Event {
    data class CameraInitialized(val cameraLensInfo: HashMap<Int, CameraInfo>, val availableExtensions: List<Int>) : Event()
    data class ImageCaptured(val imageResult: ImageCapture.OutputFileResults) : Event()
    data class Error(val exception: Exception) : Event()
    data class SelectCameraExtension(@ExtensionMode.Mode val extension: Int) : Event()

    object CameraModeTapped : Event()
    object CaptureTapped : Event()
    object DelayTimerTapped : Event()
    object FlashTapped : Event()
    object FlipTapped : Event()
    object PhotoModeTapped : Event()
    object SettingsTapped : Event()
    object ThumbnailTapped : Event()
    object VideoModeTapped : Event()
}
