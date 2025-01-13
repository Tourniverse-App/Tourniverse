package com.example.tourniverse.activities

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.tourniverse.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    // Declare fields
    private lateinit var usernameField: EditText
    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var registerButton: Button
    private lateinit var loginLink: TextView

    private lateinit var auth: FirebaseAuth
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
    private fun registerUser(username: String, email: String, password: String) {
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
                        "email" to email
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

    private fun createProgressDialog(): AlertDialog {
        val builder = AlertDialog.Builder(this)
        builder.setView(layoutInflater.inflate(R.layout.dialog_loading, null)) // Use your existing layout
        builder.setCancelable(false) // Prevent dismissal by tapping outside
        return builder.create()
    }
}
