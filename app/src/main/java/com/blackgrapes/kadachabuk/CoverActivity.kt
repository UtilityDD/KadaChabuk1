package com.blackgrapes.kadachabuk

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.AnimatorSet
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class CoverActivity : AppCompatActivity() {

    private val bookViewModel: BookViewModel by viewModels()
    private lateinit var coverLayout: View
    private lateinit var spineShadow: View
    private var peekingAnimatorSet: AnimatorSet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cover)

        // Make it fullscreen
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = ViewCompat.getWindowInsetsController(window.decorView)
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        coverLayout = findViewById(R.id.cover_layout)
        spineShadow = findViewById(R.id.spine_shadow)

        // Pre-load chapters in the background
        preloadChapters()

        // Set a click listener on the root view to proceed on tap
        coverLayout.setOnClickListener {
            navigateToMain()
        }

        // Start the subtle "peeking" animation
        startPeekingAnimation()
    }

    private fun startPeekingAnimation() {
        // This animation gives a hint that the cover can be opened.
        // It slightly lifts and rotates the right edge of the cover.
        coverLayout.pivotX = 0f // Set pivot to the left edge for a book-like feel
        coverLayout.pivotY = (coverLayout.height / 2).toFloat()

        // Animate from 0 degrees to -2.5 degrees (a subtle peek)
        val rotationAnimator = ObjectAnimator.ofFloat(coverLayout, "rotationY", 0f, -2.5f).apply {
            duration = 2500 // Slower duration for a smoother feel
            interpolator = AccelerateDecelerateInterpolator()
            repeatCount = ObjectAnimator.INFINITE // Loop forever
            repeatMode = ObjectAnimator.REVERSE // Animate back and forth (0 -> -2.5 -> 0 -> ...)
        }

        // Animate the shadow's alpha to make it more prominent as the cover lifts
        val shadowAnimator = ObjectAnimator.ofFloat(spineShadow, "alpha", 1.0f, 0.6f).apply {
            duration = 2500
            interpolator = AccelerateDecelerateInterpolator()
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
        }

        peekingAnimatorSet = AnimatorSet().apply {
            // Play both animations together
            playTogether(rotationAnimator, shadowAnimator)
            startDelay = 1000 // Wait a second before starting
        }
        peekingAnimatorSet?.start()
    }

    private fun navigateToMain() {
        // Stop the animation when we navigate away
        coverLayout.setOnClickListener(null) // Prevent double taps

        // 1. Cancel the ongoing peeking animation.
        peekingAnimatorSet?.cancel()

        // 2. Clear the animation from the views and reset their properties.
        //    This is the crucial step to prevent animation conflicts.
        coverLayout.clearAnimation()
        spineShadow.clearAnimation()
        coverLayout.rotationY = 0f
        spineShadow.alpha = 1.0f

        // 3. Now it's safe to start the new activity and its transition.
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.animator.flip_in_from_middle, R.animator.flip_out_to_middle)
        finish()
    }

    override fun onDestroy() {
        // Ensure the animation is cancelled to prevent memory leaks
        // if the activity is destroyed for any other reason.
        peekingAnimatorSet?.cancel()
        peekingAnimatorSet = null
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