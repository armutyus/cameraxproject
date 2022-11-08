package com.armutyus.cameraxproject.ui.gallery

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.sharp.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.VideoFrameDecoder
import com.armutyus.cameraxproject.R
import com.armutyus.cameraxproject.ui.gallery.models.MediaItem
import com.armutyus.cameraxproject.ui.theme.CameraXProjectTheme
import com.armutyus.cameraxproject.util.Util.Companion.PHOTO_ROUTE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    navController: NavController,
    factory: ViewModelProvider.Factory,
    galleryViewModel: GalleryViewModel = viewModel(factory = factory),
) {
    val media by galleryViewModel.mediaItem.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(galleryViewModel) {
        galleryViewModel.loadMedia()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate(PHOTO_ROUTE) {
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
                }) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(id = R.string.open_camera)
                )
            }
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        Toast.makeText(context, R.string.camera_mode_none, Toast.LENGTH_LONG).show()
                    }) {
                        Icon(
                            imageVector = Icons.Sharp.LibraryBooks,
                            contentDescription = stringResource(id = R.string.camera_mode_none)
                        )
                    }
                    IconButton(onClick = {
                        Toast.makeText(context, R.string.camera_mode_none, Toast.LENGTH_LONG).show()
                    }) {
                        Icon(
                            imageVector = Icons.Sharp.PhotoLibrary,
                            contentDescription = stringResource(id = R.string.camera_mode_none)
                        )
                    }
                    IconButton(onClick = {
                        Toast.makeText(context, R.string.camera_mode_video, Toast.LENGTH_LONG)
                            .show()
                    }) {
                        Icon(
                            imageVector = Icons.Sharp.VideoLibrary,
                            contentDescription = stringResource(id = R.string.camera_mode_video)
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
            GalleryContent(context = context, media = media.toList())
        }
    }
}

@Composable
fun GalleryContent(context: Context, media: List<MediaItem>) {
    LazyVerticalGrid(columns = GridCells.Adaptive(96.dp)) {
        items(media) { item ->
            MediaItemRow(item = item, context = context)
        }
    }
}

@Composable
fun MediaItemRow(item: MediaItem, context: Context) {
    val imageLoader = ImageLoader.Builder(context)
        .components {
            add(VideoFrameDecoder.Factory())
        }.crossfade(true)
        .build()

    val painter = rememberAsyncImagePainter(
        model = item.uri,
        imageLoader = if (item.type == MediaItem.Type.VIDEO) {
            imageLoader
        } else {
            ImageLoader.Builder(context)
                .crossfade(true)
                .build()
        }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            modifier = Modifier
                .fillMaxSize()
                .height(96.dp)
                .width(96.dp)
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.background,
                    shape = RectangleShape
                ),
            contentScale = ContentScale.Crop,
            painter = painter,
            contentDescription = stringResource(id = R.string.gallery_items)
        )
        if (item.type == MediaItem.Type.VIDEO) {
            Icon(
                imageVector = Icons.Sharp.PlayCircleOutline,
                contentDescription = null,
                modifier = Modifier.size(36.dp)
                    .align(Alignment.Center)
                    .padding(4.dp),
                tint = MaterialTheme.colorScheme.inverseSurface
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DefaultPreview() {
    CameraXProjectTheme {

    }
}