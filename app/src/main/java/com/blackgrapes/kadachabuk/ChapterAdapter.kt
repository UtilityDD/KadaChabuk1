package com.blackgrapes.kadachabuk

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChapterAdapter(private var chapters: List<Chapter>) :
    RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chapter_card, parent, false) // Ensure R is imported correctly
        return ChapterViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChapterViewHolder, position: Int) {
        val chapter = chapters[position]
        holder.bind(chapter)
    }

    override fun getItemCount(): Int = chapters.size

    fun updateChapters(newChapters: List<Chapter>) {
        chapters = newChapters
        notifyDataSetChanged() // Consider using DiffUtil for better performance
    }

    class ChapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Ensure these IDs match your item_chapter_card.xml
        private val headingTextView: TextView = itemView.findViewById(R.id.textViewHeading)
        private val writerTextView: TextView = itemView.findViewById(R.id.textViewWriter)
        private val dateTextView: TextView = itemView.findViewById(R.id.textViewDate)

        fun bind(chapter: Chapter) {
            headingTextView.text = chapter.heading
            writerTextView.text = chapter.writer
            dateTextView.text = chapter.date ?: "N/A" // Display "N/A" if date is null or blank
        }
    }
}
