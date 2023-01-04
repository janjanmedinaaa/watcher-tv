package com.medina.juanantonio.watcher.shared.utils

import kotlinx.coroutines.CoroutineDispatcher

class CoroutineDispatchers(
    val main: CoroutineDispatcher,
    val io: CoroutineDispatcher,
    val default: CoroutineDispatcher
)
