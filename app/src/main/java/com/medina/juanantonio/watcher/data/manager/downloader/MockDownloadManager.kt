package com.medina.juanantonio.watcher.data.manager.downloader

import com.medina.juanantonio.watcher.github.models.ReleaseBean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MockDownloadManager(
    private val coroutineScope: CoroutineScope
) : IDownloadManager {

    private val _progressStateFlow = MutableStateFlow<PollState>(PollState.Stopped)
    override val progressStateFlow: StateFlow<PollState>
        get() = _progressStateFlow

    private var job: Job? = null

    override fun enqueueDownload(asset: ReleaseBean.Asset) {
        job = coroutineScope.launch {
            (0..100 step 10).forEach {
                _progressStateFlow.emit(PollState.Ongoing(it))
                delay(100L)
            }
            _progressStateFlow.emit(PollState.Stopped)
        }

        job?.invokeOnCompletion {
            stopPoll()
        }
    }

    private fun stopPoll() {
        job?.cancel()
        job = null
    }
}