package com.medina.juanantonio.watcher.shared.utils

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
import java.io.File

/**
 * https://androidwave.com/download-and-install-apk-programmatically/
 */
class DownloadController(
    private val context: Context
) {

    companion object {
        private const val FILE_NAME = "latest-watcher-tv-update.apk"
        private const val FILE_BASE_PATH = "file://"
        private const val MIME_TYPE = "application/vnd.android.package-archive"
        private const val PROVIDER_PATH = ".provider"
        private const val APP_INSTALL_PATH = "\"application/vnd.android.package-archive\""
    }

    fun enqueueDownload(asset: ReleaseBean.Asset) {
        var destination =
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + "/"
        destination += FILE_NAME

        val uri = Uri.parse("$FILE_BASE_PATH$destination")

        val file = File(destination)
        if (file.exists()) file.delete()

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadUri = Uri.parse(asset.downloadUrl)
        val request = DownloadManager.Request(downloadUri).apply {
            setMimeType(MIME_TYPE)

            // This is required since we're requesting from a private repository
            addRequestHeader("Authorization", "token ${asset.apiKey}")
            addRequestHeader("Accept", "application/octet-stream")

            setTitle(context.getString(R.string.title_file_download))
            setDescription(context.getString(R.string.downloading))

            // set destination
            setDestinationUri(uri)
        }

        showInstallOption(destination, uri)
        // Enqueue a new download and same the referenceId
        downloadManager.enqueue(request)
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
            }
        }
        context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }
}