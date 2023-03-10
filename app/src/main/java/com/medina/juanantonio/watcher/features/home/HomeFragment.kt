package com.medina.juanantonio.watcher.features.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.Group
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.ListRow
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.medina.juanantonio.watcher.MainViewModel
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.data.adapters.ContentAdapter
import com.medina.juanantonio.watcher.data.adapters.NavigationAdapter
import com.medina.juanantonio.watcher.data.models.video.Video
import com.medina.juanantonio.watcher.data.models.video.ItemCategory
import com.medina.juanantonio.watcher.data.models.video.VideoGroup
import com.medina.juanantonio.watcher.features.dialog.DialogActivity
import com.medina.juanantonio.watcher.features.dialog.DialogFragment.Companion.ACTION_ID_NEGATIVE
import com.medina.juanantonio.watcher.features.dialog.DialogFragment.Companion.ACTION_ID_POSITIVE
import com.medina.juanantonio.watcher.network.models.auth.GetUserInfoResponse
import com.medina.juanantonio.watcher.network.models.player.GetVideoDetailsResponse
import com.medina.juanantonio.watcher.shared.Constants.VideoGroupTitle.ContinueWatchingTitle
import com.medina.juanantonio.watcher.shared.extensions.safeNavigate
import com.medina.juanantonio.watcher.shared.utils.observeEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : RowsSupportFragment() {

    private val viewModel: HomeViewModel by viewModels()
    private val activityViewModel: MainViewModel by activityViewModels()
    private lateinit var contentAdapter: ContentAdapter
    private lateinit var glide: RequestManager

    private lateinit var startForResultAutoPlay: ActivityResultLauncher<Intent>
    private lateinit var startForResultLogout: ActivityResultLauncher<Intent>
    private lateinit var startForResultSaveCacheVideos: ActivityResultLauncher<Intent>
    private lateinit var startForResultRemoveWatchHistory: ActivityResultLauncher<Intent>

    private var selectedVideoGroup: VideoGroup? = null

    private val navigationAdapter = NavigationAdapter()

    private var autoPlayFirstEpisode = false

    private var singleClickJob: Job? = null

    private var previewPlayer: PreviewPlayer? = null

    companion object {
        private const val CONTINUE_WATCHING_POSITION = 0
        private const val DOUBLE_CLICK_DELAY = 250L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        startForResultLogout = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            when ("${it?.data?.data}".toLongOrNull()) {
                ACTION_ID_POSITIVE -> viewModel.logout()
            }
        }

        startForResultSaveCacheVideos = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            when ("${it?.data?.data}".toLongOrNull()) {
                ACTION_ID_POSITIVE -> viewModel.saveCacheVideos()
                ACTION_ID_NEGATIVE -> viewModel.clearCacheVideos()
            }
        }

        startForResultRemoveWatchHistory = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            when ("${it?.data?.data}".toLongOrNull()) {
                ACTION_ID_POSITIVE -> viewModel.removeFromWatchHistory()
                ACTION_ID_NEGATIVE -> {
                    viewModel.videoToRemove = null
                }
            }
        }

        setOnItemViewClickedListener { _, item, _, _ ->
            if (item !is Video) return@setOnItemViewClickedListener

            // Handle Double Click
            if (singleClickJob?.isActive == true) {
                singleClickJob?.cancel()
                singleClickJob = null
                handleItemDoubleClick(item)

                return@setOnItemViewClickedListener
            }

            singleClickJob = lifecycleScope.launch {
                delay(DOUBLE_CLICK_DELAY)
                handleItemSingleClick(item)
            }
        }

        setOnItemViewSelectedListener { _, item, _, row ->
            if (item !is Video) return@setOnItemViewSelectedListener
            val isLastItem = contentAdapter.size() == contentAdapter.indexOf(row) + 1
            val isSelectedVideos = selectedVideoGroup != null

            if (isLastItem && !isSelectedVideos) viewModel.addNewContent()
            previewPlayer?.readyForNewPreview()
            viewModel.getVideoDetails(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = contentAdapter

        setupViews()
        listenVM()
        listenActivityVM()
    }

    override fun onResume() {
        super.onResume()

        autoPlayFirstEpisode =
            HomeFragmentArgs.fromBundle(requireArguments()).autoPlayFirstEpisode

        viewModel.setupVideoList(selectedVideoGroup, autoPlayFirstEpisode)
        viewModel.getUserInfo()

        previewPlayer = view?.findViewById(R.id.preview_player)
        previewPlayer?.setupPlayer()
    }

    private fun setupViews() {
        view?.findViewById<AppCompatImageView>(R.id.image_view_logo)?.apply {
            setOnClickListener {
                viewModel.handleLogoActions()
            }

            setOnFocusChangeListener { _, onFocus ->
                val logoBackground =
                    view?.findViewById<View>(R.id.view_logo_focus_background)
                logoBackground?.isVisible = onFocus
            }
        }

        view?.findViewById<AppCompatImageView>(R.id.image_view_update)?.apply {
            setOnClickListener {
                activityViewModel.askToUpdate()
            }

            setOnFocusChangeListener { _, onFocus ->
                val focusBackground =
                    view?.findViewById<CardView>(R.id.card_view_update_focus_background)
                focusBackground?.isInvisible = !onFocus
            }
        }

        view?.findViewById<AppCompatImageView>(R.id.image_view_search)?.apply {
            setOnClickListener {
                findNavController().safeNavigate(
                    HomeFragmentDirections.actionHomeFragmentToVideoSearchFragment()
                )
            }

            setOnFocusChangeListener { _, onFocus ->
                val focusBackground =
                    view?.findViewById<CardView>(R.id.card_view_search_focus_background)
                focusBackground?.isInvisible = !onFocus
            }
        }

        view?.findViewById<RecyclerView>(R.id.recycler_view_navigation)?.apply {
            adapter = navigationAdapter
            isVisible = selectedVideoGroup == null
            navigationAdapter.setNavigationItems(viewModel.navigationItems) {
                viewModel.handleNavigationItem(it.id)
            }
        }
    }

    private fun listenVM() {
        viewModel.contentList.observeEvent(viewLifecycleOwner) {
            contentAdapter.addContent(it)
        }

        viewModel.videoMedia.observeEvent(viewLifecycleOwner) {
            activityViewModel.setMovieBackground()
            findNavController().safeNavigate(
                HomeFragmentDirections.actionHomeFragmentToPlayerFragment()
            )
        }

        viewModel.episodeToAutoPlay.observeEvent(viewLifecycleOwner) {
            if (autoPlayFirstEpisode) {
                viewModel.getVideoMedia(it)
                return@observeEvent
            }

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
            if (isContinueWatchingDisplayed()) {
                if (onGoingVideoGroup.videoList.isEmpty())
                    contentAdapter.removeItems(CONTINUE_WATCHING_POSITION, 1)
                else
                    contentAdapter.addVideoGroup(
                        onGoingVideoGroup,
                        true,
                        position = CONTINUE_WATCHING_POSITION
                    )
            } else if (onGoingVideoGroup.videoList.isNotEmpty()) {
                contentAdapter.addVideoGroup(
                    onGoingVideoGroup,
                    false,
                    position = CONTINUE_WATCHING_POSITION
                )
            }
        }

        viewModel.removeNavigationContent.observeEvent(viewLifecycleOwner) {
            val itemCount = contentAdapter.size()
            if (isContinueWatchingDisplayed())
                contentAdapter.removeItems(CONTINUE_WATCHING_POSITION + 1, itemCount)
            else
                contentAdapter.removeItems(CONTINUE_WATCHING_POSITION, itemCount)
        }

        viewModel.videoDetails.observe(viewLifecycleOwner) { data ->
            setupVideoDetailsPreview(data)
        }

        viewModel.userDetails.observe(viewLifecycleOwner) { data ->
            setupUserDetailsPreview(data)
        }

        viewModel.navigateToHomeScreen.observeEvent(viewLifecycleOwner) {
            findNavController().safeNavigate(
                HomeFragmentDirections.actionHomeFragmentToSplashFragment()
            )
        }

        viewModel.showLogoutDialog.observeEvent(viewLifecycleOwner) {
            startForResultLogout.launch(
                DialogActivity.getIntent(
                    context = requireContext(),
                    title = getString(R.string.logout_title)
                )
            )
        }

        viewModel.videoMediaForPreview.observeEvent(viewLifecycleOwner) { (data, isTrailer) ->
            previewPlayer?.setVideoPreview(data.mediaUrl, data.totalDuration, isTrailer)
        }
    }

    private fun listenActivityVM() {
        activityViewModel.searchResultToWatch.observeEvent(viewLifecycleOwner) {
            viewModel.getVideoMediaFromId(it)
        }

        activityViewModel.hasGuestModeCacheVideos.observeEvent(viewLifecycleOwner) {
            startForResultSaveCacheVideos.launch(
                DialogActivity.getIntent(
                    context = requireContext(),
                    title = getString(R.string.save_local_video_title),
                    description = getString(R.string.save_local_video_description)
                )
            )
        }

        activityViewModel.hasUpdateRelease.observe(viewLifecycleOwner) {
            val imageViewUpdate =
                view?.findViewById<AppCompatImageView>(R.id.image_view_update)
            imageViewUpdate?.isVisible = it && selectedVideoGroup == null
        }
    }

    override fun onPause() {
        super.onPause()
        previewPlayer?.cleanUpPlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.isContentLoaded = false
        activityViewModel.cancelBackgroundImage()
    }

    private fun handleItemSingleClick(video: Video) {
        if (!video.onlineTime.isNullOrBlank()) {
            viewModel.getVideoMedia(video, isComingSoon = true)
            return
        }

        when (video.categoryType) {
            ItemCategory.ALBUM -> viewModel.getAlbumDetails(video)
            ItemCategory.MOVIE -> viewModel.getVideoMedia(video)
            ItemCategory.SERIES -> viewModel.handleSeries(video)
        }
    }

    private fun handleItemDoubleClick(video: Video) {
        viewModel.videoToRemove = video
        startForResultRemoveWatchHistory.launch(
            DialogActivity.getIntent(
                context = requireContext(),
                title = getString(R.string.remove_from_watch_history_title),
                description = getString(R.string.remove_from_watch_history_description, video.title)
            )
        )
    }

    private fun isContinueWatchingDisplayed(): Boolean {
        if (contentAdapter.size() <= 1) return false
        val continueWatchingRow = contentAdapter.get(CONTINUE_WATCHING_POSITION) as? ListRow
        return continueWatchingRow?.headerItem?.name == ContinueWatchingTitle
    }

    private fun setupVideoDetailsPreview(details: GetVideoDetailsResponse.Data) {
        val groupDetailsPreview =
            view?.findViewById<Group>(R.id.group_details_preview)
        val textViewPreviewTitle =
            view?.findViewById<AppCompatTextView>(R.id.text_view_preview_title)
        val textViewPreviewDescription =
            view?.findViewById<AppCompatTextView>(R.id.text_view_preview_description)
        val textViewPreviewScore =
            view?.findViewById<AppCompatTextView>(R.id.text_view_preview_score)
        val textViewPreviewYear =
            view?.findViewById<AppCompatTextView>(R.id.text_view_preview_year)
        val textViewPreviewTags =
            view?.findViewById<AppCompatTextView>(R.id.text_view_preview_tags)

        groupDetailsPreview?.isVisible = true

        textViewPreviewTitle?.text = details.name.trim()
        textViewPreviewDescription?.text = details.introduction.trim()
        textViewPreviewYear?.text = details.year.toString()
        textViewPreviewScore?.text = details.score.toString()
        textViewPreviewTags?.text = details.tagNameList.joinToString()

        textViewPreviewDescription?.maxLines =
            if ((textViewPreviewTitle?.lineCount ?: 1) > 1) 3 else 5

        previewPlayer?.setImagePreview(details.coverHorizontalUrl)
    }

    private fun setupUserDetailsPreview(details: GetUserInfoResponse.Data) {
        val imageViewIcon =
            view?.findViewById<AppCompatImageView>(R.id.image_view_logo) ?: return
        val textViewUserName =
            view?.findViewById<AppCompatTextView>(R.id.text_view_user_name)

        textViewUserName?.text = details.nickName.trim()

        glide.load(details.headImgUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .override(56 * 2, 56 * 2)
            .circleCrop()
            .error(R.mipmap.ic_launcher)
            .into(imageViewIcon)
    }
}

fun Fragment.cleanUpRows() {
    val groupDetailsPreview =
        view?.findViewById<Group>(R.id.group_details_preview)
    groupDetailsPreview?.isVisible = false
    val imageViewUpdate =
        view?.findViewById<AppCompatImageView>(R.id.image_view_update)
    imageViewUpdate?.isVisible = false

    val containerRoot =
        view?.findViewById<ConstraintLayout>(R.id.container_root)

    val constraintSet2 = ConstraintSet()
    constraintSet2.clone(containerRoot)
    constraintSet2.constrainHeight(R.id.container_list, 0)
    constraintSet2.applyTo(containerRoot)
}

fun Fragment.hideNavigationBar() {
    view?.findViewById<RecyclerView>(R.id.recycler_view_navigation)?.apply {
        isVisible = false
    }
}