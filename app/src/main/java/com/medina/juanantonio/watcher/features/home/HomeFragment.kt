package com.medina.juanantonio.watcher.features.home

import android.graphics.drawable.BitmapDrawable
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
import coil.ImageLoader
import coil.request.ImageRequest
import coil.transform.BlurTransformation
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.data.adapters.ContentAdapter
import com.medina.juanantonio.watcher.data.models.Video
import com.medina.juanantonio.watcher.network.models.home.HomePageBean
import com.medina.juanantonio.watcher.shared.utils.observeEvent
import dagger.hilt.android.AndroidEntryPoint
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
    private lateinit var imageLoader: ImageLoader
    private var imageLoadingJob: Job? = null

    // The DisplayMetrics instance is used to get the screen dimensions
    private val displayMetrics = DisplayMetrics()

    // The URI of the background we are currently displaying to avoid reloading the same one
    private var backgroundUri = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        displayMetrics.setTo(resources.displayMetrics)

        imageLoader = ImageLoader(requireContext())
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

        setOnItemViewSelectedListener { _, item, _, _ ->
            if (item is Video) {
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

        val episodeList = HomeFragmentArgs.fromBundle(requireArguments()).episodeList
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
                    contentAdapter.addVideoGroupOnStart(onGoingVideoGroup, true)
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

    private suspend fun updateBackgroundImmediate() {
        if (activity == null) {
            // Triggered after fragment detached from activity, ignore
            return
        }
        val imageRequest = ImageRequest
            .Builder(requireContext())
            .data(backgroundUri)
            .transformations(listOf(BlurTransformation(requireContext(), 20f)))
            .crossfade(true)
            .crossfade(500)
            .size(displayMetrics.widthPixels, displayMetrics.heightPixels)
            .build()

        val bitmapDrawable = imageLoader.execute(imageRequest).drawable as? BitmapDrawable
        if (bitmapDrawable != null) {
            backgroundManager.setBitmap(bitmapDrawable.bitmap)
        } else {
            showDefaultBackground()
        }
    }

    private fun showDefaultBackground() {
        backgroundUri = ""
        backgroundManager.setThemeDrawableResourceId(BACKGROUND_RESOURCE_ID)
    }
}