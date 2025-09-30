package com.blackgrapes.kadachabuk

import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import androidx.core.view.updatePadding
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView


class MainActivity : AppCompatActivity() {

    private val bookViewModel: BookViewModel by viewModels()

    private lateinit var chapterAdapter: ChapterAdapter
    private lateinit var recyclerViewChapters: RecyclerView

    private lateinit var themeToggleButton: ImageButton
    private lateinit var loadingGroup: Group
    private lateinit var lottieAnimationView: LottieAnimationView
    private lateinit var tvLoadingStatus: TextView
    private lateinit var rvDownloadedChapterHeadings: RecyclerView
    private lateinit var errorGroup: Group
    private lateinit var errorMessageTextView: TextView
    private lateinit var retryButton: Button
    private lateinit var downloadedHeadingsAdapter: DownloadedChaptersAdapter

    private lateinit var languageCodes: Array<String>
    private lateinit var languageChangeButton: ImageButton
    private lateinit var languageNames: Array<String>

    // Define a string resource for the default loading message if not already present
    // For example, in res/values/strings.xml:
    // <string name="loading_status_default">Loading...</string>
    // <string name="loading_status_processing">Processing chapters...</string>
    // <string name="loading_status_preparing">Preparing data...</string>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySavedTheme()
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val appBarLayout: com.google.android.material.appbar.AppBarLayout = findViewById(R.id.app_bar_layout)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            appBarLayout.updatePadding(top = systemBars.top)
            insets
        }

        initializeViews()
        loadLanguageArrays()
        setupAdaptersAndRecyclerViews()
        setupLanguageChangeButton()
        setupThemeToggleButton()
        checkFirstLaunch()
        observeViewModel()
    }

    private fun initializeViews() {
        recyclerViewChapters = findViewById(R.id.recyclerViewChapters)
        loadingGroup = findViewById(R.id.loading_group)
        themeToggleButton = findViewById(R.id.button_theme_toggle)
        lottieAnimationView = findViewById(R.id.lottie_animation_view)
        tvLoadingStatus = findViewById(R.id.tv_loading_status)
        languageChangeButton = findViewById(R.id.button_language_change)
        rvDownloadedChapterHeadings = findViewById(R.id.rv_downloaded_chapter_headings)
        errorGroup = findViewById(R.id.error_group)
        errorMessageTextView = findViewById(R.id.error_message)
        retryButton = findViewById(R.id.retry_button)
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

    private fun setupLanguageChangeButton() {
        languageChangeButton.setOnClickListener {
            showLanguageSelectionDialog(isCancelable = true)
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

    private fun checkFirstLaunch() {
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val savedLangCode = sharedPreferences.getString("selected_language_code", null)

        if (savedLangCode == null) {
            showLanguageSelectionDialog(isCancelable = false)
        } else {
            val savedLangIndex = languageCodes.indexOf(savedLangCode)
            if (savedLangIndex != -1) {
                bookViewModel.fetchAndLoadChapters(savedLangCode, languageNames[savedLangIndex], forceDownload = false)
            } else {
                // Fallback if saved language is no longer supported
                showLanguageSelectionDialog(isCancelable = false)
            }
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

    private fun setupThemeToggleButton() {
        updateThemeIcon()
        themeToggleButton.setOnClickListener {
            val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val newNightMode = if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                AppCompatDelegate.MODE_NIGHT_NO
            } else {
                AppCompatDelegate.MODE_NIGHT_YES
            }

            val sharedPreferences = getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putInt("NightMode", newNightMode)
                apply()
            }
            AppCompatDelegate.setDefaultNightMode(newNightMode)
        }
    }

    private fun updateThemeIcon() {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            themeToggleButton.setImageResource(R.drawable.ic_light_mode)
        } else {
            themeToggleButton.setImageResource(R.drawable.ic_dark_mode)
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
                    // Don't hide the RecyclerView if there are no chapters, just show an empty state.
                    // This prevents the UI from "jumping" if the user switches to a language with no content.
                    chapterAdapter.updateChapters(emptyList())
                    // recyclerViewChapters.visibility = View.GONE
                }
            }
        }

        bookViewModel.isLoading.observe(this) { isLoading ->
            Log.d("MainActivity", "isLoading LiveData updated: $isLoading")

            // Disable/Enable interaction with theme and language buttons
            themeToggleButton.isEnabled = !isLoading
            languageChangeButton.isEnabled = !isLoading
            themeToggleButton.alpha = if (isLoading) 0.5f else 1.0f
            languageChangeButton.alpha = if (isLoading) 0.5f else 1.0f

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
            } else {
                loadingGroup.visibility = View.GONE
                if (lottieAnimationView.isAnimating) {
                    lottieAnimationView.cancelAnimation()
                }
                rvDownloadedChapterHeadings.visibility = View.GONE // Hide progress list
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
                loadingGroup.visibility = View.GONE
                Log.e("MainActivity", "Error observed: $it. Showing error screen.")
            } ?: run {
                // This block runs when the error message is null (i.e., cleared).
                errorGroup.visibility = View.GONE
                Log.d("MainActivity", "Error cleared. Hiding error screen.")
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
