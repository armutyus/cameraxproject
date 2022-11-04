package com.armutyus.cameraxproject.ui.video

import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import androidx.camera.core.CameraInfo
import androidx.camera.core.TorchState
import androidx.camera.extensions.ExtensionMode
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.armutyus.cameraxproject.R
import com.armutyus.cameraxproject.ui.photo.models.CameraModesItem
import com.armutyus.cameraxproject.ui.video.models.PreviewVideoState
import com.armutyus.cameraxproject.ui.video.models.RecordingStatus
import com.armutyus.cameraxproject.ui.video.models.VideoEffect
import com.armutyus.cameraxproject.ui.video.models.VideoEvent
import com.armutyus.cameraxproject.util.*
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_DIR
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_MODE
import java.io.File

@Composable
fun VideoScreen(
    navController: NavController,
    factory: ViewModelProvider.Factory,
    videoViewModel: VideoViewModel = viewModel(factory = factory),
    onShowMessage: (message: String) -> Unit
) {
    val state by videoViewModel.videoState.collectAsState()
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
                videoViewModel.onEvent(VideoEvent.CameraInitialized(cameraLensInfo))
            }

            override fun recordingPaused() {
                videoViewModel.onEvent(VideoEvent.RecordingPaused)
            }

            override fun onProgress(progress: Int) {
                videoViewModel.onEvent(VideoEvent.OnProgress(progress))
            }

            override fun recordingCompleted(outputUri: Uri) {
                videoViewModel.onEvent(VideoEvent.RecordingEnded(outputUri))
            }

            override fun onError(throwable: Throwable?) {
                videoViewModel.onEvent(VideoEvent.Error(throwable))
            }
        }
    }

    val videoCaptureManager = remember(videoViewModel) {
        VideoCaptureManager.Builder(context)
            .registerLifecycleOwner(lifecycleOwner)
            .create()
            .apply { this.listener = listener }
    }

    val mediaDir = context.getExternalFilesDir("cameraXproject")?.let {
        File(it, VIDEO_DIR).apply { mkdirs() }
    }

    val latestCapturedVideo = state.latestVideoUri ?: mediaDir?.listFiles()?.firstOrNull {
        it.lastModified() == mediaDir.lastModified()
    }?.toUri()

    CompositionLocalProvider(LocalVideoCaptureManager provides videoCaptureManager) {
        VideoScreenContent(
            availableExtensions = listOf(ExtensionMode.NONE, VIDEO_MODE),
            extensionMode = state.extensionMode,
            cameraLens = state.lens,
            videoUri = latestCapturedVideo,
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
        videoViewModel.videoEffect.collect {
            when (it) {
                is VideoEffect.NavigateTo -> {
                    navController.navigate(it.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                is VideoEffect.ShowMessage -> onShowMessage(it.message)
                is VideoEffect.RecordVideo -> videoCaptureManager.startRecording(it.filePath)
                VideoEffect.PauseRecording -> videoCaptureManager.pauseRecording()
                VideoEffect.ResumeRecording -> videoCaptureManager.resumeRecording()
                VideoEffect.StopRecording -> videoCaptureManager.stopRecording()
            }
        }
    }
}

@Composable
private fun VideoScreenContent(
    availableExtensions: List<Int>,
    extensionMode: Int,
    cameraLens: Int?,
    @TorchState.State torchState: Int,
    hasFlashUnit: Boolean,
    hasDualCamera: Boolean,
    videoUri: Uri?,
    recordedLength: Int,
    recordingStatus: RecordingStatus,
    rotation: Int,
    view: View,
    onEvent: (VideoEvent) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        cameraLens?.let {
            CameraPreview(lens = it, torchState = torchState)
            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (recordingStatus == RecordingStatus.Idle) {
                    CaptureHeader(
                        showFlashIcon = hasFlashUnit,
                        torchState = torchState,
                        rotation = rotation,
                    ) { onEvent(VideoEvent.FlashTapped) }
                }
                if (recordedLength > 0) {
                    Timer(
                        seconds = recordedLength
                    )
                }
            }
            Column(
                modifier = Modifier.align(Alignment.BottomCenter),
                verticalArrangement = Arrangement.Bottom
            ) {
                VideoBottomControls(
                    availableExtensions = availableExtensions,
                    videoUri = videoUri,
                    extensionMode = extensionMode,
                    recordingStatus = recordingStatus,
                    showFlipIcon = hasDualCamera,
                    rotation = rotation,
                    view = view,
                    onThumbnailTapped = { onEvent(VideoEvent.ThumbnailTapped) },
                    onRecordTapped = { onEvent(VideoEvent.RecordTapped) },
                    onStopTapped = { onEvent(VideoEvent.StopTapped) },
                    onPauseTapped = { onEvent(VideoEvent.PauseTapped) },
                    onResumeTapped = { onEvent(VideoEvent.ResumeTapped) },
                    onCameraModeTapped = { extension ->
                        onEvent(
                            VideoEvent.SelectCameraExtension(
                                extension
                            )
                        )
                    },
                    onFlipTapped = { onEvent(VideoEvent.FlipTapped) }
                )
            }
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
internal fun VideoBottomControls(
    modifier: Modifier = Modifier,
    availableExtensions: List<Int>,
    extensionMode: Int,
    recordingStatus: RecordingStatus,
    showFlipIcon: Boolean,
    rotation: Int,
    videoUri: Uri?,
    view: View,
    onCameraModeTapped: (Int) -> Unit,
    onRecordTapped: () -> Unit,
    onStopTapped: () -> Unit,
    onPauseTapped: () -> Unit,
    onResumeTapped: () -> Unit,
    onFlipTapped: () -> Unit,
    onThumbnailTapped: () -> Unit
) {
    val cameraModes = mapOf(
        ExtensionMode.NONE to R.string.camera_mode_none,
        VIDEO_MODE to R.string.camera_mode_video
    )

    val cameraModesList = availableExtensions.map {
        CameraModesItem(
            it,
            stringResource(id = cameraModes[it]!!),
            extensionMode == it
        )
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyRow(contentPadding = PaddingValues(16.dp)) {
            items(cameraModesList) { cameraMode ->
                CaptureModesRow(cameraModesItem = cameraMode) {
                    onCameraModeTapped(it)
                }
            }
        }

        VideoControlsRow(
            showFlipIcon = showFlipIcon,
            recordingStatus = recordingStatus,
            view = view,
            videoUri = videoUri,
            rotation = rotation,
            onRecordTapped = { onRecordTapped() },
            onStopTapped = { onStopTapped() },
            onPauseTapped = { onPauseTapped() },
            onResumeTapped = { onResumeTapped() },
            onFlipTapped = { onFlipTapped() }
        ) { onThumbnailTapped() }
    }
}

@Composable
fun VideoControlsRow(
    showFlipIcon: Boolean,
    recordingStatus: RecordingStatus,
    view: View,
    videoUri: Uri?,
    rotation: Int,
    onRecordTapped: () -> Unit,
    onStopTapped: () -> Unit,
    onPauseTapped: () -> Unit,
    onResumeTapped: () -> Unit,
    onFlipTapped: () -> Unit,
    onThumbnailTapped: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .absolutePadding(left = 24.dp, right = 24.dp, bottom = 24.dp, top = 0.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CapturedVideoThumbnailIcon(imageUri = videoUri, rotation = rotation) {
                onThumbnailTapped
            }
            when (recordingStatus) {
                RecordingStatus.Idle -> {
                    CameraRecordIcon(
                        onTapped = onRecordTapped,
                        view = view
                    )
                }
                RecordingStatus.Paused -> {
                    CameraStopIcon(
                        onTapped = onStopTapped,
                        view = view
                    )
                    CameraPlayIconSmall(onTapped = onResumeTapped)
                }
                RecordingStatus.InProgress -> {
                    CameraStopIcon(
                        onTapped = onStopTapped,
                        view = view
                    )
                    CameraPauseIconSmall(onTapped = onPauseTapped)
                }
            }

            if (showFlipIcon && recordingStatus == RecordingStatus.Idle) {
                CameraFlipIcon(
                    onTapped = onFlipTapped,
                    rotation = rotation,
                    view = view
                )
            }
        }
    }
}

@Composable
fun CaptureModesRow(
    cameraModesItem: CameraModesItem,
    onCameraModeTapped: (Int) -> Unit
) {
    TextButton(
        onClick = {
            if (!cameraModesItem.selected) {
                onCameraModeTapped(cameraModesItem.cameraMode)
            }
        }
    ) {
        Text(
            text = cameraModesItem.name,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = if (cameraModesItem.selected) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.White
            }
        )
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