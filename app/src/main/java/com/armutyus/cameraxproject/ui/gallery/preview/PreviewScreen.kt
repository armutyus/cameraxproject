package com.armutyus.cameraxproject.ui.gallery.preview

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.armutyus.cameraxproject.R
import com.armutyus.cameraxproject.ui.gallery.GalleryViewModel
import com.armutyus.cameraxproject.ui.gallery.models.BottomNavItem
import com.armutyus.cameraxproject.ui.gallery.models.MediaItem
import com.armutyus.cameraxproject.ui.gallery.preview.models.PreviewScreenEffect
import com.armutyus.cameraxproject.ui.gallery.preview.models.PreviewScreenEvent
import com.armutyus.cameraxproject.util.Util.Companion.GENERAL_ERROR_MESSAGE
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState

@UnstableApi
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
@Composable
fun PreviewScreen(
    filePath: String,
    navController: NavController,
    factory: ViewModelProvider.Factory,
    previewViewModel: PreviewViewModel = viewModel(factory = factory),
    galleryViewModel: GalleryViewModel = viewModel(factory = factory),
    onShowMessage: (message: String) -> Unit
) {
    val context = LocalContext.current
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
                                        println("varx: $offsetX")
                                        println("vary: $offsetY")
                                    }
                                },
                                onTap = { if (!zoomState) showBars = !showBars }
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
                                        showBars = false
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
                        MediaItem.Type.VIDEO -> {
                            VideoPlaybackContent(currentList[page].uri)
                        }
                        else -> onShowMessage(GENERAL_ERROR_MESSAGE)
                    }
                }
            }
        }
    }
}

@UnstableApi
@Composable
private fun VideoPlaybackContent(
    filePath: Uri?
) {

    val context = LocalContext.current
    var lifecycle by remember {
        mutableStateOf(Lifecycle.Event.ON_CREATE)
    }
    var playerView by remember(filePath) {
        mutableStateOf<PlayerView?>(null)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            lifecycle = event
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
    ) {
        val videoPlayer = remember(context) { ExoPlayer.Builder(context).build() }
        DisposableEffect(
            AndroidView(
                modifier = Modifier
                    .systemBarsPadding(),
                factory = {
                    playerView = PlayerView(it)
                    playerView.apply {
                        this!!.player = videoPlayer
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                        controllerShowTimeoutMs = 3000
                        this.player?.setMediaItem(androidx.media3.common.MediaItem.fromUri(filePath!!))
                        this.player?.prepare()
                    }!!
                },
                update = {
                    it.apply {
                        player?.setMediaItem(androidx.media3.common.MediaItem.fromUri(filePath!!))
                    }
                    when (lifecycle) {
                        Lifecycle.Event.ON_PAUSE -> {
                            it.onPause()
                            it.player?.pause()
                        }
                        Lifecycle.Event.ON_RESUME -> {
                            it.onResume()
                        }
                        else -> Unit
                    }
                }
            )
        ) {
            onDispose {
                videoPlayer.release()
            }
        }
    }
}