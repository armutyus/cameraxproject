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
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.armutyus.cameraxproject.ui.camera.CameraScreen
import com.armutyus.cameraxproject.ui.gallery.GalleryScreen
import com.armutyus.cameraxproject.ui.photo.PhotoScreen
import com.armutyus.cameraxproject.ui.photo.PhotoViewModel
import com.armutyus.cameraxproject.ui.theme.CameraXProjectTheme
import com.armutyus.cameraxproject.util.FileManager
import com.armutyus.cameraxproject.util.Permissions
import com.google.accompanist.permissions.ExperimentalPermissionsApi

@OptIn(ExperimentalPermissionsApi::class)
class MainActivity : ComponentActivity() {

    private val fileManager = FileManager(this)

    private val viewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PhotoViewModel::class.java)) {
                return PhotoViewModel(fileManager) as T
            }
            /*if (modelClass.isAssignableFrom(RecordingViewModel::class.java)) {
                return RecordingViewModel(fileManager, permissionsHandler) as T
            }*/
            throw IllegalArgumentException(getString(R.string.unknown_viewmodel))
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CameraXProjectTheme {
                Permissions(permissionGrantedContent = {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "gallery_screen"
                    ) {
                        composable("gallery_screen") {
                            GalleryScreen(navController = navController)
                        }
                        composable("photo_screen") {
                            PhotoScreen(navController = navController, factory = viewModelFactory) {
                                showMessage(this@MainActivity, it)
                            }
                        }
                        composable("settings_screen") {
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DefaultPreview() {
    val context = LocalContext.current
    CameraXProjectTheme {
        CameraScreen(navController = NavController(context))
    }
}