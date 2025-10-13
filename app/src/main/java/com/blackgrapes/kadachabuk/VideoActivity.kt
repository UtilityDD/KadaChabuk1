package com.blackgrapes.kadachabuk

import android.content.res.Configuration
import android.os.Bundle
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
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
 
interface VideoPlaybackListener {
    fun onVideoPlaybackChanged(videoTitle: String?)
}

class VideoActivity : AppCompatActivity(), VideoPlaybackListener {

    private lateinit var progressBar: ProgressBar
    private lateinit var toolbar: MaterialToolbar
    private lateinit var errorGroup: Group
    private lateinit var errorMessageTextView: TextView
    private lateinit var retryButton: Button
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    private var originalTitle: String = "Video Links"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        // Allow content to draw behind the system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Adjust system icon colors based on the current theme (light/dark)
        val controller = ViewCompat.getWindowInsetsController(window.decorView)
        // Since the AppBar is now using colorPrimary, the status bar icons should be light
        // to be visible on the dark background. isAppearanceLightStatusBars = false makes them light.
        controller?.isAppearanceLightStatusBars = false

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle arrow click here
        if (item.itemId == android.R.id.home) {
            // This is the ID for the "up" button.
            onBackPressedDispatcher.onBackPressed()
            return true
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
                progressBar.visibility = View.GONE
                tabLayout.visibility = View.VISIBLE
                viewPager.visibility = View.VISIBLE
                val videoList = parseCsv(response)
                val videoMap = videoList.groupBy { it.category }

                val categories = listOf("Speech", "Mahanam", "Vedic Song")
                val fragments = categories.map { category ->
                    VideoListFragment.newInstance(videoMap[category] ?: emptyList())
                }

                val adapter = VideoPagerAdapter(this, fragments)
                viewPager.adapter = adapter

                TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                    val category = categories[position]
                    val count = videoMap[category]?.size ?: 0
                    tab.text = "$category ($count)"
                }.attach()

                originalTitle = "Video Links (${videoList.size})"
                toolbar.title = originalTitle
            },
            { error ->
                progressBar.visibility = View.GONE
                errorMessageTextView.text = "Connect to internet to load videos"
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
                    val video = Video(
                        sl = row[0].trim(),
                        link = row[1].trim(),
                        remark = row[2].trim(),
                        category = row[3].trim()
                    )
                    videos.add(video)
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
}