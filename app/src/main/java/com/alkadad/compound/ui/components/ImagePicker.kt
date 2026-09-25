package com.alkadad.compound.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

class ImagePickerLauncher(
    val openCamera: () -> Unit,
    val openGallery: () -> Unit,
)

/**
 * Camera + gallery picking. The pending camera Uri is saveable, so a photo still arrives
 * if the activity is recreated while the camera app is in front.
 */
@Composable
fun rememberImagePicker(
    onImagePicked: (Uri) -> Unit,
    onCameraPermissionDenied: () -> Unit,
): ImagePickerLauncher {
    val context = LocalContext.current
    val currentOnPicked by rememberUpdatedState(onImagePicked)
    val currentOnDenied by rememberUpdatedState(onCameraPermissionDenied)
    var pendingCameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (success && uri != null) currentOnPicked(uri)
    }

    fun launchCamera() {
        val photoFile = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else currentOnDenied()
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { currentOnPicked(it) }
    }

    return ImagePickerLauncher(
        openCamera = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                launchCamera()
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        },
        openGallery = { galleryLauncher.launch("image/*") },
    )
}
