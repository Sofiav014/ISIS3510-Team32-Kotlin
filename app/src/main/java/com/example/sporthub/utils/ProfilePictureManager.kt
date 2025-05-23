package com.example.sporthub.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ProfilePictureManager(
    private val fragment: Fragment,
    private val onImageSelected: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private val TAG = "ProfilePictureManager"
    private var currentPhotoPath: String = ""

    // Firebase Storage reference
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference

    // Activity result launchers
    private val takePictureLauncher: ActivityResultLauncher<Intent>
    private val selectImageLauncher: ActivityResultLauncher<Intent>
    private val requestPermissionLauncher: ActivityResultLauncher<Array<String>>

    init {
        Log.d(TAG, "ProfilePictureManager initialized")

        // Initialize camera launcher
        takePictureLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            Log.d(TAG, "Camera result received: ${result.resultCode}")
            if (result.resultCode == Activity.RESULT_OK) {
                // Check connectivity before processing
                if (!ConnectivityHelper.isNetworkAvailable(fragment.requireContext())) {
                    onError("Connection lost during photo capture. Please check your internet connection and try again.")
                    return@registerForActivityResult
                }
                handleCameraResult()
            }
        }

        // Initialize gallery launcher
        selectImageLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            Log.d(TAG, "Gallery result received: ${result.resultCode}")
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    // Check connectivity before processing
                    if (!ConnectivityHelper.isNetworkAvailable(fragment.requireContext())) {
                        onError("Connection lost during image selection. Please check your internet connection and try again.")
                        return@registerForActivityResult
                    }
                    handleGalleryResult(uri)
                }
            }
        }

        // Initialize permission launcher
        requestPermissionLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            Log.d(TAG, "Permission callback received: $permissions")

            val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
            val storageGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false
            } else {
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
            }

            Log.d(TAG, "Camera granted: $cameraGranted, Storage granted: $storageGranted")

            if (cameraGranted && storageGranted) {
                Log.d(TAG, "All permissions granted, showing image source dialog")
                showImageSourceDialog()
            } else {
                val missingPermissions = mutableListOf<String>()
                if (!cameraGranted) missingPermissions.add("Camera")
                if (!storageGranted) missingPermissions.add("Photos/Media")

                val message = if (missingPermissions.size == 1) {
                    "${missingPermissions[0]} permission is required to change profile picture. Please try again and grant the permission."
                } else {
                    "${missingPermissions.joinToString(" and ")} permissions are required to change profile picture. Please try again and grant both permissions."
                }

                onError(message)
            }
        }

        Log.d(TAG, "All launchers initialized successfully")
    }

    fun showImagePickerDialog() {
        Log.d(TAG, "showImagePickerDialog called")

        // Note: Connectivity should be checked by the calling code before this method
        // We trust that the caller has already verified internet connectivity

        if (hasRequiredPermissions()) {
            Log.d(TAG, "Permissions already granted, showing image source dialog")
            showImageSourceDialog()
        } else {
            Log.d(TAG, "Permissions not granted, requesting permissions")
            requestPermissions()
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val context = fragment.requireContext()
        val cameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

        val storagePermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        Log.d(TAG, "Checking permissions - Camera: $cameraPermission, Storage: $storagePermission")
        return cameraPermission && storagePermission
    }

    private fun requestPermissions() {
        Log.d(TAG, "requestPermissions() called - launching permission dialog")
        try {
            val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES
                )
            } else {
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }

            Log.d(TAG, "Requesting permissions: ${permissions.joinToString(", ")}")
            requestPermissionLauncher.launch(permissions)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching permission request", e)
            onError("Error requesting permissions: ${e.message}")
        }
    }

    private fun showImageSourceDialog() {
        // Removed redundant connectivity check - trust the calling code
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")

        AlertDialog.Builder(fragment.requireContext())
            .setTitle("Select Profile Picture")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun openCamera() {
        try {
            val photoFile = createImageFile()
            val photoURI = FileProvider.getUriForFile(
                fragment.requireContext(),
                "com.example.sporthub.fileprovider",
                photoFile
            )

            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            takePictureLauncher.launch(takePictureIntent)
        } catch (e: IOException) {
            Log.e(TAG, "Error creating image file", e)
            onError("Error opening camera")
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        selectImageLauncher.launch(intent)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = fragment.requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        return File.createTempFile(imageFileName, ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun handleCameraResult() {
        try {
            val bitmap = BitmapFactory.decodeFile(currentPhotoPath)
            if (bitmap != null) {
                uploadImageToFirebase(bitmap)
            } else {
                onError("Failed to process camera image")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling camera result", e)
            onError("Error processing camera image")
        }
    }

    private fun handleGalleryResult(uri: Uri) {
        try {
            val context = fragment.requireContext()
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                uploadImageToFirebase(bitmap)
            } else {
                onError("Failed to load selected image")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling gallery result", e)
            onError("Error processing selected image")
        }
    }

    private fun uploadImageToFirebase(bitmap: Bitmap) {
        // Check connectivity before starting upload
        if (!ConnectivityHelper.isNetworkAvailable(fragment.requireContext())) {
            onError("Connection lost during upload preparation. Please check your internet connection and try again.")
            return
        }

        fragment.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val userId = getCurrentUserId()
                if (userId.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        onError("User not authenticated")
                    }
                    return@launch
                }

                // Double-check connectivity before compression
                if (!ConnectivityHelper.isNetworkAvailable(fragment.requireContext())) {
                    withContext(Dispatchers.Main) {
                        onError("Connection lost during image processing. Please check your internet connection and try again.")
                    }
                    return@launch
                }

                // Compress and upload image
                val compressedBitmap = compressBitmap(bitmap)
                val downloadUrl = uploadBitmapToStorage(compressedBitmap, userId)

                // Return the download URL
                withContext(Dispatchers.Main) {
                    onImageSelected(downloadUrl)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading image", e)
                withContext(Dispatchers.Main) {
                    // Check if error is connectivity-related
                    if (!ConnectivityHelper.isNetworkAvailable(fragment.requireContext())) {
                        onError("Connection lost during upload. Please check your internet connection and try again.")
                    } else {
                        onError("Failed to upload image: ${e.message}")
                    }
                }
            }
        }
    }

    private suspend fun uploadBitmapToStorage(bitmap: Bitmap, userId: String): String {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "Starting upload for user: $userId")

            // Check connectivity before starting upload
            if (!ConnectivityHelper.isNetworkAvailable(fragment.requireContext())) {
                throw Exception("No internet connection available for upload")
            }

            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val imageData = baos.toByteArray()

            Log.d(TAG, "Image compressed, size: ${imageData.size} bytes")

            // Store in users folder with userId as filename
            val imageRef: StorageReference = storageRef.child("users/$userId.jpg")
            Log.d(TAG, "Upload path: users/$userId.jpg")

            try {
                val uploadTask = imageRef.putBytes(imageData).await()
                Log.d(TAG, "Upload completed successfully")

                val downloadUrl = imageRef.downloadUrl.await()
                Log.d(TAG, "Download URL obtained: $downloadUrl")

                downloadUrl.toString()
            } catch (e: Exception) {
                Log.e(TAG, "Upload failed: ${e.message}")
                // Check if it's a connectivity issue
                if (!ConnectivityHelper.isNetworkAvailable(fragment.requireContext())) {
                    throw Exception("Connection lost during upload")
                } else {
                    throw e
                }
            }
        }
    }

    private fun compressBitmap(bitmap: Bitmap): Bitmap {
        val maxDimension = 800
        val scale = if (bitmap.width > bitmap.height) {
            maxDimension.toFloat() / bitmap.width
        } else {
            maxDimension.toFloat() / bitmap.height
        }

        return if (scale < 1) {
            val newWidth = (bitmap.width * scale).toInt()
            val newHeight = (bitmap.height * scale).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
    }

    private fun getCurrentUserId(): String? {
        return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    }
}