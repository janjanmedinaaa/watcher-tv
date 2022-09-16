package com.medina.juanantonio.watcher.features.search

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import androidx.fragment.app.viewModels
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.*
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import coil.ImageLoader
import coil.request.ImageRequest
import coil.transform.BlurTransformation
import com.medina.juanantonio.watcher.data.models.Video
import com.medina.juanantonio.watcher.data.presenters.VideoCardPresenter
import com.medina.juanantonio.watcher.features.home.HomeFragment
import com.medina.juanantonio.watcher.network.models.home.HomePageBean
import com.medina.juanantonio.watcher.shared.utils.observeEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VideoSearchFragment : SearchSupportFragment(), SearchSupportFragment.SearchResultProvider {

    private val viewModel: VideoSearchViewModel by viewModels()
    private lateinit var mRowsAdapter: ArrayObjectAdapter
    private var currentQuery = ""

    private lateinit var backgroundManager: BackgroundManager
    private lateinit var imageLoader: ImageLoader
    private var imageLoadingJob: Job? = null

    // The DisplayMetrics instance is used to get the screen dimensions
    private val displayMetrics = DisplayMetrics()

    // The URI of the background we are currently displaying to avoid reloading the same one
    private var backgroundUri = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mRowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        setSearchResultProvider(this)

        displayMetrics.setTo(resources.displayMetrics)

        imageLoader = ImageLoader(requireContext())
        backgroundManager = BackgroundManager.getInstance(requireActivity()).apply {
            if (!isAttached) {
                attach(requireActivity().window)
            }
            setThemeDrawableResourceId(HomeFragment.BACKGROUND_RESOURCE_ID)
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
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listenVM()
    }

    override fun getResultsAdapter(): ObjectAdapter {
        return mRowsAdapter
    }

    override fun onQueryTextChange(newQuery: String?): Boolean {
        currentQuery = newQuery.orEmpty()
        mRowsAdapter.clear()
        viewModel.searchKeyword(newQuery.orEmpty())
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        currentQuery = query.orEmpty()
        mRowsAdapter.clear()
        viewModel.searchKeyword(query.orEmpty())
        return true
    }

    private fun listenVM() {
        viewModel.searchResults.observeEvent(viewLifecycleOwner) {
            val listRowAdapter = ArrayObjectAdapter(VideoCardPresenter())
            listRowAdapter.addAll(0, it)
            val headerItem = HeaderItem("Search Results for $currentQuery")
            mRowsAdapter.add(ListRow(headerItem, listRowAdapter))
        }

        viewModel.videoMedia.observeEvent(viewLifecycleOwner) {
            findNavController().navigate(
                VideoSearchFragmentDirections.actionVideoSearchFragmentToPlayerFragment(it)
            )
        }

        viewModel.episodeList.observeEvent(viewLifecycleOwner) {
            findNavController().navigate(
                VideoSearchFragmentDirections.actionVideoSearchFragmentToHomeFragment(it)
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
            imageLoadingJob?.cancel()
            imageLoadingJob = null
            backgroundUri = video.imageUrl

            if (backgroundUri.isEmpty()) {
                showDefaultBackground()
            } else {
                viewModel.viewModelScope.launch {
                    delay(HomeFragment.BACKGROUND_UPDATE_DELAY_MILLIS)
                    updateBackgroundImmediate()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
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
        backgroundManager.setThemeDrawableResourceId(HomeFragment.BACKGROUND_RESOURCE_ID)
    }
}