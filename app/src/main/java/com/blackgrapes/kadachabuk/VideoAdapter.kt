package com.blackgrapes.kadachabuk

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class VideoAdapter(private val videos: List<Video>) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    private var currentlyPlayingPosition = -1

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.videoThumbnail)
        val remark: TextView = view.findViewById(R.id.videoRemark)
        val webView: WebView = view.findViewById(R.id.video_webview)

        fun releasePlayer() {
            webView.stopLoading()
            webView.loadUrl("about:blank")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]
        holder.remark.text = video.remark

        val videoId = video.getYouTubeVideoId()
        if (videoId != null) {
            val thumbnailUrl = "https://img.youtube.com/vi/$videoId/0.jpg"
            Picasso.get().load(thumbnailUrl).into(holder.thumbnail)
        }

        if (position == currentlyPlayingPosition) {
            // This item should be playing
            holder.thumbnail.visibility = View.GONE
            holder.webView.visibility = View.VISIBLE

            val htmlContent = """
                <html><body style="margin:0;padding:0;background-color:black;">
                <iframe width="100%" height="100%" src="https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1" frameborder="0" allow="autoplay; fullscreen" allowfullscreen></iframe>
                </body></html>
            """.trimIndent()

            holder.webView.settings.javaScriptEnabled = true
            holder.webView.webChromeClient = WebChromeClient()
            holder.webView.loadData(htmlContent, "text/html", "utf-8")
        } else {
            // This item should show the thumbnail
            holder.releasePlayer()
            holder.thumbnail.visibility = View.VISIBLE
            holder.webView.visibility = View.GONE
        }

        holder.thumbnail.setOnClickListener {
            val previousPlayingPosition = currentlyPlayingPosition
            currentlyPlayingPosition = holder.adapterPosition

            if (previousPlayingPosition != -1) {
                notifyItemChanged(previousPlayingPosition)
            }
            notifyItemChanged(currentlyPlayingPosition)
        }
    }

    override fun onViewRecycled(holder: VideoViewHolder) {
        super.onViewRecycled(holder)
        // When a view is recycled, release its player to stop playback
        if (holder.adapterPosition == currentlyPlayingPosition) {
            currentlyPlayingPosition = -1
        }
        holder.releasePlayer()
        holder.thumbnail.visibility = View.VISIBLE
        holder.webView.visibility = View.GONE
    }

    override fun getItemCount() = videos.size
}