package com.blackgrapes.kadachabuk

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class CoverActivity : AppCompatActivity() {

    private val bookViewModel: BookViewModel by viewModels()
    private lateinit var coverLayout: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cover)

        // Make it fullscreen
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = ViewCompat.getWindowInsetsController(window.decorView)
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        coverLayout = findViewById(R.id.cover_layout)

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
        overridePendingTransition(R.animator.flip_in_from_middle, R.animator.flip_out_to_middle)
        finish()
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