package com.blackgrapes.kadachabuk

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import java.net.URLEncoder

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        webView = findViewById(R.id.video_webview)
        val video = intent.getParcelableExtra<Video>("video") ?: return

        setupWebView()

        when (video.source) {
            VideoSource.YOUTUBE -> loadYouTubeVideo(video)
            VideoSource.FACEBOOK -> loadFacebookVideo(video)
            else -> {
                // Handle unknown or unsupported video links, perhaps show an error
                finish() // Or show a toast/message
            }
        }
        title = video.remark
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.javaScriptEnabled = true
        webView.webChromeClient = WebChromeClient() // Allows fullscreen video
        webView.webViewClient = WebViewClient() // Ensures links open within the WebView
    }

    private fun loadYouTubeVideo(video: Video) {
        val videoId = video.getYouTubeVideoId()
        if (videoId != null) {
            val videoUrl = "https://www.youtube.com/embed/$videoId?autoplay=1"
            val html = """
                <html><body style="margin:0;padding:0;overflow:hidden;background-color:black;">
                <iframe width="100%" height="100%" src="$videoUrl" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
                </body></html>
            """.trimIndent()
            webView.loadData(html, "text/html", "utf-8")
        }
    }

    private fun loadFacebookVideo(video: Video) {
        // Facebook's embedded player requires the original video URL to be encoded.
        val encodedUrl = URLEncoder.encode(video.link, "UTF-8")
        val embedUrl = "https://www.facebook.com/plugins/video.php?href=$encodedUrl&show_text=false&autoplay=true"
        webView.loadUrl(embedUrl)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}