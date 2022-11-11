package com.armutyus.cameraxproject.ui.gallery.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.armutyus.cameraxproject.R

sealed class BottomNavItem(var filter: MediaItem.Filter?, var icon: ImageVector, var label: Int) {

    object Gallery : BottomNavItem(MediaItem.Filter.ALL, Icons.Sharp.LibraryBooks, R.string.gallery)
    object Photos : BottomNavItem(MediaItem.Filter.PHOTOS, Icons.Sharp.PhotoLibrary, R.string.photos)
    object Videos : BottomNavItem(MediaItem.Filter.VIDEOS, Icons.Sharp.VideoLibrary, R.string.videos)

}
