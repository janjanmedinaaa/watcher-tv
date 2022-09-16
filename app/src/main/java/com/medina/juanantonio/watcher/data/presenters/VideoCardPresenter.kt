package com.medina.juanantonio.watcher.data.presenters

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.leanback.widget.ImageCardView.*
import androidx.leanback.widget.Presenter
import coil.load
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.data.models.Video
import com.medina.juanantonio.watcher.databinding.ViewVideoCardBinding
import com.medina.juanantonio.watcher.network.models.home.HomePageBean

/**
 * Presents a [Video] as an [ImageCardView] with descriptive text based on the Video's type.
 */
class VideoCardPresenter : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val context = parent.context
        val binding = ViewVideoCardBinding.inflate(LayoutInflater.from(context), parent, false)

        // Set the image size ahead of time since loading can take a while.
        val resources = context.resources
        binding.root.setMainImageDimensions(
            resources.getDimensionPixelSize(R.dimen.image_card_width),
            resources.getDimensionPixelSize(R.dimen.image_card_height)
        )

        return ViewHolder(binding.root)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        checkNotNull(item)
        val video = item as Video
        val binding = ViewVideoCardBinding.bind(viewHolder.view)

        if (video.episodeId == 0 && !video.isSearchResult) {
            binding.root.cardType = CARD_TYPE_FLAG_IMAGE_ONLY
        }

        binding.root.titleText = video.title
        binding.root.contentText = getContentText(binding.root.resources, video)
        binding.root.mainImageView.load(video.imageUrl) {
            crossfade(true)
            crossfade(500)
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val binding = ViewVideoCardBinding.bind(viewHolder.view)
        binding.root.mainImage = null
    }

    /**
     * Returns a string to display as the "content" for an [ImageCardView].
     *
     * Since Watch Next behavior differs for episodes, movies, and clips, this string makes it
     * more clear which [VideoType] each [Video] is. For example, clips are never included in
     * Watch Next.
     */
    private fun getContentText(resources: Resources, video: Video): String? {
        return when (video.contentType) {
            HomePageBean.ContentType.MOVIE -> resources.getString(R.string.content_type_movie)
            HomePageBean.ContentType.DRAMA -> resources.getString(R.string.content_type_series)
            else -> null
        }
    }
}