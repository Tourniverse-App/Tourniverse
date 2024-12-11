package com.example.tourniverse

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    // Declare fields
    private lateinit var usernameField: EditText
    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var registerButton: Button
    private lateinit var loginLink: TextView

    private lateinit var auth: FirebaseAuth
    private val databaseReference = FirebaseDatabase.getInstance().reference

    private lateinit var loadingDialog: AlertDialog

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

        // Initialize the loading dialog
        loadingDialog = createLoadingDialog()

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
            finish()
        }
    }

    private fun registerUser(username: String, email: String, password: String) {
        loadingDialog.show()

        // Register user in Firebase Auth
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    val userMap = hashMapOf(
                        "username" to username,
                        "email" to email,
                        "id" to userId,
                        "bio" to "",
                        "imageurl" to "default"
                    )

                    // Save user to Firebase Realtime Database
                    if (userId != null) {
                        databaseReference.child("Users").child(userId).setValue(userMap)
                            .addOnCompleteListener { dbTask ->
                                if (dbTask.isSuccessful) {
                                    loadingDialog.dismiss()
                                    Toast.makeText(
                                        this,
                                        "Registration successful! Please log in.",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    // Navigate to LoginActivity
                                    val intent = Intent(this, LoginActivity::class.java)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    loadingDialog.dismiss()
                                    Toast.makeText(
                                        this,
                                        "Failed to save user data: ${dbTask.exception?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    }
                } else {
                    loadingDialog.dismiss()
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun createLoadingDialog(): AlertDialog {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null)
        return AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // Prevent dismissal by tapping outside
            .create()
    }
}
