package com.blackgrapes.kadachabuk

import android.content.ClipData
import android.content.ClipboardManager
import android.app.Dialog
import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.graphics.Color // <-- IMPORT THIS
import android.os.Bundle
import android.view.ActionMode
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.view.WindowInsetsController
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.ImageView // <-- IMPORT THIS
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import androidx.core.content.ContextCompat // <-- IMPORT THIS
import androidx.core.app.ShareCompat
import android.widget.Toast
import android.util.Log

import androidx.core.view.updatePadding
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import kotlin.random.Random // <-- IMPORT THIS
import java.util.concurrent.TimeUnit

private const val FONT_PREFS = "FontPrefs"
private const val KEY_FONT_SIZE = "fontSize"
private const val DEFAULT_FONT_SIZE = 18f
private const val BOOKMARK_PREFS = "BookmarkPrefs"
private const val SCROLL_PREFS = "ScrollPositions"

class DetailActivity : AppCompatActivity() {

    private lateinit var scrollView: ScrollView
    private lateinit var imageViewHeader: ImageView // <-- DECLARE ImageView
    private var isFullScreen = false
    private lateinit var textViewData: TextView
    private lateinit var fontSettingsButton: ImageButton
    private lateinit var backButton: ImageButton
    private lateinit var bookmarkButton: ImageButton
    private lateinit var chapterSerial: String
    private lateinit var languageCode: String
    private lateinit var searchNavigationLayout: LinearLayout
    private lateinit var previousMatchButton: ImageButton
    private lateinit var nextMatchButton: ImageButton
    private lateinit var matchCountTextView: TextView

    private var customActionMenu: View? = null
    private val matchIndices = mutableListOf<Int>()
    private var currentMatchIndex = -1

    // Array of your drawable resource IDs
    private val headerImageDrawables = intArrayOf(
        R.drawable.thakur1, // Replace with your actual drawable names
        R.drawable.thakur2,
        R.drawable.thakur3,
        R.drawable.thakur4,
        R.drawable.thakur5,
        R.drawable.thakur6,
        R.drawable.thakur7,
        R.drawable.thakur8,
        R.drawable.thakur9
        // Add all your header image drawables here
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        // Enable the action bar menu
        enableActionBarMenu()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            fontSettingsButton.updatePadding(top = systemBars.top)
            bookmarkButton.updatePadding(top = systemBars.top)
            backButton.updatePadding(top = systemBars.top)
            insets
        }

        scrollView = findViewById(R.id.scrollView)
        imageViewHeader = findViewById(R.id.imageViewHeader) // <-- INITIALIZE ImageView

        val textViewHeading: TextView = findViewById(R.id.textViewHeading)
        val textViewDate: TextView = findViewById(R.id.textViewDate)
        val textViewWriter: TextView = findViewById(R.id.textViewWriter)
        textViewData = findViewById(R.id.textViewData)
        fontSettingsButton = findViewById(R.id.button_font_settings)
        backButton = findViewById(R.id.button_back)
        bookmarkButton = findViewById(R.id.button_bookmark)
        searchNavigationLayout = findViewById(R.id.search_navigation_layout)
        previousMatchButton = findViewById(R.id.button_previous_match)
        nextMatchButton = findViewById(R.id.button_next_match)
        matchCountTextView = findViewById(R.id.text_view_match_count)
        customActionMenu = findViewById(R.id.custom_action_menu)


        val heading = intent.getStringExtra("EXTRA_HEADING")
        val date = intent.getStringExtra("EXTRA_DATE")
        val dataContent = intent.getStringExtra("EXTRA_DATA")
        val writer = intent.getStringExtra("EXTRA_WRITER")
        chapterSerial = intent.getStringExtra("EXTRA_SERIAL") ?: ""
        languageCode = intent.getStringExtra("EXTRA_LANGUAGE_CODE") ?: ""

        textViewHeading.text = heading
        textViewDate.text = date?.removeSurrounding("(", ")")
        textViewData.text = dataContent
        title = heading ?: "Details"

        loadAndApplyFontSize()
        textViewWriter.text = writer
        setupFontSettingsButton()
        setupBookmarkButton()
        setupSearchNavigation()

        // Request focus for the TextView
        textViewData.requestFocus()

