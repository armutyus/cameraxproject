package com.armutyus.cameraxproject.ui.gallery

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armutyus.cameraxproject.ui.gallery.models.GalleryEffect
import com.armutyus.cameraxproject.ui.gallery.models.MediaItem
import com.armutyus.cameraxproject.util.FileManager
import com.armutyus.cameraxproject.util.Util.Companion.PHOTO_DIR
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_DIR
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GalleryViewModel constructor(private val fileManager: FileManager) : ViewModel() {

    private val _mediaItem = MutableStateFlow(mapOf<String, List<MediaItem>>())
    val mediaItem: StateFlow<Map<String, List<MediaItem>>> = _mediaItem

    private val _photoItem = MutableStateFlow(mapOf<String, List<MediaItem>>())
    val photoItem: StateFlow<Map<String, List<MediaItem>>> = _photoItem

    private val _videoItem = MutableStateFlow(mapOf<String, List<MediaItem>>())
    val videoItem: StateFlow<Map<String, List<MediaItem>>> = _videoItem

    private val _galleryEffect = MutableSharedFlow<GalleryEffect>()
    val galleryEffect: SharedFlow<GalleryEffect> = _galleryEffect


    fun loadMedia() {
        viewModelScope.launch {
            val media = mutableListOf<MediaItem>()

            val photoDir = fileManager.getPrivateFileDirectory(PHOTO_DIR)
            val photos = photoDir?.listFiles()?.mapIndexed { _, file ->
                val takenTime = file.name.substring(0, 10).replace("-", "/")
                MediaItem(takenTime, file.toUri(), MediaItem.Type.PHOTO)
            }?.sortedByDescending { it.takenTime } as List<MediaItem>

            val videoDir = fileManager.getPrivateFileDirectory(VIDEO_DIR)
            val videos = videoDir?.listFiles()?.mapIndexed { _, file ->
                val takenTime = file.name.substring(0, 10).replace("-", "/")
                MediaItem(takenTime, file.toUri(), MediaItem.Type.VIDEO)
            }?.sortedByDescending { it.takenTime } as List<MediaItem>

            media.addAll(photos + videos)

            val groupedMedia = media.groupBy { it.takenTime }
            val groupedPhotos = photos.groupBy { it.takenTime }
            val groupedVideos = videos.groupBy { it.takenTime }

            _mediaItem.value += groupedMedia
            _photoItem.value += groupedPhotos
            _videoItem.value += groupedVideos
        }
    }
}