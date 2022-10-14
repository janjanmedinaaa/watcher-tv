package com.medina.juanantonio.watcher.features.search

import android.os.Bundle
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.*
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.medina.juanantonio.watcher.MainViewModel
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.data.models.Video
import com.medina.juanantonio.watcher.data.presenters.VideoCardPresenter
import com.medina.juanantonio.watcher.shared.extensions.safeNavigate
import com.medina.juanantonio.watcher.shared.utils.observeEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VideoSearchFragment : SearchSupportFragment(), SearchSupportFragment.SearchResultProvider {

    private val viewModel: VideoSearchViewModel by viewModels()
    private val activityViewModel: MainViewModel by activityViewModels()
    private lateinit var mRowsAdapter: ArrayObjectAdapter
    private lateinit var glide: RequestManager
    private var currentQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mRowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        glide = Glide.with(requireContext())
        setSearchResultProvider(this)

        setOnItemViewClickedListener { _, item, _, _ ->
            if (item !is Video) return@setOnItemViewClickedListener
            when {
                item.enableDeveloperMode -> {
                    viewModel.enableDeveloperMode()
                    setSearchQuery("", false)
                }
                item.isMovie -> viewModel.getVideoMedia(item)
                else -> viewModel.handleSeries(item)
            }
        }

        setOnItemViewSelectedListener { _, item, _, _ ->
            if (item !is Video) return@setOnItemViewSelectedListener
            activityViewModel.setBackgroundImage(item.imageUrl)
        }

        badgeDrawable = ResourcesCompat.getDrawable(resources, R.mipmap.ic_launcher, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listenVM()
    }

    override fun onDestroy() {
        super.onDestroy()
        activityViewModel.cancelBackgroundImage()
    }

    override fun getResultsAdapter(): ObjectAdapter {
        return mRowsAdapter
    }

    override fun onQueryTextChange(newQuery: String?): Boolean {
        if (currentQuery == newQuery?.trim()) return true
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
            val headerTitle =
                if (currentQuery.isBlank()) getString(R.string.search_leaderboard)
                else getString(R.string.search_results, currentQuery)
            val headerItem = HeaderItem(headerTitle)
            mRowsAdapter.add(ListRow(headerItem, listRowAdapter))
        }

        viewModel.videoMedia.observeEvent(viewLifecycleOwner) {
            activityViewModel.setDefaultBackgroundImage()
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
}