        // Enable text selection
        textViewData.setTextIsSelectable(true)

        // Set custom action mode callback for text selection
        textViewData.customSelectionActionModeCallback = customActionModeCallback
        
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val searchQuery = intent.getStringExtra("EXTRA_SEARCH_QUERY")
        // Check for a saved scroll position, but only if not coming from a search result.
        if (searchQuery.isNullOrEmpty()) {
            checkForSavedScrollPosition()
        }

        // Enter full screen as soon as the activity is created
        enterFullScreen()

        // Set a random header image
        setRandomHeaderImage()
    }

    private val customActionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            // Calculate the position to show the menu
            val layout = textViewData.layout
            if (layout != null) {
                val startSelection = textViewData.selectionStart
                val line = layout.getLineForOffset(startSelection)
                // Y position is the top of the line of text, minus the scroll position, plus the TextView's top margin.
                val yPos = layout.getLineTop(line) - scrollView.scrollY + textViewData.top

                customActionMenu?.let {
                    // Position the menu above the selected text line.
                    // We subtract its height to place it correctly.
                    it.translationY = (yPos - it.height - 16).toFloat() // 16px margin
                    it.visibility = View.VISIBLE
                }
            } else {
                customActionMenu?.visibility = View.VISIBLE // Fallback to top if layout is null
            }
            // Show our custom menu
            setupCustomMenuClickListeners(mode)

            // Tint single-color icons white to make them visible on the new dark background
            customActionMenu?.findViewById<ImageButton>(R.id.action_copy)?.setColorFilter(Color.WHITE)
            customActionMenu?.findViewById<ImageButton>(R.id.action_keep_notes)?.setColorFilter(Color.WHITE)

            // Prevent the default menu from showing
            menu?.clear()
            return true // We've handled it
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            // Again, prevent the default menu
            menu?.clear()
            return true // IMPORTANT: Return true to keep the action mode alive.
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            // This will not be called because we are not using the default menu items
            return false
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            // Hide our custom menu when selection is cleared
            customActionMenu?.visibility = View.GONE
            // Clear the tint when the menu is hidden to reset the icons
            customActionMenu?.findViewById<ImageButton>(R.id.action_copy)?.clearColorFilter()
            customActionMenu?.findViewById<ImageButton>(R.id.action_keep_notes)?.clearColorFilter()
        }
    }

    private fun setupCustomMenuClickListeners(mode: ActionMode?) {
        val selectedText = { getSelectedText() }

        customActionMenu?.findViewById<ImageButton>(R.id.action_copy)?.setOnClickListener {
            copyToClipboard(selectedText())
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            mode?.finish()
        }

        customActionMenu?.findViewById<ImageButton>(R.id.action_share_whatsapp)?.setOnClickListener {
            shareToApp(selectedText(), "com.whatsapp")
            mode?.finish()
        }

        customActionMenu?.findViewById<ImageButton>(R.id.action_share_facebook)?.setOnClickListener {
            shareToApp(selectedText(), "com.facebook.katana")
            mode?.finish()
        }

        customActionMenu?.findViewById<ImageButton>(R.id.action_keep_notes)?.setOnClickListener {
            // Assuming "Keep Notes" means sharing to Google Keep
            shareToApp(selectedText(), "com.google.android.keep")
            mode?.finish()
        }
    }

    private fun getSelectedText(): String {
        val startIndex = textViewData.selectionStart
        val endIndex = textViewData.selectionEnd
        return if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            textViewData.text.substring(startIndex, endIndex)
        } else {
            ""
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("KadaChabuk_Copy", text)
        clipboard.setPrimaryClip(clip)
    }

    override fun onResume() {
        super.onResume()
        highlightSearchTerm()
    }

    private fun setupBookmarkButton() {
        checkAndSetBookmarkState()

        bookmarkButton.setOnClickListener {
            val prefs = getSharedPreferences(BOOKMARK_PREFS, Context.MODE_PRIVATE)
            val key = getBookmarkKey() ?: return@setOnClickListener
            val isBookmarked = prefs.getBoolean(key, false)

            with(prefs.edit()) {
                putBoolean(key, !isBookmarked)
                apply()
            }

            if (!isBookmarked) {
                bookmarkButton.setImageResource(R.drawable.ic_bookmark_filled)
                Toast.makeText(this, "Bookmark added", Toast.LENGTH_SHORT).show()
            } else {
                bookmarkButton.setImageResource(R.drawable.ic_bookmark_border)
                Toast.makeText(this, "Bookmark removed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkAndSetBookmarkState() {
        val key = getBookmarkKey() ?: return
        val prefs = getSharedPreferences(BOOKMARK_PREFS, Context.MODE_PRIVATE)
        val isBookmarked = prefs.getBoolean(key, false)
        if (isBookmarked) {
            bookmarkButton.setImageResource(R.drawable.ic_bookmark_filled)
        } else {
            bookmarkButton.setImageResource(R.drawable.ic_bookmark_border)
        }
    }

    private fun getScrollPositionKey(): String? {
        return if (::chapterSerial.isInitialized && ::languageCode.isInitialized && chapterSerial.isNotEmpty() && languageCode.isNotEmpty()) {
            "scroll_pos_${languageCode}_${chapterSerial}"
        } else {
            null
        }
    }

    private fun getBookmarkKey(): String? {
        return if (::chapterSerial.isInitialized && ::languageCode.isInitialized && chapterSerial.isNotEmpty() && languageCode.isNotEmpty()) {
            "bookmark_${languageCode}_${chapterSerial}"
        } else {
            null
        }
    }

    private fun saveScrollPosition() {
        val scrollKey = getScrollPositionKey()
        val timeKey = getTimestampKey()

        if (scrollKey != null && timeKey != null) {
            val sharedPreferences = getSharedPreferences(SCROLL_PREFS, Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putInt(scrollKey, scrollView.scrollY)
                putLong(timeKey, System.currentTimeMillis()) // Also save the current time
                apply()
            }
        }
    }

    private fun getTimestampKey(): String? {
        return if (::chapterSerial.isInitialized && ::languageCode.isInitialized && chapterSerial.isNotEmpty() && languageCode.isNotEmpty()) {
            "scroll_time_${languageCode}_${chapterSerial}"
        } else {
            null
        }
    }

    private fun checkForSavedScrollPosition() {
        val scrollKey = getScrollPositionKey()
        val timeKey = getTimestampKey()

        if (scrollKey != null && timeKey != null) {
            val sharedPreferences = getSharedPreferences(SCROLL_PREFS, Context.MODE_PRIVATE)
            val savedScrollY = sharedPreferences.getInt(scrollKey, 0)
            val savedTimestamp = sharedPreferences.getLong(timeKey, 0)

            if (savedScrollY > 100 && savedTimestamp > 0) { // Only prompt if they've scrolled a bit
                val daysDifference = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - savedTimestamp)
                val (title, message) = getResumeDialogTexts(daysDifference)

                MaterialAlertDialogBuilder(this)
                    .setTitle(title)
                    .setMessage(message)
                    .setIcon(R.drawable.ic_bookmark) // Adds a visual cue to the dialog
                    .setPositiveButton("Resume") { dialog, _ ->
                        scrollView.post {
                            scrollView.smoothScrollTo(0, savedScrollY)
                            highlightLineAt(savedScrollY) // Add the highlight animation
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton("Start Over") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

    private fun getResumeDialogTexts(daysSinceLastRead: Long): Pair<String, String> {
        val message = "Ready to continue from where you left off?"
        val title = when {
            daysSinceLastRead < 1 -> "Picking Up Again?"
            daysSinceLastRead == 1L -> "Welcome Back!"
            daysSinceLastRead in 2..6 -> "It's Been a Little While!"
            daysSinceLastRead in 7..29 -> "Dusting This One Off?"
            daysSinceLastRead >= 30 -> "A Long-Lost Friend Returns!"
            else -> "Welcome Back!" // Fallback
        }
        return Pair(title, message)
    }

    private fun highlightLineAt(scrollY: Int) {
        val layout = textViewData.layout ?: return

        // Find the line number at the given scroll Y position.
        val line = layout.getLineForVertical(scrollY)

        // Get the start and end character indices for that line.
        val lineStart = layout.getLineStart(line)
        val lineEnd = layout.getLineEnd(line)

        if (lineStart >= lineEnd) return // Nothing to highlight

        val originalText = textViewData.text
        val spannable = SpannableStringBuilder(originalText)

        // Create a ValueAnimator to fade the highlight color
        val animator = ValueAnimator.ofArgb(
            ContextCompat.getColor(this, R.color.highlight_color),
            ContextCompat.getColor(this, android.R.color.transparent)
        )
        animator.duration = 2000 // 2 seconds
        animator.addUpdateListener { animation ->
            val color = animation.animatedValue as Int
            val highlightSpan = BackgroundColorSpan(color)
            spannable.setSpan(highlightSpan, lineStart, lineEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            textViewData.text = spannable
        }
        animator.start()
    }

    override fun onPause() {
        super.onPause()
        saveScrollPosition()
    }

    private fun setRandomHeaderImage() {
        if (headerImageDrawables.isNotEmpty()) {
            val randomIndex = Random.nextInt(headerImageDrawables.size)
            val randomImageResId = headerImageDrawables[randomIndex]
            // imageViewHeader.setImageResource(randomImageResId) // Simple way
            // For potentially smoother loading with large images or more control:
            imageViewHeader.setImageDrawable(ContextCompat.getDrawable(this, randomImageResId))
        } else {
            // Optional: Hide ImageView or set a default placeholder if no images are available
            imageViewHeader.visibility = View.GONE
        }
    }

    private fun highlightSearchTerm() {
        val searchQuery = intent.getStringExtra("EXTRA_SEARCH_QUERY")
        if (searchQuery.isNullOrEmpty()) {
            return // No query to highlight
        }

        val fullText = textViewData.text.toString()
        val spannableString = SpannableString(fullText)
        val highlightColor = ContextCompat.getColor(this, R.color.highlight_color) // Make sure to define this color

        matchIndices.clear()
        var index = fullText.indexOf(searchQuery, 0, ignoreCase = true)
        while (index >= 0) {
            matchIndices.add(index)
            val span = BackgroundColorSpan(highlightColor)
            spannableString.setSpan(span, index, index + searchQuery.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            index = fullText.indexOf(searchQuery, index + searchQuery.length, ignoreCase = true)
        }

        textViewData.text = spannableString
        
        if (matchIndices.isNotEmpty()) {
            currentMatchIndex = 0
            scrollToMatch(currentMatchIndex)
            updateNavigationState()
            searchNavigationLayout.visibility = View.VISIBLE
        } else {
            searchNavigationLayout.visibility = View.GONE
        }
    }

    private fun setupSearchNavigation() {
        previousMatchButton.setOnClickListener {
            if (matchIndices.isNotEmpty()) {
                if (currentMatchIndex > 0) {
                    currentMatchIndex--
                    scrollToMatch(currentMatchIndex)
                    updateNavigationState()
                }
            }
        }

        nextMatchButton.setOnClickListener {
            if (matchIndices.isNotEmpty()) {
                if (currentMatchIndex < matchIndices.size - 1) {
                    currentMatchIndex++
                    scrollToMatch(currentMatchIndex)
                    updateNavigationState()
                }
            }
        }
    }

    private fun updateNavigationState() {
        if (matchIndices.isNotEmpty()) {
            matchCountTextView.text = "${currentMatchIndex + 1} of ${matchIndices.size}"
            previousMatchButton.isEnabled = currentMatchIndex > 0
            nextMatchButton.isEnabled = currentMatchIndex < matchIndices.size - 1
        }
    }

    private fun scrollToMatch(matchIndex: Int) {
        if (matchIndex < 0 || matchIndex >= matchIndices.size) return

        val charIndex = matchIndices[matchIndex]
        val searchQuery = intent.getStringExtra("EXTRA_SEARCH_QUERY") ?: return
        val spannable = textViewData.text as? SpannableStringBuilder ?: SpannableStringBuilder(textViewData.text)

        // Animate the highlight for the current match
        val animator = ValueAnimator.ofArgb(
            ContextCompat.getColor(this, R.color.flash_color),
            ContextCompat.getColor(this, R.color.highlight_color)
        )
        animator.duration = 800 // A quicker, more noticeable flash (800ms)
        animator.addUpdateListener { animation ->
            val color = animation.animatedValue as Int
            val highlightSpan = BackgroundColorSpan(color)
            // Remove any previous spans on this section before adding a new one
            val existingSpans = spannable.getSpans(charIndex, charIndex + searchQuery.length, BackgroundColorSpan::class.java)
            existingSpans.forEach { spannable.removeSpan(it) }
            // Add the new, animated-color span
            spannable.setSpan(highlightSpan, charIndex, charIndex + searchQuery.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            // Set the text once with the modified spannable
            textViewData.text = spannable
        }
        animator.start()

        scrollView.post {
            val layout = textViewData.layout
            if (layout != null) {
                val line = layout.getLineForOffset(charIndex)
                // Calculate y position to scroll to, with some offset to not be at the very top
                val y = layout.getLineTop(line) - (scrollView.height / 3)
                scrollView.smoothScrollTo(0, y.coerceAtLeast(0))
            }
        }
    }
    
    private fun setupFontSettingsButton() {
        fontSettingsButton.setOnClickListener {
            showFontSettingsDialog()
        }
    }

    private fun showFontSettingsDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_font_settings)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val slider = dialog.findViewById<Slider>(R.id.font_size_slider)
        slider.value = textViewData.textSize / resources.displayMetrics.scaledDensity

        slider.addOnChangeListener { _, value, _ ->
            textViewData.textSize = value
        }

        dialog.setOnDismissListener {
            saveFontSize(textViewData.textSize / resources.displayMetrics.scaledDensity)
        }

        dialog.show()
    }

    private fun saveFontSize(size: Float) {
        val sharedPreferences = getSharedPreferences(FONT_PREFS, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putFloat(KEY_FONT_SIZE, size)
            apply()
        }
    }

    private fun loadAndApplyFontSize() {
        val sharedPreferences = getSharedPreferences(FONT_PREFS, Context.MODE_PRIVATE)
        val fontSize = sharedPreferences.getFloat(KEY_FONT_SIZE, DEFAULT_FONT_SIZE)
        textViewData.textSize = fontSize
    }

    private fun enterFullScreen() {
        // ... (enterFullScreen logic remains the same)
        if (isFullScreen) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
        supportActionBar?.hide()
        isFullScreen = true
        // After entering fullscreen, we want the ScrollView to not have extra top padding from insets.
        // Requesting insets again will trigger the listener, which now adjusts padding based on isFullScreen.
        ViewCompat.requestApplyInsets(scrollView)
        // We don't need the insets listener anymore, but we can manually adjust padding
        // to avoid the content jumping under the status bar area.
        val insets = ViewCompat.getRootWindowInsets(window.decorView)
        val systemBarInsets = insets?.getInsets(WindowInsetsCompat.Type.systemBars())
        scrollView.setPadding(systemBarInsets?.left ?: 0, 0, systemBarInsets?.right ?: 0, 0)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.detail_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share -> {
                shareText()
                true
            }
            R.id.action_share_app -> {
                shareApp()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun enableActionBarMenu() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun shareText() {
            val textToShare = textViewData.text.toString()

        if (textToShare.isNotEmpty()) {
            try {


                ShareCompat.IntentBuilder(this)
                    .setType("text/plain")
                    .setText(textToShare)
                    .setChooserTitle("Share via")
                    .`startChooser`()
            } catch (e: Exception) {
                Toast.makeText(this, "Sharing failed", Toast.LENGTH_SHORT).show()
                Log.e("ShareError", "Error sharing text", e)
            }
        } else {
            Toast.makeText(this, "No text to share", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareToApp(text: String, packageName: String) {
        try {
            val shareIntent = ShareCompat.IntentBuilder(this)
                .setType("text/plain")
                .setText(text)
                .intent
            shareIntent.setPackage(packageName)
            startActivity(shareIntent)
        } catch (e: Exception) {
            Log.e("ShareError", "Could not share to $packageName", e)
            Toast.makeText(this, "App not installed or unable to share.", Toast.LENGTH_SHORT).show()
            // Fallback to a general share chooser if the specific app fails
            ShareCompat.IntentBuilder(this).setType("text/plain").setText(text).startChooser()
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

        if (shareIntent.resolveActivity(packageManager) != null) {
            startActivity(shareIntent)
        }
    }

}
