package com.armutyus.cameraxproject.ui.video.models

import android.net.Uri
import androidx.camera.core.CameraInfo
import androidx.camera.core.ImageCapture
import androidx.camera.core.TorchState
import com.armutyus.cameraxproject.util.Util
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_MODE

data class VideoState(
    val lens: Int? = null,
    val delayTimer: Int = Util.TIMER_OFF,
    @TorchState.State val torchState: Int = TorchState.OFF,
    @ImageCapture.FlashMode val flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    val extensionMode: Int = VIDEO_MODE,
    val latestVideoUri: Uri? = null,
    val lensInfo: MutableMap<Int, CameraInfo> = mutableMapOf(),
    val recordedLength: Int = 0,
    val recordingStatus: RecordingStatus = RecordingStatus.Idle
)
