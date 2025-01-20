package com.example.tourniverse.fragments

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
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

class AccountFragment : Fragment() {

    private lateinit var viewModel: AccountViewModel
    private val pickPhotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            Log.d(TAG, "Photo selected: $uri")
            uploadPhotoToViewModel(it)
        } ?: Log.d(TAG, "No photo selected").also { showToast("No photo selected") }
    }

    // Permissions handling
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[android.Manifest.permission.CAMERA] == true &&
                permissions[android.Manifest.permission.WRITE_EXTERNAL_STORAGE] == true) {
                Log.d(TAG, "All permissions granted.")
            } else {
                showToast("Permissions required for camera and gallery access.")
            }
        }

    // Camera capture launcher
    private val cameraPhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        bitmap?.let {
            Log.d(TAG, "Photo captured with camera.")
            val uri = saveBitmapToCache(it) // Save bitmap to cache for cropping
            launchCropActivity(uri)
        } ?: Log.d(TAG, "No photo captured.")
    }

    // Crop activity launcher
    private val cropPhotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val croppedUri = result.data?.data
            croppedUri?.let {
                Log.d(TAG, "Photo cropped: $it")
                uploadPhotoToViewModel(it)
            } ?: Log.d(TAG, "Crop result returned no URI.")
        } else {
            Log.d(TAG, "Photo cropping canceled.")
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

        // Fetch user details
        viewModel.fetchUserDetails(currentUser.uid) { username, bio ->
            Log.d(TAG, "Fetched user details: username=$username, bio=$bio")
            usernameEditText.setText(username)
            bioEditText.setText(bio)
        }

        // Fetch and display profile photo
        viewModel.fetchUserPhoto(currentUser.uid) { base64String ->
            if (base64String != null) {
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

        setupPhotoPicker(view)
        return view
    }

    private fun setupPhotoPicker(view: View) {
        Log.d(TAG, "Setting up photo picker.")
        val uploadPhotoButton: TextView = view.findViewById(R.id.edit_photo)
        uploadPhotoButton.setOnClickListener {
            val options = arrayOf("Take Photo", "Choose from Gallery")
            AlertDialog.Builder(requireContext())
                .setTitle("Select Photo")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            checkAndRequestPermissions()
                            cameraPhotoLauncher.launch(null)
                        }
                        1 -> pickPhotoLauncher.launch("image/*")
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun launchCropActivity(uri: Uri) {
        Log.d(TAG, "Launching crop activity for URI: $uri")
        val cropIntent = Intent("com.android.camera.action.CROP").apply {
            setDataAndType(uri, "image/*")
            putExtra("crop", "true")
            putExtra("aspectX", 1)
            putExtra("aspectY", 1)
            putExtra("outputX", 500) // Optional: Define the output size
            putExtra("outputY", 500)
            putExtra("scale", true)
            putExtra("return-data", false)
            putExtra("output", uri)
        }
        cropPhotoLauncher.launch(cropIntent)
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

    private fun checkAndRequestPermissions() {
        requestPermissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )
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

    private fun navigateToLogin() {
        Log.d(TAG, "Navigating to login screen.")
        startActivity(Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    private fun showToast(message: String) {
        Log.d(TAG, "Showing toast: $message")
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "AccountFragment"
    }
}
