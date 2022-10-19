package com.armutyus.cameraxproject.util

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.sharp.Cameraswitch
import androidx.compose.material.icons.sharp.Lens
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.armutyus.cameraxproject.R
import com.armutyus.cameraxproject.ui.camera.CameraUIAction

@Composable
fun CameraControls(cameraUIAction: (CameraUIAction) -> Unit) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        CameraControl(
            Icons.Filled.Circle,
            R.string.app_name,
            modifier = Modifier.size(48.dp),
            onClick = { cameraUIAction(CameraUIAction.OnGalleryViewClick) }
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
            Icons.Sharp.Cameraswitch,
            R.string.app_name,
            modifier = Modifier.size(48.dp),
            onClick = { cameraUIAction(CameraUIAction.OnSwitchCameraClick) }
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