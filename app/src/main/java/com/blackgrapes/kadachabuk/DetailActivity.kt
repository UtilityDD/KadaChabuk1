package com.blackgrapes.kadachabuk // Ensure this matches your actual package name

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
// Removed: import androidx.compose.ui.layout.layout // This line was causing the error

class DetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail) // Make sure activity_detail.xml exists in res/layout

        val heading = intent.getStringExtra("EXTRA_HEADING")
        val date = intent.getStringExtra("EXTRA_DATE")
        val dataContent = intent.getStringExtra("EXTRA_DATA")

        val textViewHeading: TextView = findViewById(R.id.textViewHeading)
        val textViewDate: TextView = findViewById(R.id.textViewDate)
        val textViewData: TextView = findViewById(R.id.textViewData)
        // You can also set the "Write:" text here if it's dynamic,
        // or leave it static in the XML.

        textViewHeading.text = heading
        textViewDate.text = date
        textViewData.text = dataContent

        // Optional: Set the title of the Activity
        title = heading ?: "Details"
    }
}
