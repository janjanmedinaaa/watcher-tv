package com.medina.juanantonio.watcher.shared.extensions

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager

fun View.hideKeyboard() {
    val inputMethodManager =
        context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
}

fun View.showKeyboard() {
    val inputMethodManager =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun View.animateAlpha(toAlpha: Float, duration: Long = 250) {
    ObjectAnimator.ofFloat(this, "alpha", this.alpha, toAlpha).apply {
        this.duration = duration
        interpolator = LinearInterpolator()
    }.start()
}