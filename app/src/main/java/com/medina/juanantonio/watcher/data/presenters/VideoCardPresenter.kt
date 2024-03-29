package com.medina.juanantonio.watcher.data.presenters

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.leanback.widget.Presenter
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.data.models.video.Video
import com.medina.juanantonio.watcher.databinding.ViewVideoCardBinding
import com.medina.juanantonio.watcher.github.sources.IUpdateRepository
import com.medina.juanantonio.watcher.network.models.home.HomePageBean

class VideoCardPresenter(private val glide: RequestManager) : Presenter() {

    var viewHolder: ViewHolder? = null

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val context = parent.context
        val binding = ViewVideoCardBinding.inflate(
            LayoutInflater.from(context),
            parent, false
        )

        return ViewHolder(binding.root)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        checkNotNull(item)
        val video = item as Video
        val binding = ViewVideoCardBinding.bind(viewHolder.view)
        this.viewHolder = viewHolder

        setupDevModeUI(viewHolder, video, IUpdateRepository.isDeveloperMode)
        val (title, _) = video.getSeriesTitleDescription()
        val seriesUpdated = video.resourceStatus == HomePageBean.ResourceStatus.UPDATED

        binding.textviewTitle.text = title
        binding.textviewDescription.text = getContentText(binding.root.resources, video)
        binding.groupVideoDetails.isVisible = !video.isHomeDisplay
        binding.linearLayoutNewEpisode.isVisible =
            !video.isMovie && video.isHomeDisplay && seriesUpdated
        binding.textviewScore.apply {
            isVisible = video.showScore && !video.isHomeDisplay

            video.score.let { score ->
                text = "$score"
                val color = when (score) {
                    in 8.0..10.0 -> R.color.high_score
                    in 4.0..7.9 -> R.color.average_score
                    else -> R.color.low_score
                }
                setTextColor(ContextCompat.getColor(context, color))
            }
        }

        if (video.enableDeveloperMode) {
            glide.load(R.drawable.drawable_developer_mode)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.imageviewPoster)

            return
        }

        glide.load(video.imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .override(152 * 2, 203 * 2)
            .error(R.drawable.drawable_image_error)
            .into(binding.imageviewPoster)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val binding = ViewVideoCardBinding.bind(viewHolder.view)
        this.viewHolder = null
        binding.imageviewPoster.setImageBitmap(null)
    }

    private fun getContentText(resources: Resources, video: Video): String {
        if (video.isMovie) {
            return resources.getString(R.string.content_type_movie)
        }

        val (_, description) = video.getSeriesTitleDescription()
        return if (video.episodeNumber == 0) {
            description.ifBlank {
                // Some shows don't have a "Season {number}" in their title
                // because they're either a limited series or has only 1 season
                if (video.episodeCount != 0) {
                    resources.getString(
                        R.string.episode_count_without_season,
                        video.episodeCount
                    )
                } else {
                    // The episodeCount will always be 0 on Search
                    // results and Player video recommendations
                    resources.getString(R.string.content_type_series)
                }
            }
        } else {
            // Video will only have an episodeNumber if it is displaying an Episode
            resources.getString(
                R.string.episode_number,
                video.episodeNumber
            )
        }
    }

    private fun setupDevModeUI(viewHolder: ViewHolder, video: Video, isDevMode: Boolean) {
        val binding = ViewVideoCardBinding.bind(viewHolder.view)
        val context = binding.root.context

        binding.groupDevMode.isVisible = isDevMode
        binding.textviewId.text =
            context.getString(R.string.dev_mode_id_label, video.contentId)
        binding.textviewContentId.run {
            isVisible = video.videoResourceId != -1 && isDevMode
            text = context.getString(R.string.dev_mode_cid_label, video.videoResourceId)
        }
    }
}
