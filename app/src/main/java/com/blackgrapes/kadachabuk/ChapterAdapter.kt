package com.blackgrapes.kadachabuk

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChapterAdapter(private var chapters: List<Chapter>) :
    RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chapter_card, parent, false)
        return ChapterViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChapterViewHolder, position: Int) {
        val chapter = chapters[position]
        holder.serialTextView.text = chapter.serial
        holder.bind(chapter)

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

    fun updateChapters(newChapters: List<Chapter>) {
        chapters = newChapters
        notifyDataSetChanged()
    }

    class ChapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headingTextView: TextView = itemView.findViewById(R.id.textViewHeading)
        private val dateTextView: TextView = itemView.findViewById(R.id.textViewDate)
        val serialTextView: TextView = itemView.findViewById(R.id.textViewSerial)

        fun bind(chapter: Chapter) {
            headingTextView.text = chapter.heading
            // Remove parentheses from the date string, e.g., (dd/mm/yyyy) -> dd/mm/yyyy
            dateTextView.text = chapter.date?.removeSurrounding("(", ")") ?: "N/A"
        }
    }
}
