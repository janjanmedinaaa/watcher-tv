package com.medina.juanantonio.watcher.data.presenters

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.data.models.Video
import com.medina.juanantonio.watcher.databinding.ViewLeaderboardCardBinding
import com.medina.juanantonio.watcher.network.models.home.HomePageBean

class LeaderboardCardPresenter(private val glide: RequestManager) : Presenter() {

    var viewHolder: ViewHolder? = null

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val context = parent.context
        val binding = ViewLeaderboardCardBinding.inflate(
            LayoutInflater.from(context),
            parent, false
        )

        return ViewHolder(binding.root)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        checkNotNull(item)
        val video = item as Video
        val binding = ViewLeaderboardCardBinding.bind(viewHolder.view)
        this.viewHolder = viewHolder

        val (title, _) = video.getSeriesTitleDescription()
        binding.textviewTitle.text = title
        binding.textviewDescription.text = getContentText(binding.root.resources, video)

        glide.load(video.imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .override(256 * 2, 144 * 2)
            .error(R.mipmap.ic_launcher)
            .into(binding.imageviewPoster)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val binding = ViewLeaderboardCardBinding.bind(viewHolder.view)
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
                    when (video.resourceStatus) {
                        HomePageBean.ResourceStatus.UPDATED -> {
                            resources.getString(
                                R.string.episode_count_without_season_updated,
                                video.episodeCount
                            )
                        }
                        else -> {
                            resources.getString(
                                R.string.episode_count_without_season,
                                video.episodeCount
                            )
                        }
                    }
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
}
