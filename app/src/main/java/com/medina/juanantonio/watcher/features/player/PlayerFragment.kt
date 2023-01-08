package com.medina.juanantonio.watcher.features.player

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.view.View
import androidx.annotation.Dimension
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.PlaybackControlsRow.ClosedCaptioningAction.INDEX_ON
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.SingleSampleMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.text.CueGroup
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.CaptionStyleCompat
import com.google.android.exoplayer2.ui.CaptionStyleCompat.EDGE_TYPE_RAISED
import com.google.android.exoplayer2.ui.SubtitleView
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.util.MimeTypes
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.data.models.video.Video
import com.medina.juanantonio.watcher.data.models.video.VideoMedia
import com.medina.juanantonio.watcher.data.presenters.VideoCardPresenter
import com.medina.juanantonio.watcher.features.home.cleanUpRows
import com.medina.juanantonio.watcher.features.home.hideNavigationBar
import com.medina.juanantonio.watcher.github.sources.IUpdateRepository
import com.medina.juanantonio.watcher.network.models.player.VideoSuggestion
import com.medina.juanantonio.watcher.shared.extensions.initPoll
import com.medina.juanantonio.watcher.shared.extensions.playbackSpeed
import com.medina.juanantonio.watcher.shared.extensions.safeNavigate
import com.medina.juanantonio.watcher.shared.utils.observeEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class PlayerFragment : VideoSupportFragment() {

    companion object {
        // Update the player UI fairly often. The frequency of updates affects several UI components
        // such as the smoothness of the progress bar and time stamp labels updating. This value can
        // be tweaked for better performance.
        private const val PLAYER_UPDATE_INTERVAL_MILLIS = 50

        // A short name to identify the media session when debugging.
        private const val MEDIA_SESSION_TAG = "MEDIA_SESSION_TAG"
    }

    val videoMedia: VideoMedia
        get() = viewModel.videoMedia

    private var exoPlayer: ExoPlayer? = null
    private val viewModel: PlayerViewModel by viewModels()
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector
    private lateinit var controlGlue: ProgressTransportControlGlue<LeanbackPlayerAdapter>
    private lateinit var mTrackSelector: DefaultTrackSelector
    private lateinit var subtitleView: SubtitleView

    private lateinit var classPresenterSelector: ClassPresenterSelector
    private lateinit var rowsAdapter: ArrayObjectAdapter
    private lateinit var glide: RequestManager

    private val uiPlaybackStateListener = object : PlaybackStateListener {
        override fun onChanged(state: VideoPlaybackState) {
            view?.keepScreenOn = state is VideoPlaybackState.Play

            when (state) {
                is VideoPlaybackState.Load -> {
                    controlGlue.incrementAutoPlayedVideoCount()
                    setupVideoMedia(state.videoMedia)
                }
                is VideoPlaybackState.Prepare -> {
                    startPlaybackFromWatchProgress(state.startPosition)
                    viewModel.saveVideo(state.startPosition)
                }
                is VideoPlaybackState.End -> viewModel.handleVideoEnd()
                is VideoPlaybackState.Error -> {
                    findNavController().safeNavigate(
                        PlayerFragmentDirections.actionPlayerFragmentToPlayerErrorFragment(
                            state.videoMedia,
                            state.exception
                        )
                    )
                }
                is VideoPlaybackState.Pause -> {
                    viewModel.saveVideo(controlGlue.currentPosition)
                }
                else -> Unit
            }
        }
    }

    private fun startPlaybackFromWatchProgress(startPosition: Long) {
        exoPlayer?.apply {
            seekTo(startPosition)
            playWhenReady = controlGlue.autoPlayVideos
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.videoMedia = PlayerFragmentArgs.fromBundle(requireArguments()).videoMedia
        glide = Glide.with(requireContext())

        classPresenterSelector = ClassPresenterSelector()
        // This is required when adding the Related Videos section
        classPresenterSelector.addClassPresenter(ListRow::class.java, ListRowPresenter())
        rowsAdapter = ArrayObjectAdapter(classPresenterSelector)
        adapter = rowsAdapter

        createMediaSession()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subtitleView = view.findViewById<SubtitleView>(R.id.leanback_subtitles).apply {
            val style = CaptionStyleCompat(
                Color.WHITE,
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                EDGE_TYPE_RAISED,
                Color.BLACK,
                null
            )
            setStyle(style)
            setFixedTextSize(Dimension.DP, 75F)
            updatePadding(left = 300, right = 300)
        }

        setOnItemViewClickedListener { _, item, _, _ ->
            if (item !is Video) return@setOnItemViewClickedListener
            if (item.isMovie) viewModel.getVideoMedia(item)
            else viewModel.getEpisodeList(item)
        }

        setOnItemViewSelectedListener { _, item, _, _ ->
            if (item == null) return@setOnItemViewSelectedListener
            subtitleView.isVisible =
                if (item is Video) false
                else controlGlue.closedCaptioningAction.index == INDEX_ON
        }

        lifecycleScope.launch {
            5000.milliseconds.initPoll()
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collect {
                    if (controlGlue.isPlaying)
                        viewModel.saveVideo(controlGlue.currentPosition)
                }
        }

        viewModel.addPlaybackStateListener(uiPlaybackStateListener)
        listenVM()
    }

    override fun onResume() {
        super.onResume()
        cleanUpRows()
        hideNavigationBar()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.removePlaybackStateListener(uiPlaybackStateListener)
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onStop() {
        super.onStop()
        destroyPlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
    }

    private fun listenVM() {
        viewModel.savedProgress.observeEvent(viewLifecycleOwner) {
            viewModel.onStateChange(
                VideoPlaybackState.Prepare(videoMedia, it)
            )
        }

        viewModel.handleVideoEndNavigation.observeEvent(viewLifecycleOwner) {
            if (!videoMedia.connectedVideos.isNullOrEmpty() ||
                !videoMedia.videoSuggestions.isNullOrEmpty()
            ) {
                showConnectedVideos()
            }
        }

        viewModel.episodeList.observeEvent(viewLifecycleOwner) {
            findNavController().safeNavigate(
                PlayerFragmentDirections.actionPlayerFragmentToHomeFragment(it)
            )
        }
    }

    private fun createMediaSession() {
        mediaSession = MediaSessionCompat(requireContext(), MEDIA_SESSION_TAG)

        mediaSessionConnector = MediaSessionConnector(mediaSession).apply {
            setQueueNavigator(SingleVideoQueueNavigator(videoMedia, mediaSession))
        }
    }

    private fun initializePlayer() {
        mTrackSelector = DefaultTrackSelector(requireContext())
        exoPlayer =
            ExoPlayer.Builder(requireContext()).setTrackSelector(mTrackSelector).build().apply {
                prepare()
                addListener(PlayerEventListener())
                prepareGlue(this)
                mediaSessionConnector.setPlayer(object : ForwardingPlayer(this) {
                    override fun stop() {
                        // Treat stop commands as pause, this keeps ExoPlayer, MediaSession, etc.
                        // in memory to allow for quickly resuming. This also maintains the playback
                        // position so that the user will resume from the current position when backing
                        // out and returning to this video
                        // This both prevents playback from starting automatically and pauses it if
                        // it's already playing
                        playWhenReady = false
                    }
                })
                mediaSession.isActive = true
            }
        viewModel.onStateChange(VideoPlaybackState.Load(videoMedia))
    }

    private fun destroyPlayer() {
        mediaSession.isActive = false
        mediaSessionConnector.setPlayer(null)
        exoPlayer?.let {
            // Pause the player to notify listeners before it is released.
            it.pause()
            it.release()
            exoPlayer = null
        }
    }

    private fun prepareGlue(localExoplayer: ExoPlayer) {
        controlGlue = ProgressTransportControlGlue(
            requireContext(),
            LeanbackPlayerAdapter(
                requireContext(),
                localExoplayer,
                PLAYER_UPDATE_INTERVAL_MILLIS
            )
        ).apply {
            host = VideoSupportFragmentGlueHost(this@PlayerFragment)
            // Enable seek manually since PlaybackTransportControlGlue.getSeekProvider() is null,
            // so that PlayerAdapter.seekTo(long) will be called during user seeking.
            isSeekEnabled = true

            setOnActionListener { action ->
                when (action) {
                    skipNextAction -> endVideo()
                    skipPreviousAction -> {
                        if (viewModel.isFirstEpisode || !justStarted) {
                            exoPlayer!!.seekTo(0L)
                        } else {
                            viewModel.handleSkipPrevious()
                        }
                    }
                    increaseSpeedAction -> {
                        increaseSpeedAction.nextIndex()
                        exoPlayer!!.let { p ->
                            if (p.playbackSpeed < 2.0) {
                                p.playbackSpeed += .25f
                            } else {
                                p.playbackSpeed = 1.0f
                            }
                        }
                    }
                    closedCaptioningAction -> {
                        closedCaptioningAction.nextIndex()
                        subtitleView.isVisible = closedCaptioningAction.index == INDEX_ON
                    }
                    bedtimeModeAction -> {
                        bedtimeModeAction.nextIndex()
                        enableBedtimeMode(bedtimeModeAction.index == INDEX_ON)
                    }
                }
            }

            // Set Actions that are On by default
            onActionClicked(closedCaptioningAction)
            onActionClicked(bedtimeModeAction)
        }
    }

    private fun setupVideoMedia(videoMedia: VideoMedia) {
        // Updating the fragment's videoMedia is required
        // in playing the previous/next episode's or movie's
        // connected and related videos
        viewModel.videoMedia = videoMedia
        viewModel.setEpisodeNumbers(videoMedia.episodeNumbers)

        val dataSourceFactory = DefaultDataSource.Factory(requireContext())
        val subtitleData = videoMedia.getPreferredSubtitle()
        val subtitleUri = Uri.parse(subtitleData?.subtitlingUrl ?: "")
        val subtitleMediaItem = MediaItem.SubtitleConfiguration.Builder(subtitleUri)
            .setMimeType(MimeTypes.APPLICATION_SUBRIP)
            .setLanguage(subtitleData?.languageAbbr)
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()
        val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(
                MediaItem.Builder().setUri(videoMedia.mediaUrl)
                    .setSubtitleConfigurations(listOf(subtitleMediaItem)).build()
            )

        val subtitleSource =
            SingleSampleMediaSource
                .Factory(dataSourceFactory)
                .createMediaSource(subtitleMediaItem, C.TIME_UNSET)
        val mergedSource = MergingMediaSource(mediaSource, subtitleSource)

        exoPlayer?.seekTo(0L)
        exoPlayer?.setMediaSource(mergedSource)
        controlGlue.run {
            title = if (IUpdateRepository.isDeveloperMode) {
                getString(
                    R.string.dev_mode_player_title,
                    videoMedia.title,
                    videoMedia.contentId,
                    videoMedia.id
                )
            } else videoMedia.title
            subtitle = videoMedia.introduction
        }

        viewModel.getVideoDetails(videoMedia.contentId)
        setupRelatedVideos()
    }

    private fun setupRelatedVideos() {
        val connectedVideoListRow =
            getListRow(getString(R.string.connected_videos), videoMedia.connectedVideos)
        val relatedVideosListRow =
            getListRow(getString(R.string.related_videos), videoMedia.videoSuggestions)

        rowsAdapter.removeItems(1, 2)
        if (!videoMedia.connectedVideos.isNullOrEmpty()) {
            rowsAdapter.add(connectedVideoListRow)
        }

        if (!videoMedia.videoSuggestions.isNullOrEmpty()) {
            rowsAdapter.add(relatedVideosListRow)
        }
    }

    private fun getListRow(title: String, videos: List<VideoSuggestion>?): ListRow {
        val listRowAdapter = ArrayObjectAdapter(VideoCardPresenter(glide))
        listRowAdapter.addAll(0, videos?.map { Video(it) })
        val headerItem = HeaderItem(title)
        return ListRow(headerItem, listRowAdapter)
    }

    private fun showConnectedVideos() {
        val listItemAdapter = (rowsAdapter.get(1) as? ListRow)?.adapter
        val listItem = listItemAdapter?.get(0)
        val listPresenter = listItemAdapter?.getPresenter(listItem) as? VideoCardPresenter
        listPresenter?.viewHolder?.view?.requestFocus()
    }

    inner class PlayerEventListener : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            viewModel.onStateChange(VideoPlaybackState.Error(videoMedia, error))
        }

        override fun onCues(cueGroup: CueGroup) {
            super.onCues(cueGroup)
            subtitleView.setCues(cueGroup.cues)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            when {
                isPlaying -> viewModel.onStateChange(
                    VideoPlaybackState.Play(videoMedia)
                )
                exoPlayer!!.playbackState == Player.STATE_ENDED -> viewModel.onStateChange(
                    VideoPlaybackState.End(videoMedia)
                )
                else -> viewModel.onStateChange(
                    VideoPlaybackState.Pause(videoMedia, exoPlayer!!.currentPosition)
                )
            }
        }
    }
}