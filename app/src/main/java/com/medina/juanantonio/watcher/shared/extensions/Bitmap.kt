package com.medina.juanantonio.watcher.shared.extensions

import android.graphics.Bitmap
import android.graphics.Color

fun Bitmap.calculateBrightness(skipPixel: Int): Int {
    var R = 0
    var G = 0
    var B = 0
    val height = height
    val width = width
    var n = 0
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    var i = 0
    while (i < pixels.size) {
        val color = pixels[i]
        R += Color.red(color)
        G += Color.green(color)
        B += Color.blue(color)
        n++
        i += skipPixel
    }
    return (R + B + G) / (n * 3)
}