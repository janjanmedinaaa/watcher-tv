package com.medina.juanantonio.watcher.features.player.error

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.databinding.FragmentPlayerErrorBinding
import com.medina.juanantonio.watcher.shared.extensions.safeNavigate
import com.medina.juanantonio.watcher.shared.utils.autoCleared
import com.medina.juanantonio.watcher.shared.utils.observeEvent
import dagger.hilt.android.AndroidEntryPoint

/** Displays an error to the user when something unexpected occurred during playback. */
@AndroidEntryPoint
class PlayerErrorFragment : Fragment() {

    private lateinit var error: Exception

    private var binding: FragmentPlayerErrorBinding by autoCleared()
    private val viewModel: PlayerErrorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        error = PlayerErrorFragmentArgs.fromBundle(requireArguments()).error

        val onBackPressedCallback = object : OnBackPressedCallback(/* enabled = */ true) {
            override fun handleOnBackPressed() {
                // Upon selecting back, return the user to the browse fragment and clean up the
                // parent node in the nav graph, PlaybackFragment, from the back stack when popping.
                findNavController().popBackStack()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val contextThemeWrapper = ContextThemeWrapper(context, R.style.ErrorUiTheme)
        val themedInflater = inflater.cloneInContext(contextThemeWrapper)
        binding = FragmentPlayerErrorBinding.inflate(
            themedInflater,
            container,
            /* attachToParent = */ false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.run {
            title.text = getString(
                R.string.error_title_with_video,
                viewModel.videoMediaThatFailed?.title
            )
            // The entire stack trace is printed to logcat. We only need to show the cause's message
            // in the UI to give enough context for the error. Search logcat for "Playback error" to
            // view the full exception.
            message.text = error.cause?.message ?: error.message

            actionRetry.setOnClickListener {
                viewModel.getNewVideoMedia()
            }
            actionGoBack.setOnClickListener {
                findNavController().popBackStack()
            }
        }

        listenVM()
    }

    private fun listenVM() {
        viewModel.videoMedia.observeEvent(viewLifecycleOwner) {
            findNavController().safeNavigate(
                PlayerErrorFragmentDirections
                    .actionPlayerErrorFragmentToPlayerFragment()
            )
        }
    }
}