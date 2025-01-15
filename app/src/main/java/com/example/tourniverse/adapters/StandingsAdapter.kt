package com.example.tourniverse.adapters

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.models.Match

class StandingsAdapter(
    private val items: List<Match>,
    private val updateMatchScores: (Match, Int, Int) -> Unit,
    private val notifyScoresUpdated: () -> Unit
) : RecyclerView.Adapter<StandingsAdapter.StandingsViewHolder>() {

    companion object {
        private const val TAG = "StandingsAdapter"
    }

    class StandingsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val teamAName: TextView = view.findViewById(R.id.teamAName)
        val teamBName: TextView = view.findViewById(R.id.teamBName)
        val scoreA: EditText = view.findViewById(R.id.scoreA)
        val scoreB: EditText = view.findViewById(R.id.scoreB)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StandingsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_standing, parent, false)
        Log.d(TAG, "ViewHolder created")
        return StandingsViewHolder(view)
    }

    override fun onBindViewHolder(holder: StandingsViewHolder, position: Int) {
        val match = items[position]

        // Set team names
        holder.teamAName.text = match.teamA
        holder.teamBName.text = match.teamB

        // Display scores with "-" as default
        holder.scoreA.setText(match.scoreA?.toString() ?: "-")
        holder.scoreB.setText(match.scoreB?.toString() ?: "-")

        var isUpdating = false // Prevents recursive updates while editing

        // TextWatcher for scoreA
        holder.scoreA.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return // Skip if updating programmatically
                val input = s.toString()

                // Allow only valid inputs (0-9 or "-")
                if (!input.matches(Regex("^[-0-9]*$"))) {
                    isUpdating = true
                    holder.scoreA.setText("-") // Reset to "-"
                    holder.scoreA.setSelection(holder.scoreA.text.length) // Move cursor to end
                    isUpdating = false
                    return
                }

                // Update match score
                val score = if (input == "-") null else input.toIntOrNull()
                match.scoreA = score

                // Trigger notification when the score changes
                notifyScoresUpdated()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // TextWatcher for scoreB
        holder.scoreB.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return // Skip if updating programmatically
                val input = s.toString()

                // Allow only valid inputs (0-9 or "-")
                if (!input.matches(Regex("^[-0-9]*$"))) {
                    isUpdating = true
                    holder.scoreB.setText("-") // Reset to "-"
                    holder.scoreB.setSelection(holder.scoreB.text.length) // Move cursor to end
                    isUpdating = false
                    return
                }

                // Update match score
                val score = if (input == "-") null else input.toIntOrNull()
                match.scoreB = score

                // Trigger notification when the score changes
                notifyScoresUpdated()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }


    override fun getItemCount(): Int {
        Log.d(TAG, "Total items: ${items.size}")
        return items.size
    }
}
