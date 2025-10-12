package com.blackgrapes.kadachabuk

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

class VideoPlayerActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        val videoId = intent.getStringExtra("VIDEO_ID")
        val webView: WebView = findViewById(R.id.video_webview)

        // The HTML content to load into the WebView. This embeds the YouTube player.
        val htmlContent = """
            <html>
                <body style="margin:0;padding:0;background-color:black;">
                    <iframe width="100%" height="100%" src="https://www.youtube.com/embed/$videoId?autoplay=1" frameborder="0" allow="autoplay; fullscreen" allowfullscreen></iframe>
                </body>
            </html>
        """.trimIndent()

        webView.settings.javaScriptEnabled = true // Required for video playback
        webView.webChromeClient = WebChromeClient() // Allows fullscreen support
        webView.loadData(htmlContent, "text/html", "utf-8")
    }
}