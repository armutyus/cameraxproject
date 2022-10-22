package com.armutyus.cameraxproject.ui.camera

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.armutyus.cameraxproject.R
import com.armutyus.cameraxproject.util.Util.Companion.FLASH_AUTO
import com.armutyus.cameraxproject.util.Util.Companion.FLASH_OFF
import com.armutyus.cameraxproject.util.Util.Companion.FLASH_ON
import com.armutyus.cameraxproject.util.Util.Companion.TIMER_10S
import com.armutyus.cameraxproject.util.Util.Companion.TIMER_3S
import com.armutyus.cameraxproject.util.Util.Companion.TIMER_OFF

@Composable
fun CameraControls(imageUri: Uri, cameraUIAction: (CameraUIAction) -> Unit) {

    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Image(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .border(width = 1.dp, color = Color.White, shape = CircleShape)
                .clickable { cameraUIAction(CameraUIAction.OnGalleryViewClick) },
            painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(data = imageUri)
                    .build(),
                filterQuality = FilterQuality.Medium),
            contentDescription = "Latest captured image",
            contentScale = ContentScale.FillBounds
        )

        CameraControl(
            Icons.Sharp.Lens,
            R.string.app_name,
            modifier = Modifier
                .size(64.dp)
                .padding(1.dp)
                .border(1.dp, Color.White, CircleShape),
            onClick = { cameraUIAction(CameraUIAction.OnCameraClick) }
        )

        CameraControl(
            Icons.Sharp.FlipCameraAndroid,
            R.string.app_name,
            modifier = Modifier.size(48.dp),
            onClick = {
                cameraUIAction(CameraUIAction.OnSwitchCameraClick)
            }
        )

    }
}


@Composable
fun CameraControl(
    imageVector: ImageVector,
    contentDescId: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector,
            contentDescription = stringResource(id = contentDescId),
            modifier = modifier,
            tint = Color.White
        )
    }
}

@Composable
fun TopAppBarActionButtonsRow(navController: NavController) {

    var timerIconState by remember { mutableStateOf(TIMER_OFF) }
    var flashIconState by remember { mutableStateOf(FLASH_OFF) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(width = 0.5.dp, shape = CircleShape, color = Color.White),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {
            if (timerIconState == TIMER_OFF) {
                timerIconState = TIMER_3S
            } else if (timerIconState == TIMER_3S) {
                timerIconState = TIMER_10S
            } else {
                timerIconState = TIMER_OFF
            }
        }) {
            Icon(
                imageVector = when (timerIconState) {
                    TIMER_OFF -> Icons.Sharp.TimerOff
                    TIMER_3S -> Icons.Sharp.Timer3
                    TIMER_10S -> Icons.Sharp.Timer10
                    else -> Icons.Sharp.TimerOff
                                                    },
                contentDescription = "Go to settings"
            )
        }
        IconButton(onClick = {
            if (flashIconState == FLASH_OFF) {
                flashIconState = FLASH_ON
            } else if (flashIconState == FLASH_ON) {
                flashIconState = FLASH_AUTO
            } else {
                flashIconState = FLASH_OFF
            }
        }) {
            Icon(
                imageVector = when (flashIconState) {
                    FLASH_OFF -> Icons.Sharp.FlashOff
                    FLASH_ON -> Icons.Sharp.FlashOn
                    FLASH_AUTO -> Icons.Sharp.FlashAuto
                    else -> Icons.Sharp.FlashOff
                },
                contentDescription = "Change flash settings"
            )
        }
        IconButton(onClick = { /*TODO*/ }) {
            Icon(
                imageVector = Icons.Sharp.AutoFixHigh,
                contentDescription = "Go to settings"
            )
        }
        IconButton(onClick = { /*TODO*/ }) {
            Icon(
                imageVector = Icons.Sharp.Settings,
                contentDescription = "Go to settings"
            )
        }
    }
}