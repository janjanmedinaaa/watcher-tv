package com.medina.juanantonio.watcher

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.viewModels
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.BackgroundManager
import androidx.lifecycle.viewModelScope
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.medina.juanantonio.watcher.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import jp.wasabeef.glide.transformations.BlurTransformation
import jp.wasabeef.glide.transformations.gpu.BrightnessFilterTransformation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    companion object {
        private const val CLICK_INTERVAL = 200

        private const val BACKGROUND_UPDATE_DELAY_MILLIS = 300L
        private const val BACKGROUND_RESOURCE_ID = R.drawable.image_placeholder
    }

    private val viewModel: MainViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding

    private lateinit var glide: RequestManager
    private lateinit var backgroundManager: BackgroundManager
    private var imageLoadingJob: Job? = null

    private var lastClickTime = System.currentTimeMillis()

    private val backgroundTarget = object : CustomTarget<Bitmap>() {
        override fun onResourceReady(
            resource: Bitmap,
            transition: Transition<in Bitmap>?
        ) {
            val navController = binding.navHostFragment.findNavController()
            if (navController.currentDestination?.id != R.id.playerFragment)
                backgroundManager.setBitmap(resource)
        }

        override fun onLoadFailed(errorDrawable: Drawable?) = Unit

        override fun onLoadCleared(placeholder: Drawable?) = Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        glide = Glide.with(this)
        backgroundManager = BackgroundManager.getInstance(this).apply {
            if (!isAttached) {
                attach(window)
            }
            setThemeDrawableResourceId(BACKGROUND_RESOURCE_ID)
        }

        listenVM()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime < CLICK_INTERVAL) return true

        lastClickTime = currentTime
        return if (event.flags and KeyEvent.FLAG_LONG_PRESS == KeyEvent.FLAG_LONG_PRESS) {
            true
        } else {
            super.onKeyDown(keyCode, event)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelBackgroundImageLoading()
    }

    private fun listenVM() {
        viewModel.backgroundImageUrl.observe(this) {
            when (it) {
                null -> cancelBackgroundImageLoading()
                "" -> showDefaultBackground()
                else -> updateBackgroundDelayed(it)
            }
        }
    }

    /**
     * Updates the main fragment background after a delay
     *
     * This delay allows the user to quickly scroll through content without flashing a changing
     * background with every item that is passed.
     */
    private fun updateBackgroundDelayed(imageUrl: String) {
        cancelBackgroundImageLoading()

        imageLoadingJob = viewModel.viewModelScope.launch {
            delay(BACKGROUND_UPDATE_DELAY_MILLIS)
            updateBackgroundImmediate(imageUrl)
        }
    }

    private fun updateBackgroundImmediate(backgroundUri: String) {
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
        backgroundManager.setThemeDrawableResourceId(BACKGROUND_RESOURCE_ID)

        val drawable =
            ResourcesCompat.getDrawable(resources, BACKGROUND_RESOURCE_ID, null)
        backgroundManager.setBitmap((drawable as? BitmapDrawable)?.bitmap)
    }
}