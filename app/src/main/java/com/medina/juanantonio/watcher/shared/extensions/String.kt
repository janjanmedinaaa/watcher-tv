package com.medina.juanantonio.watcher.shared.extensions

import android.content.Context
import android.widget.Toast

fun String?.toastIfNotBlank(context: Context, length: Int = Toast.LENGTH_SHORT) {
    if (isNullOrBlank()) return
    Toast.makeText(context, this, length).show()
}