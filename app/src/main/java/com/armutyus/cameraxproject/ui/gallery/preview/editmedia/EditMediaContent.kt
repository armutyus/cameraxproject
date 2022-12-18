package com.armutyus.cameraxproject.ui.gallery.preview.editmedia

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.armutyus.cameraxproject.ui.gallery.preview.editmedia.models.ImageFilter
import jp.co.cyberagent.android.gpuimage.GPUImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun EditMediaContent(
    originalImageBitmap: Bitmap,
    filteredImageBitmap: Bitmap,
    imageFilters: List<ImageFilter>,
    gpuImage: GPUImage,
    setFilteredBitmap: (Bitmap) -> Unit,
    selectedFilter: (String) -> Unit
) {

    gpuImage.setImage(originalImageBitmap)

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            EditMediaMidContent(imageBitmap = filteredImageBitmap)
        }
        Column(
            modifier = Modifier
                .wrapContentSize(Alignment.BottomCenter),
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

@Composable
private fun EditMediaTopContent(

) {

}

@Composable
private fun EditMediaMidContent(
    imageBitmap: Bitmap
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(6.dp)
            .clip(RectangleShape)
    ) {
        SubcomposeAsyncImage(
            model = imageBitmap,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize(),
            alignment = Alignment.Center,
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.High,
            contentDescription = ""
        ) {
            val painterState = painter.state
            if (painterState is AsyncImagePainter.State.Loading || painterState is AsyncImagePainter.State.Error) {
                CircularProgressIndicator(Modifier.size(8.dp))
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

        val context = LocalContext.current
        var bitmap by remember { mutableStateOf<Bitmap?>(null) }

        LaunchedEffect(Unit) {
            CoroutineScope(Dispatchers.IO).launch {
                val loader = context.imageLoader
                val request = ImageRequest.Builder(context)
                    .data(image)
                    .build()
                val result = (loader.execute(request) as SuccessResult).drawable
                bitmap = (result as BitmapDrawable).bitmap
            }
        }

        SubcomposeAsyncImage(
            model = bitmap,
            modifier = Modifier
                .align(Alignment.Center)
                .height(160.dp)
                .width(90.dp),
            alignment = Alignment.Center,
            contentScale = ContentScale.FillBounds,
            contentDescription = ""
        ) {
            val painterState = painter.state
            if (painterState is AsyncImagePainter.State.Loading || painterState is AsyncImagePainter.State.Error) {
                CircularProgressIndicator(Modifier.size(8.dp))
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