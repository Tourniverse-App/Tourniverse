package com.example.tourniverse.adapters

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.models.Tournament

class TournamentAdapter(
    private val tournaments: List<Tournament>,
    private val onTournamentClick: (Tournament) -> Unit
) : RecyclerView.Adapter<TournamentAdapter.ViewHolder>() {

    private var filteredTournaments = tournaments.toMutableList()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tournamentName: TextView = itemView.findViewById(R.id.tvTournamentName)
        val tournamentType: TextView = itemView.findViewById(R.id.tvTournamentType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tournament, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tournament = filteredTournaments[position]
        holder.tournamentName.text = tournament.name
        holder.tournamentType.text = tournament.type
        holder.itemView.setOnClickListener {
            onTournamentClick(tournament)
        }
    }

    override fun getItemCount() = filteredTournaments.size

    // Function for filtering tournaments based on query
    fun filter(query: String) {
        filteredTournaments = if (query.isEmpty()) {
            tournaments.toMutableList()
        } else {
            tournaments.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.type.contains(query, ignoreCase = true)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    // Search Bar Filter
    fun getSearchTextWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        }
    }
}
