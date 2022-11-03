package com.armutyus.cameraxproject.ui.video

import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import androidx.camera.core.CameraInfo
import androidx.camera.core.TorchState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.armutyus.cameraxproject.ui.video.models.PreviewVideoState
import com.armutyus.cameraxproject.util.*

@Composable
fun VideoScreen(
    navController: NavController,
    factory: ViewModelProvider.Factory,
    videoViewModel: VideoViewModel = viewModel(factory = factory),
    onShowMessage: (message: String) -> Unit
) {
    val state by videoViewModel.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    var rotation by remember {
        mutableStateOf(0)
    }

    val orientationEventListener by lazy {
        object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == Util.UNKNOWN_ORIENTATION) {
                    return
                }

                rotation = when (orientation) {
                    in 45 until 135 -> Surface.ROTATION_270
                    in 135 until 225 -> Surface.ROTATION_180
                    in 225 until 315 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
            }
        }
    }

    DisposableEffect(key1 = "key1") {
        orientationEventListener.enable()
        onDispose {
            orientationEventListener.disable()
        }
    }

    LockScreenOrientation(orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

    val listener = remember(videoViewModel) {
        object : VideoCaptureManager.Listener {
            override fun onInitialised(cameraLensInfo: HashMap<Int, CameraInfo>) {
                videoViewModel.onEvent(VideoViewModel.Event.CameraInitialized(cameraLensInfo))
            }
            override fun recordingPaused() {
                videoViewModel.onEvent(VideoViewModel.Event.RecordingPaused)
            }
            override fun onProgress(progress: Int) {
                videoViewModel.onEvent(VideoViewModel.Event.OnProgress(progress))
            }
            override fun recordingCompleted(outputUri: Uri) {
                videoViewModel.onEvent(VideoViewModel.Event.RecordingEnded(outputUri))
            }
            override fun onError(throwable: Throwable?) {
                videoViewModel.onEvent(VideoViewModel.Event.Error(throwable))
            }
        }
    }

    val videoCaptureManager = remember(videoViewModel) {
        VideoCaptureManager.Builder(context)
            .registerLifecycleOwner(lifecycleOwner)
            .create()
            .apply { this.listener = listener }
    }

    CompositionLocalProvider(LocalVideoCaptureManager provides videoCaptureManager) {
        VideoScreenContent(
            cameraLens = state.lens,
            torchState = state.torchState,
            hasFlashUnit = state.lensInfo[state.lens]?.hasFlashUnit() ?: false,
            hasDualCamera = state.lensInfo.size > 1,
            recordedLength = state.recordedLength,
            recordingStatus = state.recordingStatus,
            rotation = rotation,
            view = view,
            onEvent = videoViewModel::onEvent
        )
    }

    LaunchedEffect(videoViewModel) {
        videoViewModel.effect.collect {
            when (it) {
                is VideoViewModel.Effect.NavigateTo -> navController.navigate(it.route)
                is VideoViewModel.Effect.ShowMessage -> onShowMessage(it.message)
                is VideoViewModel.Effect.RecordVideo -> videoCaptureManager.startRecording(it.filePath)
                VideoViewModel.Effect.PauseRecording -> videoCaptureManager.pauseRecording()
                VideoViewModel.Effect.ResumeRecording -> videoCaptureManager.resumeRecording()
                VideoViewModel.Effect.StopRecording -> videoCaptureManager.stopRecording()
            }
        }
    }
}

@Composable
private fun VideoScreenContent(
    cameraLens: Int?,
    @TorchState.State torchState: Int,
    hasFlashUnit: Boolean,
    hasDualCamera: Boolean,
    recordedLength: Int,
    recordingStatus: VideoViewModel.RecordingStatus,
    rotation: Int,
    view: View,
    onEvent: (VideoViewModel.Event) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        cameraLens?.let {
            CameraPreview(lens = it, torchState = torchState)
            if (recordingStatus == VideoViewModel.RecordingStatus.Idle) {
                CaptureHeader(
                    modifier = Modifier.align(Alignment.TopStart),
                    showFlashIcon = hasFlashUnit,
                    torchState = torchState,
                    rotation = rotation,
                ) { onEvent(VideoViewModel.Event.FlashTapped) }
            }
            if (recordedLength > 0) {
                Timer(
                    modifier = Modifier.align(Alignment.TopCenter),
                    seconds = recordedLength
                )
            }
            RecordFooter(
                modifier = Modifier.align(Alignment.BottomStart),
                recordingStatus = recordingStatus,
                showFlipIcon = hasDualCamera,
                rotation = rotation,
                view = view,
                onRecordTapped = { onEvent(VideoViewModel.Event.RecordTapped) },
                onStopTapped = { onEvent(VideoViewModel.Event.StopTapped) },
                onPauseTapped = { onEvent(VideoViewModel.Event.PauseTapped) },
                onResumeTapped = { onEvent(VideoViewModel.Event.ResumeTapped) }
            ) { onEvent(VideoViewModel.Event.FlipTapped) }
        }
    }
}

@Composable
internal fun CaptureHeader(
    modifier: Modifier = Modifier,
    showFlashIcon: Boolean,
    torchState: Int,
    rotation: Int,
    onFlashTapped: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(8.dp)
            .then(modifier)
    ) {
        CameraTorchIcon(
            showFlashIcon = showFlashIcon,
            torchState = torchState,
            rotation = rotation,
            onTapped = onFlashTapped
        )
    }
}


@Composable
internal fun RecordFooter(
    modifier: Modifier = Modifier,
    recordingStatus: VideoViewModel.RecordingStatus,
    showFlipIcon: Boolean,
    rotation: Int,
    view: View,
    onRecordTapped: () -> Unit,
    onStopTapped: () -> Unit,
    onPauseTapped: () -> Unit,
    onResumeTapped: () -> Unit,
    onFlipTapped: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 24.dp)
            .then(modifier)
    ) {
        when (recordingStatus) {
            VideoViewModel.RecordingStatus.Idle -> {
                CameraRecordIcon(
                    modifier = Modifier.align(Alignment.Center),
                    onTapped = onRecordTapped,
                    view = view
                )
            }
            VideoViewModel.RecordingStatus.Paused -> {
                CameraStopIcon(modifier = Modifier.align(Alignment.Center), onTapped = onStopTapped, view = view)
                CameraPlayIconSmall(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(end = 150.dp), onTapped = onResumeTapped
                )
            }
            VideoViewModel.RecordingStatus.InProgress -> {
                CameraStopIcon(modifier = Modifier.align(Alignment.Center), onTapped = onStopTapped, view = view)
                CameraPauseIconSmall(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(end = 140.dp), onTapped = onPauseTapped
                )
            }
        }

        if (showFlipIcon && recordingStatus == VideoViewModel.RecordingStatus.Idle) {
            CameraFlipIcon(
                modifier = Modifier.align(Alignment.CenterEnd),
                onTapped = onFlipTapped,
                rotation = rotation,
                view = view)
        }
    }
}

@Composable
private fun CameraPreview(lens: Int, @TorchState.State torchState: Int) {
    val captureManager = LocalVideoCaptureManager.current
    BoxWithConstraints {
        AndroidView(
            factory = {
                captureManager.showPreview(
                    PreviewVideoState(
                        cameraLens = lens,
                        torchState = torchState
                    )
                )
            },
            modifier = Modifier.fillMaxSize(),
            update = {
                captureManager.updatePreview(
                    PreviewVideoState(cameraLens = lens, torchState = torchState),
                    it
                )
            }
        )
    }
}