package com.medina.juanantonio.watcher.features.search

import android.os.Bundle
import android.view.KeyEvent.KEYCODE_DPAD_UP
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
import com.medina.juanantonio.watcher.data.adapters.ContentAdapter
import com.medina.juanantonio.watcher.data.models.video.Video
import com.medina.juanantonio.watcher.features.home.cleanUpRows
import com.medina.juanantonio.watcher.features.home.hideNavigationBar
import com.medina.juanantonio.watcher.shared.extensions.safeNavigate
import com.medina.juanantonio.watcher.shared.utils.observeEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VideoSearchFragment : SearchSupportFragment(), SearchSupportFragment.SearchResultProvider {

    private val viewModel: VideoSearchViewModel by viewModels()
    private val activityViewModel: MainViewModel by activityViewModels()
    private lateinit var contentAdapter: ContentAdapter
    private lateinit var glide: RequestManager
    private var currentQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        glide = Glide.with(requireContext())
        contentAdapter = ContentAdapter(glide)
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

        badgeDrawable = ResourcesCompat.getDrawable(resources, R.mipmap.ic_launcher, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        title = viewModel.searchResultHint
        listenVM()
    }

    override fun onResume() {
        super.onResume()
        cleanUpRows()
        hideNavigationBar()
    }

    override fun onDestroy() {
        super.onDestroy()
        activityViewModel.cancelBackgroundImage()
    }

    override fun getResultsAdapter(): ObjectAdapter {
        return contentAdapter
    }

    override fun onQueryTextChange(newQuery: String?): Boolean {
        if (currentQuery == newQuery?.trim()) return true
        currentQuery = newQuery.orEmpty()
        contentAdapter.clear()
        viewModel.searchKeyword(newQuery.orEmpty())
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return true
    }

    private fun listenVM() {
        viewModel.searchResults.observeEvent(viewLifecycleOwner) {
            contentAdapter.addContent(it)
        }

        viewModel.videoMedia.observeEvent(viewLifecycleOwner) {
            activityViewModel.setMovieBackground()
            findNavController().safeNavigate(
                VideoSearchFragmentDirections.actionVideoSearchFragmentToPlayerFragment()
            )
        }

        viewModel.episodeList.observeEvent(viewLifecycleOwner) {
            findNavController().safeNavigate(
                VideoSearchFragmentDirections.actionVideoSearchFragmentToHomeFragment(it)
            )
        }

        activityViewModel.onKeyDown.observeEvent(viewLifecycleOwner) {
            if (it != KEYCODE_DPAD_UP) return@observeEvent
            if (rowsSupportFragment.selectedPosition == 0) focusSearchEditText()
        }
    }

    private fun focusSearchEditText() {
        val rootView = view ?: return

        val searchFrame = rootView.findViewById<BrowseFrameLayout>(R.id.lb_search_frame)
        val searchBar = searchFrame.findViewById<SearchBar>(R.id.lb_search_bar)
        val searchEditText = searchBar.findViewById<SearchEditText>(R.id.lb_search_text_editor)

        if (!searchEditText.hasFocus()) searchEditText.requestFocus()
    }
}