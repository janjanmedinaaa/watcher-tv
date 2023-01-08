package com.medina.juanantonio.watcher.shared.extensions

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration

fun Duration.initPoll(): Flow<Unit> =
    flow {
        while (true) {
            emit(Unit)
            delay(this@initPoll)
        }
    }