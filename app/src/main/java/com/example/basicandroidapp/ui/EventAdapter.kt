package com.example.basicandroidapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.basicandroidapp.R
import com.example.basicandroidapp.model.MarketEvent

/**
 * Displays a list of recent market events in the News feed.
 * Green impact = positive event, red impact = negative event.
 */
class EventAdapter(private var events: List<MarketEvent>) :
    RecyclerView.Adapter<EventAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDay: TextView = view.findViewById(R.id.tvEventDay)
        val tvTitle: TextView = view.findViewById(R.id.tvEventTitle)
        val tvDescription: TextView = view.findViewById(R.id.tvEventDescription)
        val tvAffected: TextView = view.findViewById(R.id.tvEventAffected)
        val tvImpact: TextView = view.findViewById(R.id.tvEventImpact)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = events[position]

        holder.tvDay.text = "Day ${event.dayOccurred}"
        holder.tvTitle.text = event.title
        holder.tvDescription.text = event.description

        // Show which stocks were affected
        holder.tvAffected.text = when (event.eventType) {
            MarketEvent.EventType.MARKET_WIDE -> "All Markets"
            MarketEvent.EventType.SECTOR_WIDE -> event.affectedSymbols.joinToString(", ")
            MarketEvent.EventType.COMPANY_SPECIFIC -> event.affectedSymbols.firstOrNull() ?: ""
        }

        // Show impact percent in green (positive) or red (negative)
        val sign = if (event.impactPercent >= 0) "+" else ""
        holder.tvImpact.text = "$sign${"%.1f".format(event.impactPercent)}%"
        val colorRes = if (event.isPositive) R.color.stock_green else R.color.stock_red
        holder.tvImpact.setTextColor(holder.itemView.context.getColor(colorRes))
    }

    override fun getItemCount() = events.size

    fun updateEvents(newEvents: List<MarketEvent>) {
        events = newEvents
        notifyDataSetChanged()
    }
}
