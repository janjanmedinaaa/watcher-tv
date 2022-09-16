package com.medina.juanantonio.watcher.features.player

import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.view.View
import androidx.fragment.app.viewModels
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.navigation.fragment.findNavController
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.medina.juanantonio.watcher.data.models.VideoMedia
import com.medina.juanantonio.watcher.shared.utils.observeEvent
import dagger.hilt.android.AndroidEntryPoint

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

    private lateinit var videoMedia: VideoMedia

    private var exoPlayer: ExoPlayer? = null
    private val viewModel: PlayerViewModel by viewModels()
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    private var currentProgress = 0L

    private val uiPlaybackStateListener = object : PlaybackStateListener {
        override fun onChanged(state: VideoPlaybackState) {
            // While a video is playing, the screen should stay on and the device should not go to
            // sleep. When in any other state such as if the user pauses the video, the app should
            // not prevent the device from going to sleep.
            view?.keepScreenOn = state is VideoPlaybackState.Play

            when (state) {
                is VideoPlaybackState.Prepare -> startPlaybackFromWatchProgress(state.startPosition)
                is VideoPlaybackState.End -> {
                    viewModel.handleVideoEnd()
                    // To get to playback, the user always goes through browse first. Deep links for
                    // directly playing a video also go to browse before playback. If playback
                    // finishes the entire video, the PlaybackFragment is popped off the back stack
                    // and the user returns to browse.
                    findNavController().popBackStack()
                }
                is VideoPlaybackState.Error -> {
                    findNavController().navigate(
                        PlayerFragmentDirections.actionPlayerFragmentToPlayerErrorFragment(
                            state.videoMedia,
                            state.exception
                        )
                    )
                }
                is VideoPlaybackState.Pause -> {
                    viewModel.saveVideo(currentProgress)
                }
                else -> {
                    // Do nothing.
                }
            }
        }
    }

    private fun startPlaybackFromWatchProgress(startPosition: Long) {
        exoPlayer?.apply {
            seekTo(startPosition)
            playWhenReady = true
        }
    }

    private val onProgressUpdate: (Long) -> Unit = { progress ->
        // TODO(benbaxter): Calculate when end credits are displaying and show the next episode for
        //  episodic content.
        currentProgress = progress
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        videoMedia = PlayerFragmentArgs.fromBundle(requireArguments()).videoMedia

        // Create the MediaSession that will be used throughout the lifecycle of this Fragment.
        createMediaSession()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.addPlaybackStateListener(uiPlaybackStateListener)
        listenVM()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.removePlaybackStateListener(uiPlaybackStateListener)
        viewModel.cleanUpPlayer()
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
        // Releasing the mediaSession due to inactive playback and setting token for cast to null.
        mediaSession.release()
    }

    private fun listenVM() {
        viewModel.savedProgress.observeEvent(viewLifecycleOwner) {
            viewModel.onStateChange(
                VideoPlaybackState.Prepare(
                    videoMedia,
                    it
                )
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
        val dataSourceFactory = DefaultDataSource.Factory(requireContext())
        val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(videoMedia.mediaUrl))
        exoPlayer = ExoPlayer.Builder(requireContext()).build().apply {
            setMediaSource(mediaSource)
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
        viewModel.getVideoDetails(videoMedia.contentId)
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
        ProgressTransportControlGlue(
            requireContext(),
            LeanbackPlayerAdapter(
                requireContext(),
                localExoplayer,
                PLAYER_UPDATE_INTERVAL_MILLIS
            ),
            onProgressUpdate
        ).apply {
            host = VideoSupportFragmentGlueHost(this@PlayerFragment)
            title = videoMedia.title
            // Enable seek manually since PlaybackTransportControlGlue.getSeekProvider() is null,
            // so that PlayerAdapter.seekTo(long) will be called during user seeking.
            // TODO(gargsahil@): Add a PlaybackSeekDataProvider to support video scrubbing.
            isSeekEnabled = true
        }
    }

    inner class PlayerEventListener : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            viewModel.onStateChange(VideoPlaybackState.Error(videoMedia, error))
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