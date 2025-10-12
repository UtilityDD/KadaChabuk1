package com.blackgrapes.kadachabuk

import android.content.Context
import androidx.core.app.ShareCompat
import android.os.Bundle
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
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
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val NOTES_PREFS = "MyNotesPrefs"
private const val KEY_NOTES = "notes"

data class NoteItem(val text: String, val timestamp: Long, val originalJson: String)

class MyNotesActivity : AppCompatActivity()  {

    private lateinit var recyclerView: RecyclerView
    private lateinit var noNotesTextView: TextView
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var notes: MutableList<NoteItem>
    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_notes)

        // Allow content to draw behind the system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Adjust system icon colors based on the current theme (light/dark)
        val controller = ViewCompat.getWindowInsetsController(window.decorView)
        val isNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        controller?.isAppearanceLightStatusBars = isNightMode

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
        noteAdapter = NoteAdapter(
            notes,
            onDeleteClick = { noteItem, position ->
                showDeleteConfirmationDialog(noteItem, position)
            },
            onShareClick = { noteItem -> shareNote(noteItem.text) }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = noteAdapter

        updateEmptyState()
    }

    private fun updateTitle() {
        toolbar.title = "My Notes (${noteAdapter.itemCount})"
    }

    private fun shareNote(noteText: String) {
        ShareCompat.IntentBuilder(this)
            .setType("text/plain")
            .setText(noteText)
            .startChooser()
    }

    private fun getSavedNotes(): List<NoteItem> {
        val prefs = getSharedPreferences(NOTES_PREFS, Context.MODE_PRIVATE)
        val notesJsonSet = prefs.getStringSet(KEY_NOTES, emptySet()) ?: emptySet()

        return notesJsonSet.mapNotNull { jsonString ->
            try {
                val jsonObject = JSONObject(jsonString)
                val text = jsonObject.getString("text")
                val timestamp = jsonObject.getLong("timestamp")
                NoteItem(text, timestamp, jsonString)
            } catch (e: Exception) {
                // Handle legacy plain string notes if they exist
                NoteItem(jsonString, 0, jsonString)
            }
        }.sortedByDescending { it.timestamp }
    }

    private fun showDeleteConfirmationDialog(noteItem: NoteItem, position: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Note")
            .setMessage("Are you sure you want to delete this note?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                deleteNote(noteItem, position)
            }
            .show()
    }

    private fun deleteNote(noteItem: NoteItem, position: Int) {
        // Remove from SharedPreferences
        val prefs = getSharedPreferences(NOTES_PREFS, Context.MODE_PRIVATE)
        val existingNotes = prefs.getStringSet(KEY_NOTES, emptySet())?.toMutableSet() ?: return
        existingNotes.remove(noteItem.originalJson)
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
    private val notes: MutableList<NoteItem>,
    private val onDeleteClick: (NoteItem, Int) -> Unit,
    private val onShareClick: (NoteItem) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    class NoteViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        lateinit var noteContent: TextView
        lateinit var noteDate: TextView
        lateinit var shareButton: ImageButton
        lateinit var deleteButton: ImageButton

        init {
            // Check if the item view is a ViewGroup to add our button layout
            if (view is ViewGroup) {
                noteContent = view.findViewById(R.id.text_view_note_content)
                noteDate = view.findViewById(R.id.text_view_note_date)

                // Programmatically create a horizontal LinearLayout for the buttons
                val buttonLayout = LinearLayout(view.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.END
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }

                // Inflate the buttons and add them to our new layout
                shareButton = LayoutInflater.from(view.context)
                    .inflate(R.layout.item_note, null)
                    .findViewById(R.id.button_share_note)
                deleteButton = LayoutInflater.from(view.context)
                    .inflate(R.layout.item_note, null)
                    .findViewById(R.id.button_delete_note)

                (shareButton.parent as? ViewGroup)?.removeView(shareButton)
                (deleteButton.parent as? ViewGroup)?.removeView(deleteButton)

                buttonLayout.addView(shareButton)
                buttonLayout.addView(deleteButton)

                // Add the button layout to the main item view
                view.addView(buttonLayout)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note_base, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val noteItem = notes[position]
        holder.noteContent.text = noteItem.text

        if (noteItem.timestamp > 0) {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            holder.noteDate.text = sdf.format(Date(noteItem.timestamp))
            holder.noteDate.visibility = View.VISIBLE
        } else {
            holder.noteDate.visibility = View.GONE
        }
        holder.shareButton.setOnClickListener { onShareClick(noteItem) }
        holder.deleteButton.setOnClickListener { onDeleteClick(noteItem, holder.adapterPosition) }
    }

    override fun getItemCount() = notes.size
}