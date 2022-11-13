package com.armutyus.cameraxproject

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.armutyus.cameraxproject.ui.gallery.GalleryScreen
import com.armutyus.cameraxproject.ui.gallery.GalleryViewModel
import com.armutyus.cameraxproject.ui.photo.PhotoScreen
import com.armutyus.cameraxproject.ui.photo.PhotoViewModel
import com.armutyus.cameraxproject.ui.preview.PreviewScreen
import com.armutyus.cameraxproject.ui.preview.PreviewViewModel
import com.armutyus.cameraxproject.ui.theme.CameraXProjectTheme
import com.armutyus.cameraxproject.ui.video.VideoScreen
import com.armutyus.cameraxproject.ui.video.VideoViewModel
import com.armutyus.cameraxproject.util.FileManager
import com.armutyus.cameraxproject.util.Permissions
import com.armutyus.cameraxproject.util.Util.Companion.GALLERY_ROUTE
import com.armutyus.cameraxproject.util.Util.Companion.PHOTO_ROUTE
import com.armutyus.cameraxproject.util.Util.Companion.SETTINGS_ROUTE
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_ROUTE
import com.google.accompanist.permissions.ExperimentalPermissionsApi

@OptIn(ExperimentalPermissionsApi::class)
class MainActivity : ComponentActivity() {

    private val fileManager = FileManager(this)

    @Suppress("UNCHECKED_CAST")
    private val viewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PhotoViewModel::class.java))
                return PhotoViewModel(fileManager) as T
            if (modelClass.isAssignableFrom(PreviewViewModel::class.java))
                return PreviewViewModel() as T
            if (modelClass.isAssignableFrom(VideoViewModel::class.java))
                return VideoViewModel(fileManager) as T
            if (modelClass.isAssignableFrom(GalleryViewModel::class.java))
                return GalleryViewModel(fileManager) as T
            throw IllegalArgumentException(getString(R.string.unknown_viewmodel))
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CameraXProjectTheme {
                Permissions(permissionGrantedContent = {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = GALLERY_ROUTE
                    ) {
                        composable(GALLERY_ROUTE) {
                            GalleryScreen(
                                navController = navController,
                                factory = viewModelFactory,
                            ) {
                                showMessage(this@MainActivity, it)
                            }
                        }
                        composable(PHOTO_ROUTE) {
                            PhotoScreen(
                                navController = navController,
                                factory = viewModelFactory
                            ) {
                                showMessage(this@MainActivity, it)
                            }
                        }
                        composable(VIDEO_ROUTE) {
                            VideoScreen(
                                navController = navController,
                                factory = viewModelFactory
                            ) {
                                showMessage(this@MainActivity, it)
                            }
                        }
                        composable(
                            route = "preview_screen/?filePath={filePath}",
                            arguments = listOf(
                                navArgument("filePath") {
                                    type = NavType.StringType
                                    defaultValue = ""
                                }
                            )
                        ) {
                            val filePath = remember { it.arguments?.getString("filePath") }
                            PreviewScreen(filePath = filePath ?: "", navController = navController, factory = viewModelFactory) {
                                showMessage(this@MainActivity, it)
                            }
                        }
                        composable(SETTINGS_ROUTE) {
                            //SettingsScreen(navController = navController)
                        }
                    }
                })
            }
        }
    }
}

private fun showMessage(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}