package com.blackgrapes.kadachabuk

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.util.TypedValue
import android.view.View
import android.view.MenuItem
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
 
interface VideoPlaybackListener {
    fun onVideoPlaybackChanged(videoTitle: String?)
}
interface OnFavoriteChangedListener {
    fun onFavoriteChanged()
}
class VideoActivity : AppCompatActivity(), VideoPlaybackListener, OnFavoriteChangedListener {

    private lateinit var progressBar: ProgressBar
    private lateinit var toolbar: MaterialToolbar
    private lateinit var errorGroup: Group
    private lateinit var errorMessageTextView: TextView
    private lateinit var retryButton: Button
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    private var originalTitle: String = "Video Links"
    private var currentViewPagerPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        // Allow content to draw behind the system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Adjust system icon colors based on the current theme (light/dark)
        val controller = ViewCompat.getWindowInsetsController(window.decorView)
        val isNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        // isAppearanceLightStatusBars = true means DARK icons, for light backgrounds.
        controller?.isAppearanceLightStatusBars = isNightMode

        toolbar = findViewById(R.id.toolbar) // Initialize once
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the top inset as padding
            view.setPadding(view.paddingLeft, insets.top, view.paddingRight, view.paddingBottom)

            // Increase the toolbar's height to accommodate the new padding
            val typedValue = TypedValue()
            theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)
            val actionBarSize = TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics)
            view.layoutParams.height = actionBarSize + insets.top

            // Consume the insets
            WindowInsetsCompat.CONSUMED
        }
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.title = "Video Links"

        progressBar = findViewById(R.id.progressBar)
        errorGroup = findViewById(R.id.error_group)
        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)
        errorMessageTextView = findViewById(R.id.error_message)
        retryButton = findViewById(R.id.retry_button)

        retryButton.setOnClickListener {
            fetchVideoData()
        }

        fetchVideoData()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.video_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                // This is the ID for the "up" button.
                onBackPressedDispatcher.onBackPressed()
                return true
            }
            R.id.action_refresh_videos -> {
                // Call fetchVideoData when refresh is clicked
                fetchVideoData()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun fetchVideoData() {
        progressBar.visibility = View.VISIBLE
        errorGroup.visibility = View.GONE
        tabLayout.visibility = View.GONE
        viewPager.visibility = View.GONE
        // The base part of your Google Sheet URL
        val sheetId = "2PACX-1vRztE9nSnn54KQxwLlLMNgk-v1QjfC-AVy35OyBZPFssRt1zSkgrdX1Xi92oW9i3pkx4HV4AZjclLzF"
        val gid = "113075560" // The GID for your "video" sheet
        val url = "https://docs.google.com/spreadsheets/d/e/$sheetId/pub?gid=$gid&single=true&output=csv"

        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                // Launch a coroutine to handle parsing in the background
                CoroutineScope(Dispatchers.Main).launch {
                    val videoList = withContext(Dispatchers.IO) {
                        // This heavy parsing now happens on a background thread
                        parseCsv(response)
                    }

                    // Now, update the UI on the main thread
                    progressBar.visibility = View.GONE
                    tabLayout.visibility = View.VISIBLE
                    viewPager.visibility = View.VISIBLE

                    val favoritePrefs = getSharedPreferences("VideoFavorites", Context.MODE_PRIVATE)
                    val favoriteVideos = videoList.filter { favoritePrefs.getBoolean(it.getUniqueId(), false) }

                    val videoMap = videoList.groupBy { it.category }.toMutableMap()
                    videoMap["Favorites"] = favoriteVideos

                    val categories = listOf("Favorites", "Speech", "Mahanam", "Vedic Song")
                    val fragments = categories.map { category ->
                        VideoListFragment.newInstance(videoMap[category] ?: emptyList())
                    }

                    val adapter = VideoPagerAdapter(this@VideoActivity, fragments)
                    viewPager.adapter = adapter

                    // Set custom icons for tabs
                    val tabIcons = listOf(
                        R.drawable.ic_favorite_filled,
                        0, // No icon for Speech
                        0, // No icon for Mahanam
                        0  // No icon for Vedic Song
                    )

                    TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                        val category = categories[position]
                        val count = videoMap[category]?.size ?: 0

                        if (position == 0) { // This is the "Favorites" tab
                            tab.setIcon(R.drawable.ic_favorite_filled)
                            tab.contentDescription = "Favorites ($count)"

                            // Create and configure the badge
                            val badge = tab.orCreateBadge
                            badge.number = count
                            badge.isVisible = count > 0
                        } else {
                            // For other tabs, just set the text with the count
                            tab.text = "$category ($count)"
                        }
                    }.attach()

                    // Restore the previously selected tab
                    viewPager.setCurrentItem(currentViewPagerPosition, false)

                    originalTitle = "Video Links (${videoList.size})"
                    toolbar.title = originalTitle
                }
            },
            { error ->
                progressBar.visibility = View.GONE
                errorGroup.visibility = View.VISIBLE
            })

        Volley.newRequestQueue(this).add(stringRequest)
    }

    private fun parseCsv(csvData: String): List<Video> {
        val videos = mutableListOf<Video>()
        csvReader {
            skipEmptyLine = true
        }.open(csvData.byteInputStream()) {
            // Read all rows but skip the first (header) row
            readAllAsSequence().drop(1).forEach { row ->
                if (row.size >= 4) {
                    val link = row[1].trim()
                    // Only add the video if it's a YouTube link
                    if (link.contains("youtube.com") || link.contains("youtu.be")) {
                        val video = Video(
                            sl = row[0].trim(),
                            originalLink = link,
                            remark = row[2].trim(),
                            category = row[3].trim()
                        )
                        videos.add(video)
                    }
                }
            }
        }
        return videos
    }

    override fun onVideoPlaybackChanged(videoTitle: String?) {
        if (videoTitle != null) {
            toolbar.title = videoTitle
        } else {
            toolbar.title = originalTitle
        }
    }

    override fun onFavoriteChanged() {
        // Save the current tab position before refreshing
        currentViewPagerPosition = viewPager.currentItem
        fetchVideoData()
    }
}