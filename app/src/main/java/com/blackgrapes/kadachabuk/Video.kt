package com.blackgrapes.kadachabuk

import android.os.Parcelable
import java.util.regex.Pattern
import kotlinx.parcelize.Parcelize

@Parcelize
data class Video(
    val sl: String,
    val link: String,
    val remark: String,
    val category: String
) : Parcelable {
    fun getYouTubeVideoId(): String? {
        val pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*"
        val compiledPattern = Pattern.compile(pattern)
        val matcher = compiledPattern.matcher(link)
        return if (matcher.find()) matcher.group() else null
    }
}