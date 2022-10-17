package com.armutyus.cameraxproject.util

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.*

@RequiresApi(Build.VERSION_CODES.Q)
@ExperimentalPermissionsApi
@Composable
fun Permission(
    permissions: List<String> = listOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.ACCESS_MEDIA_LOCATION),
    permissionGrantedContent: @Composable () -> Unit = { }
) {
    val permissionsState = rememberMultiplePermissionsState(permissions = permissions)
    RequestPermissions(multiplePermissionsState = permissionsState,
        permissionGrantedContent = permissionGrantedContent)
}

@ExperimentalPermissionsApi
@Composable
private fun RequestPermissions(multiplePermissionsState: MultiplePermissionsState,
                               permissionGrantedContent: @Composable (() -> Unit)
) {
    if (multiplePermissionsState.allPermissionsGranted) {
        // If all permissions are granted, then show screen with the feature enabled
        permissionGrantedContent()
        val context = LocalContext.current
        Toast.makeText(context, "All permissions granted!", Toast.LENGTH_SHORT).show()
    } else {
        if (multiplePermissionsState.shouldShowRationale) {
            Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.BottomCenter) {
                Snackbar(modifier = Modifier.align(Alignment.BottomCenter), action = {
                    Button(onClick = { multiplePermissionsState.launchMultiplePermissionRequest() }) {
                        Text(text = "Give Permissions")
                    }
                }){
                    Text(text = "Permissions are required to use the app.")
                }
            }
        } else {
            AlertDialog(onDismissRequest = { /*TODO*/ },
                title = { Text(text = "Permissions") },
                text = { Text(text = "These permissions are important for the app. Please grant all of them for the app to function properly.") },
                confirmButton = {
                    Button(onClick = { multiplePermissionsState.launchMultiplePermissionRequest() }) {
                        Text(text = "Give Permissions")
                    }
                },
                dismissButton = {
                    Button(onClick = {  }) {
                        Text(text = "Deny")
                    }
                }
            )
        }
    }
}