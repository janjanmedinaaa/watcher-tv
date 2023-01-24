package com.medina.juanantonio.watcher.data.presenters

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.leanback.widget.Presenter
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.data.models.video.Video
import com.medina.juanantonio.watcher.databinding.ViewLeaderboardCardBinding
import com.medina.juanantonio.watcher.github.sources.IUpdateRepository

class LeaderboardCardPresenter(private val glide: RequestManager) : Presenter() {

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

        setupDevModeUI(viewHolder, video, IUpdateRepository.isDeveloperMode)
        val (title, _) = video.getSeriesTitleDescription()
        binding.textviewTitle.text = title
        binding.textviewDescription.text = getContentText(binding.root.resources, video)

        glide.load(video.imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .override(256 * 2, 144 * 2)
            .error(R.drawable.drawable_image_error)
            .into(binding.imageviewPoster)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val binding = ViewLeaderboardCardBinding.bind(viewHolder.view)
        binding.imageviewPoster.setImageBitmap(null)
    }

    private fun getContentText(resources: Resources, video: Video): String {
        return if (video.isMovie) {
            resources.getString(R.string.content_type_movie)
        } else {
            val (_, description) = video.getSeriesTitleDescription()
            description.ifBlank {
                resources.getString(R.string.content_type_series)
            }
        }
    }

    private fun setupDevModeUI(viewHolder: ViewHolder, video: Video, isDevMode: Boolean) {
        val binding = ViewLeaderboardCardBinding.bind(viewHolder.view)
        val context = binding.root.context

        binding.groupDevMode.isVisible = isDevMode
        binding.textviewId.text =
            context.getString(R.string.dev_mode_id_label, video.contentId)
    }
}
