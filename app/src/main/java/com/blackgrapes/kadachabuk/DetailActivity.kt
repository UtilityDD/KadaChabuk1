package com.blackgrapes.kadachabuk

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ScrollView // Or androidx.core.widget.NestedScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class DetailActivity : AppCompatActivity() {

    private lateinit var scrollView: ScrollView
    private var isFullScreen = false
    // Threshold to ENTER fullscreen when scrolling DOWN
    private val enterFullScreenScrollThreshold = 200
    // Threshold to EXIT fullscreen when scrolling UP (can be different, e.g., less sensitive)
    // Or, you might want to exit when scrollY is very low (near the top)
    private val exitFullScreenScrollThreshold = 100 // Pixels from top to exit, or simply scroll up past original entry threshold

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        scrollView = findViewById(R.id.scrollView)

        val textViewHeading: TextView = findViewById(R.id.textViewHeading)
        val textViewDate: TextView = findViewById(R.id.textViewDate)
        val textViewData: TextView = findViewById(R.id.textViewData)

        val heading = intent.getStringExtra("EXTRA_HEADING")
        val date = intent.getStringExtra("EXTRA_DATE")
        val dataContent = intent.getStringExtra("EXTRA_DATA")

        textViewHeading.text = heading
        textViewDate.text = date
        textViewData.text = dataContent
        title = heading ?: "Details"

        ViewCompat.setOnApplyWindowInsetsListener(scrollView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply padding only if not in fullscreen to avoid jumpiness when exiting
            if (!isFullScreen) {
                view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            } else {
                // When in fullscreen, system bars are hidden, so minimal/no padding needed from insets
                // You might still want some base padding defined in your XML or apply it here
                view.setPadding(view.paddingLeft, 0, view.paddingRight, 0) // Example: clear top/bottom system padding
            }
            WindowInsetsCompat.CONSUMED
        }

        scrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (scrollY > oldScrollY && scrollY > enterFullScreenScrollThreshold && !isFullScreen) {
                // Scrolling DOWN past threshold, and not yet fullscreen
                enterFullScreen()
            } else if (scrollY < oldScrollY && isFullScreen) {
                // Scrolling UP, and currently fullscreen
                // Exit if scrolled back above the initial entry threshold,
                // OR if scrolled near the top (e.g., scrollY < exitFullScreenScrollThreshold)
                if (scrollY < enterFullScreenScrollThreshold) { // Simple: exit if scrolled back above where fullscreen was triggered
                    // A more robust way could be:
                    // if (scrollY < exitFullScreenScrollThreshold || scrollY == 0) { // Exit if scrolled significantly up or to the very top
                    exitFullScreen()
                }
            } else if (scrollY == 0 && isFullScreen) {
                // Special case: if scrolled to the very top and still in fullscreen, exit.
                exitFullScreen()
            }
        }
    }

    private fun enterFullScreen() {
        if (isFullScreen) return // Already fullscreen

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
        // Request insets update if needed, though exiting fullscreen and re-applying padding
        // via the listener might be more reliable for the padding change.
        // For a smoother transition, you might animate the padding changes.
    }

    private fun exitFullScreen() {
        if (!isFullScreen) return // Already not fullscreen

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    // Optionally, re-add SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    // and SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN if you still want
                    // the layout to go edge-to-edge even when bars are visible.
                    // For simplicity here, we clear most fullscreen flags.
                    )
        }
        supportActionBar?.show()
        isFullScreen = false
        // Important: After exiting fullscreen, we need to re-apply the insets for padding.
        // Requesting a new layout pass will trigger the OnApplyWindowInsetsListener.
        ViewCompat.requestApplyInsets(scrollView)
    }

    override fun onPause() {
        super.onPause()
        // Good practice: If activity is paused while in fullscreen, exit fullscreen
        // to prevent UI issues if the app is backgrounded and then returned to.
        if (isFullScreen) {
            exitFullScreen()
        }
    }
}

