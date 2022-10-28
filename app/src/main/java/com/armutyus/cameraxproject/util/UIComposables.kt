package com.armutyus.cameraxproject.util

import android.net.Uri
import android.os.CountDownTimer
import android.text.format.DateUtils
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.camera.core.ImageCapture
import androidx.camera.core.TorchState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.armutyus.cameraxproject.R
import com.armutyus.cameraxproject.util.Util.Companion.TAG
import com.armutyus.cameraxproject.util.Util.Companion.TIMER_10S
import com.armutyus.cameraxproject.util.Util.Companion.TIMER_3S
import com.armutyus.cameraxproject.util.Util.Companion.TIMER_OFF

@Preview
@Composable
fun DefaultPreview() {
    CameraCaptureIcon(modifier = Modifier, view = LocalView.current) {

    }
}

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
                contentDescription = stringResource(id = R.string.latest_image),
                contentScale = ContentScale.Crop
            )
        }
    )
}

@Composable
fun CameraCaptureIcon(modifier: Modifier = Modifier, view: View, onTapped: () -> Unit) {
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
                modifier = modifier.size(60.dp),
                imageVector = Icons.Sharp.Lens,
                contentDescription = stringResource(id = R.string.capture_image)
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
                contentDescription = stringResource(id = R.string.pause_video)
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
                contentDescription = stringResource(id = R.string.play_video)
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
                contentDescription = stringResource(id = R.string.pause_recording)
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
                contentDescription = stringResource(id = R.string.resume_recording)
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
                contentDescription = stringResource(id = R.string.start_recording)
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
                contentDescription = stringResource(id = R.string.stop_recording)
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
                contentDescription = stringResource(id = R.string.flip_camera)
            )
        }
    )
}

@Composable
fun CameraTorchIcon(
    modifier: Modifier = Modifier,
    @TorchState.State torchState: Int,
    onTapped: () -> Unit
) {
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
                contentDescription = stringResource(id = R.string.change_flash_settings)
            )
        }
    )
}

@Composable
fun CameraDelayIcon(
    modifier: Modifier = Modifier,
    delayTimer: Int,
    onTapped: () -> Unit
) {
    IconButton(
        modifier = Modifier
            .then(modifier),
        onClick = { onTapped() },
        content = {
            Icon(
                imageVector = when (delayTimer) {
                    TIMER_OFF -> Icons.Sharp.TimerOff
                    TIMER_3S -> Icons.Sharp.Timer3
                    TIMER_10S -> Icons.Sharp.Timer10
                    else -> Icons.Sharp.TimerOff
                },
                contentDescription = stringResource(id = R.string.delay_settings)
            )
        }
    )
}

@Composable
fun CameraFlashIcon(
    modifier: Modifier = Modifier,
    showFlashIcon: Boolean,
    @ImageCapture.FlashMode flashMode: Int,
    onTapped: () -> Unit
) {
    IconButton(
        modifier = Modifier
            .then(modifier),
        onClick = { onTapped() },
        enabled = showFlashIcon,
        content = {
            Icon(
                imageVector = when (flashMode) {
                    ImageCapture.FLASH_MODE_AUTO -> Icons.Sharp.FlashAuto
                    ImageCapture.FLASH_MODE_OFF -> Icons.Sharp.FlashOff
                    ImageCapture.FLASH_MODE_ON -> Icons.Sharp.FlashOn
                    else -> Icons.Sharp.FlashOff
                },
                contentDescription = stringResource(id = R.string.change_flash_settings)
            )
        }
    )
}

@Composable
fun CameraEditIcon(modifier: Modifier = Modifier, onTapped: () -> Unit) {
    IconButton(
        modifier = Modifier
            .then(modifier),
        onClick = { },
        content = {
            Icon(
                imageVector = Icons.Sharp.AutoFixHigh,
                contentDescription = stringResource(id = R.string.add_filter)
            )
        }
    )
}

@Composable
fun SettingsIcon(modifier: Modifier = Modifier, onTapped: () -> Unit) {
    IconButton(
        modifier = Modifier
            .then(modifier),
        onClick = { },
        content = {
            Icon(
                imageVector = Icons.Sharp.Settings,
                contentDescription = stringResource(id = R.string.go_settings)
            )
        }
    )
}

@Composable
fun Timer(modifier: Modifier = Modifier, seconds: Int) {
    if (seconds > 0) {
        Box(
            modifier = Modifier
                .padding(vertical = 24.dp)
                .then(modifier)
        ) {
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

@Composable
fun DelayTimer(millisInFuture: Long, captureImage: () -> Unit) {
    val timeData = remember {
        mutableStateOf(millisInFuture)
    }

    val countDownTimer =
        object : CountDownTimer(millisInFuture, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                Log.d(TAG, "onTick: ")
                timeData.value = millisUntilFinished
            }

            override fun onFinish() {
                captureImage()
            }
        }

    DisposableEffect(key1 = "key") {
        countDownTimer.start()
        onDispose {
            countDownTimer.cancel()
        }
    }

    Text (
        text = timeData.value.toInt().toString(),
        textAlign = TextAlign.Center
    )
}