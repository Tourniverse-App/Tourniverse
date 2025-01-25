package com.example.tourniverse.activities

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.tourniverse.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import android.Manifest
import android.text.method.PasswordTransformationMethod
import android.view.MotionEvent
import androidx.core.content.ContextCompat

class RegisterActivity : AppCompatActivity() {

    // Declare fields
    private lateinit var usernameField: EditText
    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var registerButton: Button
    private lateinit var loginLink: TextView
    private var isPasswordVisible = false

    private val TAG = "RegisterActivity"

    lateinit var auth: FirebaseAuth
    private val databaseReference = FirebaseDatabase.getInstance().reference

    private lateinit var progressDialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize fields
        usernameField = findViewById(R.id.etUsername)
        emailField = findViewById(R.id.etEmail)
        passwordField = findViewById(R.id.etPassword)
        registerButton = findViewById(R.id.btnRegister)
        loginLink = findViewById(R.id.tvLogin)

        auth = FirebaseAuth.getInstance()
        progressDialog = createProgressDialog()

        // Add toggle visibility logic for password
        passwordField.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                // Check if the click occurred within the drawable bounds
                val drawableEnd = passwordField.compoundDrawablesRelative[2] // End drawable
                if (drawableEnd != null && event.rawX >= (passwordField.right - passwordField.compoundPaddingEnd)) {
                    togglePasswordVisibility()
                    return@setOnTouchListener true
                }
            }
            false
        }

        // Handle register button click
        registerButton.setOnClickListener {
            val username = usernameField.text.toString()
            val email = emailField.text.toString()
            val password = passwordField.text.toString()

            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            } else {
                registerUser(username, email, password)
            }
        }

        // Navigate to LoginActivity when login link is clicked
        loginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    /**
     * Registers a new user and adds them to Firestore's "users" collection.
     *
     * @param username The desired username for the user.
     * @param email The user's email address.
     * @param password The user's password.
     */
    fun registerUser(username: String, email: String, password: String) {
        progressDialog.show()

        // Register user in Firebase Auth
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener

                    // Initialize user data for Firestore "users" collection
                    val userMap = hashMapOf(
                        "username" to username,
                        "bio" to "This is ${username}'s bio!",
                        "email" to email,
                        "profilePhoto" to ""
                    )

                    val db = FirebaseFirestore.getInstance()
                    val userRef = db.collection("users").document(userId)

                    // Use set() to create or overwrite the user document
                    userRef.set(userMap)
                        .addOnCompleteListener { dbTask ->
                            if (dbTask.isSuccessful) {
                                // Initialize notifications document
                                val notificationsData = hashMapOf(
                                    "Push" to false,
                                    "Scores" to false,
                                    "ChatMessages" to false,
                                    "Comments" to false,
                                    "Likes" to false,
                                )
                                userRef.collection("notifications").document("settings").set(notificationsData)

                                progressDialog.dismiss()
                                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()

                                val sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE)
                                sharedPreferences.edit().putBoolean("permissions_requested", false).apply()

                                // Navigate to MainActivity
                                val intent = Intent(this, MainActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                                finish()
                            } else {
                                progressDialog.dismiss()
                                Toast.makeText(this, "Failed to save user data: ${dbTask.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    /**
     * Toggles the visibility of the password field.
     */
    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Hide the password
            passwordField.transformationMethod = PasswordTransformationMethod.getInstance()
            passwordField.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null, null, ContextCompat.getDrawable(this, R.drawable.ic_eye_closed), null
            )
        } else {
            // Show the password
            passwordField.transformationMethod = null
            passwordField.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null, null, ContextCompat.getDrawable(this, R.drawable.ic_eye_open), null
            )
        }
        // Toggle the state
        isPasswordVisible = !isPasswordVisible
        // Move the cursor to the end of the text
        passwordField.setSelection(passwordField.text.length)
    }

    private fun createProgressDialog(): AlertDialog {
        val builder = AlertDialog.Builder(this)
        builder.setView(layoutInflater.inflate(R.layout.dialog_loading, null)) // Use your existing layout
        builder.setCancelable(false) // Prevent dismissal by tapping outside
        return builder.create()
    }

    companion object {
        fun setupNewUser(
            userId: String,
            username: String,
            email: String,
            db: FirebaseFirestore,
            onSuccess: () -> Unit,
            onFailure: (String) -> Unit
        ) {
            // Initialize user data for Firestore "users" collection
            val userMap = hashMapOf(
                "username" to username,
                "bio" to "This is $username's bio!",
                "email" to email,
                "profilePhoto" to ""
            )

            val userRef = db.collection("users").document(userId)

            // Use set() to create or overwrite the user document
            userRef.set(userMap)
                .addOnSuccessListener {
                    Log.d("RegisterActivity", "User document created in Firestore.")
                    // Initialize notifications document
                    val notificationsData = hashMapOf(
                        "Push" to false,
                        "Scores" to false,
                        "ChatMessages" to false,
                        "Comments" to false,
                        "Likes" to false
                    )
                    userRef.collection("notifications").document("settings").set(notificationsData)
                        .addOnSuccessListener {
                            Log.d("RegisterActivity", "Notifications initialized in Firestore.")
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            Log.e("RegisterActivity", "Failed to initialize notifications: ${e.message}")
                            onFailure("Failed to initialize notifications: ${e.message}")
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("RegisterActivity", "Failed to save user data: ${e.message}")
                    onFailure("Failed to save user data: ${e.message}")
                }
        }
    }

    private fun showPermissionDeniedToast() {
        Toast.makeText(
            this,
            "You can enable permissions in the app settings to use all features.",
            Toast.LENGTH_LONG
        ).show()
    }

    fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}
