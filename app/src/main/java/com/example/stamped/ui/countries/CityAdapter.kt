package com.example.stamped.ui.countries

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stamped.data.model.City
import com.example.stamped.databinding.ItemCityBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CityAdapter(
    private val onLongClick: (City) -> Unit
) : ListAdapter<City, CityAdapter.CityViewHolder>(DiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    inner class CityViewHolder(
        private val binding: ItemCityBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(city: City) {
            binding.tvCityName.text = city.name
            binding.tvCityDate.text = dateFormat.format(Date(city.visitedAt))
            binding.tvCityEmoji.text = if (city.isCapital) "🏛️" else "🏙️"
            binding.tvCapitalBadge.visibility = if (city.isCapital) View.VISIBLE else View.GONE

            binding.root.setOnLongClickListener {
                onLongClick(city)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
        val binding = ItemCityBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<City>() {
        override fun areItemsTheSame(oldItem: City, newItem: City) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: City, newItem: City) = oldItem == newItem
    }
}
