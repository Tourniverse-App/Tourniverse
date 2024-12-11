package com.example.tourniverse

import android.os.Bundle
import android.util.Log
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

        val navController = findNavController(R.id.nav_host_fragment)
        val bottomNavView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavView.setupWithNavController(navController)
    }

    private fun buildActionCodeSettings(): ActionCodeSettings {
        return ActionCodeSettings.newBuilder()
            .setUrl("https://www.example.com/finishSignUp?cartId=1234")
            .setHandleCodeInApp(true)
            .setIOSBundleId("com.example.ios")
            .setAndroidPackageName("com.example.android", true, "12")
            .build()
    }

    private fun sendSignInLink(email: String) {
        val actionCodeSettings = buildActionCodeSettings()
        Firebase.auth.sendSignInLinkToEmail(email, actionCodeSettings)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Email sent.")
                } else {
                    Log.e(TAG, "Error: ${task.exception?.message}")
                }
            }
    }
}


//package com.example.tourniverse
//
//import android.os.Bundle
//import android.util.Log
//import androidx.appcompat.app.AppCompatActivity
//import androidx.navigation.findNavController
//import androidx.navigation.ui.setupWithNavController
//import com.google.android.material.bottomnavigation.BottomNavigationView
//import com.google.firebase.Firebase
//import com.google.firebase.auth.ActionCodeSettings
//import com.google.firebase.auth.actionCodeSettings
//import com.google.firebase.auth.auth
//
//class MainActivity : AppCompatActivity() {
//
//    private val TAG = "MainActivity"
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        val navController = findNavController(R.id.nav_host_fragment)
//        val bottomNavView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
//        bottomNavView.setupWithNavController(navController)
//
//
//    }
//
//    private fun buildActionCodeSettings() {
//        // [START auth_build_action_code_settings]
//        val actionCodeSettings = actionCodeSettings {
//            // URL you want to redirect back to. The domain (www.example.com) for this
//            // URL must be whitelisted in the Firebase Console.
//            url = "https://www.example.com/finishSignUp?cartId=1234"
//            // This must be true
//            handleCodeInApp = true
//            setIOSBundleId("com.example.ios")
//            setAndroidPackageName(
//                "com.example.android",
//                true, // installIfNotAvailable
//                "12", // minimumVersion
//            )
//        }
//        // [END auth_build_action_code_settings]
//    }
//
//    private fun sendSignInLink(email: String, actionCodeSettings: ActionCodeSettings) {
//        // [START auth_send_sign_in_link]
//        Firebase.auth.sendSignInLinkToEmail(email, actionCodeSettings)
//            .addOnCompleteListener { task ->
//                if (task.isSuccessful) {
//                    Log.d(TAG, "Email sent.")
//                }
//            }
//        // [END auth_send_sign_in_link]
//    }
//
//
//}
