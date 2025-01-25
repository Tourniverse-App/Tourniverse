package com.example.tourniverse.fragments

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.tourniverse.R
import com.example.tourniverse.activities.LoginActivity
import com.example.tourniverse.viewmodels.AccountViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

private const val CROP_IMAGE_REQUEST_CODE = 1001

class AccountFragment : Fragment() {

    private lateinit var viewModel: AccountViewModel
    private var onPermissionsGranted: (() -> Unit)? = null
    private val pickPhotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            Log.d(TAG, "Photo selected: $uri")
            uploadPhotoToViewModel(it)
        } ?: Log.d(TAG, "No photo selected").also { showToast("No photo selected") }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
            val readMediaGranted = permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false

            Log.d(TAG, "Permission Results:")
            Log.d(TAG, "Camera Granted: $cameraGranted")
            Log.d(TAG, "Read Media Images Granted: $readMediaGranted")

            if (cameraGranted && readMediaGranted) {
                Log.d(TAG, "All permissions granted.")
                onPermissionsGranted?.invoke() // Execute the action (e.g., camera/gallery)
            } else {
                Log.d(TAG, "Permissions denied.")
                showToast("Camera and storage permissions are required for this feature.")
                navigateToAppSettings()
            }
        }

    // Camera capture launcher
    private val cameraPhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        bitmap?.let {
            Log.d(TAG, "Photo captured with camera.")
            try {
                val uri = saveBitmapToCache(it) // Save bitmap to cache
                uploadPhotoToViewModel(uri) // Upload directly without cropping
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save photo for upload: ${e.message}", e)
                showToast("Error saving photo for upload.")
            }
        } ?: Log.d(TAG, "No photo captured.")
    }

    // Crop activity launcher
    private val cropPhotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val croppedUri = result.data?.data
            if (croppedUri != null) {
                Log.d(TAG, "Photo cropped: $croppedUri")
                uploadPhotoToViewModel(croppedUri)
            } else {
                Log.e(TAG, "Crop result returned no URI. Uploading original photo.")
                showToast("Cropping failed. Uploading original photo.")
            }
        } else {
            Log.e(TAG, "Crop operation was canceled or failed. Uploading original photo.")
            showToast("Cropping canceled. Uploading original photo.")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView called.")
        val view = inflater.inflate(R.layout.fragment_account, container, false)
        viewModel = ViewModelProvider(this).get(AccountViewModel::class.java)

        // UI Elements
        val usernameEditText: EditText = view.findViewById(R.id.edit_name)
        val emailEditText: EditText = view.findViewById(R.id.edit_email)
        val bioEditText: EditText = view.findViewById(R.id.edit_bio)
        val saveButton: Button = view.findViewById(R.id.save_button)
        val resetPasswordButton: Button = view.findViewById(R.id.reset_password)
        val deleteAccountButton: Button = view.findViewById(R.id.delete_account)

        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "User not logged in.")
            showToast("User not logged in")
            return view
        }

        Log.d(TAG, "Fetching user details for UID: ${currentUser.uid}")

        // Make email EditText read-only
        emailEditText.inputType = InputType.TYPE_NULL
        emailEditText.isFocusable = false

        // Fetch user details
        viewModel.fetchUserDetails(currentUser.uid) { username,email, bio  ->
            Log.d(TAG, "Fetched user details: username=$username, bio=$bio")
            usernameEditText.setText(username)
            emailEditText.setText(email)
            bioEditText.setText(bio)
        }

        // Fetch and display profile photo
        viewModel.fetchUserPhoto(currentUser.uid) { base64String ->
            if (!base64String.isNullOrEmpty()) {
                Log.d(TAG, "Fetched profile photo.")
                try {
                    val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    displayProfilePhoto(bitmap)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to decode profile photo: ${e.message}", e)
                }
            } else {
                Log.d(TAG, "No profile photo found.")
            }
        }

        // Set up buttons
        saveButton.setOnClickListener {
            Log.d(TAG, "Save button clicked.")
            viewModel.saveProfile(
                currentUser,
                usernameEditText.text.toString(),
                emailEditText.text.toString(),
                bioEditText.text.toString(),
                onSuccess = {
                    Log.d(TAG, "Profile updated successfully.")
                    showToast("Profile updated successfully!")
                },
                onError = {
                    Log.e(TAG, "Error updating profile: $it")
                    showToast(it)
                }
            )
        }

        resetPasswordButton.setOnClickListener {
            Log.d(TAG, "Reset password button clicked.")
            viewModel.sendPasswordReset(
                currentUser.email,
                onSuccess = {
                    Log.d(TAG, "Password reset email sent.")
                    showToast("Password reset email sent!")
                },
                onError = {
                    Log.e(TAG, "Error sending password reset email: $it")
                    showToast(it)
                }
            )
        }

        deleteAccountButton.setOnClickListener {
            Log.d(TAG, "Delete account button clicked.")

            // Show confirmation dialog before deleting the account
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Yes") { _, _ ->
                    // Proceed with account deletion
                    viewModel.deleteAccount(
                        currentUser,
                        onSuccess = {
                            Log.d(TAG, "Account deleted successfully.")
                            navigateToLogin()
                        },
                        onError = {
                            Log.e(TAG, "Error deleting account: $it")
                            showToast(it)
                        }
                    )
                }
                .setNegativeButton("No") { dialog, _ ->
                    // Cancel the deletion process
                    dialog.dismiss()
                    Log.d(TAG, "Account deletion canceled by the user.")
                }
                .show()
        }

        setupPhotoPicker(view)
        return view
    }

    private fun setupPhotoPicker(view: View) {
        Log.d(TAG, "Setting up photo picker.")
        val uploadPhotoButton: TextView = view.findViewById(R.id.edit_photo)
        uploadPhotoButton.setOnClickListener {
            Log.d(TAG, "Checking permissions before showing picker.")
            checkAndRequestPermissions {
                val options = arrayOf("Take Photo", "Choose from Gallery")
                AlertDialog.Builder(requireContext())
                    .setTitle("Select Photo")
                    .setItems(options) { _, which ->
                        when (which) {
                            0 -> cameraPhotoLauncher.launch(null)
                            1 -> pickPhotoLauncher.launch("image/*")
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun showRationale() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permissions Required")
            .setMessage("Camera and media permissions are required to upload or capture a photo. Please grant the permissions to proceed.")
            .setPositiveButton("Grant Permissions") { _, _ ->
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_MEDIA_IMAGES
                    )
                )
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                Log.d(TAG, "Permissions denied by user.")
                showToast("Permissions are required for this feature.")
            }
            .show()
    }

    private fun launchCropActivity(uri: Uri) {
        try {
            Log.d(TAG, "Launching crop activity for URI: $uri")
            val cropIntent = Intent("com.android.camera.action.CROP").apply {
                setDataAndType(uri, "image/*")
                putExtra("crop", "true")
                putExtra("aspectX", 1)
                putExtra("aspectY", 1)
                putExtra("outputX", 500)
                putExtra("outputY", 500)
                putExtra("scale", true)
                putExtra("return-data", false)
                val destinationUri = Uri.fromFile(File(requireContext().cacheDir, "cropped_image.jpg"))
                putExtra("output", destinationUri)
            }
            cropPhotoLauncher.launch(cropIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching crop activity: ${e.message}", e)
            showToast("This device does not support cropping.")
        }
    }

    private fun saveBitmapToCache(bitmap: Bitmap): Uri {
        Log.d(TAG, "Saving bitmap to cache.")
        val cachePath = File(requireContext().cacheDir, "images").apply { mkdirs() }
        val file = File(cachePath, "captured_image.jpg")
        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        }
        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider", // Matches manifest authority
            file
        )
    }

    private fun checkAndRequestPermissions(action: () -> Unit) {
        onPermissionsGranted = action // Save the action to perform later

        val cameraPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
        val readMediaPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)

        Log.d(TAG, "Checking permissions:")
        Log.d(TAG, "Camera Permission: ${if (cameraPermission == PackageManager.PERMISSION_GRANTED) "Granted" else "Denied"}")
        Log.d(TAG, "Read Media Images Permission: ${if (readMediaPermission == PackageManager.PERMISSION_GRANTED) "Granted" else "Denied"}")

        when {
            // All permissions are granted
            cameraPermission == PackageManager.PERMISSION_GRANTED &&
                    readMediaPermission == PackageManager.PERMISSION_GRANTED -> {
                Log.d(TAG, "All permissions granted.")
                action()
            }

            // Show rationale if needed
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) ||
                    shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_IMAGES) -> {
                showRationale() // Show a rationale dialog
            }

            // Request all permissions
            else -> {
                Log.d(TAG, "Requesting all permissions.")
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_MEDIA_IMAGES
                    )
                )
            }
        }
    }

    private fun uploadPhotoToViewModel(photoUri: Uri) {
        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "User not logged in while uploading photo.")
            showToast("User not logged in.")
            return
        }

        try {
            Log.d(TAG, "Processing selected photo: $photoUri")
            val inputStream = requireContext().contentResolver.openInputStream(photoUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
            val compressedImage = byteArrayOutputStream.toByteArray()
            val base64String = Base64.encodeToString(compressedImage, Base64.DEFAULT)

            Log.d(TAG, "Uploading photo as Base64 string.")
            viewModel.uploadProfilePhoto(
                currentUser.uid,
                base64String,
                onSuccess = {
                    Log.d(TAG, "Photo uploaded successfully.")
                    displayProfilePhoto(bitmap)
                },
                onError = {
                    Log.e(TAG, "Error uploading photo: $it")
                    showToast("Error uploading photo: $it")
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error processing photo: ${e.message}", e)
            showToast("Error processing photo: ${e.message}")
        }
    }

    private fun displayProfilePhoto(bitmap: Bitmap) {
        Log.d(TAG, "Displaying profile photo.")
        val profileImageView: ImageView = view?.findViewById(R.id.profile_picture) ?: return
        profileImageView.setImageBitmap(bitmap)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CROP_IMAGE_REQUEST_CODE) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                val croppedUri = data?.data
                if (croppedUri != null) {
                    Log.d(TAG, "Cropped image URI: $croppedUri")
                    uploadPhotoToViewModel(croppedUri)
                } else {
                    Log.e(TAG, "No cropped URI returned, uploading original photo.")
                    showToast("Cropping failed. Uploading the original photo.")
                }
            } else {
                Log.e(TAG, "Crop operation was canceled or failed. Uploading original photo.")
                showToast("Cropping canceled. Uploading original photo.")
            }
        }
    }

    private fun navigateToLogin() {
        Log.d(TAG, "Navigating to login screen.")
        startActivity(Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    private fun navigateToAppSettings() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permissions Required")
            .setMessage("You need to grant camera and storage permissions for this feature. Open app settings to enable them.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showToast(message: String) {
        Log.d(TAG, "Showing toast: $message")
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "AccountFragment"
    }

    private fun logCurrentPermissions() {
        val cameraGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val readStorageGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val writeStorageGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

        Log.d(TAG, "Current Permissions Status:")
        Log.d(TAG, "Camera Permission: ${if (cameraGranted) "Granted" else "Denied"}")
        Log.d(TAG, "Read Storage Permission: ${if (readStorageGranted) "Granted" else "Denied"}")
        Log.d(TAG, "Write Storage Permission: ${if (writeStorageGranted) "Granted" else "Denied"}")
    }

}
