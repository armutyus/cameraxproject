package com.armutyus.cameraxproject.ui.gallery.models

import android.content.Context

sealed class GalleryEvent {

    data class ItemClicked(val item: MediaItem) : GalleryEvent()
    data class ItemChecked(val item: MediaItem) : GalleryEvent()
    data class ItemUnchecked(val item: MediaItem) : GalleryEvent()
    data class ShareTapped(val context: Context) : GalleryEvent()

    object CancelSelectableMode : GalleryEvent()
    object DeleteTapped : GalleryEvent()
    object ItemLongClicked : GalleryEvent()
    object FabClicked : GalleryEvent()
    object SelectAllClicked : GalleryEvent()

}
