package com.medina.juanantonio.watcher.data.presenters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.data.models.Video
import com.medina.juanantonio.watcher.databinding.ViewPersonCardBinding

class PersonCardPresenter(private val glide: RequestManager) : Presenter() {

    var viewHolder: ViewHolder? = null

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val context = parent.context
        val binding = ViewPersonCardBinding.inflate(
            LayoutInflater.from(context),
            parent, false
        )

        return ViewHolder(binding.root)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        checkNotNull(item)
        val video = item as Video
        val binding = ViewPersonCardBinding.bind(viewHolder.view)
        this.viewHolder = viewHolder

        val (title, _) = video.getSeriesTitleDescription()
        binding.textviewTitle.text = title

        glide.load(video.imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .override(120 * 2, 120 * 2)
            .error(R.mipmap.ic_launcher)
            .into(binding.imageviewPoster)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val binding = ViewPersonCardBinding.bind(viewHolder.view)
        this.viewHolder = null
        binding.imageviewPoster.setImageBitmap(null)
    }
}
