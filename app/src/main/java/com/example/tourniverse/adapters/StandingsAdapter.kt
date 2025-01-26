package com.example.tourniverse.adapters

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.compose.material3.DatePickerDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.models.Match
import java.util.Calendar
import java.util.Locale

class StandingsAdapter(
    private val items: List<Match>,
    private val updateMatchScores: (Match, Int, Int) -> Unit,
    private val notifyScoresUpdated: () -> Unit,
    private val isOwner: Boolean
) : RecyclerView.Adapter<StandingsAdapter.StandingsViewHolder>() {

    companion object {
        private const val TAG = "StandingsAdapter"
        private const val DATE_PATTERN = "^\\d{2}/\\d{2}/\\d{2}$"
    }

    class StandingsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val teamAName: TextView = view.findViewById(R.id.teamAName)
        val teamBName: TextView = view.findViewById(R.id.teamBName)
        val scoreA: EditText = view.findViewById(R.id.scoreA)
        val scoreB: EditText = view.findViewById(R.id.scoreB)
        val dateInput: EditText = view.findViewById(R.id.dateInput)
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
        // Display date with "DD/MM/YY" as default
        holder.dateInput.setText(match.date ?: "DD/MM/YY")

        // Display scores with "-" as default
        holder.scoreA.setText(match.scoreA?.toString() ?: "-")
        holder.scoreB.setText(match.scoreB?.toString() ?: "-")

        // Make the EditText editable only if the user is the owner
        holder.dateInput.isEnabled

        var isUpdating = false // Prevents recursive updates while editing
        // Add TextWatcher for date validation
        holder.dateInput.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return

                val rawInput = s.toString().replace("/", "") // Remove slashes for processing
                val formattedInput = buildFormattedDate(rawInput)

                isUpdating = true
                holder.dateInput.setText(formattedInput)
                holder.dateInput.setSelection(formattedInput.indexOf('_').takeIf { it != -1 } ?: formattedInput.length) // Set cursor position
                isUpdating = false

                // Update match object only when valid numbers are entered
                if (!formattedInput.contains("_")) {
                    match.date = if (isValidDate(formattedInput)) formattedInput else null.toString()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            private fun buildFormattedDate(input: String): String {
                val builder = StringBuilder("__/__/__")

                for (i in input.indices) {
                    when (i) {
                        0, 1 -> builder.setCharAt(i, input[i]) // Day
                        2, 3 -> builder.setCharAt(i + 1, input[i]) // Month
                        4, 5 -> builder.setCharAt(i + 2, input[i]) // Year
                    }
                }

                return builder.toString()
            }

            private fun isValidDate(date: String): Boolean {
                if (!date.matches(Regex("\\d{2}/\\d{2}/\\d{2}"))) return false

                val parts = date.split("/")
                val day = parts[0].toIntOrNull() ?: return false
                val month = parts[1].toIntOrNull() ?: return false

                if (day !in 1..31 || month !in 1..12) return false

                return true
            }
        })

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
//        Log.d(TAG, "Total items: ${items.size}")
        return items.size
    }
}
