package com.armutyus.cameraxproject.ui.gallery.models

import android.net.Uri

data class MediaItem(
    val id: Long? = 0,
    val uri: Uri? = Uri.EMPTY,
    val type: Type? = Type.UNKNOWN
) {
    enum class Type { UNKNOWN, PHOTO, VIDEO }
}