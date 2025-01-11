package com.example.tourniverse.adapters

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.fragments.TournamentSettingsFragment
import com.example.tourniverse.models.User
import com.google.firebase.firestore.FirebaseFirestore

class MembersAdapter(
    private val tournamentId: String,
    private val isOwner: Boolean,
    private val fragment: Fragment,
    private val ownerId: String
) : ListAdapter<User, MembersAdapter.MemberViewHolder>(MemberViewHolder.MemberDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user, tournamentId, isOwner, fragment, ownerId)
    }

    class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.text_username)
        private val profileImageView: ImageView = itemView.findViewById(R.id.image_profile)
        private val removeButton: Button = itemView.findViewById(R.id.button_remove)

        fun bind(
            user: User,
            tournamentId: String,
            isOwner: Boolean,
            fragment: Fragment,
            ownerId: String
        ) {
            nameTextView.text = user.name

            // Placeholder for profile image
            profileImageView.setImageResource(R.drawable.ic_user)

            // Show remove button only if the logged-in user is the owner and the member is not the owner
            if (isOwner && user.userId != ownerId) {
                removeButton.visibility = View.VISIBLE
                removeButton.setOnClickListener {
                    showRemoveConfirmation(user, tournamentId, fragment)
                }
            } else {
                removeButton.visibility = View.GONE
            }
        }

        private fun showRemoveConfirmation(user: User, tournamentId: String, fragment: Fragment) {
            AlertDialog.Builder(fragment.requireContext())
                .setTitle("Remove Member")
                .setMessage("Are you sure you want to remove ${user.name}?")
                .setPositiveButton("Yes") { _, _ ->
                    removeMember(user, tournamentId, fragment)
                }
                .setNegativeButton("No", null)
                .show()
        }

        private fun removeMember(user: User, tournamentId: String, fragment: Fragment) {
            val db = FirebaseFirestore.getInstance()

            db.collection("tournaments").document(tournamentId)
                .update(
                    "viewers",
                    com.google.firebase.firestore.FieldValue.arrayRemove(user.userId)
                )
                .addOnSuccessListener {
                    db.collection("tournaments").document(tournamentId)
                        .update(
                            "memberCount",
                            com.google.firebase.firestore.FieldValue.increment(-1)
                        )
                        .addOnSuccessListener {
                            db.collection("users").document(user.userId)
                                .update(
                                    "viewedTournaments",
                                    com.google.firebase.firestore.FieldValue.arrayRemove(
                                        tournamentId
                                    )
                                )
                                .addOnSuccessListener {
                                    android.widget.Toast.makeText(
                                        fragment.requireContext(),
                                        "${user.name} removed",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()

                                    // Access the adapter from TournamentSettingsFragment
                                    val adapter =
                                        (fragment as TournamentSettingsFragment).membersAdapter
                                    val updatedList =
                                        adapter.currentList.filter { it.userId != user.userId }
                                    adapter.submitList(updatedList.toMutableList())

                                    // Refresh the member count
                                    fragment.refreshTournamentSettings()
                                }
                                .addOnFailureListener {
                                    android.widget.Toast.makeText(
                                        fragment.requireContext(),
                                        "Failed to update user viewedTournaments",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                        .addOnFailureListener {
                            android.widget.Toast.makeText(
                                fragment.requireContext(),
                                "Failed to update member count",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                .addOnFailureListener {
                    android.widget.Toast.makeText(
                        fragment.requireContext(),
                        "Failed to remove member",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
        }

        class MemberDiffCallback : DiffUtil.ItemCallback<User>() {
            override fun areItemsTheSame(oldItem: User, newItem: User): Boolean =
                oldItem.userId == newItem.userId

            override fun areContentsTheSame(oldItem: User, newItem: User): Boolean =
                oldItem == newItem
        }
    }
}
