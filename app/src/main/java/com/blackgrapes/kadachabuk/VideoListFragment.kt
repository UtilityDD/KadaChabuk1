package com.blackgrapes.kadachabuk

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class VideoListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var scrollToTopButton: FloatingActionButton
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
        scrollToTopButton = view.findViewById(R.id.fab_scroll_to_top)

        recyclerView.layoutManager = LinearLayoutManager(context)
        playbackListener?.let {
            recyclerView.adapter = VideoAdapter(videos, it)
        }

        scrollToTopButton.setOnClickListener {
            recyclerView.smoothScrollToPosition(0)
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                // Show the button if the user has scrolled down past the first two items
                if ((recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition() > 2) {
                    if (scrollToTopButton.visibility != View.VISIBLE) {
                        scrollToTopButton.animate().alpha(1f).setDuration(200).withStartAction { scrollToTopButton.visibility = View.VISIBLE }
                    }
                } else {
                    if (scrollToTopButton.visibility == View.VISIBLE) {
                        scrollToTopButton.animate().alpha(0f).setDuration(200).withEndAction { scrollToTopButton.visibility = View.GONE }
                    }
                }
            }
        })
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