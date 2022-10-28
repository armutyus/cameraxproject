package com.armutyus.cameraxproject.ui.photo

import android.net.Uri
import android.os.CountDownTimer
import android.util.Log
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armutyus.cameraxproject.util.FileManager
import com.armutyus.cameraxproject.util.Util.Companion.CAPTURE_FAIL
import com.armutyus.cameraxproject.util.Util.Companion.GENERAL_ERROR_MESSAGE
import com.armutyus.cameraxproject.util.Util.Companion.PHOTO_DIR
import com.armutyus.cameraxproject.util.Util.Companion.PHOTO_EXTENSION
import com.armutyus.cameraxproject.util.Util.Companion.TAG
import com.armutyus.cameraxproject.util.Util.Companion.TIMER_10S
import com.armutyus.cameraxproject.util.Util.Companion.TIMER_3S
import com.armutyus.cameraxproject.util.Util.Companion.TIMER_OFF
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PhotoViewModel constructor(private val fileManager: FileManager) : ViewModel() {

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    private val _effect = MutableSharedFlow<Effect>()
    val effect: SharedFlow<Effect> = _effect

    fun onEvent(event: Event) {
        when (event) {
            Event.CaptureTapped -> onCaptureTapped()
            Event.DelayTimerTapped -> onDelayTimerTapped()
            Event.FlashTapped -> onFlashTapped()
            Event.FlipTapped -> onFlipTapped()
            Event.PhotoModeTapped -> onPhotoModeTapped()
            Event.SettingsTapped -> onSettingsTapped()
            Event.ThumbnailTapped -> onThumbnailTapped()
            Event.VideoModeTapped -> onVideoModeTapped()

            is Event.CameraInitialized -> onCameraInitialized(event.cameraLensInfo)
            is Event.Error -> onError()
            is Event.ImageCaptured -> onImageCaptured(event.imageResult.savedUri)
        }
    }

    private fun onCaptureTapped() {
        viewModelScope.launch {
            try {
                val filePath = fileManager.createFile(PHOTO_DIR, PHOTO_EXTENSION)
                _effect.emit(Effect.CaptureImage(filePath))
            } catch (exception: IllegalArgumentException) {
                Log.e(TAG, exception.localizedMessage ?: CAPTURE_FAIL)
                _effect.emit(Effect.ShowMessage())
            }
        }
    }

    private fun onDelayTimerTapped() {
        _state.update {
            when (_state.value.delayTimer) {
                TIMER_OFF -> it.copy(delayTimer = TIMER_3S)
                TIMER_3S -> it.copy(delayTimer = TIMER_10S)
                TIMER_10S -> it.copy(delayTimer = TIMER_OFF)
                else -> it.copy(delayTimer = TIMER_OFF)
            }
        }
    }

    private fun onFlashTapped() {
        _state.update {
            when (_state.value.flashMode) {
                ImageCapture.FLASH_MODE_OFF -> it.copy(flashMode = ImageCapture.FLASH_MODE_AUTO)
                ImageCapture.FLASH_MODE_AUTO -> it.copy(flashMode = ImageCapture.FLASH_MODE_ON)
                ImageCapture.FLASH_MODE_ON -> it.copy(flashMode = ImageCapture.FLASH_MODE_OFF)
                else -> it.copy(flashMode = ImageCapture.FLASH_MODE_OFF)
            }
        }
    }

    private fun onFlipTapped() {
        val lens = if (_state.value.lens == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        //Check if the lens has flash unit
        val flashMode = if (_state.value.lensInfo[lens]?.hasFlashUnit() == true) {
            _state.value.flashMode
        } else {
            ImageCapture.FLASH_MODE_OFF
        }
        if (_state.value.lensInfo[lens] != null) {
            _state.getAndUpdate { it.copy(lens = lens, flashMode = flashMode) }
        }
    }

    private fun onPhotoModeTapped() {
        viewModelScope.launch {
            _effect.emit(Effect.NavigateTo("photo_screen"))
        }
    }

    private fun onSettingsTapped() {
        viewModelScope.launch {
            _effect.emit(Effect.NavigateTo("settings_screen"))
        }
    }

    private fun onThumbnailTapped() {
        viewModelScope.launch {
            _effect.emit(Effect.NavigateTo("photo_preview_screen"))
        }
    }

    private fun onVideoModeTapped() {
        viewModelScope.launch {
            _effect.emit(Effect.NavigateTo("video_screen"))
        }
    }

    private fun onImageCaptured(uri: Uri?) {
        if (uri != null && uri.path != null) {
            _state.update {
                it.copy(
                    latestImageUri = uri
                )
            }
        } else {
            val mediaDir = fileManager.getPrivateFileDirectory(PHOTO_DIR)
            val latestImageUri = mediaDir?.listFiles()?.lastOrNull()?.toUri() ?: Uri.EMPTY
            _state.update {
                it.copy(
                    latestImageUri = latestImageUri
                )
            }
        }
    }

    private fun onError() {
        viewModelScope.launch {
            _effect.emit(Effect.ShowMessage())
        }
    }

    private fun onCameraInitialized(cameraLensInfo: HashMap<Int, CameraInfo>) {
        if (cameraLensInfo.isNotEmpty()) {
            val defaultLens = if (cameraLensInfo[CameraSelector.LENS_FACING_BACK] != null) {
                CameraSelector.LENS_FACING_BACK
            } else if (cameraLensInfo[CameraSelector.LENS_FACING_BACK] != null) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                null
            }
            _state.update {
                it.copy(
                    lens = it.lens ?: defaultLens,
                    lensInfo = cameraLensInfo
                )
            }
        }
    }

    data class State(
        val delayTimer: Int = TIMER_OFF,
        @ImageCapture.FlashMode val flashMode: Int = ImageCapture.FLASH_MODE_OFF,
        val latestImageUri: Uri? = null,
        val lens: Int? = null,
        val lensInfo: MutableMap<Int, CameraInfo> = mutableMapOf(),
    )

    sealed class Event {
        data class CameraInitialized(val cameraLensInfo: HashMap<Int, CameraInfo>) : Event()
        data class ImageCaptured(val imageResult: ImageCapture.OutputFileResults) : Event()
        data class Error(val exception: Exception) : Event()

        object CaptureTapped : Event()
        object DelayTimerTapped : Event()
        object FlashTapped : Event()
        object FlipTapped : Event()
        object PhotoModeTapped : Event()
        object SettingsTapped : Event()
        object ThumbnailTapped : Event()
        object VideoModeTapped : Event()
    }

    sealed class Effect {
        data class ShowMessage(val message: String = GENERAL_ERROR_MESSAGE) : Effect()
        data class CaptureImage(val filePath: String) : Effect()
        data class NavigateTo(val route: String) : Effect()
    }
}