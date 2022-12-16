package com.medina.juanantonio.watcher.features.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ListRow
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.medina.juanantonio.watcher.MainViewModel
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.data.adapters.ContentAdapter
import com.medina.juanantonio.watcher.data.models.Video
import com.medina.juanantonio.watcher.data.models.VideoGroup
import com.medina.juanantonio.watcher.features.dialog.DialogActivity
import com.medina.juanantonio.watcher.features.dialog.DialogFragment.Companion.ACTION_ID_POSITIVE
import com.medina.juanantonio.watcher.shared.extensions.safeNavigate
import com.medina.juanantonio.watcher.shared.utils.observeEvent
import dagger.hilt.android.AndroidEntryPoint

// TODO: Refactor to more generic screen name
@AndroidEntryPoint
class HomeFragment : BrowseSupportFragment() {

    private val viewModel: HomeViewModel by viewModels()
    private val activityViewModel: MainViewModel by activityViewModels()
    private lateinit var contentAdapter: ContentAdapter
    private lateinit var glide: RequestManager

    private lateinit var startForResultAutoPlay: ActivityResultLauncher<Intent>

    private var selectedVideoGroup: VideoGroup? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        headersState = HEADERS_DISABLED

        selectedVideoGroup = HomeFragmentArgs.fromBundle(requireArguments()).selectedVideoGroup
        glide = Glide.with(requireContext())
        contentAdapter = ContentAdapter(glide)

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
            if (item !is Video) return@setOnItemViewClickedListener
            when {
                item.isAlbum -> viewModel.getAlbumDetails(item)
                item.isMovie -> viewModel.getVideoMedia(item)
                else -> viewModel.handleSeries(item)
            }
        }

        setOnItemViewSelectedListener { _, item, _, row ->
            if (item !is Video) return@setOnItemViewSelectedListener
            val isLastItem = contentAdapter.size() == contentAdapter.indexOf(row) + 1
            val isSelectedVideos = selectedVideoGroup != null

            if (isLastItem && !isSelectedVideos) viewModel.addNewContent()
            if (!item.isAlbum) activityViewModel.setBackgroundImage(item.imageUrl)
        }

        setOnSearchClickedListener {
            findNavController().safeNavigate(
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

        viewModel.setupVideoList(selectedVideoGroup)
        activityViewModel.resetBackgroundImage()
    }

    private fun listenVM() {
        viewModel.contentList.observeEvent(viewLifecycleOwner) {
            contentAdapter.addContent(it)
        }

        viewModel.videoMedia.observeEvent(viewLifecycleOwner) {
            activityViewModel.setDefaultBackgroundImage()
            findNavController().safeNavigate(
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

        viewModel.selectedVideoGroup.observeEvent(viewLifecycleOwner) {
            findNavController().safeNavigate(
                HomeFragmentDirections.actionHomeFragmentSelf(it)
            )
        }

        viewModel.onGoingVideosList.observeEvent(viewLifecycleOwner) { onGoingVideoGroup ->
            if (contentAdapter.size() == 0) return@observeEvent
            val firstRow = contentAdapter.get(0) as? ListRow
            firstRow?.let { first ->
                if (first.headerItem?.name == getString(R.string.continue_watching)) {
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

    override fun onDestroy() {
        super.onDestroy()
        viewModel.contentLoaded = false
        activityViewModel.cancelBackgroundImage()
    }
}