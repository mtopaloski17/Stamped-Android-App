package com.example.stamped.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.stamped.databinding.ItemAchievementBinding
import com.example.stamped.util.Achievement

class AchievementAdapter(
    private val items: List<Achievement>,
    private val lang: String
) : RecyclerView.Adapter<AchievementAdapter.AchievementViewHolder>() {

    inner class AchievementViewHolder(
        private val binding: ItemAchievementBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(a: Achievement) {
            binding.tvEmoji.text = a.emoji
            binding.tvTitle.text = a.title(lang)
            binding.tvDescription.text = a.description(lang)
            val alpha = if (a.unlocked) 1.0f else 0.25f
            binding.tvEmoji.alpha = alpha
            binding.tvTitle.alpha = alpha
            binding.tvDescription.alpha = alpha
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val binding = ItemAchievementBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AchievementViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
