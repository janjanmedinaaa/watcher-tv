package com.medina.juanantonio.watcher.features.splash

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.medina.juanantonio.watcher.MainViewModel
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.databinding.FragmentSplashBinding
import com.medina.juanantonio.watcher.features.dialog.DialogActivity
import com.medina.juanantonio.watcher.features.dialog.DialogFragment
import com.medina.juanantonio.watcher.shared.extensions.safeNavigate
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
        binding = FragmentSplashBinding.inflate(inflater, container, false)
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
                    viewModel.navigateToHomeScreen()
                }
            }

        }

        listenVM()
        listenActivityVM()
    }

    private fun listenVM() {
        viewModel.navigateToHomeScreen.observeEvent(viewLifecycleOwner) {
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
                    )
                )
            )
        }
    }

    private fun listenActivityVM() {
        mainViewModel.startDownload.observeEvent(viewLifecycleOwner) { permissionGranted ->
            if (permissionGranted) downloadLatestAPK()
            viewModel.navigateToHomeScreen()
        }
    }

    private fun downloadLatestAPK() {
        val downloadUrl = viewModel.assetToDownload?.downloadUrl ?: return
        val downloadController = DownloadController(requireContext(), downloadUrl)

        downloadController.enqueueDownload()
    }
}