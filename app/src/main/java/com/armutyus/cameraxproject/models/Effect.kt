package com.armutyus.cameraxproject.models

import com.armutyus.cameraxproject.util.Util

sealed class Effect {
    data class ShowMessage(val message: String = Util.GENERAL_ERROR_MESSAGE) : Effect()
    data class CaptureImage(val filePath: String) : Effect()
    data class NavigateTo(val route: String) : Effect()
}
