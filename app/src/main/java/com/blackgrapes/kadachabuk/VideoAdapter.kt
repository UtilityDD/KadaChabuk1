package com.blackgrapes.kadachabuk

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.content.SharedPreferences
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class VideoAdapter(
    private var videos: List<Video>,
    private val playbackListener: VideoPlaybackListener,
    private val onFavoriteChanged: () -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    private var currentlyPlayingPosition: Int = -1
    private lateinit var favoritePrefs: SharedPreferences

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.videoThumbnail)
        val remark: TextView = view.findViewById(R.id.videoRemark)
        val webView: WebView = view.findViewById(R.id.video_webview)
        val favoriteButton: ImageButton = view.findViewById(R.id.favoriteButton)

        fun releasePlayer() {
            webView.stopLoading()
            webView.loadUrl("about:blank")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        if (!::favoritePrefs.isInitialized) {
            favoritePrefs = parent.context.getSharedPreferences("VideoFavorites", Context.MODE_PRIVATE)
        }
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]
        // Combine serial number and remark into one TextView
        holder.remark.text = "${position + 1}. ${video.remark}"
        val videoId = video.getYouTubeVideoId()
        if (videoId != null) {
            val thumbnailUrl = "https://img.youtube.com/vi/$videoId/0.jpg"
            Picasso.get().load(thumbnailUrl).into(holder.thumbnail)
        }

        // Set favorite button state
        val isFavorite = favoritePrefs.getBoolean(video.getUniqueId(), false)
        holder.favoriteButton.setImageResource(if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border)

        holder.favoriteButton.setOnClickListener {
            val growAnim = AnimationUtils.loadAnimation(it.context, R.anim.heart_grow)
            val shrinkAnim = AnimationUtils.loadAnimation(it.context, R.anim.heart_shrink)

            growAnim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}

                override fun onAnimationEnd(animation: Animation?) {
                    val currentIsFavorite = favoritePrefs.getBoolean(video.getUniqueId(), false)
                    val newIsFavorite = !currentIsFavorite
                    with(favoritePrefs.edit()) {
                        putBoolean(video.getUniqueId(), newIsFavorite)
                        apply()
                    }
                    holder.favoriteButton.setImageResource(if (newIsFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border)
                    holder.favoriteButton.startAnimation(shrinkAnim)
                    onFavoriteChanged()
                }

                override fun onAnimationRepeat(animation: Animation?) {}
            })

            // Disable the button during the animation to prevent rapid clicks
            holder.favoriteButton.isClickable = false
            holder.favoriteButton.startAnimation(growAnim)
            holder.favoriteButton.postDelayed({ holder.favoriteButton.isClickable = true }, 300) // Re-enable after animation
        }

        if (position == currentlyPlayingPosition) {
            // This item should be playing
            playbackListener.onVideoPlaybackChanged(video.remark)
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
            // If this view was previously playing, reset the title.
            holder.releasePlayer()
            holder.thumbnail.visibility = View.VISIBLE
            holder.webView.visibility = View.GONE
        }

        holder.thumbnail.setOnClickListener {
            val previousPlayingPosition = currentlyPlayingPosition
            val clickedPosition = holder.adapterPosition

            if (clickedPosition == currentlyPlayingPosition) {
                // Tapped on the currently playing video, so stop it.
                currentlyPlayingPosition = -1
                notifyItemChanged(clickedPosition)
                playbackListener.onVideoPlaybackChanged(null) // Reset title
                return@setOnClickListener
            }

            currentlyPlayingPosition = clickedPosition

            if (previousPlayingPosition != -1) {
                notifyItemChanged(previousPlayingPosition)
            }
            notifyItemChanged(currentlyPlayingPosition)
        }
    }

    override fun onViewDetachedFromWindow(holder: VideoViewHolder) {
        super.onViewDetachedFromWindow(holder)
        // This is called when a view is scrolled off-screen.
        // We stop the video playback if this is the currently playing item.
        if (holder.adapterPosition == currentlyPlayingPosition) {
            currentlyPlayingPosition = -1
            notifyItemChanged(holder.adapterPosition)
            playbackListener.onVideoPlaybackChanged(null) // Reset title
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

    fun updateVideos(newVideos: List<Video>) {
        val diffCallback = VideoDiffCallback(this.videos, newVideos)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.videos = newVideos.toList() // Use a new list instance
        diffResult.dispatchUpdatesTo(this)
    }

    private class VideoDiffCallback(
        private val oldList: List<Video>,
        private val newList: List<Video>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].getUniqueId() == newList[newItemPosition].getUniqueId()
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            // We only care about the favorite status for content changes in this context,
            // but comparing the whole object is safer and handles other potential changes.
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}