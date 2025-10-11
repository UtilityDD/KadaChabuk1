package com.blackgrapes.kadachabuk

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.appbar.MaterialToolbar

private const val NOTES_PREFS = "MyNotesPrefs"
private const val KEY_NOTES = "notes"

class MyNotesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var noNotesTextView: TextView
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var notes: MutableList<String>
    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_notes)

        // Allow content to draw behind the system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        toolbar = findViewById(R.id.toolbar_notes)
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the top inset as padding to push the toolbar's content down, without changing its height
            view.setPadding(view.paddingLeft, insets.top, view.paddingRight, 0)
            windowInsets
        }

        recyclerView = findViewById(R.id.recycler_view_notes)
        noNotesTextView = findViewById(R.id.text_view_no_notes)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        notes = getSavedNotes().toMutableList()
        updateTitle()
        noteAdapter = NoteAdapter(notes) { note, position ->
            showDeleteConfirmationDialog(note, position)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = noteAdapter

        updateEmptyState()
    }

    private fun updateTitle() {
        toolbar.title = "My Notes (${notes.size})"
    }

    private fun getSavedNotes(): List<String> {
        val prefs = getSharedPreferences(NOTES_PREFS, Context.MODE_PRIVATE)
        // Retrieve the set and convert to a list, sort to keep it consistent
        return prefs.getStringSet(KEY_NOTES, emptySet())?.toList()?.sortedDescending() ?: emptyList()
    }

    private fun showDeleteConfirmationDialog(note: String, position: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Note")
            .setMessage("Are you sure you want to delete this note?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                deleteNote(note, position)
            }
            .show()
    }

    private fun deleteNote(note: String, position: Int) {
        // Remove from SharedPreferences
        val prefs = getSharedPreferences(NOTES_PREFS, Context.MODE_PRIVATE)
        val existingNotes = prefs.getStringSet(KEY_NOTES, emptySet())?.toMutableSet() ?: return
        existingNotes.remove(note)
        prefs.edit().putStringSet(KEY_NOTES, existingNotes).apply()

        // Remove from the local list and notify the adapter
        notes.removeAt(position)
        noteAdapter.notifyItemRemoved(position)

        updateEmptyState()
        updateTitle()
    }

    private fun updateEmptyState() {
        if (notes.isEmpty()) {
            recyclerView.visibility = View.GONE
            noNotesTextView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            noNotesTextView.visibility = View.GONE
        }
    }
}

class NoteAdapter(
    private val notes: List<String>,
    private val onDeleteClick: (String, Int) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val noteContent: TextView = view.findViewById(R.id.text_view_note_content)
        val deleteButton: ImageButton = view.findViewById(R.id.button_delete_note)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.noteContent.text = note
        holder.deleteButton.setOnClickListener { onDeleteClick(note, holder.adapterPosition) }
    }

    override fun getItemCount() = notes.size
}