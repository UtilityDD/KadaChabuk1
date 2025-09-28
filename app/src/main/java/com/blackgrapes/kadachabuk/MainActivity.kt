package com.blackgrapes.kadachabuk

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.glance.visibility
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val bookViewModel: BookViewModel by viewModels()
    private lateinit var chapterAdapter: ChapterAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var languageSpinner: Spinner

    private lateinit var languageCodes: Array<String>
    private lateinit var languageNames: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.recyclerViewChapters)
        progressBar = findViewById(R.id.progressBar)
        languageSpinner = findViewById(R.id.spinnerLanguage)

        // Load language arrays from resources
        languageNames = resources.getStringArray(R.array.language_names)
        languageCodes = resources.getStringArray(R.array.language_codes)

        setupRecyclerView()
        setupLanguageSpinner() // New function to set up the spinner
        observeViewModel()

        // No initial load here; it will be triggered by the spinner's default selection or user interaction
        // If you want a default language loaded on startup before user interaction:
        // bookViewModel.loadChapters(languageCodes[0]) // Loads the first language in the array
    }

    private fun setupRecyclerView() {
        chapterAdapter = ChapterAdapter(emptyList())
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = chapterAdapter
        }
    }

    private fun setupLanguageSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languageNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = adapter
        languageSpinner.prompt = getString(R.string.select_language_prompt)

        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLanguageCode = languageCodes[position]
                // Clear previous chapters when language changes to provide better UX
                chapterAdapter.updateChapters(emptyList())
                recyclerView.visibility = View.GONE // Hide recycler while new language loads
                bookViewModel.loadChapters(selectedLanguageCode)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Optionally handle the case where nothing is selected
            }
        }

        // Optional: Set a default selection if you didn't load initial data in onCreate
        // For example, to select English by default if it's in your list:
        // val defaultLanguageIndex = languageCodes.indexOf("en")
        // if (defaultLanguageIndex != -1) {
        //     languageSpinner.setSelection(defaultLanguageIndex)
        // } else if (languageCodes.isNotEmpty()) {
        //     languageSpinner.setSelection(0) // Default to the first language
        // }
        // If you load initial data in onCreate, the spinner will reflect that language if you set its selection.
        // For simplicity, the onItemSelected will trigger the first load when the adapter is set.
    }

    private fun observeViewModel() {
        bookViewModel.chapters.observe(this) { chapters ->
            chapters?.let {
                // Update chapters only if the list is not empty or if it's an intended clear
                if (it.isNotEmpty()) {
                    chapterAdapter.updateChapters(it)
                    recyclerView.visibility = View.VISIBLE
                } else if (bookViewModel.isLoading.value == false && bookViewModel.error.value == null) {
                    // If loading is finished, no error, and chapters are empty, show message.
                    // This covers the case where a language might legitimately have no chapters.
                    Toast.makeText(this, "No chapters found for the selected language.", Toast.LENGTH_SHORT).show()
                    recyclerView.visibility = View.GONE
                }
                // If it's empty due to loading or error, those observers will handle visibility.
            }
        }

        bookViewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) {
                recyclerView.visibility = View.GONE
            }
        }

        bookViewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                // Only hide recycler view if there are no chapters to show from a previous successful load
                if (bookViewModel.chapters.value.isNullOrEmpty()) {
                    recyclerView.visibility = View.GONE
                }
                bookViewModel.onErrorShown() // Reset error after showing
            }
        }
    }
}
