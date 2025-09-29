package com.blackgrapes.kadachabuk

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView

class MainActivity : AppCompatActivity() {

    private val bookViewModel: BookViewModel by viewModels()

    private lateinit var chapterAdapter: ChapterAdapter
    private lateinit var recyclerViewChapters: RecyclerView
    private lateinit var languageSpinner: Spinner

    private lateinit var loadingGroup: Group
    private lateinit var lottieAnimationView: LottieAnimationView
    private lateinit var tvLoadingStatus: TextView
    private lateinit var rvDownloadedChapterHeadings: RecyclerView
    private lateinit var downloadedHeadingsAdapter: DownloadedChaptersAdapter

    private lateinit var languageCodes: Array<String>
    private lateinit var languageNames: Array<String>

    // Define a string resource for the default loading message if not already present
    // For example, in res/values/strings.xml:
    // <string name="loading_status_default">Loading...</string>
    // <string name="loading_status_processing">Processing chapters...</string>
    // <string name="loading_status_preparing">Preparing data...</string>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        loadLanguageArrays()
        setupAdaptersAndRecyclerViews()
        setupLanguageSpinner()
        observeViewModel()
    }

    private fun initializeViews() {
        recyclerViewChapters = findViewById(R.id.recyclerViewChapters)
        languageSpinner = findViewById(R.id.spinnerLanguage)
        loadingGroup = findViewById(R.id.loading_group)
        lottieAnimationView = findViewById(R.id.lottie_animation_view)
        tvLoadingStatus = findViewById(R.id.tv_loading_status)
        rvDownloadedChapterHeadings = findViewById(R.id.rv_downloaded_chapter_headings)
        lottieAnimationView.loop(true)
    }

    private fun loadLanguageArrays() {
        languageNames = resources.getStringArray(R.array.language_names)
        languageCodes = resources.getStringArray(R.array.language_codes)
    }

    private fun setupAdaptersAndRecyclerViews() {
        chapterAdapter = ChapterAdapter(emptyList())
        recyclerViewChapters.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = chapterAdapter
        }

        try {
            downloadedHeadingsAdapter = DownloadedChaptersAdapter(mutableListOf())
            rvDownloadedChapterHeadings.apply {
                layoutManager = LinearLayoutManager(this@MainActivity)
                adapter = downloadedHeadingsAdapter
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up DownloadedChaptersAdapter.", e)
            Toast.makeText(this, "Error initializing download progress display.", Toast.LENGTH_LONG).show()
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
                val selectedLanguageName = languageNames[position] // Get the full name
                Log.d("MainActivity", "Language selected: $selectedLanguageName ($selectedLanguageCode). Requesting chapters.")
                chapterAdapter.updateChapters(emptyList())
                recyclerViewChapters.visibility = View.GONE
                downloadedHeadingsAdapter.clearItems()
                bookViewModel.fetchAndLoadChapters(selectedLanguageCode, selectedLanguageName, forceDownload = false)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) { /* Optionally handle */ }
        }
    }

    private fun observeViewModel() {
        bookViewModel.chapters.observe(this) { chapters ->
            Log.d("MainActivity", "Chapters LiveData updated. Count: ${chapters?.size ?: 0}")
            chapters?.let {
                if (it.isNotEmpty()) {
                    chapterAdapter.updateChapters(it)
                    recyclerViewChapters.visibility = View.VISIBLE
                } else {
                    if (bookViewModel.isLoading.value == false && bookViewModel.error.value == null) {
                        Toast.makeText(this, getString(R.string.no_chapters_found), Toast.LENGTH_SHORT).show()
                    }
                    recyclerViewChapters.visibility = View.GONE
                }
            }
        }

        bookViewModel.isLoading.observe(this) { isLoading ->
            Log.d("MainActivity", "isLoading LiveData updated: $isLoading")
            if (isLoading) {
                // The visibility of loadingGroup and animation is controlled here.
                // Text for tvLoadingStatus will be primarily set by loadingStatusMessage observer.
                // Set a very basic default if loadingStatusMessage hasn't emitted yet.
                if (tvLoadingStatus.text.isNullOrEmpty() || bookViewModel.loadingStatusMessage.value.isNullOrEmpty()) {
                    tvLoadingStatus.text = getString(R.string.loading_status_default)
                }
                loadingGroup.visibility = View.VISIBLE
                if (!lottieAnimationView.isAnimating) {
                    lottieAnimationView.playAnimation()
                }
                rvDownloadedChapterHeadings.visibility = View.VISIBLE // Show progress list
                recyclerViewChapters.visibility = View.GONE
                languageSpinner.visibility = View.GONE
            } else {
                loadingGroup.visibility = View.GONE
                if (lottieAnimationView.isAnimating) {
                    lottieAnimationView.cancelAnimation()
                }
                rvDownloadedChapterHeadings.visibility = View.GONE // Hide progress list
                languageSpinner.visibility = View.VISIBLE
                // Optionally clear tvLoadingStatus or set to an idle message if desired
                // tvLoadingStatus.text = ""
            }
        }

        bookViewModel.loadingStatusMessage.observe(this) { statusMessage ->
            // This observer is now the primary driver for tvLoadingStatus text when loading.
            if (bookViewModel.isLoading.value == true) { // Only update if loading is active
                if (!statusMessage.isNullOrEmpty()) {
                    tvLoadingStatus.text = statusMessage
                    Log.d("MainActivity", "Loading Status Message Update: $statusMessage")
                } else {
                    // Fallback to default loading text if message is cleared but still loading
                    tvLoadingStatus.text = getString(R.string.loading_status_default)
                }
            }
            // If isLoading is false, loadingGroup is hidden, so this text won't be visible.
        }

        bookViewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Log.e("MainActivity", "Error LiveData updated: $it")
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                // Loading UI is already hidden by isLoading observer when it goes to false
                // (which should happen on error in ViewModel)
                if (bookViewModel.chapters.value.isNullOrEmpty()) {
                    recyclerViewChapters.visibility = View.GONE
                }
                bookViewModel.onErrorShown()
            }
        }

        bookViewModel.downloadingChaptersList.observe(this) { downloadingList ->
            if (bookViewModel.isLoading.value == true) {
                downloadedHeadingsAdapter.updateList(downloadingList)
                if (downloadingList.isNotEmpty()) {
                    rvDownloadedChapterHeadings.smoothScrollToPosition(downloadedHeadingsAdapter.itemCount - 1)
                }
            }
        }
    }
}
