package com.blackgrapes.kadachabuk

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

class VideoActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        toolbar = findViewById(R.id.toolbar)
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
        val lines = csvData.split("\n").drop(1) // Drop header row
        for (line in lines) {
            val columns = line.split(",")
            if (columns.size >= 3) {
                // Assuming columns are: sl, link, remark
                videos.add(Video(sl = columns[0].trim(), link = columns[1].trim(), remark = columns[2].trim()))
            }
        }
        return videos
    }
}