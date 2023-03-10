package com.medina.juanantonio.watcher.shared.utils

import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.LinearInterpolator

fun View.animateAlpha(toAlpha: Float, duration: Long = 250) {
    ObjectAnimator.ofFloat(this, "alpha", this.alpha, toAlpha).apply {
        this.duration = duration
        interpolator = LinearInterpolator()
    }.start()
}