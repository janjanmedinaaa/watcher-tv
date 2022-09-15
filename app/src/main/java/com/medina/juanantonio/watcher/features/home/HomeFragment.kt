package com.medina.juanantonio.watcher.features.home

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.viewModels
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.BrowseSupportFragment
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
import kotlinx.coroutines.launch

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
    private lateinit var handler: Handler

    // The DisplayMetrics instance is used to get the screen dimensions
    private val displayMetrics = DisplayMetrics()

    // The URI of the background we are currently displaying to avoid reloading the same one
    private var backgroundUri = ""

    private val backgroundRunnable: Runnable = Runnable {
        updateBackgroundImmediate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val episodeList = HomeFragmentArgs.fromBundle(requireArguments()).episodeList
        viewModel.setupVideoList(episodeList)

        displayMetrics.setTo(resources.displayMetrics)

        handler = Handler(Looper.getMainLooper())
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
                    HomePageBean.ContentType.MOVIE -> viewModel.getVideo(item)
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
    }

    /**
     * Updates the main fragment background after a delay
     *
     * This delay allows the user to quickly scroll through content without flashing a changing
     * background with every item that is passed.
     */
    private fun updateBackgroundDelayed(video: Video) {
        if (backgroundUri != video.imageUrl) {
            handler.removeCallbacks(backgroundRunnable)
            backgroundUri = video.imageUrl

            if (backgroundUri.isEmpty()) {
                showDefaultBackground()
            } else {
                handler.postDelayed(backgroundRunnable, BACKGROUND_UPDATE_DELAY_MILLIS)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.contentLoaded = false
    }

    private fun updateBackgroundImmediate() {
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

        viewModel.viewModelScope.launch {
            val bitmapDrawable = imageLoader.execute(imageRequest).drawable as? BitmapDrawable
            if (bitmapDrawable != null) {
                backgroundManager.setBitmap(bitmapDrawable.bitmap)
            } else {
                showDefaultBackground()
            }
        }
    }

    private fun showDefaultBackground() {
        backgroundUri = ""
        backgroundManager.setThemeDrawableResourceId(BACKGROUND_RESOURCE_ID)
    }
}