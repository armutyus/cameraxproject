package com.armutyus.cameraxproject.ui.camera

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.armutyus.cameraxproject.ui.theme.CameraXProjectTheme
import com.armutyus.cameraxproject.util.Util.Companion.TAG

@Composable
fun CameraScreen(navController: NavController) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
    ) {
        CameraContent(navController, Modifier.fillMaxSize())
    }
}

@Composable
fun CameraContent(navController: NavController, modifier: Modifier = Modifier) {
    val emptyImageUri = Uri.parse("file://dev/null")
    var imageUri by remember { mutableStateOf(emptyImageUri) }
    CameraCapture(
        imageUri,
        navController,
        onImageCaptured = { uri, _ ->
            Log.d(TAG, "Image captured successfully")
            imageUri = uri
        }
    ) { imageCaptureException ->
        Log.e(TAG, imageCaptureException.localizedMessage ?: "Image capture failed")
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DefaultPreview() {
    val context = LocalContext.current
    CameraXProjectTheme {
        CameraScreen(navController = NavController(context))
    }
}