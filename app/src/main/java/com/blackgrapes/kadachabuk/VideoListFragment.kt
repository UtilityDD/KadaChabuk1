package com.blackgrapes.kadachabuk

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class VideoListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private var videos: List<Video> = emptyList()
    private var playbackListener: VideoPlaybackListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is VideoPlaybackListener) {
            playbackListener = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            videos = it.getParcelableArrayList(ARG_VIDEOS) ?: emptyList()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_video_list, container, false)
        recyclerView = view.findViewById(R.id.videosRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        playbackListener?.let {
            recyclerView.adapter = VideoAdapter(videos, it)
        }
        return view
    }

    companion object {
        private const val ARG_VIDEOS = "videos"

        fun newInstance(videos: List<Video>) = VideoListFragment().apply {
            arguments = Bundle().apply {
                putParcelableArrayList(ARG_VIDEOS, ArrayList(videos))
            }
        }
    }
}