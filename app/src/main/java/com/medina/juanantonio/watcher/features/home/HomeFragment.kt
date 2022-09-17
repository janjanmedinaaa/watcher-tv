package com.medina.juanantonio.watcher.features.home

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.viewModels
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ListRow
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.data.adapters.ContentAdapter
import com.medina.juanantonio.watcher.data.models.Video
import com.medina.juanantonio.watcher.data.models.VideoGroup
import com.medina.juanantonio.watcher.network.models.home.HomePageBean
import com.medina.juanantonio.watcher.shared.utils.observeEvent
import dagger.hilt.android.AndroidEntryPoint
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// TODO: Refactor to more generic screen name
@AndroidEntryPoint
class HomeFragment : BrowseSupportFragment() {

    companion object {
        const val BACKGROUND_UPDATE_DELAY_MILLIS = 500L
        const val BACKGROUND_RESOURCE_ID = R.drawable.image_placeholder
    }

    private val viewModel: HomeViewModel by viewModels()
    private val contentAdapter = ContentAdapter()

    private lateinit var backgroundManager: BackgroundManager
    private var imageLoadingJob: Job? = null

    private var episodeList: VideoGroup? = null

    // The DisplayMetrics instance is used to get the screen dimensions
    private val displayMetrics = DisplayMetrics()

    // The URI of the background we are currently displaying to avoid reloading the same one
    private var backgroundUri = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        displayMetrics.setTo(resources.displayMetrics)

        episodeList = HomeFragmentArgs.fromBundle(requireArguments()).episodeList
        backgroundManager = BackgroundManager.getInstance(requireActivity()).apply {
            if (!isAttached) {
                attach(requireActivity().window)
            }
            setThemeDrawableResourceId(BACKGROUND_RESOURCE_ID)
        }

        setOnItemViewClickedListener { _, item, _, _ ->
            if (item is Video) {
                when (item.contentType) {
                    HomePageBean.ContentType.MOVIE -> viewModel.getVideoMedia(item)
                    HomePageBean.ContentType.DRAMA -> viewModel.handleSeries(item)
                    else -> Unit
                }
            }
        }

        setOnItemViewSelectedListener { _, item, _, row ->
            if (item is Video) {
                val isLastItem = contentAdapter.size() == contentAdapter.indexOf(row) + 1
                val isEpisodeList = episodeList != null

                if (isLastItem && !isEpisodeList) viewModel.addNewContent()
                updateBackgroundDelayed(item)
            }
        }

        setOnSearchClickedListener {
            findNavController().navigate(
                HomeFragmentDirections.actionHomeFragmentToVideoSearchFragment()
            )
        }

        searchAffordanceColor =
            ResourcesCompat.getColor(resources, android.R.color.transparent, null)

        badgeDrawable = ResourcesCompat.getDrawable(resources, R.mipmap.ic_launcher, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = contentAdapter

        listenVM()
    }

    override fun onResume() {
        super.onResume()

        viewModel.setupVideoList(episodeList)
    }

    private fun listenVM() {
        viewModel.contentList.observeEvent(viewLifecycleOwner) {
            contentAdapter.addContent(it)
        }

        viewModel.videoMedia.observeEvent(viewLifecycleOwner) {
            findNavController().navigate(
                HomeFragmentDirections.actionHomeFragmentToPlayerFragment(it)
            )
        }

        viewModel.episodeList.observeEvent(viewLifecycleOwner) {
            findNavController().navigate(
                HomeFragmentDirections.actionHomeFragmentSelf(it)
            )
        }

        viewModel.onGoingVideosList.observeEvent(viewLifecycleOwner) { onGoingVideoGroup ->
            val firstRow = contentAdapter.get(0) as? ListRow
            firstRow?.let { first ->
                if (first.headerItem?.name == "Continue Watching") {
                    if (onGoingVideoGroup.videoList.isEmpty())
                        contentAdapter.removeItems(0, 1)
                    else
                        contentAdapter.addVideoGroupOnStart(onGoingVideoGroup, true)
                } else if (onGoingVideoGroup.videoList.isNotEmpty()) {
                    contentAdapter.addVideoGroupOnStart(onGoingVideoGroup, false)
                }
            }
        }
    }

    /**
     * Updates the main fragment background after a delay
     *
     * This delay allows the user to quickly scroll through content without flashing a changing
     * background with every item that is passed.
     */
    private fun updateBackgroundDelayed(video: Video) {
        if (backgroundUri != video.imageUrl) {
            imageLoadingJob?.cancel()
            imageLoadingJob = null
            backgroundUri = video.imageUrl

            if (backgroundUri.isEmpty()) {
                showDefaultBackground()
            } else {
                viewModel.viewModelScope.launch {
                    delay(BACKGROUND_UPDATE_DELAY_MILLIS)
                    updateBackgroundImmediate()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.contentLoaded = false
        imageLoadingJob?.cancel()
        imageLoadingJob = null
    }

    private fun updateBackgroundImmediate() {
        if (activity == null) {
            // Triggered after fragment detached from activity, ignore
            return
        }

        Glide.with(this)
            .asBitmap()
            .load(backgroundUri)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .override(100, 133)
            .apply(RequestOptions.bitmapTransform(BlurTransformation(5, 1)))
            .into(
                object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        backgroundManager.setBitmap(resource)
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        showDefaultBackground()
                    }

                    override fun onLoadCleared(placeholder: Drawable?) = Unit
                }
            )
    }

    private fun showDefaultBackground() {
        backgroundUri = ""
        backgroundManager.setThemeDrawableResourceId(BACKGROUND_RESOURCE_ID)
    }
}