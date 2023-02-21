package com.medina.juanantonio.watcher.features.settings.display

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.medina.juanantonio.watcher.data.adapters.SettingsAdapter
import com.medina.juanantonio.watcher.data.models.settings.SettingsScreen
import com.medina.juanantonio.watcher.data.models.settings.SettingsSelectionItem
import com.medina.juanantonio.watcher.databinding.FragmentSettingsDisplayBinding
import com.medina.juanantonio.watcher.shared.extensions.safeNavigate
import com.medina.juanantonio.watcher.shared.utils.autoCleared
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class SettingsDisplayFragment : Fragment() {

    private var binding: FragmentSettingsDisplayBinding by autoCleared()
    private val viewModel: SettingsDisplayViewModel by viewModels()
    private val settingsAdapter = SettingsAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsDisplayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewSettings.apply {
            adapter = settingsAdapter
            layoutManager = LinearLayoutManager(context)
        }

        lifecycleScope.launchWhenResumed {
            delay(50)
            binding.recyclerViewSettings.getChildAt(0)?.requestFocus()
        }

        setupSettingsScreen()
    }

    private fun setupSettingsScreen() {
        val key = SettingsDisplayFragmentArgs.fromBundle(requireArguments()).key
        val (screenTitle, settingsList) = viewModel.getSettingsList(key)

        binding.textViewTitle.text = screenTitle
        settingsAdapter.setSettingsItems(settingsList) {
            when (it) {
                is SettingsScreen -> {
                    findNavController().safeNavigate(
                        SettingsDisplayFragmentDirections.actionSettingsDisplayFragmentSelf(it.key)
                    )
                }
                is SettingsSelectionItem -> {
                    viewModel.selectedSelectionItem(it)
                    findNavController().popBackStack()
                }
            }
        }
    }
}