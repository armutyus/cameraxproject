package com.armutyus.cameraxproject.ui.gallery

import androidx.compose.runtime.mutableStateOf
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

    var isLoading = mutableStateOf(false)

    private val _mediaItem = MutableStateFlow(setOf<MediaItem>())
    val mediaItem: StateFlow<Set<MediaItem>> = _mediaItem

    private val _galleryEffect = MutableSharedFlow<GalleryEffect>()
    val galleryEffect: SharedFlow<GalleryEffect> = _galleryEffect

    fun loadMedia() {
        isLoading.value = true
        viewModelScope.launch {
            val photoDir = fileManager.getPrivateFileDirectory(PHOTO_DIR)
            val photos = photoDir?.listFiles()?.mapIndexed { index, file ->
                MediaItem(file.lastModified(), file.toUri(), MediaItem.Type.PHOTO)
            } as List<MediaItem>

            val videoDir = fileManager.getPrivateFileDirectory(VIDEO_DIR)
            val videos = videoDir?.listFiles()?.mapIndexed { index, file ->
                MediaItem(file.lastModified(), file.toUri(), MediaItem.Type.VIDEO)
            } as List<MediaItem>

            isLoading.value = false

            _mediaItem.value += photos
            _mediaItem.value += videos
        }
    }
}