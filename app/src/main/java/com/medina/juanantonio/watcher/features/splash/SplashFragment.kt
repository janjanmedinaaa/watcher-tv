package com.medina.juanantonio.watcher.features.splash

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.medina.juanantonio.watcher.BuildConfig
import com.medina.juanantonio.watcher.MainViewModel
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.databinding.FragmentSplashBinding
import com.medina.juanantonio.watcher.features.dialog.DialogActivity
import com.medina.juanantonio.watcher.features.dialog.DialogFragment
import com.medina.juanantonio.watcher.shared.extensions.hideKeyboard
import com.medina.juanantonio.watcher.shared.extensions.safeNavigate
import com.medina.juanantonio.watcher.shared.extensions.showKeyboard
import com.medina.juanantonio.watcher.shared.utils.DownloadController
import com.medina.juanantonio.watcher.shared.utils.autoCleared
import com.medina.juanantonio.watcher.shared.utils.observeEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashFragment : Fragment() {

    private val viewModel: SplashViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()
    private var binding: FragmentSplashBinding by autoCleared()

    private lateinit var startForResultUpdate: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_splash,
            container,
            false
        )
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        startForResultUpdate = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            when ("${it?.data?.data}".toLongOrNull()) {
                DialogFragment.ACTION_ID_POSITIVE -> {
                    mainViewModel.requestPermission()
                }
                DialogFragment.ACTION_ID_NEGATIVE -> {
                    viewModel.saveLastUpdateReminder()
                }
            }
        }

        listenViews()
        listenVM()
        listenActivityVM()
    }

    private fun listenViews() {
        binding.editTextPhoneNumber.setOnFocusChangeListener { view, onFocus ->
            if (onFocus && !viewModel.preventKeyboardPopup) view.showKeyboard()
            else view.hideKeyboard()
        }

        binding.editTextCode.setOnFocusChangeListener { view, onFocus ->
            if (onFocus && !viewModel.preventKeyboardPopup) view.showKeyboard()
            else view.hideKeyboard()
        }
    }

    private fun listenVM() {
        viewModel.otpCode.observe(viewLifecycleOwner) {
            if (!it.isNullOrBlank() && it.length == 6) {
                binding.editTextCode.clearFocus()
                viewModel.login()
            } else if (it.isNullOrBlank()) {
                binding.editTextCode.requestFocus()
            }
        }

        viewModel.navigateToHomeScreen.observeEvent(viewLifecycleOwner) {
            binding.root.hideKeyboard()
            findNavController().safeNavigate(
                SplashFragmentDirections.actionSplashFragmentToHomeFragment()
            )
        }

        viewModel.newerRelease.observeEvent(viewLifecycleOwner) {
            startForResultUpdate.launch(
                DialogActivity.getIntent(
                    context = requireContext(),
                    title = getString(R.string.update_available_title),
                    description = getString(
                        R.string.update_available_description,
                        it.name
                    ),
                    positiveButton = getString(R.string.update_button),
                    negativeButton = getString(R.string.no_thanks_button)
                )
            )
        }

        viewModel.splashState.observeEvent(viewLifecycleOwner) {
            when (it) {
                SplashState.INPUT_PHONE_NUMBER -> {
                    binding.motionLayout.run {
                        setTransition(R.id.transition_show_phone_number)
                        transitionToEnd {
                            binding.editTextPhoneNumber.requestFocus()
                        }
                    }
                }
                SplashState.INPUT_CODE -> {
                    binding.editTextPhoneNumber.isEnabled = false
                    binding.buttonSendOtp.isEnabled = false
                    binding.motionLayout.run {
                        setTransition(R.id.transition_show_code)
                        transitionToEnd {
                            binding.editTextCode.requestFocus()
                        }
                    }
                }
            }
        }
    }

    private fun listenActivityVM() {
        mainViewModel.startDownload.observeEvent(viewLifecycleOwner) { permissionGranted ->
            if (permissionGranted) downloadLatestAPK()
            viewModel.checkAuthentication()
        }
    }

    private fun downloadLatestAPK() {
        if (BuildConfig.DEBUG) return

        val asset = viewModel.assetToDownload ?: return
        val downloadController = DownloadController(requireContext())

        downloadController.enqueueDownload(asset)
    }
}