package com.armutyus.cameraxproject.ui.gallery

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.sharp.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
    val groupedPhotos by galleryViewModel.photoItem.collectAsState()
    val groupedVideos by galleryViewModel.videoItem.collectAsState()
    val context = LocalContext.current
    var filterContent by remember { mutableStateOf(MediaItem.Filter.ALL) }

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
            NavigationBar {
                NavigationBarItem(
                    selected = filterContent == MediaItem.Filter.ALL,
                    icon = {
                        Icon(
                            imageVector = Icons.Sharp.LibraryBooks,
                            contentDescription = stringResource(id = R.string.gallery_items)
                        )
                           },
                    label = {
                            Text(text = stringResource(id = R.string.gallery))
                            },
                    alwaysShowLabel = false,
                    onClick = { filterContent = MediaItem.Filter.ALL }
                )
                NavigationBarItem(
                    selected = filterContent == MediaItem.Filter.PHOTOS,
                    icon = {
                        Icon(
                            imageVector = Icons.Sharp.PhotoLibrary,
                            contentDescription = stringResource(id = R.string.photos)
                        )
                    },
                    label = {
                        Text(text = stringResource(id = R.string.photos))
                    },
                    alwaysShowLabel = false,
                    onClick = { filterContent = MediaItem.Filter.PHOTOS }
                )
                NavigationBarItem(
                    selected = filterContent == MediaItem.Filter.VIDEOS,
                    icon = {
                        Icon(
                            imageVector = Icons.Sharp.VideoLibrary,
                            contentDescription = stringResource(id = R.string.videos)
                        )
                    },
                    label = {
                        Text(text = stringResource(id = R.string.videos))
                    },
                    alwaysShowLabel = false,
                    onClick = { filterContent = MediaItem.Filter.VIDEOS }
                )
            }
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            GalleryContent(
                context = context,
                groupedMedia = media,
                groupedPhotos = groupedPhotos,
                groupedVideos = groupedVideos,
                filterContent = filterContent)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryContent(
    context: Context,
    groupedMedia: Map<String, List<MediaItem>>,
    groupedPhotos: Map<String, List<MediaItem>>,
    groupedVideos: Map<String, List<MediaItem>>,
    filterContent: MediaItem.Filter
) {
    val numberOfItemsByRow = LocalConfiguration.current.screenWidthDp / 96
    LazyColumn {
        when (filterContent) {
            MediaItem.Filter.ALL -> {
                groupedMedia.forEach { (takenTime, mediaForTakenTime) ->
                    stickyHeader {
                        Text(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            text = takenTime
                        )
                    }
                    items(items = mediaForTakenTime.chunked(numberOfItemsByRow)) { mediaList ->
                        Row(
                            modifier = Modifier.padding(vertical = 1.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (media in mediaList) {
                                MediaItemBox(item = media, context = context)
                            }
                        }
                    }
                }
            }
            MediaItem.Filter.PHOTOS -> {
                groupedPhotos.forEach { (takenTime, photosForTakenTime) ->
                    stickyHeader {
                        Text(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            text = takenTime
                        )
                    }
                    items(items = photosForTakenTime.chunked(numberOfItemsByRow)) { photos ->
                        Row(
                            modifier = Modifier.padding(vertical = 1.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (item in photos) {
                                MediaItemBox(item = item, context = context)
                            }
                        }
                    }
                }
            }
            MediaItem.Filter.VIDEOS -> {
                groupedVideos.forEach { (takenTime, videosForTakenTime) ->
                    stickyHeader {
                        Text(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            text = takenTime
                        )
                    }
                    items(items = videosForTakenTime.chunked(numberOfItemsByRow)) { videos ->
                        Row(
                            modifier = Modifier.padding(vertical = 1.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (item in videos) {
                                MediaItemBox(item = item, context = context)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MediaItemBox(item: MediaItem, context: Context) {
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

    Box(
        modifier = Modifier
            .height(96.dp)
            .width(96.dp)
    ) {
        Image(
            modifier = Modifier
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
                modifier = Modifier
                    .size(36.dp)
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