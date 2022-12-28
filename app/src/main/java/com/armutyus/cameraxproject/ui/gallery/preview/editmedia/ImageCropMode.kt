package com.armutyus.cameraxproject.ui.gallery.preview.editmedia

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Brush
import androidx.compose.material.icons.sharp.Cancel
import androidx.compose.material.icons.sharp.Crop
import androidx.compose.material.icons.sharp.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import com.armutyus.cameraxproject.R
import com.armutyus.cameraxproject.ui.gallery.preview.editmedia.cropproperties.CropStyleSelectionMenu
import com.armutyus.cameraxproject.ui.gallery.preview.editmedia.cropproperties.PropertySelectionSheet
import com.smarttoolfactory.colorpicker.widget.drawChecker
import com.smarttoolfactory.cropper.ImageCropper
import com.smarttoolfactory.cropper.model.OutlineType
import com.smarttoolfactory.cropper.model.RectCropShape
import com.smarttoolfactory.cropper.settings.*
import kotlinx.coroutines.launch

internal enum class SelectionPage {
    Properties, Style
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ImageCropMode(
    editedImageBitmap: ImageBitmap,
    onCropCancelClicked: () -> Unit
) {

    val bottomSheetState =
        rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val coroutineScope = rememberCoroutineScope()
    val handleSize: Float = LocalDensity.current.run { 20.dp.toPx() }

    val defaultImage1 = ImageBitmap.imageResource(id = R.drawable.squircle)
    val defaultImage2 = ImageBitmap.imageResource(id = R.drawable.cloud)
    val defaultImage3 = ImageBitmap.imageResource(id = R.drawable.sun)
    val cropFrameFactory = remember {
        CropFrameFactory(
            listOf(
                defaultImage1,
                defaultImage2,
                defaultImage3
            )
        )
    }
    var cropProperties by remember(CropDefaults) {
        mutableStateOf(
            CropDefaults.properties(
                cropOutlineProperty = CropOutlineProperty(
                    OutlineType.Rect,
                    RectCropShape(0, "Rect")
                ),
                handleSize = handleSize
            )
        )
    }
    var cropStyle by remember { mutableStateOf(CropDefaults.style()) }
    var selectionPage by remember { mutableStateOf(SelectionPage.Properties) }

    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetElevation = 16.dp,
        sheetShape = RoundedCornerShape(
            bottomStart = 0.dp,
            bottomEnd = 0.dp,
            topStart = 28.dp,
            topEnd = 28.dp
        ),
        sheetContent = {
            if (selectionPage == SelectionPage.Properties) {
                PropertySelectionSheet(
                    cropFrameFactory = cropFrameFactory,
                    cropProperties = cropProperties,
                    onCropPropertiesChange = {
                        cropProperties = it
                    }
                )
            } else {
                CropStyleSelectionMenu(
                    cropType = cropProperties.cropType,
                    cropStyle = cropStyle,
                    onCropStyleChange = {
                        cropStyle = it
                    }
                )
            }
        },
    ) {
        MainContent(
            cropProperties = cropProperties,
            cropStyle = cropStyle,
            originalImageBitmap = editedImageBitmap,
            onCropCancelClicked = onCropCancelClicked,
        ) {
            selectionPage = it

            coroutineScope.launch {
                if (bottomSheetState.isVisible) {
                    bottomSheetState.hide()
                } else {
                    bottomSheetState.show()
                }
            }
        }
    }
}

@Composable
private fun MainContent(
    cropProperties: CropProperties,
    cropStyle: CropStyle,
    originalImageBitmap: ImageBitmap,
    onCropCancelClicked: () -> Unit,
    onSelectionPageMenuClicked: (SelectionPage) -> Unit
) {

    val imageBitmap by remember { mutableStateOf(originalImageBitmap) }
    var croppedImage by remember { mutableStateOf<ImageBitmap?>(null) }


    var crop by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var isCropping by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            ImageCropper(
                modifier = Modifier
                    .fillMaxWidth(),
                imageBitmap = imageBitmap,
                contentDescription = "Image Cropper",
                cropProperties = cropProperties,
                cropStyle = cropStyle,
                crop = crop,
                onCropStart = {
                    isCropping = true
                }
            ) {
                croppedImage = it
                isCropping = false
                crop = false
                showDialog = true
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                onClick = {
                    onCropCancelClicked()
                }
            ) {
                Icon(
                    Icons.Sharp.Cancel,
                    contentDescription = "Cancel Crop"
                )
            }
            IconButton(
                onClick = {
                    onSelectionPageMenuClicked(SelectionPage.Properties)
                }
            ) {
                Icon(
                    Icons.Sharp.Settings,
                    contentDescription = "Settings",
                )
            }
            IconButton(
                onClick = {
                    onSelectionPageMenuClicked(SelectionPage.Style)
                }
            ) {
                Icon(Icons.Sharp.Brush, contentDescription = "Style")
            }
            IconButton(
                onClick = {
                    crop = true
                }
            ) {
                Icon(
                    Icons.Sharp.Crop,
                    contentDescription = "Crop Image"
                )
            }
        }

        if (isCropping) {
            CircularProgressIndicator()
        }
    }

    if (showDialog) {
        croppedImage?.let {
            ShowCroppedImageDialog(imageBitmap = it) {
                showDialog = !showDialog
                croppedImage = null
            }
        }
    }
}

@Composable
private fun ShowCroppedImageDialog(imageBitmap: ImageBitmap, onDismissRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        text = {
            Image(
                modifier = Modifier
                    .drawChecker(RoundedCornerShape(8.dp))
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentScale = ContentScale.Fit,
                bitmap = imageBitmap,
                contentDescription = "result"
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("Dismiss")
            }
        }
    )
}