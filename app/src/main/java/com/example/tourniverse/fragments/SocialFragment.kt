package com.example.tourniverse.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.adapters.PostAdapter
import com.example.tourniverse.models.Post

class SocialFragment : Fragment() {

    private lateinit var postRecyclerView: RecyclerView
    private lateinit var postInput: EditText
    private lateinit var sendIcon: ImageView
    private lateinit var attachIcon: ImageView
    private val posts = mutableListOf<Post>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_social, container, false)

        postRecyclerView = view.findViewById(R.id.postsRecyclerView)
        postInput = view.findViewById(R.id.newPostInput)
        sendIcon = view.findViewById(R.id.sendPostButton)
        attachIcon = view.findViewById(R.id.attachmentIcon)

        postRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Pre-populated posts
        posts.add(Post("Alice", "Hapoel zonaaaaa", 3, mutableListOf("Great post!", "Welcome!")))
        posts.add(Post("Bob", "איך זה לקבל 5?!!!", 5, mutableListOf("Nice!", "Keep it up!")))

        val adapter = PostAdapter(posts)
        postRecyclerView.adapter = adapter

        sendIcon.setOnClickListener {
            val content = postInput.text.toString()
            if (content.isNotEmpty()) {
                val newPost = Post("User", content)
                posts.add(0, newPost)
                adapter.notifyItemInserted(0)
                postRecyclerView.scrollToPosition(0)
                postInput.text.clear()
            }
        }

        // TODO: Add Firebase integration for sharing posts globally

        return view
    }
}

