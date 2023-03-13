package com.medina.juanantonio.watcher.data.adapters

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.data.models.settings.SettingsItem
import com.medina.juanantonio.watcher.data.models.settings.SettingsNumberPickerItem
import com.medina.juanantonio.watcher.data.models.settings.SettingsScreen
import com.medina.juanantonio.watcher.data.models.settings.SettingsSelectionItem
import com.medina.juanantonio.watcher.databinding.ItemSettingsNumberPickerBinding
import com.medina.juanantonio.watcher.databinding.ItemSettingsScreenBinding
import com.medina.juanantonio.watcher.databinding.ItemSettingsSelectionBinding

class SettingsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val _settingsItemList = arrayListOf<SettingsItem>()
    private var _onClickListener: (SettingsItem) -> Unit = {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_settings_screen -> SettingsScreenViewHolder(
                ItemSettingsScreenBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            R.layout.item_settings_selection -> SettingsSelectionItemViewHolder(
                ItemSettingsSelectionBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            else -> SettingsNumberPickerItemViewHolder(
                ItemSettingsNumberPickerBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }

    override fun getItemCount() = _settingsItemList.size

    override fun getItemViewType(position: Int): Int {
        return _settingsItemList[position].viewType
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = _settingsItemList[position]) {
            is SettingsScreen -> (holder as? SettingsScreenViewHolder)?.bind(item)
            is SettingsSelectionItem -> (holder as? SettingsSelectionItemViewHolder)?.bind(item)
            is SettingsNumberPickerItem -> (holder as? SettingsNumberPickerItemViewHolder)?.bind(
                item
            )
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setSettingsItems(
        settingsItemList: List<SettingsItem>,
        onClickListener: (SettingsItem) -> Unit = {}
    ) {
        _settingsItemList.clear()
        _settingsItemList.addAll(settingsItemList)
        _onClickListener = onClickListener
        notifyDataSetChanged()
    }

    inner class SettingsScreenViewHolder(
        private val binding: ItemSettingsScreenBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SettingsScreen) {
            binding.root.setOnFocusChangeListener { _, onFocus ->
                binding.textViewTitle.setTextColor(if (onFocus) Color.BLACK else Color.WHITE)
                binding.imageViewIcon.setColorFilter(if (onFocus) Color.BLACK else Color.WHITE)
                binding.imageViewArrow.setColorFilter(if (onFocus) Color.BLACK else Color.WHITE)
            }

            binding.root.setOnClickListener {
                _onClickListener(item)
            }

            binding.imageViewIcon.setImageResource(item.icon)
            binding.textViewTitle.text = item.title
            binding.textViewDescription.apply {
                isVisible = !item.description.isNullOrBlank()
                text = item.description
            }
        }
    }

    inner class SettingsSelectionItemViewHolder(
        private val binding: ItemSettingsSelectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SettingsSelectionItem) {
            binding.root.setOnFocusChangeListener { _, onFocus ->
                binding.textViewTitle.setTextColor(if (onFocus) Color.BLACK else Color.WHITE)
                binding.imageViewIcon.setColorFilter(if (onFocus) Color.BLACK else Color.WHITE)
                binding.imageViewSelected.setColorFilter(if (onFocus) Color.BLACK else Color.WHITE)
            }

            binding.root.setOnClickListener {
                _onClickListener(item)
            }

            binding.textViewTitle.text = item.title
            binding.textViewDescription.apply {
                isVisible = !item.description.isNullOrBlank()
                text = item.description
            }
            binding.imageViewSelected.isVisible = item.isSelected
        }
    }

    inner class SettingsNumberPickerItemViewHolder(
        private val binding: ItemSettingsNumberPickerBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SettingsNumberPickerItem) {
            binding.root.setOnFocusChangeListener { _, onFocus ->
                binding.textViewTitle.setTextColor(if (onFocus) Color.BLACK else Color.WHITE)
                binding.imageViewIcon.setColorFilter(if (onFocus) Color.BLACK else Color.WHITE)
                binding.viewFocusBackground.isVisible = onFocus

                binding.numberPickerWhite.isInvisible = onFocus
                binding.numberPickerBlack.isInvisible = !onFocus
            }

            binding.root.setOnClickListener {
                binding.numberPickerBlack.requestFocus()
            }

            binding.numberPickerWhite.run {
                minValue = 0
                maxValue = 100
                value = item.value
            }

            binding.numberPickerBlack.run {
                minValue = 0
                maxValue = 100
                value = item.value

                setOnValueChangedListener { _, _, newValue ->
                    binding.numberPickerWhite.value = newValue
                    _onClickListener(item.copy(value = newValue))
                }

                setOnClickListener {
                    binding.root.requestFocus()
                }

                setOnFocusChangeListener { _, onFocus ->
                    binding.textViewTitle.setTextColor(if (onFocus) Color.BLACK else Color.WHITE)
                    binding.imageViewIcon.setColorFilter(if (onFocus) Color.BLACK else Color.WHITE)
                    binding.viewFocusBackground.isVisible = onFocus

                    binding.numberPickerWhite.isInvisible = onFocus
                    binding.numberPickerBlack.isInvisible = !onFocus

                    binding.viewNumberPickerBackground.isVisible = onFocus

                    if (onFocus) binding.numberPickerBlack.requestFocus()
                }
            }

            binding.imageViewIcon.setImageResource(item.icon)
            binding.textViewTitle.text = item.title
            binding.textViewDescription.apply {
                isVisible = !item.description.isNullOrBlank()
                text = item.description
            }
        }
    }
}