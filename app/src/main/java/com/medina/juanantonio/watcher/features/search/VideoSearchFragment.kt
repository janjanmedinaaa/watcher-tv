package com.medina.juanantonio.watcher.features.search

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.viewModels
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.*
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.data.models.Video
import com.medina.juanantonio.watcher.data.presenters.VideoCardPresenter
import com.medina.juanantonio.watcher.features.home.HomeFragment
import com.medina.juanantonio.watcher.network.models.home.HomePageBean
import com.medina.juanantonio.watcher.shared.extensions.safeNavigate
import com.medina.juanantonio.watcher.shared.utils.observeEvent
import dagger.hilt.android.AndroidEntryPoint
import jp.wasabeef.glide.transformations.BlurTransformation
import jp.wasabeef.glide.transformations.gpu.BrightnessFilterTransformation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VideoSearchFragment : SearchSupportFragment(), SearchSupportFragment.SearchResultProvider {

    private val viewModel: VideoSearchViewModel by viewModels()
    private lateinit var mRowsAdapter: ArrayObjectAdapter
    private lateinit var glide: RequestManager
    private var currentQuery = ""

    private lateinit var backgroundManager: BackgroundManager
    private var imageLoadingJob: Job? = null

    // The DisplayMetrics instance is used to get the screen dimensions
    private val displayMetrics = DisplayMetrics()

    private var backgroundUri = ""

    private val backgroundTarget = object : CustomTarget<Bitmap>() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mRowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        glide = Glide.with(requireContext())
        setSearchResultProvider(this)

        displayMetrics.setTo(resources.displayMetrics)

        backgroundManager = BackgroundManager.getInstance(requireActivity()).apply {
            if (!isAttached) {
                attach(requireActivity().window)
            }
            setThemeDrawableResourceId(HomeFragment.BACKGROUND_RESOURCE_ID)
        }

        setOnItemViewClickedListener { _, item, _, _ ->
            if (item !is Video) return@setOnItemViewClickedListener
            when (item.contentType) {
                HomePageBean.ContentType.MOVIE -> viewModel.getVideoMedia(item)
                HomePageBean.ContentType.DRAMA -> viewModel.handleSeries(item)
                else -> Unit
            }
        }

        setOnItemViewSelectedListener { _, item, _, _ ->
            if (item !is Video) return@setOnItemViewSelectedListener
            updateBackgroundDelayed(item)
        }

        badgeDrawable = ResourcesCompat.getDrawable(resources, R.mipmap.ic_launcher, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listenVM()
    }

    override fun onResume() {
        super.onResume()

        if (backgroundUri.isNotEmpty()) updateBackgroundImmediate(backgroundUri)
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
        return true
    }

    private fun listenVM() {
        viewModel.searchResults.observeEvent(viewLifecycleOwner) {
            val listRowAdapter = ArrayObjectAdapter(VideoCardPresenter(glide))
            listRowAdapter.addAll(0, it)
            val headerItem = HeaderItem("Search Results for $currentQuery")
            mRowsAdapter.add(ListRow(headerItem, listRowAdapter))
        }

        viewModel.videoMedia.observeEvent(viewLifecycleOwner) {
            showDefaultBackground()
            findNavController().safeNavigate(
                VideoSearchFragmentDirections.actionVideoSearchFragmentToPlayerFragment(it)
            )
        }

        viewModel.episodeList.observeEvent(viewLifecycleOwner) {
            findNavController().safeNavigate(
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
        cancelBackgroundImageLoading()
        backgroundUri = video.imageUrl

        imageLoadingJob = viewModel.viewModelScope.launch {
            delay(HomeFragment.BACKGROUND_UPDATE_DELAY_MILLIS)
            updateBackgroundImmediate(video.imageUrl)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelBackgroundImageLoading()
    }

    private fun updateBackgroundImmediate(backgroundUri: String) {
        if (activity == null) {
            // Triggered after fragment detached from activity, ignore
            return
        }

        val multi = MultiTransformation(
            BlurTransformation(5),
            BrightnessFilterTransformation(-0.2f)
        )

        glide
            .asBitmap()
            .load(backgroundUri)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .override(100, 133)
            .apply(RequestOptions.bitmapTransform(multi))
            .into(backgroundTarget)
    }

    private fun cancelBackgroundImageLoading() {
        glide.clear(backgroundTarget)
        imageLoadingJob?.cancel()
        imageLoadingJob = null
    }

    private fun showDefaultBackground() {
        cancelBackgroundImageLoading()
        backgroundManager.setThemeDrawableResourceId(HomeFragment.BACKGROUND_RESOURCE_ID)

        val drawable =
            ResourcesCompat.getDrawable(resources, HomeFragment.BACKGROUND_RESOURCE_ID, null)
        backgroundManager.setBitmap((drawable as? BitmapDrawable)?.bitmap)
    }
}