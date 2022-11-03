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
import com.armutyus.cameraxproject.ui.photo.models.CameraState
import com.armutyus.cameraxproject.ui.photo.models.PhotoEffect
import com.armutyus.cameraxproject.ui.photo.models.PhotoEvent
import com.armutyus.cameraxproject.ui.photo.models.PhotoState
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
    private val _photoState = MutableStateFlow(PhotoState())
    val photoState: StateFlow<PhotoState> = _photoState

    private val _photoEffect = MutableSharedFlow<PhotoEffect>()
    val photoEffect: SharedFlow<PhotoEffect> = _photoEffect

    fun onEvent(photoEvent: PhotoEvent) {
        when (photoEvent) {
            PhotoEvent.CaptureTapped -> onCaptureTapped()
            PhotoEvent.DelayTimerTapped -> onDelayTimerTapped()
            PhotoEvent.FlashTapped -> onFlashTapped()
            PhotoEvent.FlipTapped -> onFlipTapped()
            PhotoEvent.PhotoModeTapped -> onPhotoModeTapped()
            PhotoEvent.SettingsTapped -> onSettingsTapped()
            PhotoEvent.ThumbnailTapped -> onThumbnailTapped()
            PhotoEvent.VideoModeTapped -> onVideoModeTapped()

            is PhotoEvent.CameraInitialized -> onCameraInitialized(photoEvent.cameraLensInfo)
            is PhotoEvent.ExtensionModeChanged -> onExtensionModeChanged(photoEvent.availableExtensions)
            is PhotoEvent.Error -> onError()
            is PhotoEvent.ImageCaptured -> onImageCaptured(photoEvent.imageResult.savedUri)
            is PhotoEvent.SelectCameraExtension -> setExtensionMode(photoEvent.extension)
        }
    }

    private fun onCaptureTapped() {
        _photoState.update {
            when (_photoState.value.delayTimer) {
                TIMER_OFF -> it.copy(captureWithDelay = 0)
                TIMER_3S -> it.copy(captureWithDelay = DELAY_3S)
                TIMER_10S -> it.copy(captureWithDelay = DELAY_10S)
                else -> it.copy(captureWithDelay = 0)
            }
        }
        viewModelScope.launch {
            when (_photoState.value.delayTimer) {
                TIMER_OFF -> {
                    _photoState.update { it.copy(captureWithDelay = 0) }
                    try {
                        val filePath = fileManager.createFile(PHOTO_DIR, PHOTO_EXTENSION)
                        _photoEffect.emit(PhotoEffect.CaptureImage(filePath))
                    } catch (exception: IllegalArgumentException) {
                        Log.e(TAG, exception.localizedMessage ?: CAPTURE_FAIL)
                        _photoEffect.emit(PhotoEffect.ShowMessage())
                    }
                }
                TIMER_3S -> {
                    _photoState.update { it.copy(captureWithDelay = DELAY_3S) }
                    delay(3000)
                    try {
                        val filePath = fileManager.createFile(PHOTO_DIR, PHOTO_EXTENSION)
                        _photoEffect.emit(PhotoEffect.CaptureImage(filePath))
                    } catch (exception: IllegalArgumentException) {
                        Log.e(TAG, exception.localizedMessage ?: CAPTURE_FAIL)
                        _photoEffect.emit(PhotoEffect.ShowMessage())
                    }
                }
                TIMER_10S -> {
                    delay(10000)
                    try {
                        val filePath = fileManager.createFile(PHOTO_DIR, PHOTO_EXTENSION)
                        _photoEffect.emit(PhotoEffect.CaptureImage(filePath))
                    } catch (exception: IllegalArgumentException) {
                        Log.e(TAG, exception.localizedMessage ?: CAPTURE_FAIL)
                        _photoEffect.emit(PhotoEffect.ShowMessage())
                    }
                }
            }
        }
    }

    private fun onDelayTimerTapped() {
        _photoState.update {
            when (_photoState.value.delayTimer) {
                TIMER_OFF -> it.copy(delayTimer = TIMER_3S)
                TIMER_3S -> it.copy(delayTimer = TIMER_10S)
                TIMER_10S -> it.copy(delayTimer = TIMER_OFF)
                else -> it.copy(delayTimer = TIMER_OFF)
            }
        }
    }

    private fun onFlashTapped() {
        _photoState.update {
            when (_photoState.value.flashMode) {
                ImageCapture.FLASH_MODE_OFF -> it.copy(flashMode = ImageCapture.FLASH_MODE_AUTO)
                ImageCapture.FLASH_MODE_AUTO -> it.copy(flashMode = ImageCapture.FLASH_MODE_ON)
                ImageCapture.FLASH_MODE_ON -> it.copy(flashMode = ImageCapture.FLASH_MODE_OFF)
                else -> it.copy(flashMode = ImageCapture.FLASH_MODE_OFF)
            }
        }
    }

    private fun onFlipTapped() {
        val lens = if (_photoState.value.lens == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        //Check if the lens has flash unit
        val flashMode = if (_photoState.value.lensInfo[lens]?.hasFlashUnit() == true) {
            _photoState.value.flashMode
        } else {
            ImageCapture.FLASH_MODE_OFF
        }
        if (_photoState.value.lensInfo[lens] != null) {
            _photoState.getAndUpdate { it.copy(lens = lens, flashMode = flashMode) }
        }
    }

    private fun setExtensionMode(@ExtensionMode.Mode extension: Int) {
        if (extension == VIDEO_MODE) {
            viewModelScope.launch {
                _photoEffect.emit(PhotoEffect.NavigateTo(VIDEO_ROUTE))
            }
        } else {
            _photoState.update {
                it.copy(
                    cameraState = CameraState.NOT_READY,
                    extensionMode = extension
                )
            }
        }
    }

    private fun onPhotoModeTapped() {
        viewModelScope.launch {
            _photoEffect.emit(PhotoEffect.NavigateTo(PHOTO_ROUTE))
        }
    }

    private fun onSettingsTapped() {
        viewModelScope.launch {
            _photoEffect.emit(PhotoEffect.NavigateTo(SETTINGS_ROUTE))
        }
    }

    private fun onThumbnailTapped() {
        viewModelScope.launch {
            _photoEffect.emit(PhotoEffect.NavigateTo(PHOTO_PREVIEW_ROUTE))
        }
    }

    private fun onVideoModeTapped() {
        viewModelScope.launch {
            _photoEffect.emit(PhotoEffect.NavigateTo(VIDEO_ROUTE))
        }
    }

    private fun onImageCaptured(uri: Uri?) {
        if (uri != null && uri.path != null) {
            _photoState.update {
                it.copy(
                    latestImageUri = uri
                )
            }
        } else {
            val mediaDir = fileManager.getPrivateFileDirectory(PHOTO_DIR)
            val latestImageUri = mediaDir?.listFiles()?.lastOrNull()?.toUri() ?: Uri.EMPTY
            _photoState.update {
                it.copy(
                    latestImageUri = latestImageUri
                )
            }
        }
    }

    private fun onError() {
        viewModelScope.launch {
            _photoEffect.emit(PhotoEffect.ShowMessage())
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
            _photoState.update {
                it.copy(
                    lens = it.lens ?: defaultLens,
                    lensInfo = cameraLensInfo
                )
            }
        }
    }

    private fun onExtensionModeChanged(availableExtensions: List<Int>) {
        val currentState = _photoState.value
        if (availableExtensions.isEmpty()) {
            _photoState.update {
                it.copy(
                    extensionMode = ExtensionMode.NONE
                )
            }
        } else {
            _photoState.update {
                it.copy(
                    cameraState = CameraState.READY,
                    availableExtensions = availableExtensions,
                    extensionMode = currentState.extensionMode
                )
            }
        }
    }
}