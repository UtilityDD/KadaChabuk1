package com.blackgrapes.kadachabuk

import android.content.DialogInterface
import android.content.Intent
import com.blackgrapes.kadachabuk.VideoActivity
import android.app.Dialog
import android.database.MatrixCursor
import android.animation.ValueAnimator
import android.provider.BaseColumns
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ScrollView
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.core.graphics.ColorUtils
import android.widget.CheckBox
import android.view.animation.AnimationUtils
import android.view.ViewGroup
import androidx.cursoradapter.widget.SimpleCursorAdapter
import android.view.Window
import android.util.TypedValue
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SEARCH_HISTORY_PREFS = "SearchHistoryPrefs"
private const val KEY_SEARCH_HISTORY = "search_history"
private const val MAX_SEARCH_HISTORY = 10
private const val ABOUT_PREFS = "AboutPrefs"


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
    private lateinit var noResultsGroup: ViewGroup // Changed from TextView
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

        // Allow the app to draw behind the system bars for a seamless UI
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Make the status bar transparent to show the AppBarLayout's color underneath
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        // Adjust system icon colors to match the current theme (light/dark)
        setStatusBarIconColor()

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = ""

        initializeViews()

        // Handle window insets to prevent overlap with the status bar
        handleWindowInsets()

        loadLanguageArrays()
        setupAdaptersAndRecyclerViews()
        checkIfLanguageNotSet()
        setupFab()
        observeViewModel()
    }

    /**
     * Sets the status bar icon color (light/dark) to match the AppBar's icon color.
     * This is done by checking if the app is currently in night mode.
     */
    private fun setStatusBarIconColor() {
        ViewCompat.getWindowInsetsController(window.decorView)?.let { controller ->
            val isNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

            // isAppearanceLightStatusBars = true means DARK icons
            // isAppearanceLightStatusBars = false means LIGHT icons
            // In dark theme (isNightMode), we want dark icons.
            controller.isAppearanceLightStatusBars = isNightMode
        }
    }

    private fun handleWindowInsets() {
        // We apply the listener to the toolbar, not the AppBarLayout.
        // This way, the AppBarLayout's background draws behind the status bar,
        // and we only add padding to the toolbar itself to prevent content overlap.
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Apply the top inset as padding to push the toolbar's content down
            view.setPadding(view.paddingLeft, insets.top, view.paddingRight, view.paddingBottom)

            // Increase the toolbar's height to accommodate the new padding
            // Get the default toolbar height from the theme attribute for robustness
            val typedValue = TypedValue()
            theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)
            val actionBarSize = TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics)

            view.layoutParams.height = actionBarSize + insets.top

            // We've handled the insets, so consume them
            WindowInsetsCompat.CONSUMED
        }
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
        noResultsGroup = findViewById(R.id.no_results_group) // Changed to the new group ID
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
                hideNoResultsView() // Hide "no results" when returning to the full list
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
            showNoResultsView("No bookmarks added yet")
        } else {
            hideNoResultsView()
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
            // If a language is saved but no chapters are loaded (e.g., app was closed during initial load),
            // automatically resume fetching the chapters without showing a dialog.
            val langIndex = languageCodes.indexOf(savedLangCode)
            bookViewModel.fetchAndLoadChapters(savedLangCode, languageNames[langIndex], forceDownload = false)
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
        val nightMode = sharedPreferences.getInt("NightMode", AppCompatDelegate.MODE_NIGHT_NO)
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        optionsMenu = menu
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        setupSearchSuggestions(searchView)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchJob?.cancel()
                saveSearchQuery(query ?: "")
                filter(query)
                // Hide keyboard on submit
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchJob?.cancel()
                searchJob = uiScope.launch {
                    kotlinx.coroutines.delay(500L) // 500ms debounce delay
                    populateSearchSuggestions(newText, searchView)
                    filter(newText)
                }
                return true
            }
        })

        updateThemeIcon(menu.findItem(R.id.action_theme_toggle))
        return true
    }

    private fun setupSearchSuggestions(searchView: SearchView) {
        val from = arrayOf("query")
        val to = intArrayOf(R.id.suggestion_text)
        val cursorAdapter = SimpleCursorAdapter(
            this,
            R.layout.item_search_suggestion,
            null,
            from,
            to,
            SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
        )

        searchView.suggestionsAdapter = cursorAdapter

        searchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int): Boolean {
                return false
            }

            override fun onSuggestionClick(position: Int): Boolean {
                val cursor = searchView.suggestionsAdapter.cursor
                if (cursor.moveToPosition(position)) {
                    val query = cursor.getString(cursor.getColumnIndexOrThrow("query"))
                    searchView.setQuery(query, true)
                }
                return true
            }
        })
    }



    override fun onDestroy() {
        super.onDestroy()
        // Cancel any running jobs to avoid memory leaks
        searchJob?.cancel()
    }

    private fun saveSearchQuery(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) return

        val prefs = getSharedPreferences(SEARCH_HISTORY_PREFS, Context.MODE_PRIVATE)
        val history = getSearchHistory().toMutableList()

        history.remove(trimmedQuery) // Remove if it already exists to move it to the top
        history.add(0, trimmedQuery) // Add to the top (most recent)

        val trimmedHistory = history.take(MAX_SEARCH_HISTORY)

        with(prefs.edit()) {
            putStringSet(KEY_SEARCH_HISTORY, trimmedHistory.toSet())
            apply()
        }
    }

    private fun getSearchHistory(): List<String> {
        val prefs = getSharedPreferences(SEARCH_HISTORY_PREFS, Context.MODE_PRIVATE)
        // The set is unordered, so we can't rely on its iteration order.
        // For simplicity here, we'll just sort it alphabetically. A more complex
        // implementation might store timestamps.
        return prefs.getStringSet(KEY_SEARCH_HISTORY, emptySet())?.sorted() ?: emptyList()
    }

    private fun populateSearchSuggestions(query: String?, searchView: SearchView) {
        val history = getSearchHistory().filter { it.contains(query ?: "", ignoreCase = true) }
        val cursor = MatrixCursor(arrayOf(BaseColumns._ID, "query"))
        history.forEachIndexed { index, suggestion ->
            cursor.addRow(arrayOf(index, suggestion))
        }
        searchView.suggestionsAdapter.changeCursor(cursor)
    }

    private fun filter(text: String?) {
        val query = text?.lowercase()?.trim()

        // Disable the bookmark FAB during a search to prevent conflicting states.
        val isSearching = !query.isNullOrEmpty()
        fabBookmarks.isEnabled = !isSearching
        fabBookmarks.alpha = if (isSearching) 0.5f else 1.0f

        uiScope.launch {
            // If the search query is empty, restore the original adapter and hide search UI.
            if (query.isNullOrEmpty()) {
                recyclerViewChapters.adapter = chapterAdapter
                chapterAdapter.updateChapters(originalChapters)
                hideNoResultsView()
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
                showNoResultsView("No results found for your search")
            } else {
                val summary = "\"$text\" found in ${searchResults.size} chapters, $totalOccurrences times total."
                searchSummaryTextView.text = summary
                searchSummaryTextView.visibility = View.VISIBLE
                hideNoResultsView()
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
            R.id.action_my_notes -> {
                startActivity(Intent(this, MyNotesActivity::class.java))
                true
            }
            R.id.action_videos -> {
                startActivity(Intent(this, VideoActivity::class.java))
                true
            }
            R.id.action_about -> {
                showAboutDialog()
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
                    // This is the first time we are displaying chapters in this session.
                    // This is the perfect place to show the initial "About" dialog.
                    if (!bookViewModel.hasShownInitialAboutDialog) {
                        val aboutPrefs = getSharedPreferences(ABOUT_PREFS, Context.MODE_PRIVATE)
                        val showAbout = aboutPrefs.getBoolean("show_about_on_startup", true)
                        if (showAbout) {
                            val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                            val savedLangCode = sharedPreferences.getString("selected_language_code", null)
                            savedLangCode?.let { langCode -> bookViewModel.fetchAboutInfo(langCode, forceRefresh = false) }
                        }
                        bookViewModel.hasShownInitialAboutDialog = true
                    }
                    chapterAdapter.updateChapters(it)
                    hideNoResultsView()
                    originalChapters = it // Store the full list
                    recyclerViewChapters.visibility = View.VISIBLE
                } else { // This block runs when the observed chapter list is empty.
                    // Don't hide the RecyclerView if there are no chapters, just show an empty state.
                    // This prevents the UI from "jumping" if the user switches to a language with no content.
                    chapterAdapter.updateChapters(emptyList())
                    originalChapters = emptyList()
                    // Only show the "no results" message if we are certain loading is finished and there's no error.
                    if (bookViewModel.isLoading.value == false && errorGroup.visibility == View.GONE) {
                        // Also check that we are not in the middle of a search that has no results
                        val searchItem = optionsMenu?.findItem(R.id.action_search)
                        val searchView = searchItem?.actionView as? SearchView
                        val isSearching = searchView != null && !searchView.isIconified && !searchView.query.isNullOrEmpty()
                        if (!isSearching) showNoResultsView(getString(R.string.no_chapters_found))
                    }
                }
            }
        }

        bookViewModel.isLoading.observe(this) { isLoading ->
            Log.d("MainActivity", "isLoading LiveData updated: $isLoading")
            optionsMenu?.findItem(R.id.action_search)?.isEnabled = !isLoading
            optionsMenu?.findItem(R.id.action_theme_toggle)?.isEnabled = !isLoading
            optionsMenu?.findItem(R.id.action_overflow)?.isEnabled = !isLoading
            val alpha = if (isLoading) 128 else 255 // 50% transparent when disabled
            optionsMenu?.findItem(R.id.action_search)?.icon?.alpha = alpha
            optionsMenu?.findItem(R.id.action_theme_toggle)?.icon?.alpha = alpha
            // The search icon is part of the SearchView, so we can't just set its icon alpha. Disabling the item is sufficient.
            optionsMenu?.findItem(R.id.action_overflow)?.icon?.alpha = alpha

            // Disable the bookmark FAB during loading to prevent interaction.
            fabBookmarks.isEnabled = !isLoading
            fabBookmarks.alpha = if (isLoading) 0.5f else 1.0f

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
                hideNoResultsView()
            } else {
                loadingGroup.visibility = View.GONE
                if (lottieAnimationView.isAnimating) {
                    lottieAnimationView.cancelAnimation()
                }
                rvDownloadedChapterHeadings.visibility = View.GONE // Hide progress list
                // The logic to show/hide "no results" is now primarily handled by the chapters observer,
                // which is the source of truth. This prevents a flicker when loading finishes.
                // When loading is finished, ensure the main content or error is visible.
                if (bookViewModel.error.value == null && chapterAdapter.itemCount > 0) {
                    recyclerViewChapters.visibility = View.VISIBLE
                }
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
                hideNoResultsView()
                loadingGroup.visibility = View.GONE
                Log.e("MainActivity", "Error observed: $it. Showing error screen.")
            } ?: run {
                errorGroup.visibility = View.GONE
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

        bookViewModel.aboutInfo.observe(this) { result ->
            result.onSuccess { aboutText ->
                showAboutDialog(aboutText)
            }.onFailure {
                // Optionally handle error, e.g., show a toast
                Log.e("MainActivity", "Failed to get 'About' info", it)
            }
        }
    }

    private fun showAboutDialog(content: String? = null) {
        if (content == null) {
            val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val savedLangCode = sharedPreferences.getString("selected_language_code", null)
            savedLangCode?.let { bookViewModel.fetchAboutInfo(it, forceRefresh = false) }
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_about, null)
        val aboutContentTextView = dialogView.findViewById<TextView>(R.id.about_content)
        val dontShowAgainCheckbox = dialogView.findViewById<CheckBox>(R.id.checkbox_dont_show_again)
        val closeButton = dialogView.findViewById<Button>(R.id.button_close)

        aboutContentTextView.text = content

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        val scrollView = dialogView.findViewById<ScrollView>(R.id.about_scroll_view)

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnShowListener {
            (it as? Dialog)?.window?.attributes?.windowAnimations = R.style.DialogAnimation

            // Post a runnable to check for scrollability after the layout is drawn
            scrollView.post {
                val canScroll = scrollView.getChildAt(0).height > scrollView.height
                if (canScroll) {
                    // Use a coroutine to add a small delay before starting the animation
                    uiScope.launch {
                        delay(500) // Wait half a second
                        val scrollDistance = (50 * resources.displayMetrics.density).toInt() // 50dp
                        ValueAnimator.ofInt(0, scrollDistance, 0).apply {
                            duration = 1500
                            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                            addUpdateListener { animation ->
                                scrollView.scrollTo(0, animation.animatedValue as Int)
                            }
                            start()
                        }
                    }
                }
            }
        }
        dialog.setOnDismissListener {
            val aboutPrefs = getSharedPreferences(ABOUT_PREFS, Context.MODE_PRIVATE)
            aboutPrefs.edit().putBoolean("show_about_on_startup", !dontShowAgainCheckbox.isChecked).apply()
        }
        dialog.show()
    }

    private fun showNoResultsView(message: String) {
        val noResultsTextView = noResultsGroup.findViewById<TextView>(R.id.tv_no_results_text)
        noResultsTextView.text = message

        if (noResultsGroup.visibility == View.VISIBLE) return

        val animation = AnimationUtils.loadAnimation(this, R.anim.fade_in_slide_up)
        noResultsGroup.startAnimation(animation)
        noResultsGroup.visibility = View.VISIBLE
    }

    private fun hideNoResultsView() {
        if (noResultsGroup.visibility == View.GONE) return // Do nothing if already hidden

        // You could add a fade-out animation here if desired, but for now, just hide it.
        noResultsGroup.clearAnimation() // Clear any running animation
        noResultsGroup.visibility = View.GONE
    }
}
