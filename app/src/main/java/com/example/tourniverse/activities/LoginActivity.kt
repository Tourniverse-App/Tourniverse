package com.example.tourniverse.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tourniverse.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val emailField = findViewById<EditText>(R.id.etEmail)
        val passwordField = findViewById<EditText>(R.id.etPassword)
        val loginButton = findViewById<Button>(R.id.btnLogin)
        val googleSignInButton = findViewById<LinearLayout>(R.id.btnGoogleSignIn)
        val forgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val registerLink = findViewById<TextView>(R.id.tvRegister)

        configureGoogleSignIn()

        googleSignInButton.setOnClickListener {
            signIn()
        }

        loginButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                if (password.length < 6) {
                    Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                } else {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }

        registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        forgotPassword.setOnClickListener {
            val email = emailField.text.toString()
            if (email.isNotEmpty()) {
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Failed to send reset email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please enter your email to reset password", Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    private fun configureGoogleSignIn() {
        Log.d(TAG, "Configuring Google Sign-In")
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        Log.d(TAG, "GoogleSignInClient initialized")
    }

    private fun signIn() {
        Log.d(TAG, "Starting Google sign-in process")
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
        Log.d(TAG, "Sign-in intent launched")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult called with requestCode: $requestCode, resultCode: $resultCode")

        if (requestCode == RC_SIGN_IN) {
            Log.d(TAG, "Processing Google Sign-In result")
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "Google Sign-In successful, account ID: ${account.id}")
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w(TAG, "Google Sign-In failed, error code: ${e.statusCode}", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        Log.d(TAG, "Authenticating with Firebase using Google token: $idToken")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Firebase authentication with Google succeeded")
                    val user = auth.currentUser

                    user?.let {
                        val email = it.email ?: "" // Extract email from the FirebaseUser
                        val username = email.substringBefore("@") // Extract part before "@"

                        Log.d(
                            TAG,
                            "Authenticated user UID: ${it.uid}, email: $email, username: $username"
                        )

                        val userRef =
                            FirebaseFirestore.getInstance().collection("users").document(it.uid)

                        userRef.get().addOnCompleteListener { snapshotTask ->
                            if (snapshotTask.isSuccessful && !snapshotTask.result.exists()) {
                                Log.d(TAG, "New Google user detected, setting up...")
                                RegisterActivity.setupNewUser(
                                    userId = it.uid,
                                    username = username,
                                    email = email,
                                    db = FirebaseFirestore.getInstance(),
                                    onSuccess = {
                                        Log.d(TAG, "Google user setup completed successfully")
                                        updateUI(user)
                                    },
                                    onFailure = { error ->
                                        Log.e(TAG, "Failed to setup new user: $error")
                                        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } else {
                                Log.d(
                                    TAG,
                                    "User already exists in Firestore, proceeding to MainActivity"
                                )
                                updateUI(user)
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "Firebase authentication failed", task.exception)
                    updateUI(null)
                }
            }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            Log.d(TAG, "Navigating to MainActivity for user: ${user.uid}")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            Log.d(TAG, "User is null, staying on LoginActivity.")
        }
    }

    companion object {
        private const val TAG = "GoogleActivity"
    }
}
