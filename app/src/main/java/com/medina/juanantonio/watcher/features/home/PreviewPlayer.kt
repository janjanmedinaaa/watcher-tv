package com.medina.juanantonio.watcher.features.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ClippingMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.databinding.ViewPreviewPlayerBinding
import com.medina.juanantonio.watcher.shared.extensions.animateAlpha
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * This View handles the Image and Video Preview, it will play the
 * video clip only after the image preview is loaded.
 *
 * 1. Call setupPlayer() on start or on resume of the screen
 * 2. Call the setImagePreview() to set the Image Preview
 * 2.1 Call the setVideoPreview() if needed
 * 3. If you need to call the setImagePreview() again, call the readyForNewPreview()
 * so that it will display the current image preview with an overlay. This is needed
 * if the user suddenly clicks a new image while the video is playing,
 * show the image preview again.
 * 4. Call the cleanUpPlayer() on destroy
 */
class PreviewPlayer(
    context: Context,
    attributeSet: AttributeSet
) : FrameLayout(context, attributeSet) {
    private val binding = ViewPreviewPlayerBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    private var exoPlayer: ExoPlayer? = null
    private val glide = Glide.with(context)
    private var cachedPreviewBitmap: Bitmap? = null
    private var imagePreviewLoaded = false
    private var playVideoAfterImageLoadJob: Job? = null

    companion object {
        private const val START_POSITION_PERCENTAGE = 0.3
        private const val VIDEO_CLIP_LENGTH_SECONDS = 30
    }

    private val backgroundTarget = object : CustomTarget<Bitmap>() {
        override fun onResourceReady(
            resource: Bitmap,
            transition: Transition<in Bitmap>?
        ) {
            cachedPreviewBitmap = resource

            playVideoAfterImageLoadJob = MainScope().launch {
                binding.viewOverlay.animateAlpha(0f, 0)
                setBitmapToImageView(resource)
                delay(1500)

                imagePreviewLoaded = true
                if (exoPlayer?.playbackState == Player.STATE_READY) {
                    animateViews(showImageView = false)
                    exoPlayer?.play()
                }
            }
        }

        override fun onLoadFailed(errorDrawable: Drawable?) = Unit

        override fun onLoadCleared(placeholder: Drawable?) = Unit
    }

    fun setupPlayer() {
        exoPlayer = ExoPlayer.Builder(context).build()
        exoPlayer?.prepare()
        binding.playerView.player = exoPlayer
    }

    fun cleanUpPlayer() {
        exoPlayer?.pause()
        exoPlayer?.release()
        exoPlayer = null
    }

    fun readyForNewPreview() {
        // Clear all ongoing request, and prevent caching in case the request when through
        glide.clear(backgroundTarget)

        // Display the latest bitmap
        cachedPreviewBitmap?.let {
            binding.viewOverlay.animateAlpha(0.5f)
            setBitmapToImageView(it)
        }

        imagePreviewLoaded = false
        cachedPreviewBitmap = null
        playVideoAfterImageLoadJob?.cancel()

        exoPlayer?.clearMediaItems()
        exoPlayer?.pause()
    }

    fun setImagePreview(url: String) {
        glide.asBitmap()
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .error(R.drawable.drawable_image_error)
            .into(backgroundTarget)
    }

    fun setVideoPreview(url: String, durationInSeconds: Int, isTrailer: Boolean) {
        val dataSourceFactory = DefaultDataSource.Factory(context)
        val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.Builder().setUri(url).build())
        val mediaSourceToPlay =
            if (isTrailer) mediaSource
            else {
                val startPositionInSeconds = durationInSeconds * START_POSITION_PERCENTAGE
                val startPositionUs = (startPositionInSeconds * 1000000).toLong()
                val endPositionUs =
                    ((startPositionInSeconds + VIDEO_CLIP_LENGTH_SECONDS) * 1000000).toLong()

                ClippingMediaSource(mediaSource, startPositionUs, endPositionUs)
            }

        exoPlayer?.seekTo(0L)
        exoPlayer?.setMediaSource(mediaSourceToPlay)
        exoPlayer?.playWhenReady = false
        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        if (imagePreviewLoaded) {
                            animateViews(showImageView = false)
                            exoPlayer?.play()
                        }
                    }
                    Player.STATE_ENDED -> {
                        setBitmapToImageView(cachedPreviewBitmap)
                    }
                    else -> Unit
                }
            }
        })
    }

    private fun setBitmapToImageView(bitmap: Bitmap?) {
        if (bitmap == null) return
        animateViews(showImageView = true)
        glide.load(bitmap)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .transition(DrawableTransitionOptions.withCrossFade())
            .error(R.drawable.drawable_image_error)
            .into(binding.imageViewPosterPreview)
    }

    private fun animateViews(showImageView: Boolean) {
        val imageAlpha = if (showImageView) 1f else 0f
        val playerAlpha = if (showImageView) 0f else 1f
        binding.imageViewPosterPreview.animateAlpha(imageAlpha)
        binding.playerView.animateAlpha(playerAlpha)
    }
}