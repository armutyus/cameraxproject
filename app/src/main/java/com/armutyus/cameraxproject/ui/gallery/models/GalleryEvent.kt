package com.armutyus.cameraxproject.ui.gallery.models

import android.net.Uri

sealed class GalleryEvent {

    data class ItemClicked(val uri: Uri?) : GalleryEvent()
    data class ItemChecked(val item: MediaItem) : GalleryEvent()
    data class ItemUnchecked(val item: MediaItem) : GalleryEvent()

    object CancelSelectableMode : GalleryEvent()
    object DeleteTapped : GalleryEvent()
    object ShareTapped : GalleryEvent()
    object ItemLongClicked : GalleryEvent()
    object FabClicked : GalleryEvent()
    object SelectAllClicked : GalleryEvent()

}
