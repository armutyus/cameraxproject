package com.armutyus.cameraxproject.util

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


@Composable
fun CaptureButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = { },
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val color =
        if (isPressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    val contentPadding = PaddingValues(if (isPressed) 10.dp else 12.dp)
    OutlinedButton(
        modifier = modifier,
        shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        contentPadding = contentPadding,
        onClick = { /* GNDN */ },
        enabled = false
    ) {
        Button(
            modifier = Modifier
                .fillMaxSize(),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(color),
            interactionSource = interactionSource,
            onClick = onClick
        ) {
            // No content
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true)
@Composable
fun PreviewCapturePictureButton() {
    Scaffold(
        modifier = Modifier
            .size(125.dp)
            .wrapContentSize()
    ) { innerPadding ->
        CaptureButton(
            modifier = Modifier
                .padding(innerPadding)
                .size(100.dp)
        )
    }
}