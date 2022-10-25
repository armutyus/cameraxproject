package com.armutyus.cameraxproject.ui.camera

import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
import androidx.camera.core.ImageCaptureException
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.armutyus.cameraxproject.util.Util
import com.armutyus.cameraxproject.util.Util.Companion.TAG
import com.armutyus.cameraxproject.util.aspectRatio
import com.armutyus.cameraxproject.util.getOutputDirectory
import com.armutyus.cameraxproject.util.takePicture
import kotlinx.coroutines.launch

@Composable
fun CameraCapture(
    imageUri: Uri,
    navController: NavController,
    onImageCaptured: (Uri, Boolean) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Get screen metrics used to setup camera for full screen resolution
    val metrics = Util.ScreenSizeCompat.getScreenSize(context)
    Log.d(TAG, "Screen metrics: ${metrics.width} x ${metrics.height}")
    val screenAspectRatio = aspectRatio(metrics.width, metrics.height)
    Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

    val configuration = LocalConfiguration.current
    val rotation = configuration.orientation
    var rotationState by remember { mutableStateOf(Configuration.ORIENTATION_PORTRAIT) }
    Log.d(TAG,"Current orientation: $rotationState")
    // If our configuration changes then this will launch a new coroutine scope for it
    LaunchedEffect(configuration) {
        // Save any changes to the orientation value on the configuration object
        snapshotFlow { rotation }
            .collect { rotationState = it }
    }

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    val imageCapture: ImageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotationState)
            .build()
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) onImageCaptured(uri, true)
    }

    CameraPreviewView(
        imageUri,
        imageCapture,
        lensFacing,
        navController,
        rotationState,
        screenAspectRatio,
    ) { cameraUIAction ->
        when (cameraUIAction) {
            is CameraUIAction.OnCameraClick -> {
                scope.launch {
                    imageCapture.takePicture(context, lensFacing, onError)?.let {
                        onImageCaptured(it, false)
                    }
                }
            }
            is CameraUIAction.OnSwitchCameraClick -> {
                lensFacing =
                    if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT
                    else
                        CameraSelector.LENS_FACING_BACK
            }
            is CameraUIAction.OnGalleryViewClick -> {
                if (true == context.getOutputDirectory("Photo").listFiles()?.isNotEmpty()) {
                    galleryLauncher.launch("image/*")
                }
            }
        }
    }
}