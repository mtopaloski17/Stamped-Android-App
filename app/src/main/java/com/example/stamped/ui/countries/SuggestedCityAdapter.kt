package com.example.stamped.ui.countries

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stamped.databinding.ItemSuggestedCityBinding

class SuggestedCityAdapter(
    private var capitalName: String? = null,
    private val onClick: (String) -> Unit
) : ListAdapter<String, SuggestedCityAdapter.SuggestedCityViewHolder>(DiffCallback()) {

    fun setCapital(name: String?) {
        capitalName = name
    }

    inner class SuggestedCityViewHolder(
        private val binding: ItemSuggestedCityBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(cityName: String) {
            val isCapital = capitalName?.equals(cityName, ignoreCase = true) == true
            binding.tvCityEmoji.text = if (isCapital) "🏛️" else "🏙️"
            binding.tvCityName.text = cityName
            binding.tvCityPopulation.visibility = View.GONE
            binding.root.setOnClickListener { onClick(cityName) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestedCityViewHolder {
        val binding = ItemSuggestedCityBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SuggestedCityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SuggestedCityViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
    }
}
