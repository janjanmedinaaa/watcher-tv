package com.medina.juanantonio.watcher.data.manager.downloader

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.medina.juanantonio.watcher.BuildConfig
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.github.models.ReleaseBean
import com.medina.juanantonio.watcher.github.sources.UpdateRepository
import com.medina.juanantonio.watcher.shared.extensions.initPoll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

/**
 * https://androidwave.com/download-and-install-apk-programmatically/
 */
class DownloadManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) : IDownloadManager {

    companion object {
        private const val FILE_NAME = "latest-watcher-tv-update.apk"
        private const val FILE_BASE_PATH = "file://"
        private const val MIME_TYPE = "application/vnd.android.package-archive"
        private const val PROVIDER_PATH = ".provider"
        private const val APP_INSTALL_PATH = "\"application/vnd.android.package-archive\""
    }

    private val _progressStateFlow = MutableStateFlow<PollState>(PollState.Stopped)
    override val progressStateFlow: StateFlow<PollState>
        get() = _progressStateFlow

    private var job: Job? = null

    private lateinit var downloadManager: DownloadManager

    override fun enqueueDownload(asset: ReleaseBean.Asset) {
        var destination =
            "${Environment.getExternalStorageDirectory()}/${Environment.DIRECTORY_DOWNLOADS}/"
        destination += FILE_NAME

        val uri = Uri.parse("$FILE_BASE_PATH$destination")

        val file = File(destination)
        if (file.exists()) file.delete()

        downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadUri = Uri.parse(asset.downloadUrl)
        val request = DownloadManager.Request(downloadUri).apply {
            setMimeType(MIME_TYPE)

            // This is required since we're requesting from a private repository
            addRequestHeader(
                "Authorization",
                "token ${UpdateRepository.temporaryAccessToken}"
            )
            addRequestHeader("Accept", "application/octet-stream")

            setTitle(context.getString(R.string.title_file_download))
            setDescription(context.getString(R.string.downloading))

            // set destination
            setDestinationUri(uri)
        }

        showInstallOption(destination, uri)
        // Enqueue a new download and same the referenceId
        val downloadId = downloadManager.enqueue(request)
        startPoll(downloadId)
    }

    private fun showInstallOption(
        destination: String,
        uri: Uri
    ) {
        // set BroadcastReceiver to install app when .apk is downloaded
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val contentUri = FileProvider.getUriForFile(
                        context,
                        BuildConfig.APPLICATION_ID + PROVIDER_PATH,
                        File(destination)
                    )
                    val install = Intent(Intent.ACTION_VIEW)
                    install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    install.data = contentUri
                    context.startActivity(install)
                    context.unregisterReceiver(this)
                    // finish()
                } else {
                    val install = Intent(Intent.ACTION_VIEW)
                    install.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    install.setDataAndType(
                        uri,
                        APP_INSTALL_PATH
                    )
                    context.startActivity(install)
                    context.unregisterReceiver(this)
                    // finish()
                }
                stopPoll()
            }
        }
        context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    private fun startPoll(id: Long, delay: Long = 100L) {
        job = coroutineScope.launch {
            delay.milliseconds.initPoll()
                .onStart {
                    _progressStateFlow.emit(PollState.Ongoing(0))
                }
                .onCompletion {
                    _progressStateFlow.emit(PollState.Stopped)
                }
                .collect {
                    val query = DownloadManager.Query()
                    query.setFilterById(id)
                    val cursor = downloadManager.query(query)
                    cursor.moveToFirst()
                    val bytesDownloadedCursor =
                        cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalBytesCursor =
                        cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                    if (bytesDownloadedCursor > 0) {
                        val bytesDownloaded = cursor.getInt(bytesDownloadedCursor)
                        val totalBytes = cursor.getInt(totalBytesCursor)
                        cursor.close()

                        val progress = (bytesDownloaded.toFloat() / totalBytes.toFloat()) * 100F
                        _progressStateFlow.emit(PollState.Ongoing(progress.roundToInt()))
                    }
                }
        }
    }

    private fun stopPoll() {
        job?.cancel()
        job = null
    }
}

interface IDownloadManager {
    val progressStateFlow: StateFlow<PollState>
    val isDownloading: Boolean
        get() = progressStateFlow.value is PollState.Ongoing

    fun enqueueDownload(asset: ReleaseBean.Asset)
}

sealed class PollState {
    object Stopped : PollState()
    data class Ongoing(val progress: Int) : PollState()
}