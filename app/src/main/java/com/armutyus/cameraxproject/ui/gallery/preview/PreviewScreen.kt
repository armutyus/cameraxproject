package com.armutyus.cameraxproject.ui.gallery.preview

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toFile
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.armutyus.cameraxproject.R
import com.armutyus.cameraxproject.ui.gallery.GalleryViewModel
import com.armutyus.cameraxproject.ui.gallery.models.BottomNavItem
import com.armutyus.cameraxproject.ui.gallery.models.MediaItem
import com.armutyus.cameraxproject.ui.gallery.preview.models.PlaybackStatus
import com.armutyus.cameraxproject.ui.gallery.preview.models.PreviewScreenEffect
import com.armutyus.cameraxproject.ui.gallery.preview.models.PreviewScreenEvent
import com.armutyus.cameraxproject.ui.gallery.preview.models.PreviewScreenState
import com.armutyus.cameraxproject.util.CameraPauseIcon
import com.armutyus.cameraxproject.util.CameraPlayIcon
import com.armutyus.cameraxproject.util.Util.Companion.GENERAL_ERROR_MESSAGE
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
@Composable
fun PreviewScreen(
    filePath: String,
    itemType: String,
    navController: NavController,
    factory: ViewModelProvider.Factory,
    previewViewModel: PreviewViewModel = viewModel(factory = factory),
    galleryViewModel: GalleryViewModel = viewModel(factory = factory),
    onShowMessage: (message: String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by previewViewModel.previewScreenState.collectAsState()
    val media by galleryViewModel.mediaItems.collectAsState()
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var rotationState by remember { mutableStateOf(0f) }
    var zoomState by remember { mutableStateOf(false) }
    var showBars by remember { mutableStateOf(false) }

    val currentFile = Uri.parse(filePath).toFile()
    val fileName = currentFile.nameWithoutExtension
    val takenDate = fileName.substring(0, 10).replace("-", "/")
    val takenTime = fileName.substring(11, 16).replace("-", ":")

    val bottomNavItems = listOf(
        BottomNavItem.Share,
        BottomNavItem.EditItem,
        BottomNavItem.Delete
    )

    val listener = object : PlaybackManager.PlaybackListener {
        override fun onPrepared() {
            previewViewModel.onEvent(PreviewScreenEvent.Prepared)
        }

        override fun onProgress(progress: Int) {
            previewViewModel.onEvent(PreviewScreenEvent.OnProgress(progress))
        }

        override fun onCompleted() {
            previewViewModel.onEvent(PreviewScreenEvent.Completed)
        }
    }

    val playbackManager = remember {
        PlaybackManager.Builder(context)
            .apply {
                this.uri = Uri.parse(filePath)
                this.listener = listener
                this.lifecycleOwner = lifecycleOwner
            }
            .build()
    }

    LaunchedEffect(previewViewModel) {
        previewViewModel.previewEffect.collect {
            when (it) {
                is PreviewScreenEffect.NavigateTo -> {
                    navController.navigate(it.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                }
                is PreviewScreenEffect.ShowMessage -> onShowMessage(it.message)
                PreviewScreenEffect.Pause -> playbackManager.pausePlayback()
                PreviewScreenEffect.Play -> playbackManager.start(state.playbackPosition)
            }
        }
    }

    Scaffold(
        topBar = {
            if (showBars)
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
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
        },
        bottomBar = {
            if (showBars) {
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
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            val currentList = media.values.flatten()
            val count = currentList.size
            val initialItem = currentList.firstOrNull { mediaItem ->  mediaItem.name == currentFile.name }
            var currentItemIndex by remember { mutableStateOf(currentList.indexOf(initialItem)) }
            val pagerState = rememberPagerState(currentItemIndex)
            LaunchedEffect(pagerState) {
                // Collect from the pager state a snapshotFlow reading the currentPage
                snapshotFlow { pagerState.currentPage }.collect { page ->
                    currentItemIndex = page
                }
            }
            HorizontalPager(
                modifier = Modifier.fillMaxSize(),
                count = count,
                state = pagerState,
                itemSpacing = 2.dp
            ) {
                Box(
                    modifier = Modifier
                        .clip(RectangleShape)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    if (scale >= 2f) {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    } else {
                                        scale = 3f
                                        offsetY = offset.x
                                        rotationState = 0f
                                    }
                                },
                                onTap = {
                                    if (zoomState) {
                                        showBars = !showBars
                                    }
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
                                        zoomState = false
                                        showBars = false
                                    } else {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                        rotationState = 0f
                                        zoomState = true
                                    }
                                } while (event.changes.any { pointerInputChange -> pointerInputChange.pressed })
                            }
                        }
                ) {
                    when (currentList[currentItemIndex].type) {
                        MediaItem.Type.PHOTO -> {
                            AsyncImage(
                                model = currentList[currentItemIndex].uri,
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
                            )
                        }
                        MediaItem.Type.VIDEO -> {
                            CompositionLocalProvider(LocalPlaybackManager provides playbackManager) {
                                VideoPlaybackContent(state, previewViewModel::onEvent)
                            }
                        }
                        else -> onShowMessage(GENERAL_ERROR_MESSAGE)
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoPlaybackContent(
    state: PreviewScreenState,
    onEvent: (PreviewScreenEvent) -> Unit
) {
    val playbackManager = LocalPlaybackManager.current

    Box(modifier = Modifier
        .fillMaxSize()
        .background(color = MaterialTheme.colorScheme.background)) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { playbackManager.videoView })
        when (state.playbackStatus) {
            PlaybackStatus.Idle -> {
                CameraPlayIcon(Modifier.align(Alignment.BottomCenter)) {
                    onEvent(PreviewScreenEvent.PlayTapped)
                }
            }
            PlaybackStatus.InProgress -> {
                CameraPauseIcon(Modifier.align(Alignment.BottomCenter)) {
                    onEvent(PreviewScreenEvent.PauseTapped)
                }
            }
            else -> {
                CircularProgressIndicator()
            }
        }
    }
}

//old code without horizontal pager
/*
{
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            Box(
                modifier = Modifier
                    .clip(RectangleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { offset ->
                                if (scale >= 2f) {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    scale = 3f
                                    offsetY = offset.x
                                    rotationState = 0f
                                }
                            },
                            onTap = {
                                if (zoomState) {
                                    showBars = !showBars
                                }
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
                                    zoomState = false
                                    showBars = false
                                } else {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                    rotationState = 0f
                                    zoomState = true
                                }
                            } while (event.changes.any { pointerInputChange -> pointerInputChange.pressed })
                        }
                    }
            ) {
                if (itemType == "photo") {
                    AsyncImage(
                        model = filePath,
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
                    )
                } else {
                    CompositionLocalProvider(LocalPlaybackManager provides playbackManager) {
                        VideoPlaybackContent(state, previewViewModel::onEvent)
                    }
                }
            }
        }
    }
*/