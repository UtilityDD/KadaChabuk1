package com.blackgrapes.kadachabuk

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar

private const val NOTES_PREFS = "MyNotesPrefs"
private const val KEY_NOTES = "notes"

class MyNotesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_notes)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar_notes)
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view_notes)
        val noNotesTextView: TextView = findViewById(R.id.text_view_no_notes)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        val notes = getSavedNotes()

        if (notes.isEmpty()) {
            recyclerView.visibility = View.GONE
            noNotesTextView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            noNotesTextView.visibility = View.GONE
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = NoteAdapter(notes)
        }
    }

    private fun getSavedNotes(): List<String> {
        val prefs = getSharedPreferences(NOTES_PREFS, Context.MODE_PRIVATE)
        // Retrieve the set and convert to a list, sort to keep it consistent
        return prefs.getStringSet(KEY_NOTES, emptySet())?.toList()?.sortedDescending() ?: emptyList()
    }
}

class NoteAdapter(private val notes: List<String>) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val noteContent: TextView = view.findViewById(R.id.text_view_note_content)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.noteContent.text = notes[position]
    }

    override fun getItemCount() = notes.size
}