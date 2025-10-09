package com.blackgrapes.kadachabuk

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class SearchResult(val chapter: Chapter, val matchCount: Int)

class SearchResultAdapter(private var searchResults: List<SearchResult>) :
    RecyclerView.Adapter<SearchResultAdapter.SearchResultViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result_card, parent, false)
        return SearchResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        val searchResult = searchResults[position]
        holder.bind(searchResult)
    }

    override fun getItemCount(): Int = searchResults.size

    fun updateResults(newResults: List<SearchResult>) {
        searchResults = newResults
        notifyDataSetChanged()
    }

    class SearchResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headingTextView: TextView = itemView.findViewById(R.id.textViewHeading)
        private val serialTextView: TextView = itemView.findViewById(R.id.textViewSerial)
        private val dateTextView: TextView = itemView.findViewById(R.id.textViewDate)

        fun bind(searchResult: SearchResult) {
            val chapter = searchResult.chapter
            headingTextView.text = chapter.heading
            serialTextView.text = chapter.serial
            dateTextView.text = "${searchResult.matchCount} matches"

            itemView.setOnClickListener {
                val context = it.context
                val intent = Intent(context, DetailActivity::class.java).apply {
                    putExtra("EXTRA_HEADING", chapter.heading)
                    putExtra("EXTRA_DATE", chapter.date)
                    putExtra("EXTRA_DATA", chapter.dataText)
                    putExtra("EXTRA_WRITER", chapter.writer)
                    putExtra("EXTRA_SERIAL", chapter.serial)
                    putExtra("EXTRA_LANGUAGE_CODE", chapter.languageCode)
                }
                context.startActivity(intent)
            }
        }
    }
}