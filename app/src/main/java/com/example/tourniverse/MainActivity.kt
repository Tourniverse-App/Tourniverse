package com.example.tourniverse

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Navigation setup
        val navController = findNavController(R.id.nav_host_fragment)
        val bottomNavView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavView.setupWithNavController(navController)
    }

    /**
     * Builds ActionCodeSettings for email authentication links
     */
    private fun buildActionCodeSettings(): ActionCodeSettings {
        return ActionCodeSettings.newBuilder()
            .setUrl("https://www.example.com/finishSignUp?cartId=1234") // Must be whitelisted in Firebase Console
            .setHandleCodeInApp(true) // Handle link in the app
            .setIOSBundleId("com.example.ios")
            .setAndroidPackageName("com.example.tourniverse", true, "12") // Replace with your app's package name
            .build()
    }

    /**
     * Sends a sign-in link to the provided email using Firebase Authentication
     */
    private fun sendSignInLink(email: String) {
        val actionCodeSettings = buildActionCodeSettings()

        Firebase.auth.sendSignInLinkToEmail(email, actionCodeSettings)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Email sent successfully.")
                    Toast.makeText(this, "Sign-in link sent to $email", Toast.LENGTH_LONG).show()
                } else {
                    val errorMessage = task.exception?.message ?: "Unknown error"
                    Log.e(TAG, "Error: $errorMessage")
                    Toast.makeText(this, "Failed to send sign-in link: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
    }
}
