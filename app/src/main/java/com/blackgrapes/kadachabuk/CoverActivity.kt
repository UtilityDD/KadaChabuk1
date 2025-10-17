package com.blackgrapes.kadachabuk

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.content.res.Configuration
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class CoverActivity : AppCompatActivity() {

    private val bookViewModel: BookViewModel by viewModels()
    private lateinit var coverLayout: View
    private lateinit var tapToOpenText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySavedTheme() // Apply the theme first to prevent visual glitches
        setContentView(R.layout.activity_cover)

        // Allow the app to draw behind the system bars for a seamless UI
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Make the status bar transparent to show the background color underneath
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        // Adjust system icon colors based on the current theme (light/dark)
        val controller = ViewCompat.getWindowInsetsController(window.decorView)
        val isNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        // For black icons in dark mode, isAppearanceLightStatusBars should be true.
        // For white icons in light mode, isAppearanceLightStatusBars should be false.
        // This is the opposite of the default behavior.
        controller?.isAppearanceLightStatusBars = isNightMode

        coverLayout = findViewById(R.id.cover_layout)
        tapToOpenText = findViewById(R.id.textView)

        // Animate the "tap to open" text
        val fadeInOut = AnimationUtils.loadAnimation(this, R.anim.fade_in_out)
        tapToOpenText.startAnimation(fadeInOut)

        // Pre-load chapters in the background
        preloadChapters()

        // Set a click listener on the root view to proceed on tap
        coverLayout.setOnClickListener {
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        // Stop the animation when we navigate away
        coverLayout.setOnClickListener(null) // Prevent double taps

        // Now it's safe to start the new activity and its transition.
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }

    private fun applySavedTheme() {
        val sharedPreferences = getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)
        val nightMode = sharedPreferences.getInt("NightMode", AppCompatDelegate.MODE_NIGHT_NO)
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun preloadChapters() {
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val savedLangCode = sharedPreferences.getString("selected_language_code", null)

        // If a language is selected, start fetching chapters.
        // If not, MainActivity will show the language selection dialog on first launch.
        if (savedLangCode != null) {
            val languageNames = resources.getStringArray(R.array.language_names)
            val languageCodes = resources.getStringArray(R.array.language_codes)
            val langIndex = languageCodes.indexOf(savedLangCode)

            if (langIndex != -1) {
                Log.d("CoverActivity", "Preloading chapters for $savedLangCode")
                bookViewModel.fetchAndLoadChapters(savedLangCode, languageNames[langIndex], forceDownload = false)
            }
        }
    }
}