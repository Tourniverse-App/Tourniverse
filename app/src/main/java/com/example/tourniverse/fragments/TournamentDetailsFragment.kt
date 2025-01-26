package com.example.tourniverse.fragments

import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.example.tourniverse.R
import com.example.tourniverse.adapters.TournamentPagerAdapter
import com.example.tourniverse.viewmodels.TournamentDetailsViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

/**
 * A fragment that displays the details of a tournament.
 */
class TournamentDetailsFragment : Fragment() {

    private var tournamentId: String? = null
    private lateinit var tvTournamentName: TextView
    private lateinit var tvTournamentType: TextView
    private lateinit var tvTournamentFormat: TextView
    private lateinit var tvTournamentDescription: TextView

    private lateinit var viewModel: TournamentDetailsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve tournamentId from arguments
        tournamentId = arguments?.getString("tournamentId")
        if (tournamentId.isNullOrEmpty()) {
            Log.e("TournamentDetails", "Tournament ID is null or empty.")
            Toast.makeText(requireContext(), "Invalid tournament ID.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_tournament_details, container, false)

        viewModel = ViewModelProvider(this).get(TournamentDetailsViewModel::class.java)

        // Initialize views
        tvTournamentName = view.findViewById(R.id.tvTournamentName)
        tvTournamentType = view.findViewById(R.id.tvTournamentType)
        tvTournamentFormat = view.findViewById(R.id.tvTournamentFormat)
        tvTournamentDescription = view.findViewById(R.id.tvTournamentDescription)

        // Fetch tournament details
        if (!tournamentId.isNullOrEmpty()) {
            fetchTournamentDetails()
        }

        // Setup TabLayout and ViewPager2
        setupViewPager(view)

        return view
    }

    private fun fetchTournamentDetails() {
        tournamentId?.let { id ->
            viewModel.fetchTournamentDetails(
                id,
                onSuccess = { name, privacy, description, format ->
                    tvTournamentName.text = name
                    tvTournamentType.text = "Type: $privacy"
                    tvTournamentFormat.text = "Format: $format"
                    tvTournamentDescription.text = description

                    if (format == "Knockout") {
                        viewModel.initializeKnockoutBracket(id)
                    }
                },
                onError = { errorMessage ->
                    Log.e("TournamentDetailsFragment", errorMessage)
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun setupViewPager(view: View) {
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPager)

        val adapter = TournamentPagerAdapter(requireActivity(), tournamentId ?: "", "Tables")
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Social"
                1 -> "Fixtures"
                2 -> "Statistics"
                3 -> "Settings"
                else -> "Tab $position"
            }
        }.attach()

        // Wrap TabLayout in a FrameLayout
        val frameLayout = tabLayout.parent as ViewGroup

        // Create the highlighter view
        val highlighter = View(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                resources.getDimensionPixelSize(R.dimen.tab_highlight_height)
            ).apply {
                gravity = android.view.Gravity.BOTTOM
            }
            setBackgroundResource(R.drawable.tab_highlighter) // Rounded drawable for the highlighter
        }
        frameLayout.addView(highlighter)

        // Initialize the highlighter position
        highlighter.post {
            val initialTab = tabLayout.getTabAt(0)?.view
            highlighter.layoutParams.width = initialTab?.width ?: 0 // Match the initial tab width
            val x = initialTab?.x ?: 0f
            highlighter.translationX = x
        }

        // Animate the highlighter on tab selection
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    highlighter.layoutParams.width = tab.view.width // Match the tab width dynamically
                    val x = tab.view.x
                    ObjectAnimator.ofFloat(highlighter, "translationX", x).apply {
                        duration = 500
                        interpolator = AccelerateDecelerateInterpolator()
                        start()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

}
