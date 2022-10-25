package com.armutyus.cameraxproject.util

import android.net.Uri
import android.text.format.DateUtils
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.camera.core.ImageCapture
import androidx.camera.core.TorchState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

@Composable
fun CameraPreviewIcon(modifier: Modifier, imageUri: Uri, onTapped: () -> Unit) {
    IconButton(
        modifier = Modifier
            .then(modifier),
        onClick = {
            onTapped()
        },
        content = {
            Image(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(width = 1.dp, color = Color.White, shape = CircleShape),
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(data = imageUri)
                        .build(),
                    filterQuality = FilterQuality.Medium
                ),
                contentDescription = "Latest captured image",
                contentScale = ContentScale.Crop
            )
        }
    )
}

@Composable
fun CameraCaptureIcon(modifier: Modifier, view: View, onTapped: () -> Unit) {
    IconButton(
        modifier = Modifier
            .size(64.dp)
            .padding(1.dp)
            .border(1.dp, Color.White, CircleShape)
            .then(modifier),
        onClick = {
            view.vibrate(HapticFeedbackConstants.LONG_PRESS)
            onTapped()
        },
        content = {
            Icon(
                imageVector = Icons.Sharp.Lens,
                contentDescription = "Pause video"
            )
        }
    )
}

@Composable
fun CameraPauseIcon(modifier: Modifier = Modifier, onTapped: () -> Unit) {
    IconButton(
        modifier = Modifier.then(modifier),
        onClick = { onTapped() },
        content = {
            Icon(
                modifier = Modifier.size(64.dp),
                imageVector = Icons.Sharp.PauseCircle,
                contentDescription = "Pause video"
            )
        }
    )
}


@Composable
fun CameraPlayIcon(modifier: Modifier = Modifier, onTapped: () -> Unit) {
    IconButton(
        modifier = Modifier
            .then(modifier),
        onClick = { onTapped() },
        content = {
            Icon(
                modifier = Modifier.size(64.dp),
                imageVector = Icons.Sharp.PlayCircle,
                contentDescription = "Play video"
            )
        }
    )
}

@Composable
fun CameraPauseIconSmall(modifier: Modifier = Modifier, onTapped: () -> Unit) {
    IconButton(
        modifier = Modifier.then(modifier),
        onClick = { onTapped() },
        content = {
            Icon(
                imageVector = Icons.Sharp.PauseCircle,
                contentDescription = "Pause recording"
            )
        }
    )
}


@Composable
fun CameraPlayIconSmall(modifier: Modifier = Modifier, onTapped: () -> Unit) {
    IconButton(
        modifier = Modifier
            .then(modifier),
        onClick = { onTapped() },
        content = {
            Icon(
                imageVector = Icons.Sharp.PlayCircle,
                contentDescription = "Resume recording"
            )
        }
    )
}

@Composable
fun CameraRecordIcon(modifier: Modifier = Modifier, view: View, onTapped: () -> Unit) {
    IconButton(
        modifier = Modifier
            .then(modifier),
        onClick = {
            view.vibrate(HapticFeedbackConstants.LONG_PRESS)
            onTapped()
        },
        content = {
            Icon(
                modifier = Modifier.size(64.dp),
                imageVector = Icons.Sharp.ModeStandby,
                contentDescription = "Start recording"
            )
        })
}

@Composable
fun CameraStopIcon(modifier: Modifier = Modifier, view: View, onTapped: () -> Unit) {
    IconButton(
        modifier = Modifier
            .then(modifier),
        onClick = {
            view.vibrate(HapticFeedbackConstants.LONG_PRESS)
            onTapped()
        },
        content = {
            Icon(
                modifier = Modifier.size(64.dp),
                imageVector = Icons.Sharp.StopCircle,
                contentDescription = "Stop recording"
            )
        }
    )
}

@Composable
fun CameraFlipIcon(modifier: Modifier = Modifier, view: View, onTapped: () -> Unit) {
    IconButton(
        modifier = Modifier
            .then(modifier),
        onClick = {
            view.vibrate(HapticFeedbackConstants.LONG_PRESS)
            onTapped()
                  },
        content = {
            Icon(
                modifier = Modifier.size(64.dp),
                imageVector = Icons.Sharp.FlipCameraAndroid,
                contentDescription = "Flip camera"
            )
        }
    )
}

@Composable
fun CameraTorchIcon(modifier: Modifier = Modifier, @TorchState.State torchState: Int, onTapped: () -> Unit) {
    IconButton(
        modifier = Modifier
            .then(modifier),
        onClick = { onTapped() },
        content = {
            Icon(
                imageVector = if (torchState == TorchState.ON) {
                    Icons.Sharp.FlashOff
                } else {
                    Icons.Sharp.FlashOn
                },
                contentDescription = "Change flash settings"
            )
        }
    )
}

@Composable
fun CameraFlashIcon(modifier: Modifier = Modifier, @ImageCapture.FlashMode flashMode: Int, onTapped: () -> Unit) {
    IconButton(
        modifier = Modifier
            .then(modifier),
        onClick = { onTapped() },
        content = {
            Icon(
                imageVector = when(flashMode) {
                    ImageCapture.FLASH_MODE_AUTO -> Icons.Sharp.FlashAuto
                    ImageCapture.FLASH_MODE_OFF -> Icons.Sharp.FlashOff
                    ImageCapture.FLASH_MODE_ON -> Icons.Sharp.FlashOn
                    else -> Icons.Sharp.FlashOff
                },
                contentDescription = "Change flash settings"
            )
        }
    )
}

@Composable
fun Timer(modifier: Modifier = Modifier, seconds: Int) {
    if (seconds > 0) {
        Box(modifier = Modifier
            .padding(vertical = 24.dp)
            .then(modifier)) {
            Text(
                text = DateUtils.formatElapsedTime(seconds.toLong()),
                color = Color.White,
                modifier = Modifier
                    .background(color = Color.Red)
                    .padding(horizontal = 10.dp)
                    .then(modifier)
            )
        }
    }
}