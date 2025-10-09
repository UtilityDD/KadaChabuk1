package com.blackgrapes.kadachabuk

import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.AppBarLayout
import androidx.constraintlayout.widget.Group
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.core.app.ShareCompat
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    private val bookViewModel: BookViewModel by viewModels()

    private lateinit var chapterAdapter: ChapterAdapter
    private lateinit var searchResultAdapter: SearchResultAdapter
    private lateinit var recyclerViewChapters: RecyclerView

    private lateinit var loadingGroup: Group
    private lateinit var lottieAnimationView: LottieAnimationView
    private lateinit var tvLoadingStatus: TextView
    private lateinit var rvDownloadedChapterHeadings: RecyclerView
    private lateinit var searchSummaryTextView: TextView
    private lateinit var errorGroup: Group
    private lateinit var errorMessageTextView: TextView
    private lateinit var noResultsTextView: TextView
    private var originalChapters: List<Chapter> = emptyList()
    private lateinit var retryButton: Button
    private lateinit var downloadedHeadingsAdapter: DownloadedChaptersAdapter

    private lateinit var fabBookmarks: FloatingActionButton
    private var isShowingBookmarks = false
    private lateinit var languageCodes: Array<String>
    private lateinit var languageNames: Array<String>

    private var optionsMenu: Menu? = null

    private var searchJob: Job? = null
    private val uiScope = CoroutineScope(Dispatchers.Main)

    // Define a string resource for the default loading message if not already present
    // For example, in res/values/strings.xml:
    // <string name="loading_status_default">Loading...</string>
    // <string name="loading_status_processing">Processing chapters...</string>
    // <string name="loading_status_preparing">Preparing data...</string>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySavedTheme()
        setContentView(R.layout.activity_main)

        // Make the activity fullscreen, hiding system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = ViewCompat.getWindowInsetsController(window.decorView)
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val appBarLayout: AppBarLayout = findViewById(R.id.app_bar_layout)
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = ""

        initializeViews()
        loadLanguageArrays()
        setupAdaptersAndRecyclerViews()
        checkIfLanguageNotSet()
        setupFab()
        observeViewModel()
    }

    private fun initializeViews() {
        recyclerViewChapters = findViewById(R.id.recyclerViewChapters)
        loadingGroup = findViewById(R.id.loading_group)
        lottieAnimationView = findViewById(R.id.lottie_animation_view)
        tvLoadingStatus = findViewById(R.id.tv_loading_status)
        rvDownloadedChapterHeadings = findViewById(R.id.rv_downloaded_chapter_headings)
        errorGroup = findViewById(R.id.error_group)
        errorMessageTextView = findViewById(R.id.error_message)
        searchSummaryTextView = findViewById(R.id.tv_search_summary)
        noResultsTextView = findViewById(R.id.tv_no_results)
        retryButton = findViewById(R.id.retry_button)
        fabBookmarks = findViewById(R.id.fab_bookmarks)
        lottieAnimationView.loop(true)
    }

    private fun loadLanguageArrays() {
        languageNames = resources.getStringArray(R.array.language_names)
        languageCodes = resources.getStringArray(R.array.language_codes)
    }

    private fun setupAdaptersAndRecyclerViews() {
        chapterAdapter = ChapterAdapter(emptyList())
        searchResultAdapter = SearchResultAdapter(emptyList())
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

    private fun setupRetryButton() {
        retryButton.setOnClickListener {
            val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val savedLangCode = sharedPreferences.getString("selected_language_code", null)
            if (savedLangCode != null) {
                val langIndex = languageCodes.indexOf(savedLangCode)
                bookViewModel.fetchAndLoadChapters(savedLangCode, languageNames[langIndex], forceDownload = true)
            }
        }
    }

    private fun setupFab() {
        fabBookmarks.setOnClickListener {
            isShowingBookmarks = !isShowingBookmarks
            if (isShowingBookmarks) {
                filterBookmarkedChapters()
                fabBookmarks.setImageResource(R.drawable.ic_bookmark_filled)
            } else {
                chapterAdapter.updateChapters(originalChapters)
                fabBookmarks.setImageResource(R.drawable.ic_bookmark_border)
                // If a search query is active, re-apply it
                val searchItem = optionsMenu?.findItem(R.id.action_search)
                val searchView = searchItem?.actionView as? SearchView
                if (searchView != null && !searchView.isIconified && !searchView.query.isNullOrEmpty()) {
                    filter(searchView.query.toString())
                }
            }
        }
    }

    private fun filterBookmarkedChapters() {
        val bookmarkPrefs = getSharedPreferences("BookmarkPrefs", Context.MODE_PRIVATE)
        val bookmarkedChapters = originalChapters.filter { chapter ->
            val key = "bookmark_${chapter.languageCode}_${chapter.serial}"
            bookmarkPrefs.getBoolean(key, false)
        }
        chapterAdapter.updateChapters(bookmarkedChapters)

        if (bookmarkedChapters.isEmpty()) {
            noResultsTextView.visibility = View.VISIBLE
        }
    }

    private fun checkIfLanguageNotSet() {
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val savedLangCode = sharedPreferences.getString("selected_language_code", null)

        if (savedLangCode == null) {
            // If no language is set, show the selection dialog.
            // This will only happen on the very first app launch.
            showLanguageSelectionDialog(isCancelable = false)
        } else if (bookViewModel.chapters.value.isNullOrEmpty() && bookViewModel.isLoading.value != true) {
            // This is a crucial check. If MainActivity starts and finds:
            // 1. There are no chapters displayed.
            // 2. A loading process is NOT already running (started by CoverActivity).
            // This means pre-loading didn't happen or failed. We must trigger the load now.
            bookViewModel.fetchAndLoadChapters(savedLangCode, languageNames[languageCodes.indexOf(savedLangCode)], forceDownload = false)
        }
    }

    private fun showLanguageSelectionDialog(isCancelable: Boolean) {
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val savedLangCode = sharedPreferences.getString("selected_language_code", null)

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_language_selector)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.setCancelable(isCancelable)

        val rvLanguages = dialog.findViewById<RecyclerView>(R.id.rv_languages)
        rvLanguages.layoutManager = LinearLayoutManager(this)
        (rvLanguages.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        val languageAdapter = LanguageAdapter(languageNames.zip(languageCodes).toList(), savedLangCode) { langCode, langName ->
            saveLanguagePreference(langCode)
            bookViewModel.fetchAndLoadChapters(langCode, langName, forceDownload = false)
            dialog.dismiss()
        }
        rvLanguages.adapter = languageAdapter

        dialog.show()
    }

    private fun saveLanguagePreference(languageCode: String) {
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("selected_language_code", languageCode)
            apply()
        }
    }

    private fun applySavedTheme() {
        val sharedPreferences = getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)
        val nightMode = sharedPreferences.getInt("NightMode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        optionsMenu = menu
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchJob?.cancel()
                filter(query)
                // Hide keyboard on submit
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchJob?.cancel()
                searchJob = uiScope.launch {
                    kotlinx.coroutines.delay(500L) // 500ms debounce delay
                    filter(newText)
                }
                return true
            }
        })

        updateThemeIcon(menu.findItem(R.id.action_theme_toggle))
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel any running jobs to avoid memory leaks
        searchJob?.cancel()
    }

    private fun filter(text: String?) {
        val query = text?.lowercase()?.trim()

        uiScope.launch {
            // If the search query is empty, restore the original adapter and hide search UI.
            if (query.isNullOrEmpty()) {
                recyclerViewChapters.adapter = chapterAdapter
                chapterAdapter.updateChapters(originalChapters) // Make sure the original list is shown
                noResultsTextView.visibility = View.GONE
                searchSummaryTextView.visibility = View.GONE
                return@launch
            }

            // Perform the heavy filtering and counting on a background thread.
            val (searchResults, totalOccurrences) = withContext(Dispatchers.IO) {
                val results = mutableListOf<SearchResult>()
                var occurrences = 0

                originalChapters.forEach { chapter ->
                    val totalMatchesInChapter = countOccurrences(chapter.heading, query) +
                            countOccurrences(chapter.serial, query) +
                            countOccurrences(chapter.writer, query) +
                            countOccurrences(chapter.dataText, query)

                    if (totalMatchesInChapter > 0) {
                        results.add(SearchResult(chapter, totalMatchesInChapter))
                        occurrences += totalMatchesInChapter
                    }
                }
                Pair(results, occurrences)
            }

            // Switch to the search adapter on the main thread
            if (recyclerViewChapters.adapter !is SearchResultAdapter) {
                recyclerViewChapters.adapter = searchResultAdapter
            }

            // When filtering, exit the "bookmarks only" view for a better user experience.
            if (isShowingBookmarks) {
                isShowingBookmarks = false
                fabBookmarks.setImageResource(R.drawable.ic_bookmark_border)
            }

            searchResultAdapter.updateResults(searchResults, query)

            if (searchResults.isEmpty()) {
                searchSummaryTextView.visibility = View.GONE
                noResultsTextView.visibility = View.VISIBLE
            } else {
                val summary = "\"$text\" found in ${searchResults.size} chapters, $totalOccurrences times total."
                searchSummaryTextView.text = summary
                searchSummaryTextView.visibility = View.VISIBLE
                noResultsTextView.visibility = View.GONE
            }
        }
    }

    // A more performant way to count occurrences, ignoring case.
    private fun countOccurrences(text: String, query: String): Int {
        if (query.isEmpty()) return 0
        var count = 0
        var index = text.indexOf(query, 0, ignoreCase = true)
        while (index != -1) {
            count++
            index = text.indexOf(query, index + query.length, ignoreCase = true)
        }
        return count
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_language_change -> {
                showLanguageSelectionDialog(isCancelable = true)
                true
            }
            R.id.action_theme_toggle -> {
                val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                val newNightMode = if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                    AppCompatDelegate.MODE_NIGHT_NO
                } else {
                    AppCompatDelegate.MODE_NIGHT_YES
                }
                AppCompatDelegate.setDefaultNightMode(newNightMode)
                getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE).edit().putInt("NightMode", newNightMode).apply()
                true
            }
            R.id.action_share_app -> {
                shareApp()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateThemeIcon(themeMenuItem: MenuItem) {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            themeMenuItem.setIcon(R.drawable.ic_light_mode)
        } else {
            themeMenuItem.setIcon(R.drawable.ic_dark_mode)
        }
    }
    
    private fun shareApp() {
        val appPackageName = packageName // Get the package name of your app
        val appLink = "https://play.google.com/store/apps/details?id=$appPackageName"

        val shareIntent = ShareCompat.IntentBuilder(this)
            .setType("text/plain")
            .setText("Check out this app: $appLink")
            .setSubject("Share App")
            .setChooserTitle("Share App via")
            .intent

        startActivity(shareIntent)
    }

    private fun observeViewModel() {
        bookViewModel.chapters.observe(this) { chapters ->
            Log.d("MainActivity", "Chapters LiveData updated. Count: ${chapters?.size ?: 0}")
            chapters?.let {
                if (it.isNotEmpty()) {
                    chapterAdapter.updateChapters(it)
                    originalChapters = it // Store the full list
                    recyclerViewChapters.visibility = View.VISIBLE
                } else {
                    if (bookViewModel.isLoading.value == false && bookViewModel.error.value == null) {
                        Toast.makeText(this, getString(R.string.no_chapters_found), Toast.LENGTH_SHORT).show()
                    }
                    // Don't hide the RecyclerView if there are no chapters, just show an empty state.
                    // This prevents the UI from "jumping" if the user switches to a language with no content.
                    chapterAdapter.updateChapters(emptyList())
                    // recyclerViewChapters.visibility = View.GONE
                    noResultsTextView.visibility = View.VISIBLE
                    originalChapters = emptyList()
                }
            }
        }

        bookViewModel.isLoading.observe(this) { isLoading ->
            Log.d("MainActivity", "isLoading LiveData updated: $isLoading")
            optionsMenu?.findItem(R.id.action_search)?.isEnabled = !isLoading
            optionsMenu?.findItem(R.id.action_theme_toggle)?.isEnabled = !isLoading
            optionsMenu?.findItem(R.id.action_overflow)?.isEnabled = !isLoading
            val alpha = if (isLoading) 128 else 255 // 50% transparent when disabled
            optionsMenu?.findItem(R.id.action_theme_toggle)?.icon?.alpha = alpha
            // The search icon is part of the SearchView, so we can't just set its icon alpha. Disabling the item is sufficient.
            optionsMenu?.findItem(R.id.action_overflow)?.icon?.alpha = alpha

            if (isLoading) {
                if (tvLoadingStatus.text.isNullOrEmpty() || bookViewModel.loadingStatusMessage.value.isNullOrEmpty()) {
                    tvLoadingStatus.text = getString(R.string.loading_status_default)
                }
                loadingGroup.visibility = View.VISIBLE
                if (!lottieAnimationView.isAnimating) {
                    lottieAnimationView.playAnimation()
                }
                rvDownloadedChapterHeadings.visibility = View.VISIBLE // Show progress list
                recyclerViewChapters.visibility = View.GONE
                noResultsTextView.visibility = View.GONE
            } else {
                loadingGroup.visibility = View.GONE
                if (lottieAnimationView.isAnimating) {
                    lottieAnimationView.cancelAnimation()
                }
                rvDownloadedChapterHeadings.visibility = View.GONE // Hide progress list
                noResultsTextView.visibility = if (chapterAdapter.itemCount == 0) View.VISIBLE else View.GONE
                // When loading is finished, ensure the main content or error is visible.
                if (bookViewModel.error.value == null) {
                    // If there's no error, the chapters list should be visible (even if empty).
                    recyclerViewChapters.visibility = View.VISIBLE
                }
                // Optionally clear tvLoadingStatus or set to an idle message if desired
                // tvLoadingStatus.text = ""
            }
        }
        setupRetryButton()

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
                errorMessageTextView.text = it
                errorGroup.visibility = View.VISIBLE
                recyclerViewChapters.visibility = View.GONE
                noResultsTextView.visibility = View.GONE
                loadingGroup.visibility = View.GONE
                Log.e("MainActivity", "Error observed: $it. Showing error screen.")
            } ?: run {
                // This block runs when the error message is null (i.e., cleared).
                errorGroup.visibility = View.GONE
                noResultsTextView.visibility = if (chapterAdapter.itemCount == 0) View.VISIBLE else View.GONE
                Log.d("MainActivity", "Error cleared. Hiding error screen.")
            }
        }

        bookViewModel.downloadingChaptersList.observe(this) { downloadingList ->
            if (bookViewModel.isLoading.value == true) {
                downloadedHeadingsAdapter.updateList(downloadingList)
                if (downloadingList.isNotEmpty()) {
                    rvDownloadedChapterHeadings.smoothScrollToPosition(downloadedHeadingsAdapter.itemCount - 1)
                    tvLoadingStatus.text = "Downloading ${downloadingList.last().heading}"
                }
            }
        }
    }
}
