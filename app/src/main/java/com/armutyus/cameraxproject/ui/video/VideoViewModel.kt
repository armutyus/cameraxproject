package com.armutyus.cameraxproject.ui.video

import android.net.Uri
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.TorchState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armutyus.cameraxproject.util.FileManager
import com.armutyus.cameraxproject.util.Util.Companion.GENERAL_ERROR_MESSAGE
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_DIR
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_EXTENSION
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_ROUTE
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException

class VideoViewModel constructor(
    private val fileManager: FileManager
) : ViewModel() {

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    private val _effect = MutableSharedFlow<Effect>()
    val effect: SharedFlow<Effect> = _effect

    fun onEvent(event: Event) {
        when (event) {
            Event.FlashTapped -> onFlashTapped()
            Event.FlipTapped -> onFlipTapped()

            Event.RecordTapped -> onRecordTapped()
            Event.PauseTapped -> onPauseTapped()
            Event.ResumeTapped -> onResumeTapped()
            Event.StopTapped -> onStopTapped()

            is Event.CameraInitialized -> onCameraInitialized(event.cameraLensInfo)
            is Event.OnProgress -> onProgress(event.progress)
            is Event.RecordingPaused -> onPaused()
            is Event.RecordingEnded -> onRecordingFinished(event.outputUri)
            is Event.Error -> onError()
        }
    }

    private fun onFlashTapped() {
        _state.update {
            when (_state.value.torchState) {
                TorchState.OFF -> it.copy(torchState = TorchState.ON)
                TorchState.ON -> it.copy(torchState = TorchState.OFF)
                else -> it.copy(torchState = TorchState.OFF)
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
            _state.update { it.copy(lens = lens, flashMode = flashMode) }
        }
    }

    private fun onPauseTapped() {
        viewModelScope.launch {
            _effect.emit(Effect.PauseRecording)
        }
    }

    private fun onResumeTapped() {
        viewModelScope.launch {
            _effect.emit(Effect.ResumeRecording)
        }
    }

    private fun onStopTapped() {
        viewModelScope.launch {
            _effect.emit(Effect.StopRecording)
        }
    }

    private fun onRecordTapped() {
        viewModelScope.launch {
            try {
                val filePath = fileManager.createFile(VIDEO_DIR, VIDEO_EXTENSION)
                _effect.emit(Effect.RecordVideo(filePath))
            } catch (exception: IllegalArgumentException) {
                _effect.emit(Effect.ShowMessage())
            }
        }
    }

    private fun onRecordingFinished(uri: Uri) {
        viewModelScope.launch {
            _effect.emit(Effect.NavigateTo(VIDEO_ROUTE))
        }
        _state.update { it.copy(recordingStatus = RecordingStatus.Idle, recordedLength = 0) }
    }

    private fun onError() {
        _state.update { it.copy(recordedLength = 0, recordingStatus = RecordingStatus.Idle) }
        viewModelScope.launch {
            _effect.emit(Effect.ShowMessage())
        }
    }

    private fun onPaused() {
        _state.update { it.copy(recordingStatus = RecordingStatus.Paused) }
    }

    private fun onProgress(progress: Int) {
        _state.update {
            it.copy(
                recordedLength = progress,
                recordingStatus = RecordingStatus.InProgress
            )
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
        val lens: Int? = null,
        @TorchState.State val torchState: Int = TorchState.OFF,
        @ImageCapture.FlashMode val flashMode: Int = ImageCapture.FLASH_MODE_OFF,
        val lensInfo: MutableMap<Int, CameraInfo> = mutableMapOf(),
        val recordedLength: Int = 0,
        val recordingStatus: RecordingStatus = RecordingStatus.Idle
    )

    sealed class Event {
        data class CameraInitialized(val cameraLensInfo: HashMap<Int, CameraInfo>) : Event()
        data class OnProgress(val progress: Int) : Event()
        object RecordingPaused : Event()
        data class RecordingEnded(val outputUri: Uri) : Event()
        data class Error(val throwable: Throwable?) : Event()

        object FlashTapped : Event()
        object FlipTapped : Event()

        object RecordTapped : Event()
        object PauseTapped : Event()
        object ResumeTapped : Event()
        object StopTapped : Event()

    }

    sealed class Effect {

        data class ShowMessage(val message: String = GENERAL_ERROR_MESSAGE) : Effect()
        data class RecordVideo(val filePath: String) : Effect()
        data class NavigateTo(val route: String) : Effect()

        object PauseRecording : Effect()
        object ResumeRecording : Effect()
        object StopRecording : Effect()
    }

    sealed class RecordingStatus {
        object Idle : RecordingStatus()
        object InProgress : RecordingStatus()
        object Paused : RecordingStatus()
    }
}