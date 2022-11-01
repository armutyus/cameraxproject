package com.armutyus.cameraxproject.ui.photo

import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.extensions.ExtensionMode
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armutyus.cameraxproject.models.CameraState
import com.armutyus.cameraxproject.models.State
import com.armutyus.cameraxproject.models.Effect
import com.armutyus.cameraxproject.models.Event
import com.armutyus.cameraxproject.util.FileManager
import com.armutyus.cameraxproject.util.Util.Companion.CAPTURE_FAIL
import com.armutyus.cameraxproject.util.Util.Companion.DELAY_10S
import com.armutyus.cameraxproject.util.Util.Companion.DELAY_3S
import com.armutyus.cameraxproject.util.Util.Companion.PHOTO_DIR
import com.armutyus.cameraxproject.util.Util.Companion.PHOTO_EXTENSION
import com.armutyus.cameraxproject.util.Util.Companion.PHOTO_PREVIEW_ROUTE
import com.armutyus.cameraxproject.util.Util.Companion.PHOTO_ROUTE
import com.armutyus.cameraxproject.util.Util.Companion.SETTINGS_ROUTE
import com.armutyus.cameraxproject.util.Util.Companion.TAG
import com.armutyus.cameraxproject.util.Util.Companion.TIMER_10S
import com.armutyus.cameraxproject.util.Util.Companion.TIMER_3S
import com.armutyus.cameraxproject.util.Util.Companion.TIMER_OFF
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_MODE
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_ROUTE
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

            is Event.CameraInitialized -> onCameraInitialized(event.cameraLensInfo, event.availableExtensions)
            is Event.Error -> onError()
            is Event.ImageCaptured -> onImageCaptured(event.imageResult.savedUri)
            is Event.SelectCameraExtension -> setExtensionMode(event.extension)
        }
    }

    private fun onCaptureTapped() {
        _state.update {
            when (_state.value.delayTimer) {
                TIMER_OFF -> it.copy(captureWithDelay = 0)
                TIMER_3S -> it.copy(captureWithDelay = DELAY_3S)
                TIMER_10S -> it.copy(captureWithDelay = DELAY_10S)
                else -> it.copy(captureWithDelay = 0)
            }
        }
        viewModelScope.launch {
            when (_state.value.delayTimer) {
                TIMER_OFF -> {
                    _state.update { it.copy(captureWithDelay = 0) }
                    try {
                        val filePath = fileManager.createFile(PHOTO_DIR, PHOTO_EXTENSION)
                        _effect.emit(Effect.CaptureImage(filePath))
                    } catch (exception: IllegalArgumentException) {
                        Log.e(TAG, exception.localizedMessage ?: CAPTURE_FAIL)
                        _effect.emit(Effect.ShowMessage())
                    }
                }
                TIMER_3S -> {
                    _state.update { it.copy(captureWithDelay = DELAY_3S) }
                    delay(3000)
                    try {
                        val filePath = fileManager.createFile(PHOTO_DIR, PHOTO_EXTENSION)
                        _effect.emit(Effect.CaptureImage(filePath))
                    } catch (exception: IllegalArgumentException) {
                        Log.e(TAG, exception.localizedMessage ?: CAPTURE_FAIL)
                        _effect.emit(Effect.ShowMessage())
                    }
                }
                TIMER_10S -> {
                    delay(10000)
                    try {
                        val filePath = fileManager.createFile(PHOTO_DIR, PHOTO_EXTENSION)
                        _effect.emit(Effect.CaptureImage(filePath))
                    } catch (exception: IllegalArgumentException) {
                        Log.e(TAG, exception.localizedMessage ?: CAPTURE_FAIL)
                        _effect.emit(Effect.ShowMessage())
                    }
                }
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

    private fun setExtensionMode(@ExtensionMode.Mode extension: Int) {
        if (extension == VIDEO_MODE) {
            viewModelScope.launch {
                _effect.emit(Effect.NavigateTo(VIDEO_ROUTE))
            }
        } else {
            _state.update {
                it.copy(
                    cameraState = CameraState.NOT_READY,
                    extensionMode = extension
                )
            }
        }
    }

    private fun onPhotoModeTapped() {
        viewModelScope.launch {
            _effect.emit(Effect.NavigateTo(PHOTO_ROUTE))
        }
    }

    private fun onSettingsTapped() {
        viewModelScope.launch {
            _effect.emit(Effect.NavigateTo(SETTINGS_ROUTE))
        }
    }

    private fun onThumbnailTapped() {
        viewModelScope.launch {
            _effect.emit(Effect.NavigateTo(PHOTO_PREVIEW_ROUTE))
        }
    }

    private fun onVideoModeTapped() {
        viewModelScope.launch {
            _effect.emit(Effect.NavigateTo(VIDEO_ROUTE))
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

    private fun onCameraInitialized(cameraLensInfo: HashMap<Int, CameraInfo>, availableExtensions: List<Int>) {
        val currentState = _state.value
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

        if (availableExtensions.isEmpty()) {
            _state.update {
                it.copy(
                    extensionMode = ExtensionMode.NONE
                )
            }
        } else {
            _state.update {
                it.copy(
                    cameraState = CameraState.READY,
                    availableExtensions = availableExtensions,
                    extensionMode = currentState.extensionMode
                )
            }
        }
    }
}