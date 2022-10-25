package com.armutyus.cameraxproject.util

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.util.DisplayMetrics
import android.util.Size
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.annotation.RequiresApi

class Util {
    companion object {
        const val TAG = "CameraXProject"
        const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val PHOTO_EXTENSION = ".jpg"
        const val VIDEO_EXTENSION = ".mp4"
        const val RATIO_4_3_VALUE = 4.0 / 3.0
        const val RATIO_16_9_VALUE = 16.0 / 9.0
        const val TIMER_OFF = 0
        const val TIMER_3S = 1
        const val TIMER_10S = 2
        const val FLASH_OFF = 0
        const val FLASH_ON = 1
        const val FLASH_AUTO = 2
    }

    object ScreenSizeCompat {
        private val api: Api =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ApiLevel30()
            else Api()

        /**
         * Returns screen size in pixels.
         */
        fun getScreenSize(context: Context): Size = api.getScreenSize(context)

        @Suppress("DEPRECATION")
        private open class Api {
            open fun getScreenSize(context: Context): Size {
                val display = context.getSystemService(WindowManager::class.java).defaultDisplay
                val metrics = if (display != null) {
                    DisplayMetrics().also { display.getRealMetrics(it) }
                } else {
                    Resources.getSystem().displayMetrics
                }
                return Size(metrics.widthPixels, metrics.heightPixels)
            }
        }

        @RequiresApi(Build.VERSION_CODES.R)
        private class ApiLevel30 : Api() {
            override fun getScreenSize(context: Context): Size {
                val metrics: WindowMetrics = context.getSystemService(WindowManager::class.java).currentWindowMetrics
                return Size(metrics.bounds.width(), metrics.bounds.height())
            }
        }
    }
}