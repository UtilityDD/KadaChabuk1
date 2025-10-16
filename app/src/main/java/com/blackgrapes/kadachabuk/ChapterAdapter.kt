package com.blackgrapes.kadachabuk

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.R as AppCompatR
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.TimeUnit
import com.google.android.material.card.MaterialCardView

class ChapterAdapter(private var chapters: List<Chapter>) :
    RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chapter_card, parent, false)
        return ChapterViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChapterViewHolder, position: Int) {
        val chapter = chapters[position]
        val context = holder.itemView.context

        // Retrieve the last read serial from SharedPreferences
        val prefs = context.getSharedPreferences("LastReadPrefs", Context.MODE_PRIVATE)
        val lastReadSerial = prefs.getString("lastReadSerial", null)

        holder.serialTextView.text = chapter.serial
        // Pass whether this is the last read chapter to the bind method
        holder.bind(chapter, chapter.serial == lastReadSerial)

        // Handle click → open DetailActivity
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, DetailActivity::class.java).apply {
                // Note: EXTRA_WRITER is still passed for DetailActivity, but not displayed on the card.
                putExtra("EXTRA_HEADING", chapter.heading)
                putExtra("EXTRA_DATE", chapter.date ?: "N/A")
                putExtra("EXTRA_WRITER", chapter.writer)
                putExtra("EXTRA_DATA", chapter.dataText) // ✅ use dataText instead of data
                putExtra("EXTRA_SERIAL", chapter.serial)
                putExtra("EXTRA_LANGUAGE_CODE", chapter.languageCode)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = chapters.size

    fun updateChapters(newChapters: List<Chapter>, lastReadSerial: String? = null) {
        chapters = newChapters
        notifyDataSetChanged()
    }

    class ChapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView as MaterialCardView
        private val headingTextView: TextView = itemView.findViewById(R.id.textViewHeading)
        private val dateTextView: TextView = itemView.findViewById(R.id.textViewDate)
        private val historyTextView: TextView = itemView.findViewById(R.id.textViewHistory)
        val serialTextView: TextView = itemView.findViewById(R.id.textViewSerial)

        fun bind(chapter: Chapter, isLastRead: Boolean) {
            headingTextView.text = chapter.heading
            // Remove parentheses from the date string, e.g., (dd/mm/yyyy) -> dd/mm/yyyy
            dateTextView.text = chapter.date?.removeSurrounding("(", ")") ?: "N/A"

            // --- Reading History Display Logic ---
            val historyPrefs = itemView.context.getSharedPreferences("ReadingHistoryPrefs", Context.MODE_PRIVATE)
            val historyKeyBase = "${chapter.languageCode}_${chapter.serial}"
            val count = historyPrefs.getInt("count_$historyKeyBase", 0)
            val totalTimeMs = historyPrefs.getLong("time_$historyKeyBase", 0)

            if (count > 0) {
                val formattedTime = TimeUtils.formatDuration(totalTimeMs)
                val historyText = "$count / $formattedTime"
                historyTextView.text = historyText
                historyTextView.visibility = View.VISIBLE
            } else {
                historyTextView.visibility = View.GONE
            }
            // --- End of History Logic ---

            // Visually distinguish the last read chapter
            if (isLastRead) {
                // Example: Change stroke color and width
                cardView.strokeWidth = 4 // Set a noticeable stroke width
                // Use the theme's primary color for the stroke.
                // R.color.design_default_color_primary is a private resource.
                val typedValue = android.util.TypedValue()
                itemView.context.theme.resolveAttribute(AppCompatR.attr.colorPrimary, typedValue, true)
                cardView.strokeColor = typedValue.data

            } else {
                // Reset to default for other items
                cardView.strokeWidth = 0
            }
        }
    }
}
