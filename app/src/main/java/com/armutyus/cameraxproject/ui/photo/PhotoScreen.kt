package com.armutyus.cameraxproject.ui.photo

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.extensions.ExtensionMode
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.armutyus.cameraxproject.models.CameraModesItem
import com.armutyus.cameraxproject.models.Effect
import com.armutyus.cameraxproject.models.Event
import com.armutyus.cameraxproject.models.PreviewState
import com.armutyus.cameraxproject.util.*
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_MODE
import java.io.File

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun PhotoScreen(
    navController: NavController,
    factory: ViewModelProvider.Factory,
    photoViewModel: PhotoViewModel = viewModel(factory = factory),
    onShowMessage: (message: String) -> Unit
) {
    val state by photoViewModel.state.collectAsState()

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

    val listener = remember {
        object : PhotoCaptureManager.PhotoListener {
            override fun onInitialised(
                cameraLensInfo: HashMap<Int, CameraInfo>,
                availableExtensions: List<Int>
            ) {
                photoViewModel.onEvent(Event.CameraInitialized(cameraLensInfo, availableExtensions))
            }

            override fun onSuccess(imageResult: ImageCapture.OutputFileResults) {
                photoViewModel.onEvent(Event.ImageCaptured(imageResult))
            }

            override fun onError(exception: Exception) {
                photoViewModel.onEvent(Event.Error(exception))
            }
        }
    }

    val photoCaptureManager = remember {
        PhotoCaptureManager.Builder(context)
            .registerLifecycleOwner(lifecycleOwner)
            .create()
            .apply { photoListener = listener }
    }

    LaunchedEffect(photoViewModel) {
        photoViewModel.effect.collect {
            when (it) {
                is Effect.NavigateTo -> navController.navigate(it.route)
                is Effect.CaptureImage -> photoCaptureManager.takePhoto(
                    it.filePath, state.lens
                        ?: CameraSelector.LENS_FACING_BACK
                )
                is Effect.ShowMessage -> onShowMessage(it.message)
            }
        }
    }

    val mediaDir = context.getExternalFilesDir("cameraXproject")?.let {
        File(it, "Photos").apply { mkdirs() }
    }

    val latestCapturedPhoto = state.latestImageUri ?: mediaDir?.listFiles()?.firstOrNull {
        it.lastModified() == mediaDir.lastModified()
    }?.toUri()

    CompositionLocalProvider(LocalPhotoCaptureManager provides photoCaptureManager) {
        PhotoScreenContent(
            cameraLens = state.lens,
            delayTimer = state.delayTimer,
            flashMode = state.flashMode,
            availableExtensions = state.availableExtensions,
            extensionMode = state.extensionMode,
            hasFlashUnit = state.lensInfo[state.lens]?.hasFlashUnit() ?: false,
            hasDualCamera = state.lensInfo.size > 1,
            imageUri = latestCapturedPhoto,
            view = view,
            rotation = rotation,
            onEvent = photoViewModel::onEvent
        )
    }
}

@RequiresApi(Build.VERSION_CODES.R)
@Composable
private fun PhotoScreenContent(
    cameraLens: Int?,
    delayTimer: Int,
    @ImageCapture.FlashMode flashMode: Int,
    availableExtensions: List<Int>,
    @ExtensionMode.Mode extensionMode: Int,
    hasFlashUnit: Boolean,
    hasDualCamera: Boolean,
    imageUri: Uri?,
    view: View,
    rotation: Int,
    onEvent: (Event) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        cameraLens?.let {
            CameraPreview(
                lens = it,
                flashMode = flashMode
            )
            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TopControls(
                    showFlashIcon = hasFlashUnit,
                    delayTimer = delayTimer,
                    flashMode = flashMode,
                    rotation = rotation,
                    onDelayTimerTapped = { onEvent(Event.DelayTimerTapped) },
                    onFlashTapped = { onEvent(Event.FlashTapped) },
                    onSettingsTapped = { onEvent(Event.SettingsTapped) }
                )
                /*if (captureWithDelay == DELAY_3S) {
                    DelayTimer(millisInFuture = captureWithDelay.toLong())
                } else if (captureWithDelay == DELAY_10S) {
                    DelayTimer(millisInFuture = captureWithDelay.toLong())
                }*/
            }
            Column(
                modifier = Modifier.align(Alignment.BottomCenter),
                verticalArrangement = Arrangement.Bottom
            ) {
                BottomControls(
                    availableExtensions = availableExtensions,
                    extensionMode = extensionMode,
                    showFlipIcon = hasDualCamera,
                    onCaptureTapped = { onEvent(Event.CaptureTapped) },
                    view = view,
                    imageUri = imageUri,
                    rotation = rotation,
                    onFlipTapped = { onEvent(Event.FlipTapped) },
                    onCameraModeTapped = {}
                ) { onEvent(Event.ThumbnailTapped) }
            }
        }
    }
}

