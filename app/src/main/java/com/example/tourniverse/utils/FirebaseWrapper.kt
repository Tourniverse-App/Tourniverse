package com.example.tourniverse.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class FirebaseWrapper(private val firebaseAuth: FirebaseAuth) {
    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid
}