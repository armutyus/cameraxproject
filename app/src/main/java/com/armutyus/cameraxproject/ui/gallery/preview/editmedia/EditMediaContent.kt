package com.armutyus.cameraxproject.ui.gallery.preview.editmedia

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ArrowBack
import androidx.compose.material.icons.sharp.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.armutyus.cameraxproject.R
import com.armutyus.cameraxproject.ui.gallery.preview.editmedia.models.ImageFilter
import jp.co.cyberagent.android.gpuimage.GPUImage

@Composable
fun EditMediaContent(
    originalImageBitmap: Bitmap,
    filteredImageBitmap: Bitmap,
    imageFilters: List<ImageFilter>,
    gpuImage: GPUImage,
    setFilteredBitmap: (Bitmap) -> Unit,
    selectedFilter: (String) -> Unit,
    hasFilteredImage: Boolean,
    cancelEditMode: () -> Unit,
    onSaveTapped: () -> Unit
) {

    gpuImage.setImage(originalImageBitmap)

    var isBackTapped by remember { mutableStateOf(false) }

    BackHandler {
        if (hasFilteredImage) {
            isBackTapped = true
        } else {
            cancelEditMode()
        }
    }

    if (isBackTapped) {
        AlertDialog(
            onDismissRequest = { /* */ },
            text = { Text(text = stringResource(id = R.string.confirm_changes)) },
            confirmButton = {
                Button(onClick = {
                    onSaveTapped()
                    cancelEditMode()
                    isBackTapped = false
                }) {
                    Text(text = stringResource(id = R.string.save_changes))
                }
            },
            dismissButton = {
                Button(onClick = {
                    cancelEditMode()
                    isBackTapped = false
                }
                ) {
                    Text(text = stringResource(id = R.string.deny))
                }
            }
        )
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            EditMediaTopContent(
                navigateBack = {
                    if (hasFilteredImage) {
                        isBackTapped = true
                    } else {
                        cancelEditMode()
                    }
                },
                onSaveTapped = onSaveTapped
            )
            EditMediaMidContent(imageBitmap = filteredImageBitmap)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            EditMediaBottomContent(
                imageFilters = imageFilters,
                gpuImage = gpuImage,
                setFilteredBitmap = { setFilteredBitmap(it) },
                selectedFilter = { selectedFilter(it) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditMediaTopContent(
    navigateBack: () -> Unit,
    onSaveTapped: () -> Unit
) {
    AnimatedVisibility(
        modifier = Modifier,
        visible = true,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        TopAppBar(
            modifier = Modifier.fillMaxWidth(),
            navigationIcon = {
                IconButton(onClick = { navigateBack() }) {
                    Icon(
                        imageVector = Icons.Sharp.ArrowBack,
                        contentDescription = stringResource(id = R.string.cancel)
                    )
                }
            },
            title = { Text(text = stringResource(id = R.string.edit), fontSize = 18.sp) },
            actions = {
                IconButton(onClick = { onSaveTapped() }) {
                    Icon(
                        imageVector = Icons.Sharp.CheckCircle,
                        contentDescription = stringResource(id = R.string.save)
                    )
                }
            }
        )
    }
}

@Composable
private fun EditMediaMidContent(
    imageBitmap: Bitmap
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clip(RectangleShape)
    ) {
        SubcomposeAsyncImage(
            model = imageBitmap,
            modifier = Modifier
                .fillMaxSize(),
            alignment = Alignment.TopCenter,
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.High,
            contentDescription = ""
        ) {
            val painterState = painter.state
            if (painterState is AsyncImagePainter.State.Loading || painterState is AsyncImagePainter.State.Error) {
                CircularProgressIndicator(Modifier.requiredSize(32.dp))
            } else {
                SubcomposeAsyncImageContent()
            }
            println("PainterState: $painterState")
        }
    }
}

@Composable
private fun EditMediaBottomContent(
    imageFilters: List<ImageFilter>,
    gpuImage: GPUImage,
    setFilteredBitmap: (Bitmap) -> Unit,
    selectedFilter: (String) -> Unit
) {
    val listState = rememberLazyListState()
    Box {
        LazyRow(state = listState) {
            items(imageFilters) { imageFilter ->
                ImageWithFilter(
                    image = imageFilter.filterPreview,
                    filterName = imageFilter.name
                ) {
                    with(imageFilter) {
                        gpuImage.setFilter(filter)
                        setFilteredBitmap(gpuImage.bitmapWithFilterApplied)
                        selectedFilter(imageFilter.name)
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageWithFilter(
    image: Bitmap,
    filterName: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(90.dp)
            .height(160.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(6.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = true) {
                onClick.invoke()
            }
    ) {

        SubcomposeAsyncImage(
            model = image,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize(),
            alignment = Alignment.Center,
            contentScale = ContentScale.FillBounds,
            filterQuality = FilterQuality.Medium,
            contentDescription = ""
        ) {
            val painterState = painter.state
            if (painterState is AsyncImagePainter.State.Loading || painterState is AsyncImagePainter.State.Error) {
                CircularProgressIndicator(Modifier.requiredSize(8.dp))
            } else {
                SubcomposeAsyncImageContent()
            }
            println("PainterState: $painterState")
        }
        Text(
            text = filterName,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .align(Alignment.BottomCenter),
            textAlign = TextAlign.Center,
            fontSize = 12.sp
        )
    }
}