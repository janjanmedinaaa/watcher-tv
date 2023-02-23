package com.medina.juanantonio.watcher.features.settings

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.*
import android.widget.FrameLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.databinding.FragmentSettingsModalBinding
import com.medina.juanantonio.watcher.shared.utils.autoCleared
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsModalFragment : DialogFragment() {

    companion object {
        private const val WIDTH_PERCENTAGE = 0.45F
    }

    private var binding: FragmentSettingsModalBinding by autoCleared()
    private val viewModel: SettingsModalViewModel by viewModels()
    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsModalBinding.inflate(inflater, container, false)
        isCancelable = false
        dialog?.window?.apply {
            setGravity(Gravity.END or Gravity.BOTTOM)
            decorView.apply {
                val (width, height) = getScreenSize()

                setBackgroundColor(Color.TRANSPARENT)
                minimumHeight = height
                minimumWidth = (width * WIDTH_PERCENTAGE).toInt()
                setPadding(0, 0, 0, 0)
            }
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setWindowAnimations(R.style.settings_modal_animation)
            val (width, _) = getScreenSize()

            setLayout(
                (width * WIDTH_PERCENTAGE).toInt(),
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navHostFragment =
            childFragmentManager.findFragmentById(R.id.nav_host_fragment_modal) as NavHostFragment
        navController = navHostFragment.navController
        navController.addOnDestinationChangedListener { controller, _, _ ->
            if (controller.backQueue.size == 2) {
                viewModel.setModalButton(SettingsModalButton.EXIT)
            } else {
                viewModel.setModalButton(SettingsModalButton.BACK)
            }
        }

        listenVM()
        listenViews()
    }

    private fun listenVM() {
        viewModel.settingsModalButton.observe(viewLifecycleOwner) {
            val drawable = when (it) {
                SettingsModalButton.EXIT -> R.drawable.ic_close
                else -> R.drawable.ic_arrow_back
            }

            binding.buttonModal.setImageResource(drawable)
        }
    }

    private fun listenViews() {
        binding.cardViewButton.setOnFocusChangeListener { _, onFocus ->
            binding.buttonModal.setColorFilter(if (onFocus) Color.BLACK else Color.WHITE)
        }

        binding.cardViewButton.setOnClickListener {
            when (viewModel.currentModalButton) {
                SettingsModalButton.EXIT -> findNavController().popBackStack()
                else -> navController.popBackStack()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun getScreenSize(): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = activity?.windowManager?.currentWindowMetrics
            val insets = windowMetrics?.windowInsets
                ?.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()) ?: return Pair(-1, -1)
            val bounds = windowMetrics.bounds
            val width = bounds.width() - insets.left - insets.right
            val height = bounds.height() - insets.left - insets.right
            Pair(width, height)
        } else {
            val displayMetrics = DisplayMetrics()
            activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
            Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
        }
    }
}