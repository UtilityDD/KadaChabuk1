package com.blackgrapes.kadachabuk

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class VideoAdapter(private val videos: List<Video>) :
    RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.videoThumbnail)
        val remark: TextView = view.findViewById(R.id.videoRemark)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]
        holder.remark.text = video.remark

        val videoId = video.getYouTubeVideoId() // For loading the thumbnail
        if (videoId != null) {
            val thumbnailUrl = "https://img.youtube.com/vi/$videoId/0.jpg"
            Picasso.get().load(thumbnailUrl).into(holder.thumbnail)
        }

        holder.itemView.setOnClickListener {
            // Re-fetch the video and its ID at the time of click to ensure correctness
            val clickedPosition = holder.adapterPosition
            if (clickedPosition == RecyclerView.NO_POSITION) return@setOnClickListener

            val clickedVideo = videos[clickedPosition]
            val clickedVideoId = clickedVideo.getYouTubeVideoId()
            // Only start the activity if we successfully extracted a video ID.
            if (clickedVideoId != null) {
                val context = holder.itemView.context
                val intent = Intent(context, VideoPlayerActivity::class.java)
                intent.putExtra("VIDEO_ID", clickedVideoId)
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount() = videos.size
}