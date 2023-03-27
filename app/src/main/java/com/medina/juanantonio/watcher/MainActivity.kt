package com.medina.juanantonio.watcher

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.BackgroundManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.fondesa.kpermissions.allGranted
import com.fondesa.kpermissions.extension.permissionsBuilder
import com.fondesa.kpermissions.extension.send
import com.medina.juanantonio.watcher.databinding.ActivityMainBinding
import com.medina.juanantonio.watcher.features.dialog.DialogFragment
import com.medina.juanantonio.watcher.features.loader.LoaderUseCase
import com.medina.juanantonio.watcher.data.manager.downloader.IDownloadManager
import com.medina.juanantonio.watcher.data.manager.downloader.PollState
import com.medina.juanantonio.watcher.shared.extensions.initPoll
import com.medina.juanantonio.watcher.shared.extensions.toastIfNotBlank
import com.medina.juanantonio.watcher.shared.utils.observeEvent
import com.medina.juanantonio.watcher.sources.content.TVProviderUseCase.Companion.PROGRAM_INFO_EXTRA
import dagger.hilt.android.AndroidEntryPoint
import jp.wasabeef.glide.transformations.BlurTransformation
import jp.wasabeef.glide.transformations.gpu.BrightnessFilterTransformation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    companion object {
        private const val CLICK_INTERVAL = 200

        private const val BACKGROUND_UPDATE_DELAY_MILLIS = 300L
        private const val BACKGROUND_RESOURCE_ID = R.drawable.image_placeholder
        private const val MOVIE_BACKGROUND_RESOURCE_ID = R.drawable.movie_black_background
        const val SHOW_MOVIE_BACKGROUND = "SHOW_MOVIE_BACKGROUND"
    }

    @Inject
    lateinit var loaderUseCase: LoaderUseCase

    @Inject
    lateinit var downloadManager: IDownloadManager

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

    private lateinit var startForResultUpdate: ActivityResultLauncher<Intent>

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

        startForResultUpdate = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            when ("${it?.data?.data}".toLongOrNull()) {
                DialogFragment.ACTION_ID_POSITIVE -> {
                    viewModel.requestPermission()
                }
                DialogFragment.ACTION_ID_NEGATIVE -> {
                    viewModel.saveLastUpdateReminder()
                }
            }
        }

        lifecycleScope.launch {
            downloadManager.progressStateFlow
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collect {
                    binding.downloadProgressBar.isVisible = it != PollState.Stopped
                    if (it is PollState.Ongoing)
                        binding.downloadProgressBar.progress = it.progress
                }
        }

        lifecycleScope.launch {
            60.seconds.initPoll()
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collect {
                    if (!downloadManager.isDownloading)
                        viewModel.checkForUpdates()
                }
        }

        handleIntent()
        listenVM()
        setupLoading()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime < CLICK_INTERVAL) return true

        lastClickTime = currentTime
        return if (event.flags and KeyEvent.FLAG_LONG_PRESS == KeyEvent.FLAG_LONG_PRESS) {
            true
        } else {
            viewModel.setKeyDown(keyCode)
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
                null -> showDefaultBackground()
                SHOW_MOVIE_BACKGROUND -> showMovieBackground()
                else -> updateBackgroundDelayed(it)
            }
        }

        viewModel.askToUpdate.observeEvent(this) {
            try {
                val downloadUrl = viewModel.assetToDownload?.downloadUrl ?: return@observeEvent
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                startActivity(browserIntent)
            } catch (e: ActivityNotFoundException) {
                getString(R.string.install_browser_for_update).toastIfNotBlank(this)
            }
        }

        viewModel.requestPermissions.observeEvent(this) {
            permissionsBuilder(WRITE_EXTERNAL_STORAGE).build().send {
                viewModel.startDownload(it.allGranted())
            }
        }

        viewModel.startDownload.observeEvent(this) { permissionGranted ->
            if (permissionGranted) downloadLatestAPK()
        }
    }

    /**
     * Updates the activity background after a delay
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

    private fun showMovieBackground() {
        cancelBackgroundImageLoading()
        backgroundManager.setThemeDrawableResourceId(MOVIE_BACKGROUND_RESOURCE_ID)

        val drawable =
            ResourcesCompat.getDrawable(resources, MOVIE_BACKGROUND_RESOURCE_ID, null)
        backgroundManager.setBitmap((drawable as? BitmapDrawable)?.bitmap)
    }

    private fun setupLoading() {
        loaderUseCase.loadingStatus.observe(this) {
            binding.spinKit.isVisible = it
        }
    }

    private fun handleIntent() {
        if (intent.action == Intent.ACTION_VIEW || intent.action == Intent.ACTION_SEARCH) {
            val uri = intent.data ?: Uri.EMPTY
            when (uri.pathSegments.firstOrNull()) {
                "program" -> {
                    val contentId = uri.pathSegments.lastOrNull().toString()
                    viewModel.readySearchResultToWatch(contentId)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val programInfoExtra = intent.getStringExtra(PROGRAM_INFO_EXTRA)
                if (!programInfoExtra.isNullOrBlank()) {
                    viewModel.readySearchResultToWatch("$programInfoExtra")
                }
            }
        }
    }

    private fun downloadLatestAPK() {
        val asset = viewModel.assetToDownload ?: return
        downloadManager.enqueueDownload(asset)
        viewModel.clearUpdateRelease()
    }
}