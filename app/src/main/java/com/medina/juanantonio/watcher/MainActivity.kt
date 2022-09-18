package com.medina.juanantonio.watcher

import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.FragmentActivity
import com.medina.juanantonio.watcher.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    companion object {
        private const val CLICK_INTERVAL = 200
    }

    private lateinit var binding: ActivityMainBinding

    private var lastClickTime = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime < CLICK_INTERVAL) return true

        lastClickTime = currentTime
        return if (event.flags and KeyEvent.FLAG_LONG_PRESS == KeyEvent.FLAG_LONG_PRESS) {
            true
        } else {
            super.onKeyDown(keyCode, event)
        }
    }
}