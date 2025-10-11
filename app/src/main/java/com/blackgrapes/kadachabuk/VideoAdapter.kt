package com.blackgrapes.kadachabuk

import android.content.Intent
import android.net.Uri
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

        val videoId = video.getYouTubeVideoId()
        if (videoId != null) {
            val thumbnailUrl = "https://img.youtube.com/vi/$videoId/0.jpg"
            Picasso.get().load(thumbnailUrl).into(holder.thumbnail)
        }

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + videoId))
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=" + videoId))
            try {
                context.startActivity(appIntent)
            } catch (ex: android.content.ActivityNotFoundException) {
                context.startActivity(webIntent)
            }
        }
    }

    override fun getItemCount() = videos.size
}