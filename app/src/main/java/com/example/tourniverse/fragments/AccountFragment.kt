package com.example.tourniverse.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.tourniverse.R
import com.example.tourniverse.activities.LoginActivity
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.example.tourniverse.utils.FirebaseHelper
import com.example.tourniverse.viewmodels.AccountViewModel
import com.google.android.gms.tasks.Tasks

class AccountFragment : Fragment() {

    private lateinit var viewModel: AccountViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_account, container, false)
        viewModel = ViewModelProvider(this).get(AccountViewModel::class.java)

        val usernameEditText: EditText = view.findViewById(R.id.edit_name)
        val emailEditText: EditText = view.findViewById(R.id.edit_email)
        val bioEditText: EditText = view.findViewById(R.id.edit_bio)
        val saveButton: Button = view.findViewById(R.id.save_button)
        val resetPasswordButton: Button = view.findViewById(R.id.reset_password)
        val deleteAccountButton: Button = view.findViewById(R.id.delete_account)

        val currentUser = Firebase.auth.currentUser
        currentUser?.let { user ->
            emailEditText.setText(user.email.orEmpty())
            viewModel.fetchUserDetails(user.uid) { username, bio ->
                usernameEditText.setText(username)
                bioEditText.setText(bio)
            }
        }

        saveButton.setOnClickListener {
            viewModel.saveProfile(
                currentUser,
                usernameEditText.text.toString(),
                emailEditText.text.toString(),
                bioEditText.text.toString(),
                onSuccess = { showToast("Profile updated successfully!") },
                onError = { showToast(it) }
            )
        }

        resetPasswordButton.setOnClickListener {
            viewModel.sendPasswordReset(
                currentUser?.email,
                onSuccess = { showToast("Password reset email sent!") },
                onError = { showToast(it) }
            )
        }

        deleteAccountButton.setOnClickListener {
            viewModel.deleteAccount(
                currentUser,
                onSuccess = { navigateToLogin() },
                onError = { showToast(it) }
            )
        }

        return view
    }

    private fun navigateToLogin() {
        startActivity(Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