@Composable
internal fun TopControls(
    showFlashIcon: Boolean,
    delayTimer: Int,
    flashMode: Int,
    rotation: Int,
    onDelayTimerTapped: () -> Unit,
    onFlashTapped: () -> Unit,
    onSettingsTapped: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .border(width = 0.5.dp, shape = CircleShape, color = Color.White),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CameraDelayIcon(
                delayTimer = delayTimer,
                rotation = rotation,
                onTapped = onDelayTimerTapped
            )
            CameraFlashIcon(
                showFlashIcon = showFlashIcon,
                rotation = rotation,
                flashMode = flashMode,
                onTapped = onFlashTapped
            )
            CameraEditIcon(rotation = rotation) {}
            SettingsIcon(rotation = rotation, onTapped = onSettingsTapped)
        }
    }
}

@Composable
internal fun BottomControls(
    availableExtensions: List<Int>,
    extensionMode: Int,
    showFlipIcon: Boolean,
    onCameraModeTapped: () -> Unit,
    onCaptureTapped: () -> Unit,
    view: View,
    imageUri: Uri?,
    rotation: Int,
    onFlipTapped: () -> Unit,
    onThumbnailTapped: () -> Unit
) {
    val cameraModes = mapOf(
        ExtensionMode.AUTO to R.string.camera_mode_auto,
        ExtensionMode.NIGHT to R.string.camera_mode_night,
        ExtensionMode.HDR to R.string.camera_mode_hdr,
        ExtensionMode.FACE_RETOUCH to R.string.camera_mode_face_retouch,
        ExtensionMode.BOKEH to R.string.camera_mode_bokeh,
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
        modifier = Modifier
            .verticalScroll(scrollState)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyRow(contentPadding = PaddingValues(16.dp)) {
            items(cameraModesList) { cameraModes ->
                CameraModesRow(cameraModesItem = cameraModes) {
                    onCameraModeTapped()
                }
            }
        }

        CameraControlsRow(
            showFlipIcon = showFlipIcon,
            view = view,
            imageUri = imageUri,
            rotation = rotation,
            onCaptureTapped = onCaptureTapped,
            onFlipTapped = onFlipTapped,
            onThumbnailTapped = onThumbnailTapped
        )
    }
}

@Composable
fun CameraControlsRow(
    showFlipIcon: Boolean,
    view: View,
    imageUri: Uri?,
    rotation: Int,
    onCaptureTapped: () -> Unit,
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
            CapturedImageThumbnailIcon(imageUri = imageUri, rotation = rotation, onTapped = onThumbnailTapped)
            CameraCaptureIcon(view = view, onTapped = onCaptureTapped)
            if (showFlipIcon) {
                CameraFlipIcon(view = view, rotation = rotation, onTapped = onFlipTapped)
            }
        }
    }
}

@Composable
fun CameraModesRow(
    cameraModesItem: CameraModesItem,
    onCameraModeTapped: () -> Unit
) {
    TextButton(
        onClick = {
            if (!cameraModesItem.selected) {
                onCameraModeTapped()
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

@RequiresApi(Build.VERSION_CODES.R)
@Composable
private fun CameraPreview(
    lens: Int,
    @ImageCapture.FlashMode flashMode: Int
) {
    val captureManager = LocalPhotoCaptureManager.current

    Box {
        AndroidView(
            factory = {
                captureManager.showPreview(
                    PreviewState(
                        flashMode = flashMode,
                        cameraLens = lens
                    )
                )
            },
            modifier = Modifier.fillMaxSize(),
            update = {
                captureManager.updatePreview(
                    PreviewState(
                        flashMode = flashMode,
                        cameraLens = lens
                    ), it
                )
            }
        )
    }
}