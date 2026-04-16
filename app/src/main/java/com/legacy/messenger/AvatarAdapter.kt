package com.legacy.messenger

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.legacy.messenger.databinding.ItemAvatarBinding

class AvatarAdapter(
    private val avatars: List<String>,
    private val onSelected: (String) -> Unit
) : RecyclerView.Adapter<AvatarAdapter.ViewHolder>() {

    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAvatarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(avatars[position], position == selectedPosition)
    }

    override fun getItemCount() = avatars.size

    inner class ViewHolder(private val binding: ItemAvatarBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(avatar: String, isSelected: Boolean) {
            binding.tvAvatar.text = avatar
            if (isSelected) {
                binding.tvAvatar.setBackgroundResource(R.drawable.bg_avatar_selected)
            } else {
                binding.tvAvatar.setBackgroundResource(R.drawable.bg_avatar)
            }
            binding.root.setOnClickListener {
                selectedPosition = adapterPosition
                notifyDataSetChanged()
                onSelected(avatar)
            }
        }
    }
}