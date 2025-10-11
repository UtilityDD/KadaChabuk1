package com.blackgrapes.kadachabuk

import android.os.Bundle
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.android.volley.Request
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

class VideoActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        // Allow content to draw behind the system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

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

        recyclerView = findViewById(R.id.videosRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        recyclerView.layoutManager = LinearLayoutManager(this)

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

    private fun updateTitle(count: Int) {
        toolbar.title = "Video Links ($count)"
    }

    private fun fetchVideoData() {
        progressBar.visibility = View.VISIBLE
        // The base part of your Google Sheet URL
        val sheetId = "2PACX-1vRztE9nSnn54KQxwLlLMNgk-v1QjfC-AVy35OyBZPFssRt1zSkgrdX1Xi92oW9i3pkx4HV4AZjclLzF"
        val gid = "113075560" // The GID for your "video" sheet
        val url = "https://docs.google.com/spreadsheets/d/e/$sheetId/pub?gid=$gid&single=true&output=csv"

        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                progressBar.visibility = View.GONE
                val videoList = parseCsv(response)
                updateTitle(videoList.size)
                recyclerView.adapter = VideoAdapter(videoList)
            },
            { error ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Error fetching data: ${error.message}", Toast.LENGTH_LONG).show()
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
                if (row.size >= 3) {
                    // Assuming columns are: sl, link, remark
                    val video = Video(sl = row[0].trim(), link = row[1].trim(), remark = row[2].trim())
                    videos.add(video)
                }
            }
        }
        return videos
    }
}