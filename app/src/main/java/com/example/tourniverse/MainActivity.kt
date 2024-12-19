package com.example.tourniverse

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private lateinit var navController: NavController
    private var globalTournamentId: String? = null // Global variable to store tournament ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navController = findNavController(R.id.nav_host_fragment)
        val bottomNavView: BottomNavigationView = findViewById(R.id.bottom_navigation)

        // Attach navigation controller to BottomNavigationView
        bottomNavView.setupWithNavController(navController)

        // Fetch global tournament ID
        fetchGlobalTournamentId()

        // Handle reselection and proper backstack clearing
        bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    handleNavigation(R.id.nav_home)
                    true
                }
                R.id.nav_add -> {
                    handleNavigation(R.id.nav_add)
                    true
                }
                R.id.nav_user -> {
                    handleNavigation(R.id.nav_user)
                    true
                }
                R.id.nav_settings -> {
                    handleNavigation(R.id.nav_settings)
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Handles navigation to the specified destination, passing the tournament ID as arguments.
     *
     * @param destinationId The fragment to navigate to.
     */
    private fun handleNavigation(destinationId: Int) {
        // Check if the current destination is the same as the selected one
        if (navController.currentDestination?.id == destinationId) {
            // Pop back to the selected fragment to reset its state
            navController.popBackStack(destinationId, true)
        }

        val bundle = Bundle().apply {
            globalTournamentId?.let {
                putString("tournamentId", it) // Pass the global tournament ID
            }
        }

        // Navigate to the selected fragment with arguments
        try {
            navController.navigate(destinationId, bundle)
        } catch (e: Exception) {
            Log.e(TAG, "Navigation error: ${e.message}")
        }
    }

    /**
     * Fetches the global tournament ID from Firestore.
     */
    private fun fetchGlobalTournamentId() {
        FirebaseFirestore.getInstance().collection("tournaments")
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    globalTournamentId = documents.documents.first().id
                    Log.d(TAG, "Global Tournament ID fetched: $globalTournamentId")
                } else {
                    Log.e(TAG, "No tournaments found.")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to fetch global tournament ID: ${e.message}")
                Toast.makeText(this, "Unable to fetch tournament information. Please try again later.", Toast.LENGTH_LONG).show()
            }
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
