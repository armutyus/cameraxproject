package com.armutyus.cameraxproject.ui.gallery.preview

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toFile
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.armutyus.cameraxproject.R
import com.armutyus.cameraxproject.ui.gallery.GalleryViewModel
import com.armutyus.cameraxproject.ui.gallery.models.BottomNavItem
import com.armutyus.cameraxproject.ui.gallery.models.MediaItem
import com.armutyus.cameraxproject.ui.gallery.preview.editmedia.EditMediaContent
import com.armutyus.cameraxproject.ui.gallery.preview.models.PreviewScreenEvent
import com.armutyus.cameraxproject.ui.gallery.preview.videoplayback.VideoPlaybackContent
import com.armutyus.cameraxproject.util.toBitmap
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import jp.co.cyberagent.android.gpuimage.GPUImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@UnstableApi
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
@Composable
fun PreviewScreen(
    filePath: String,
    factory: ViewModelProvider.Factory,
    previewViewModel: PreviewViewModel = viewModel(factory = factory),
    galleryViewModel: GalleryViewModel = viewModel(factory = factory)
) {
    DisposableEffect(Unit) {
        galleryViewModel.loadMedia()
        onDispose {
            galleryViewModel.loadMedia().cancel()
        }
    }
    val context = LocalContext.current
    val media by galleryViewModel.mediaItems.observeAsState(mapOf())
    val state by previewViewModel.previewScreenState.observeAsState()
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var rotationState by remember { mutableStateOf(0f) }
    var zoomState by remember { mutableStateOf(false) }

    val currentFile = Uri.parse(filePath).toFile()
    val fileName = currentFile.nameWithoutExtension
    val takenDate = fileName.substring(0, 10).replace("-", "/")
    val takenTime = fileName.substring(11, 16).replace("-", ":")

    val bottomNavItems = listOf(
        BottomNavItem.Share,
        BottomNavItem.EditItem,
        BottomNavItem.Delete
    )

    Scaffold(
        topBar = {
            AnimatedVisibility(
                modifier = Modifier,
                visible = state!!.showBars,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { previewViewModel.onEvent(PreviewScreenEvent.NavigateBack) }) {
                            Icon(
                                imageVector = Icons.Sharp.ArrowBack,
                                contentDescription = stringResource(id = R.string.cancel)
                            )
                        }
                    },
                    title = {
                        Column(
                            verticalArrangement = Arrangement.SpaceEvenly,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(text = takenDate, fontSize = 18.sp)
                            Text(text = takenTime, fontSize = 14.sp)
                        }
                    }
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                modifier = Modifier,
                visible = state!!.showBars,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                NavigationBar {
                    bottomNavItems.forEach { bottomNavItem ->
                        NavigationBarItem(
                            selected = false,
                            icon = {
                                Icon(
                                    imageVector = bottomNavItem.icon,
                                    contentDescription = stringResource(id = bottomNavItem.label)
                                )
                            },
                            label = {
                                Text(text = stringResource(id = bottomNavItem.label))
                            },
                            alwaysShowLabel = true,
                            onClick = {
                                when (bottomNavItem) {
                                    BottomNavItem.Share -> {
                                        previewViewModel.onEvent(
                                            PreviewScreenEvent.ShareTapped(
                                                context,
                                                currentFile
                                            )
                                        )
                                    }
                                    BottomNavItem.EditItem -> {
                                        previewViewModel.onEvent(PreviewScreenEvent.EditTapped)
                                    }
                                    BottomNavItem.Delete -> {
                                        previewViewModel.onEvent(
                                            PreviewScreenEvent.DeleteTapped(
                                                currentFile
                                            )
                                        )
                                    }
                                    else -> {}
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val currentList = media.values.flatten()
            val count = currentList.size
            val initialItem =
                currentList.firstOrNull { mediaItem -> mediaItem.name == currentFile.name }
            val initialItemIndex by remember { mutableStateOf(currentList.indexOf(initialItem)) }
            val pagerState = rememberPagerState(initialItemIndex)

            HorizontalPager(
                modifier = Modifier.fillMaxSize(),
                count = count,
                state = pagerState,
                userScrollEnabled = !zoomState,
                itemSpacing = 16.dp
            ) { page ->
                Box(
                    modifier = Modifier
                        .clip(RectangleShape)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    offset.getDistance()
                                    if (scale >= 2f) {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    } else {
                                        scale = 3f
                                        offsetX -= offset.x
                                        offsetY -= offset.y
                                        rotationState = 0f
                                        zoomState = false
                                        println("x: ${offset.x}")
                                        println("y: ${offset.y}")
                                    }
                                },
                                onTap = {
                                    if (!zoomState && state?.isInEditMode == false) previewViewModel.onEvent(
                                        PreviewScreenEvent.ChangeBarState(zoomState)
                                    )
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown()
                                do {
                                    val event = awaitPointerEvent()
                                    scale *= event.calculateZoom()
                                    if (scale > 1) {
                                        val offset = event.calculatePan()
                                        offsetX += offset.x
                                        offsetY += offset.y
                                        rotationState += event.calculateRotation()
                                        zoomState = true
                                        previewViewModel.onEvent(
                                            PreviewScreenEvent.ChangeBarState(
                                                zoomState
                                            )
                                        )
                                    } else {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                        rotationState = 0f
                                        zoomState = false
                                    }
                                } while (event.changes.any { pointerInputChange -> pointerInputChange.pressed })
                            }
                        }
                ) {
                    when (currentList[page].type) {
                        MediaItem.Type.PHOTO -> {
                            if (state?.isInEditMode == true) {
                                zoomState = true
                                val gpuImage = GPUImage(context)

                                var originalImageBitmap by remember { mutableStateOf<Bitmap?>(null) }

                                LaunchedEffect(currentList[page]) {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        originalImageBitmap =
                                            currentList[page].uri!!.toBitmap(context)
                                        previewViewModel.loadImageFilters(originalImageBitmap)
                                    }
                                }

                                val imageFilters by previewViewModel.imageFilterList.observeAsState()
                                val filteredImageBitmap by previewViewModel.filteredBitmap.observeAsState()

                                originalImageBitmap?.let { bitmap ->
                                    EditMediaContent(
                                        originalImageBitmap = bitmap,
                                        filteredImageBitmap = filteredImageBitmap ?: bitmap,
                                        imageFilters = imageFilters ?: emptyList(),
                                        gpuImage = gpuImage,
                                        setFilteredBitmap = { previewViewModel.setFilteredBitmap(it) },
                                        selectedFilter = { previewViewModel.selectedFilter(it) }
                                    )
                                }
                            } else {
                                SubcomposeAsyncImage(
                                    model = currentList[page].uri,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .graphicsLayer(
                                            scaleX = maxOf(1f, minOf(3f, scale)),
                                            scaleY = maxOf(1f, minOf(3f, scale)),
                                            rotationZ = rotationState,
                                            translationX = offsetX,
                                            translationY = offsetY
                                        ),
                                    filterQuality = FilterQuality.High,
                                    contentDescription = ""
                                ) {
                                    val painterState = painter.state
                                    if (painterState is AsyncImagePainter.State.Loading || painterState is AsyncImagePainter.State.Error) {
                                        LinearProgressIndicator()
                                    } else {
                                        SubcomposeAsyncImageContent()
                                    }
                                }
                            }
                        }
                        MediaItem.Type.VIDEO -> {
                            VideoPlaybackContent(
                                currentList[page].uri,
                                state!!.isFullScreen,
                                state!!.showMediaController,
                                {
                                    previewViewModel.onEvent(
                                        PreviewScreenEvent.FullScreenToggleTapped(
                                            state!!.isFullScreen
                                        )
                                    )
                                },
                                { previewViewModel.onEvent(PreviewScreenEvent.HideController(it)) },
                                { previewViewModel.onEvent(PreviewScreenEvent.PlayerViewTapped) },
                                { previewViewModel.onEvent(PreviewScreenEvent.NavigateBack) }
                            )
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}