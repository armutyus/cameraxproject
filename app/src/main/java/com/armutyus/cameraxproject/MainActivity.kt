package com.armutyus.cameraxproject

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.armutyus.cameraxproject.ui.camera.CameraScreen
import com.armutyus.cameraxproject.ui.gallery.GalleryScreen
import com.armutyus.cameraxproject.ui.theme.CameraXProjectTheme
import com.armutyus.cameraxproject.util.Permissions
import com.google.accompanist.permissions.ExperimentalPermissionsApi

@OptIn(ExperimentalPermissionsApi::class)
class MainActivity : ComponentActivity() {
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
                        composable("camera_screen") {
                            CameraScreen(
                                navController = navController
                            )
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DefaultPreview() {
    val context = LocalContext.current
    CameraXProjectTheme {
        GalleryScreen(navController = NavController(context))
    }
}