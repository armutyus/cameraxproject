package com.armutyus.cameraxproject.ui.gallery.preview

import android.net.Uri
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.core.net.toFile
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.armutyus.cameraxproject.R
import com.armutyus.cameraxproject.ui.gallery.GalleryViewModel
import com.armutyus.cameraxproject.ui.gallery.models.BottomNavItem

@OptIn(ExperimentalMaterial3Api::class)
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
    val state by previewViewModel.previewScreenState.collectAsState()
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(1f) }
    var offsetY by remember { mutableStateOf(1f) }
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
                            onClick = { /*onEvent*/ }
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
            Box(
                modifier = Modifier
                    .clip(RectangleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { /* Called when the gesture starts */ },
                            onDoubleTap = { offset ->
                                if (scale >= 2f) {
                                    scale = 1f
                                    offsetX = offset.x
                                    offsetY = offset.y
                                } else {
                                    scale = 3f
                                    rotationState = 0f
                                }
                            },
                            onTap = { if (zoomState) { showBars = !showBars } }
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
                                    offsetX = 1f
                                    offsetY = 1f
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
                        contentScale = ContentScale.Fit,
                        filterQuality = FilterQuality.High,
                        contentDescription = ""
                    )
                } else {
                    //VideoContent
                }
            }
        }
    }
}