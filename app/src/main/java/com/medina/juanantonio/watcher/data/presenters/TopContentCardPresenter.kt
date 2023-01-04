package com.medina.juanantonio.watcher.data.presenters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.leanback.widget.Presenter
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.data.models.Video
import com.medina.juanantonio.watcher.databinding.ViewTopContentCardBinding
import com.medina.juanantonio.watcher.github.sources.IUpdateRepository

class TopContentCardPresenter(private val glide: RequestManager) : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val context = parent.context
        val binding = ViewTopContentCardBinding.inflate(
            LayoutInflater.from(context),
            parent, false
        )

        return ViewHolder(binding.root)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        checkNotNull(item)
        val video = item as Video
        val binding = ViewTopContentCardBinding.bind(viewHolder.view)

        setupDevModeUI(viewHolder, video, IUpdateRepository.isDeveloperMode)
        glide.load(video.imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .override(120 * 2, 120 * 2)
            .error(R.drawable.drawable_image_error)
            .into(binding.imageviewPoster)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val binding = ViewTopContentCardBinding.bind(viewHolder.view)
        binding.imageviewPoster.setImageBitmap(null)
    }

    private fun setupDevModeUI(viewHolder: ViewHolder, video: Video, isDevMode: Boolean) {
        val binding = ViewTopContentCardBinding.bind(viewHolder.view)
        val context = binding.root.context

        binding.groupDevMode.isVisible = isDevMode
        binding.textviewId.text =
            context.getString(R.string.dev_mode_id_label, video.contentId)
    }
}