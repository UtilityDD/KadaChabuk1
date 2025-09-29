package com.blackgrapes.kadachabuk

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LanguageAdapter(
    private val languages: List<Pair<String, String>>,
    private val selectedLanguageCode: String?,
    private val onLanguageSelected: (String, String) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_language, parent, false)
        return LanguageViewHolder(view)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        val (langName, langCode) = languages[position]
        holder.bind(langName, langCode == selectedLanguageCode)
        holder.itemView.setOnClickListener {
            onLanguageSelected(langCode, langName)
        }
    }

    override fun getItemCount(): Int = languages.size

    class LanguageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val languageNameTextView: TextView = itemView.findViewById(R.id.language_name)

        fun bind(languageName: String, isSelected: Boolean) {
            languageNameTextView.text = languageName
            itemView.isSelected = isSelected
        }
    }
}