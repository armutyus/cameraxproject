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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.armutyus.cameraxproject.R

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
                .border(width = 1.dp, color = Color.White, shape = CircleShape)
                .clickable { cameraUIAction(CameraUIAction.OnGalleryViewClick) },
            painter = rememberAsyncImagePainter(imageUri),
            contentDescription = "Latest captured image",
            contentScale = ContentScale.Crop
        )

        /*AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUri)
                .crossfade(true)
                .build(),
            contentDescription = "Latest captured image",
            imageLoader = ImageLoader(context),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .border(width = 1.dp, color = Color.White, shape = CircleShape)
                .clickable { cameraUIAction(CameraUIAction.OnGalleryViewClick) }
        )

        CameraControl(
            Icons.Sharp.Circle,
            R.string.app_name,
            modifier = Modifier.size(48.dp),
            onClick = { cameraUIAction(CameraUIAction.OnGalleryViewClick) }
        )*/

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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { /*TODO*/ }) {
            Icon(
                imageVector = Icons.Sharp.Timer,
                contentDescription = "Go to settings"
            )
        }
        IconButton(onClick = { /*TODO*/ }) {
            Icon(
                imageVector = Icons.Sharp.FlashOn,
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