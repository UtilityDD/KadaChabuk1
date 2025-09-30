package com.blackgrapes.kadachabuk

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ImageButton
import android.widget.ImageView // <-- IMPORT THIS
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat // <-- IMPORT THIS
import androidx.core.view.updatePadding
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import kotlin.random.Random // <-- IMPORT THIS

private const val FONT_PREFS = "FontPrefs"
private const val KEY_FONT_SIZE = "fontSize"
private const val DEFAULT_FONT_SIZE = 18f
private const val SCROLL_PREFS = "ScrollPositions"

class DetailActivity : AppCompatActivity() {

    private lateinit var scrollView: ScrollView
    private lateinit var imageViewHeader: ImageView // <-- DECLARE ImageView
    private var isFullScreen = false
    private lateinit var textViewData: TextView
    private lateinit var fontSettingsButton: ImageButton
    private lateinit var chapterSerial: String
    private lateinit var languageCode: String

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

        WindowCompat.setDecorFitsSystemWindows(window, false)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            fontSettingsButton.updatePadding(top = systemBars.top)
            insets
        }

        scrollView = findViewById(R.id.scrollView)
        imageViewHeader = findViewById(R.id.imageViewHeader) // <-- INITIALIZE ImageView

        val textViewHeading: TextView = findViewById(R.id.textViewHeading)
        val textViewDate: TextView = findViewById(R.id.textViewDate)
        val textViewWriter: TextView = findViewById(R.id.textViewWriter)
        textViewData = findViewById(R.id.textViewData)
        fontSettingsButton = findViewById(R.id.button_font_settings)

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

        // Check for a saved scroll position and prompt the user
        checkForSavedScrollPosition()

        // Enter full screen as soon as the activity is created
        enterFullScreen()

        // Set a random header image
        setRandomHeaderImage()
    }

    private fun getScrollPositionKey(): String? {
        return if (::chapterSerial.isInitialized && ::languageCode.isInitialized && chapterSerial.isNotEmpty() && languageCode.isNotEmpty()) {
            "scroll_pos_${languageCode}_${chapterSerial}"
        } else {
            null
        }
    }

    private fun saveScrollPosition() {
        getScrollPositionKey()?.let { key ->
            val sharedPreferences = getSharedPreferences(SCROLL_PREFS, Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putInt(key, scrollView.scrollY)
                apply()
            }
        }
    }

    private fun checkForSavedScrollPosition() {
        getScrollPositionKey()?.let { key ->
            val sharedPreferences = getSharedPreferences(SCROLL_PREFS, Context.MODE_PRIVATE)
            val savedScrollY = sharedPreferences.getInt(key, 0)

            if (savedScrollY > 100) { // Only prompt if they've scrolled a bit
                MaterialAlertDialogBuilder(this)
                    .setTitle("Continue from where you left off?")
                    .setPositiveButton("Resume") { dialog, _ ->
                        scrollView.post { scrollView.smoothScrollTo(0, savedScrollY) }
                        dialog.dismiss()
                    }
                    .setNegativeButton("Start Over") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
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

    override fun onPause() {
        super.onPause()
        saveScrollPosition()
    }
}
