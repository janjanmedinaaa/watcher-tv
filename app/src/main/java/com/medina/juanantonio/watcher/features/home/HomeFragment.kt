package com.medina.juanantonio.watcher.features.home

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.viewModels
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ListRow
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.data.adapters.ContentAdapter
import com.medina.juanantonio.watcher.data.models.Video
import com.medina.juanantonio.watcher.data.models.VideoGroup
import com.medina.juanantonio.watcher.features.dialog.DialogActivity
import com.medina.juanantonio.watcher.features.dialog.DialogFragment.Companion.ACTION_ID_POSITIVE
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
        const val BACKGROUND_UPDATE_DELAY_MILLIS = 300L
        const val BACKGROUND_RESOURCE_ID = R.drawable.image_placeholder
    }

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var contentAdapter: ContentAdapter
    private lateinit var glide: RequestManager

    private lateinit var startForResultAutoPlay: ActivityResultLauncher<Intent>

    private lateinit var backgroundManager: BackgroundManager
    private var imageLoadingJob: Job? = null

    private var episodeList: VideoGroup? = null

    private var backgroundUri = ""

    // The DisplayMetrics instance is used to get the screen dimensions
    private val displayMetrics = DisplayMetrics()

    private val backgroundTarget = object : CustomTarget<Bitmap>() {
        override fun onResourceReady(
            resource: Bitmap,
            transition: Transition<in Bitmap>?
        ) {
            backgroundManager.setBitmap(resource)
        }

        override fun onLoadFailed(errorDrawable: Drawable?) = Unit

        override fun onLoadCleared(placeholder: Drawable?) = Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        displayMetrics.setTo(resources.displayMetrics)

        episodeList = HomeFragmentArgs.fromBundle(requireArguments()).episodeList
        glide = Glide.with(requireContext())
        contentAdapter = ContentAdapter(glide)
        backgroundManager = BackgroundManager.getInstance(requireActivity()).apply {
            if (!isAttached) {
                attach(requireActivity().window)
            }
            setThemeDrawableResourceId(BACKGROUND_RESOURCE_ID)
        }

        startForResultAutoPlay = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            when ("${it?.data?.data}".toLongOrNull()) {
                ACTION_ID_POSITIVE -> {
                    viewModel.episodeToAutoPlay.value?.peek()?.let { video ->
                        viewModel.getVideoMedia(video)
                    }
                }
            }
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
        if (backgroundUri.isNotEmpty()) updateBackgroundImmediate(backgroundUri)
    }

    private fun listenVM() {
        viewModel.contentList.observeEvent(viewLifecycleOwner) {
            contentAdapter.addContent(it)
        }

        viewModel.videoMedia.observeEvent(viewLifecycleOwner) {
            showDefaultBackground()
            findNavController().navigate(
                HomeFragmentDirections.actionHomeFragmentToPlayerFragment(it)
            )
        }

        viewModel.episodeToAutoPlay.observeEvent(viewLifecycleOwner) {
            startForResultAutoPlay.launch(
                DialogActivity.getIntent(
                    context = requireContext(),
                    title = getString(R.string.continue_watching_title),
                    description = getString(
                        R.string.continue_watching_description,
                        it.title,
                        it.episodeNumber
                    )
                )
            )
        }

        viewModel.episodeList.observeEvent(viewLifecycleOwner) {
            findNavController().navigate(
                HomeFragmentDirections.actionHomeFragmentSelf(it)
            )
        }

        viewModel.onGoingVideosList.observeEvent(viewLifecycleOwner) { onGoingVideoGroup ->
            if (contentAdapter.size() == 0) return@observeEvent
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
        cancelBackgroundImageLoading()
        backgroundUri = video.imageUrl

        imageLoadingJob = viewModel.viewModelScope.launch {
            delay(BACKGROUND_UPDATE_DELAY_MILLIS)
            updateBackgroundImmediate(video.imageUrl)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.contentLoaded = false
        cancelBackgroundImageLoading()
    }

    private fun updateBackgroundImmediate(backgroundUri: String) {
        if (activity == null) {
            // Triggered after fragment detached from activity, ignore
            return
        }

        glide
            .asBitmap()
            .load(backgroundUri)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .override(100, 133)
            .apply(RequestOptions.bitmapTransform(BlurTransformation(5, 1)))
            .into(backgroundTarget)
    }

    private fun cancelBackgroundImageLoading() {
        glide.clear(backgroundTarget)
        imageLoadingJob?.cancel()
        imageLoadingJob = null
    }

    private fun showDefaultBackground() {
        backgroundManager.setThemeDrawableResourceId(BACKGROUND_RESOURCE_ID)

        val drawable =
            ResourcesCompat.getDrawable(resources, BACKGROUND_RESOURCE_ID, null)
        backgroundManager.setBitmap((drawable as? BitmapDrawable)?.bitmap)
    }
}