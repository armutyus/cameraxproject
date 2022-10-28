package com.armutyus.cameraxproject.ui.photo

import android.net.Uri
import android.view.View
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.armutyus.cameraxproject.R
import com.armutyus.cameraxproject.util.*
import com.armutyus.cameraxproject.util.Util.Companion.TIMER_10S
import com.armutyus.cameraxproject.util.Util.Companion.TIMER_3S

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

    val listener = remember {
        object : PhotoCaptureManager.PhotoListener {
            override fun onInitialised(cameraLensInfo: HashMap<Int, CameraInfo>) {
                photoViewModel.onEvent(PhotoViewModel.Event.CameraInitialized(cameraLensInfo))
            }

            override fun onSuccess(imageResult: ImageCapture.OutputFileResults) {
                photoViewModel.onEvent(PhotoViewModel.Event.ImageCaptured(imageResult))
            }

            override fun onError(exception: Exception) {
                photoViewModel.onEvent(PhotoViewModel.Event.Error(exception))
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
                is PhotoViewModel.Effect.NavigateTo -> navController.navigate(it.route)
                is PhotoViewModel.Effect.CaptureImage -> photoCaptureManager.takePhoto(
                    it.filePath, state.lens
                        ?: CameraSelector.LENS_FACING_BACK
                )
                is PhotoViewModel.Effect.ShowMessage -> onShowMessage(it.message)
            }
        }
    }

    CompositionLocalProvider(LocalPhotoCaptureManager provides photoCaptureManager) {
        PhotoScreenContent(
            cameraLens = state.lens,
            delayTimer = state.delayTimer,
            flashMode = state.flashMode,
            hasFlashUnit = state.lensInfo[state.lens]?.hasFlashUnit() ?: false,
            hasDualCamera = state.lensInfo.size > 1,
            imageUri = state.latestImageUri,
            view = view,
            onEvent = photoViewModel::onEvent
        )
    }
}

@Composable
private fun PhotoScreenContent(
    cameraLens: Int?,
    delayTimer: Int,
    @ImageCapture.FlashMode flashMode: Int,
    hasFlashUnit: Boolean,
    hasDualCamera: Boolean,
    imageUri: Uri?,
    view: View,
    onEvent: (PhotoViewModel.Event) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        cameraLens?.let {
            CameraPreview(
                lens = it,
                flashMode = flashMode
            )
            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                verticalArrangement = Arrangement.Top
            ) {
                TopControls(
                    showFlashIcon = hasFlashUnit,
                    delayTimer = delayTimer,
                    flashMode = flashMode,
                    onDelayTimerTapped = { onEvent(PhotoViewModel.Event.DelayTimerTapped) },
                    onFlashTapped = { onEvent(PhotoViewModel.Event.FlashTapped) },
                    onSettingsTapped = { onEvent(PhotoViewModel.Event.SettingsTapped) }
                )
                if (delayTimer == TIMER_3S) {
                    DelayTimer(millisInFuture = delayTimer.toLong()) {
                        onEvent(PhotoViewModel.Event.CaptureTapped)
                    }
                } else if (delayTimer == TIMER_10S) {
                    DelayTimer(millisInFuture = delayTimer.toLong()) {
                        onEvent(PhotoViewModel.Event.CaptureTapped)
                    }
                }
            }
            Column(
                modifier = Modifier.align(Alignment.BottomCenter),
                verticalArrangement = Arrangement.Bottom
            ) {
                BottomControls(
                    showFlipIcon = hasDualCamera,
                    onPhotoModeTapped = { onEvent(PhotoViewModel.Event.PhotoModeTapped) },
                    onVideoModeTapped = { onEvent(PhotoViewModel.Event.VideoModeTapped) },
                    onCaptureTapped = { onEvent(PhotoViewModel.Event.CaptureTapped) },
                    view = view,
                    imageUri = imageUri,
                    onFlipTapped = { onEvent(PhotoViewModel.Event.FlipTapped) },
                    onThumbnailTapped = { onEvent(PhotoViewModel.Event.ThumbnailTapped) }
                )
            }
        }
    }
}

@Composable
internal fun TopControls(
    showFlashIcon: Boolean,
    delayTimer: Int,
    flashMode: Int,
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
            CameraDelayIcon(delayTimer = delayTimer, onTapped = onDelayTimerTapped)
            CameraFlashIcon(
                showFlashIcon = showFlashIcon,
                flashMode = flashMode,
                onTapped = onFlashTapped
            )
            CameraEditIcon {}
            SettingsIcon(onTapped = onSettingsTapped)
        }
    }
}

@Composable
internal fun BottomControls(
    showFlipIcon: Boolean,
    onPhotoModeTapped: () -> Unit,
    onVideoModeTapped: () -> Unit,
    onCaptureTapped: () -> Unit,
    view: View,
    imageUri: Uri?,
    onFlipTapped: () -> Unit,
    onThumbnailTapped: () -> Unit
) {
    val cameraModes = listOf(
        "Photo",
        "Video"
    )
    var selectedMode by remember {
        mutableStateOf("Photo")
    }
    val onSelectionChange = { text: String ->
        selectedMode = text
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CameraModesRow(
            cameraModes = cameraModes,
            onCameraModeTapped = if (selectedMode == "Photo") onPhotoModeTapped else onVideoModeTapped,
            onSelectionChange = onSelectionChange,
            selectedMode = selectedMode
        )
        CameraControlsRow(
            showFlipIcon = showFlipIcon,
            view = view,
            imageUri = imageUri,
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
            Image(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(width = 1.dp, color = Color.White, shape = CircleShape)
                    .clickable { onThumbnailTapped() },
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(data = imageUri)
                        .build(),
                    filterQuality = FilterQuality.Medium
                ),
                contentDescription = stringResource(id = R.string.latest_image),
                contentScale = ContentScale.Crop
            )
            CameraCaptureIcon(view = view, onTapped = onCaptureTapped)
            if (showFlipIcon) {
                CameraFlipIcon(view = view, onTapped = onFlipTapped)
            }
        }
    }
}

@Composable
fun CameraModesRow(
    cameraModes: List<String>,
    onCameraModeTapped: () -> Unit,
    onSelectionChange: (String) -> Unit,
    selectedMode: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        cameraModes.forEach { text ->
            TextButton(
                onClick = {
                    if (text != selectedMode) {
                        onSelectionChange(text)
                        onCameraModeTapped()
                    }
                }
            ) {
                Text(
                    text = text,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = if (text == selectedMode) {
                        Color.Red
                    } else {
                        Color.White
                    }
                )
            }
        }
    }
}

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
                        cameraLens = lens,
                        flashMode = flashMode
                    )
                )
            },
            modifier = Modifier.fillMaxSize(),
            update = {
                captureManager.updatePreview(
                    PreviewState(
                        cameraLens = lens,
                        flashMode = flashMode
                    ), it
                )
            }
        )
    }
}