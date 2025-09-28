package com.blackgrapes.kadachabuk

import android.content.Intent // <-- IMPORT THIS
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
        holder.bind(chapter)

        // --- START OF MODIFICATION FOR STEP 2 ---
        holder.itemView.setOnClickListener { // Or use a specific ID if your card has one e.g. holder.cardView.setOnClickListener
            val context = holder.itemView.context
            val intent = Intent(context, DetailActivity::class.java).apply {
                // Assuming your Chapter data class has 'heading', 'date', and a 'data' field for details
                putExtra("EXTRA_HEADING", chapter.heading)
                putExtra("EXTRA_DATE", chapter.date ?: "N/A") // Pass what's displayed or the original
                putExtra("EXTRA_DATA", chapter.data) // <<<==== ADD THIS: Assuming 'data' is the field with detailed content
                // If your Chapter object doesn't have a 'data' field for the full content,
                // you'll need to add it to your Chapter data class and populate it.
            }
            context.startActivity(intent)
        }
        // --- END OF MODIFICATION FOR STEP 2 ---
    }

    override fun getItemCount(): Int = chapters.size

    fun updateChapters(newChapters: List<Chapter>) {
        chapters = newChapters
        notifyDataSetChanged()
    }

    class ChapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headingTextView: TextView = itemView.findViewById(R.id.textViewHeading)
        private val writerTextView: TextView = itemView.findViewById(R.id.textViewWriter)
        private val dateTextView: TextView = itemView.findViewById(R.id.textViewDate)

        fun bind(chapter: Chapter) {
            headingTextView.text = chapter.heading
            writerTextView.text = chapter.writer
            dateTextView.text = chapter.date ?: "N/A"
        }
    }
}

// Ensure your Chapter data class looks something like this (or has these fields)
// data class Chapter(
//    val heading: String,
//    val writer: String,
//    val date: String?,
//    val data: String // <<<==== This field would hold the detailed content
// )
