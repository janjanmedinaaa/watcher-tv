package com.medina.juanantonio.watcher.data.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.medina.juanantonio.watcher.databinding.ItemNavigationBinding
import com.medina.juanantonio.watcher.network.models.home.NavigationItemBean

class NavigationAdapter : RecyclerView.Adapter<NavigationAdapter.ViewHolder>() {
    private val _navigationItemList = arrayListOf<NavigationItemBean>()
    private var _onClickListener: (NavigationItemBean) -> Unit = {}
    private var _currentlySelected = -1

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val binding = ItemNavigationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount() = _navigationItemList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(_navigationItemList[position])
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setNavigationItems(
        navigationItemList: List<NavigationItemBean>,
        onClickListener: (NavigationItemBean) -> Unit = {}
    ) {
        _navigationItemList.clear()
        _navigationItemList.addAll(navigationItemList)
        _onClickListener = onClickListener
        notifyDataSetChanged()
    }

    inner class ViewHolder(
        private val binding: ItemNavigationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(bean: NavigationItemBean) {
            binding.textviewTitle.text = bean.name

            binding.root.setOnFocusChangeListener { _, onFocus ->
                binding.viewFocusBackground.isVisible = onFocus
            }

            binding.root.setOnClickListener {
                _onClickListener(bean)
                _currentlySelected = absoluteAdapterPosition
                notifyItemRangeChanged(0, _navigationItemList.size)
            }
        }
    }
}