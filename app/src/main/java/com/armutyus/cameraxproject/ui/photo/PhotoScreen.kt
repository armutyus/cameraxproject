package com.armutyus.cameraxproject.ui.photo

import android.view.View
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.armutyus.cameraxproject.util.*

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
                is PhotoViewModel.Effect.CaptureImage -> photoCaptureManager.takePhoto(it.filePath, state.lens ?: CameraSelector.LENS_FACING_BACK)
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
    view: View,
    onEvent: (PhotoViewModel.Event) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        cameraLens?.let {
            CameraPreview(
                lens = it,
                flashMode = flashMode
            )
            TopControls(
                modifier = Modifier.align(Alignment.TopStart),
                showFlashIcon = hasFlashUnit,
                view = view,
                flashMode = flashMode,
                delayTimer = delayTimer,
                onDelayTimerTapped = { onEvent(PhotoViewModel.Event.DelayTimerTapped) },
                onFlashTapped =  { onEvent(PhotoViewModel.Event.FlashTapped) },
                onSettingsTapped = { onEvent(PhotoViewModel.Event.SettingsTapped) }
            )
            BottomControls(
                modifier = Modifier.align(Alignment.BottomStart),
                showFlipIcon = hasDualCamera,
                view = view,
                onCaptureTapped = { onEvent(PhotoViewModel.Event.CaptureTapped) },
                onFlipTapped = { onEvent(PhotoViewModel.Event.FlipTapped) }
            )
        }
    }
}

@Composable
internal fun TopControls(
    modifier: Modifier = Modifier,
    showFlashIcon: Boolean,
    delayTimer: Int,
    flashMode: Int,
    view: View,
    onDelayTimerTapped: () -> Unit,
    onFlashTapped: () -> Unit,
    onSettingsTapped: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(8.dp)
            .then(modifier)
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
            CameraFlashIcon(showFlashIcon = showFlashIcon, flashMode = flashMode, onTapped = onFlashTapped)
            CameraEditIcon{}
            SettingsIcon(onTapped = onSettingsTapped)
        }
    }
}

@Composable
internal fun BottomControls(
    modifier: Modifier = Modifier,
    showFlipIcon: Boolean,
    onCaptureTapped: () -> Unit,
    view: View,
    onFlipTapped: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 24.dp)
            .then(modifier)
    ) {
        CameraCaptureIcon(modifier = Modifier.align(Alignment.Center), view = view, onTapped = onCaptureTapped)

        if (showFlipIcon) {
            CameraFlipIcon(modifier = Modifier.align(Alignment.CenterEnd), view = view, onTapped = onFlipTapped)
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