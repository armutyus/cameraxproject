package com.armutyus.cameraxproject.ui.video

import android.net.Uri
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.TorchState
import androidx.camera.extensions.ExtensionMode
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armutyus.cameraxproject.ui.video.models.RecordingStatus
import com.armutyus.cameraxproject.ui.video.models.VideoEffect
import com.armutyus.cameraxproject.ui.video.models.VideoEvent
import com.armutyus.cameraxproject.ui.video.models.VideoState
import com.armutyus.cameraxproject.util.FileManager
import com.armutyus.cameraxproject.util.Util
import com.armutyus.cameraxproject.util.Util.Companion.PHOTO_PREVIEW_ROUTE
import com.armutyus.cameraxproject.util.Util.Companion.PHOTO_ROUTE
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_DIR
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_EXTENSION
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class VideoViewModel constructor(
    private val fileManager: FileManager
) : ViewModel() {

    private val _videoState = MutableStateFlow(VideoState())
    val videoState: StateFlow<VideoState> = _videoState

    private val _videoEffect = MutableSharedFlow<VideoEffect>()
    val videoEffect: SharedFlow<VideoEffect> = _videoEffect

    fun onEvent(videoEvent: VideoEvent) {
        when (videoEvent) {
            VideoEvent.FlashTapped -> onFlashTapped()
            VideoEvent.FlipTapped -> onFlipTapped()
            VideoEvent.ThumbnailTapped -> onThumbnailTapped()

            VideoEvent.RecordTapped -> onRecordTapped()
            VideoEvent.PauseTapped -> onPauseTapped()
            VideoEvent.ResumeTapped -> onResumeTapped()
            VideoEvent.StopTapped -> onStopTapped()

            is VideoEvent.CameraInitialized -> onCameraInitialized(videoEvent.cameraLensInfo)
            is VideoEvent.SelectCameraExtension -> setExtensionMode(videoEvent.extension)
            is VideoEvent.OnProgress -> onProgress(videoEvent.progress)
            is VideoEvent.RecordingPaused -> onPaused()
            is VideoEvent.RecordingEnded -> onRecordingFinished(videoEvent.outputUri)
            is VideoEvent.Error -> onError()
        }
    }

    private fun onFlashTapped() {
        _videoState.update {
            when (_videoState.value.torchState) {
                TorchState.OFF -> it.copy(torchState = TorchState.ON)
                TorchState.ON -> it.copy(torchState = TorchState.OFF)
                else -> it.copy(torchState = TorchState.OFF)
            }
        }
    }

    private fun onFlipTapped() {
        val lens = if (_videoState.value.lens == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        //Check if the lens has flash unit
        val flashMode = if (_videoState.value.lensInfo[lens]?.hasFlashUnit() == true) {
            _videoState.value.flashMode
        } else {
            ImageCapture.FLASH_MODE_OFF
        }
        if (_videoState.value.lensInfo[lens] != null) {
            _videoState.update { it.copy(lens = lens, flashMode = flashMode) }
        }
    }

    private fun onThumbnailTapped() {
        viewModelScope.launch {
            _videoEffect.emit(VideoEffect.NavigateTo(PHOTO_PREVIEW_ROUTE))
        }
    }

    private fun onPauseTapped() {
        viewModelScope.launch {
            _videoEffect.emit(VideoEffect.PauseRecording)
        }
    }

    private fun onResumeTapped() {
        viewModelScope.launch {
            _videoEffect.emit(VideoEffect.ResumeRecording)
        }
    }

    private fun onStopTapped() {
        viewModelScope.launch {
            _videoEffect.emit(VideoEffect.StopRecording)
        }
    }

    private fun onRecordTapped() {
        viewModelScope.launch {
            try {
                val filePath = fileManager.createFile(VIDEO_DIR, VIDEO_EXTENSION)
                _videoEffect.emit(VideoEffect.RecordVideo(filePath))
            } catch (exception: IllegalArgumentException) {
                _videoEffect.emit(VideoEffect.ShowMessage())
            }
        }
    }

    private fun onRecordingFinished(uri: Uri?) {
        if (uri != null && uri.path != null) {
            _videoState.update {
                it.copy(
                    latestVideoUri = uri
                )
            }
        } else {
            val mediaDir = fileManager.getPrivateFileDirectory(VIDEO_DIR)
            val latestVideoUri = mediaDir?.listFiles()?.lastOrNull()?.toUri() ?: Uri.EMPTY
            _videoState.update {
                it.copy(
                    latestVideoUri = latestVideoUri
                )
            }
        }
        _videoState.update { it.copy(recordingStatus = RecordingStatus.Idle, recordedLength = 0) }
    }

    private fun onError() {
        _videoState.update { it.copy(recordedLength = 0, recordingStatus = RecordingStatus.Idle) }
        viewModelScope.launch {
            _videoEffect.emit(VideoEffect.ShowMessage())
        }
    }

    private fun onPaused() {
        _videoState.update { it.copy(recordingStatus = RecordingStatus.Paused) }
    }

    private fun onProgress(progress: Int) {
        _videoState.update {
            it.copy(
                recordedLength = progress,
                recordingStatus = RecordingStatus.InProgress
            )
        }
    }

    private fun setExtensionMode(extension: Int) {
        if (extension == ExtensionMode.NONE) {
            viewModelScope.launch {
                _videoEffect.emit(VideoEffect.NavigateTo(PHOTO_ROUTE))
            }
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
            _videoState.update {
                it.copy(
                    lens = it.lens ?: defaultLens,
                    lensInfo = cameraLensInfo
                )
            }
        }
    }
}