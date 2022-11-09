package com.armutyus.cameraxproject.ui.gallery.models

import android.net.Uri

data class MediaItem(
    val takenTime: String = "",
    val uri: Uri? = Uri.EMPTY,
    val type: Type? = Type.UNKNOWN
) {
    enum class Type { UNKNOWN, PHOTO, VIDEO }
    enum class Filter { ALL, PHOTOS, VIDEOS }
}