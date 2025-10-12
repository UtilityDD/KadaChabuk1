package com.blackgrapes.kadachabuk

import android.content.ClipData
import android.content.ClipboardManager
import android.app.Dialog
import android.animation.ValueAnimator
import android.net.Uri
import android.content.Intent
import android.view.LayoutInflater
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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
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
private const val NOTES_PREFS = "MyNotesPrefs"
private const val HISTORY_PREFS = "ReadingHistoryPrefs"
private const val KEY_NOTES = "notes"

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
    private lateinit var readingHistoryLayout: LinearLayout
    private lateinit var readingHistoryTextView: TextView

    private var chapterHeading: String? = null
    private var chapterDate: String? = null
    private var customActionMenu: View? = null
    private val matchIndices = mutableListOf<Int>()
    private var previousMatchIndex = -1
    private var sessionStartTime: Long = 0
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
        readingHistoryLayout = findViewById(R.id.reading_history_layout)
        readingHistoryTextView = findViewById(R.id.text_view_reading_history)
        customActionMenu = findViewById(R.id.custom_action_menu)


        chapterHeading = intent.getStringExtra("EXTRA_HEADING")
        chapterDate = intent.getStringExtra("EXTRA_DATE")
        val dataContent = intent.getStringExtra("EXTRA_DATA")
        val writer = intent.getStringExtra("EXTRA_WRITER")
        chapterSerial = intent.getStringExtra("EXTRA_SERIAL") ?: ""
        languageCode = intent.getStringExtra("EXTRA_LANGUAGE_CODE") ?: ""

        textViewHeading.text = chapterHeading
        textViewDate.text = chapterDate?.removeSurrounding("(", ")")
        textViewData.text = dataContent
        title = chapterHeading ?: "Details"

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

        // Handle reading history tracking and display
        setupReadingHistory()

        // Add a scroll listener to fade out the header image on scroll.
        scrollView.viewTreeObserver.addOnScrollChangedListener {
            val scrollY = scrollView.scrollY
            val imageHeight = imageViewHeader.height.toFloat()

            // Calculate alpha: 1.0 (fully visible) at scrollY 0, to 0.0 (fully transparent)
            // as the user scrolls past the image's height.
            val alpha = 1.0f - (scrollY / imageHeight)
            imageViewHeader.alpha = alpha.coerceIn(0f, 1f) // Ensure alpha stays between 0 and 1
        }
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
                    val finalY = (yPos - it.height - 16).toFloat() // 16px margin

                    // Set initial state for animation: slightly lower and invisible
                    it.translationY = finalY + 20f // Start 20px lower
                    it.alpha = 0f
                    it.visibility = View.VISIBLE

                    // Animate to final state: fade in and slide up
                    it.animate()
                        .translationY(finalY)
                        .alpha(1f)
                        .setDuration(150) // A short, subtle duration
                        .start()
                }
            } else {
                customActionMenu?.visibility = View.VISIBLE // Fallback to top if layout is null
            }
            setupCustomMenuClickListeners(mode)

            // Do not tint any icons to preserve their original colors.

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
            // Animate the menu out (fade out)
            customActionMenu?.animate()
                ?.alpha(0f)
                ?.setDuration(150)
                ?.withEndAction {
                    // After animation, hide the view and clear tints
                    customActionMenu?.visibility = View.GONE
                    // No need to clear filters as none are applied.
                }
                ?.start()
        }
    }

    private fun setupCustomMenuClickListeners(mode: ActionMode?) {
        customActionMenu?.findViewById<ImageButton>(R.id.action_copy)?.setOnClickListener {
            val rawSelectedText = getSelectedText()
            if (rawSelectedText.isNotEmpty()) {
                val textToShare = getFormattedTextForAction(rawSelectedText)
                copyToClipboard(textToShare)
                Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
            mode?.finish()
        }

        customActionMenu?.findViewById<ImageButton>(R.id.action_share_whatsapp)?.setOnClickListener {
            val rawSelectedText = getSelectedText()
            if (rawSelectedText.isNotEmpty()) {
                val textToShare = getFormattedTextForAction(rawSelectedText)
                shareToApp(textToShare, "com.whatsapp")
            }
            mode?.finish()
        }

        customActionMenu?.findViewById<ImageButton>(R.id.action_share_facebook)?.setOnClickListener {
            val rawSelectedText = getSelectedText()
            if (rawSelectedText.isNotEmpty()) {
                val textToShare = getFormattedTextForAction(rawSelectedText)
                shareToApp(textToShare, "com.facebook.katana")
            }
            mode?.finish()
        }

        customActionMenu?.findViewById<ImageButton>(R.id.action_keep_notes)?.setOnClickListener {
            val rawSelectedText = getSelectedText()
            if (rawSelectedText.isNotEmpty()) {
                val textToSave = getFormattedTextForAction(rawSelectedText)
                saveNote(textToSave)
                Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show()
            }
            mode?.finish()
        }

        customActionMenu?.findViewById<ImageButton>(R.id.action_report_error)?.setOnClickListener {
            val rawSelectedText = getSelectedText()
            if (rawSelectedText.isNotEmpty()) {
                val textToReport = getFormattedTextForAction(rawSelectedText)
                val subject = "I found an error!"
                val body = "Please rectify the errors in spelling etc. in the following lines:\n\n\"$textToReport\"\n\n[Corrected lines are: write corrected lines here]"

                // For ACTION_SENDTO, subject and body must be part of the mailto URI.
                val uriText = "mailto:kadachabuk@gmail.com" +
                        "?subject=" + Uri.encode(subject) +
                        "&body=" + Uri.encode(body)
                val mailtoUri = Uri.parse(uriText)

                val emailIntent = Intent(Intent.ACTION_SENDTO, mailtoUri)
                try {
                    startActivity(emailIntent)
                } catch (ex: android.content.ActivityNotFoundException) {
                    Toast.makeText(this, "There are no email clients installed.", Toast.LENGTH_SHORT).show()
                }
            }
            mode?.finish()
        }
    }

    private fun saveNote(note: String) {
        val prefs = getSharedPreferences(NOTES_PREFS, Context.MODE_PRIVATE)
        val existingNotes = prefs.getStringSet(KEY_NOTES, emptySet())?.toMutableSet()
            ?: mutableSetOf()

        // Create a JSON object for the new note
        val noteObject = JSONObject()
        noteObject.put("text", note)
        noteObject.put("timestamp", System.currentTimeMillis())

        existingNotes.add(noteObject.toString())
        with(prefs.edit()) {
            putStringSet(KEY_NOTES, existingNotes)
            apply()
        }
    }

    private fun getFormattedTextForAction(selectedText: String): String {
        val header = chapterHeading ?: ""
        val date = chapterDate?.removeSurrounding("(", ")") ?: ""
        val attribution = "\n\n[${header}, ${date}]"
        return "$selectedText$attribution"
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
        sessionStartTime = System.currentTimeMillis()
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

            if (savedScrollY > 100 && savedTimestamp > 0) { // Only prompt if they've scrolled a bit.
                val customView = LayoutInflater.from(this).inflate(R.layout.dialog_resume_reading, null)
                MaterialAlertDialogBuilder(this)
                    .setView(customView)
                    .setPositiveButton("Yes") { dialog, _ ->
                        scrollView.post {
                            scrollView.smoothScrollTo(0, savedScrollY)
                            highlightLineAt(savedScrollY) // Add the highlight animation
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

    private fun setupReadingHistory() {
        val historyKeyBase = getHistoryKeyBase() ?: return
        val prefs = getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE)

        // 1. Increment read count for this session
        val countKey = "count_$historyKeyBase"
        val currentCount = prefs.getInt(countKey, 0)
        val newCount = currentCount + 1
        prefs.edit().putInt(countKey, newCount).apply()

        // 2. Load total time and format the message
        val timeKey = "time_$historyKeyBase"
        val totalTimeMs = prefs.getLong(timeKey, 0)
        updateHistoryTextView(newCount, totalTimeMs)

        // 3. Schedule the fade-in animation
        CoroutineScope(Dispatchers.Main).launch {
            delay(300000) // Wait for 5 minutes before showing

            // Fade in
            readingHistoryLayout.visibility = View.VISIBLE
            readingHistoryLayout.animate()
                .alpha(1f)
                .setDuration(1000) // 1-second fade-in
                .start()

            delay(30000) // Keep it on screen for 30 seconds

            // Fade out
            readingHistoryLayout.animate()
                .alpha(0f)
                .setDuration(1000) // 1-second fade-out
                .withEndAction { readingHistoryLayout.visibility = View.GONE }
                .start()
        }
    }

    private fun saveReadingTime() {
        val historyKeyBase = getHistoryKeyBase() ?: return
        if (sessionStartTime == 0L) return

        val sessionDuration = System.currentTimeMillis() - sessionStartTime
        // Only save if the session was longer than 10 seconds to avoid counting brief views
        if (sessionDuration < 10000) return

        val prefs = getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE)
        val timeKey = "time_$historyKeyBase"

        val existingTime = prefs.getLong(timeKey, 0)
        val newTotalTime = existingTime + sessionDuration

        prefs.edit().putLong(timeKey, newTotalTime).apply()
    }

    private fun updateHistoryTextView(count: Int, totalTimeMs: Long) {
        val hours = TimeUnit.MILLISECONDS.toHours(totalTimeMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(totalTimeMs) % 60

        val countText = if (count == 1) "1st time" else "$count times"
        val timeText = if (hours > 0) "$hours H, $minutes m" else "$minutes m"

        readingHistoryTextView.text =
            "You are reading this chapter for the $countText, total reading time $timeText"
    }

    private fun getHistoryKeyBase(): String? {
        return if (::chapterSerial.isInitialized && ::languageCode.isInitialized && chapterSerial.isNotEmpty() && languageCode.isNotEmpty()) {
            "${languageCode}_${chapterSerial}"
        } else {
            null
        }
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
        saveReadingTime()
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
        val searchQueryLength = intent.getStringExtra("EXTRA_SEARCH_QUERY")?.length ?: 0
        if (searchQueryLength == 0) return

        val spannable = SpannableStringBuilder(textViewData.text)

        // 1. Revert the previously highlighted item (if any) to the standard highlight color.
        if (previousMatchIndex != -1 && previousMatchIndex != matchIndex) {
            val prevCharIndex = matchIndices[previousMatchIndex]
            spannable.setSpan(
                BackgroundColorSpan(ContextCompat.getColor(this, R.color.highlight_color)),
                prevCharIndex,
                prevCharIndex + searchQueryLength,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // 2. Highlight the new current item with a distinct color.
        spannable.setSpan(
            BackgroundColorSpan(ContextCompat.getColor(this, R.color.current_match_highlight)),
            charIndex,
            charIndex + searchQueryLength,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // 3. Update the TextView with the new spannable text.
        textViewData.text = spannable

        // 4. Update the previous match index to the current one for the next navigation.
        previousMatchIndex = matchIndex

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
