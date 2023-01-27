package com.medina.juanantonio.watcher.features.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.medina.juanantonio.watcher.MainViewModel
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.databinding.FragmentSplashBinding
import com.medina.juanantonio.watcher.shared.extensions.hideKeyboard
import com.medina.juanantonio.watcher.shared.extensions.safeNavigate
import com.medina.juanantonio.watcher.shared.extensions.showKeyboard
import com.medina.juanantonio.watcher.shared.utils.autoCleared
import com.medina.juanantonio.watcher.shared.utils.observeEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashFragment : Fragment() {

    private val viewModel: SplashViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()
    private var binding: FragmentSplashBinding by autoCleared()

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

        listenViews()
        listenVM()
        checkForSearchResult()
    }

    private fun listenViews() {
        binding.editTextPhoneNumber.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                viewModel.requestOTP()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

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
                binding.root.hideKeyboard()
                viewModel.login()
                checkCacheVideos()
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

        viewModel.splashState.observeEvent(viewLifecycleOwner) {
            when (it) {
                SplashState.LOADING -> {
                    binding.spinKit.isVisible = true
                }
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

    private fun checkForSearchResult() {
        viewModel.hasPendingSearchResultToWatch =
            mainViewModel.searchResultToWatch.value?.peekConsumedContent() != null
    }

    private fun checkCacheVideos() {
        viewModel.viewModelScope.launch {
            if (viewModel.hasCacheVideos())
                mainViewModel.hasGuestModeCacheVideos()
        }
    }
}