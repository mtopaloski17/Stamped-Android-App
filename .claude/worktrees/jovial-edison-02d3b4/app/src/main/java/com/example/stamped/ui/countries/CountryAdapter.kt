package com.example.stamped.ui.countries

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stamped.data.model.Country
import com.example.stamped.databinding.ItemCountryBinding

class CountryAdapter(
    private val onCountryClick: (Country) -> Unit
) : ListAdapter<Country, CountryAdapter.CountryViewHolder>(DiffCallback()) {

    inner class CountryViewHolder(
        private val binding: ItemCountryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(country: Country) {
            binding.tvCountryName.text = country.name
            binding.tvContinent.text = country.continent
            binding.tvFlag.text = getFlagEmoji(country.code)

            if (country.isVisited) {
                binding.ivVisited.setImageResource(
                    android.R.drawable.checkbox_on_background
                )
                binding.root.setCardBackgroundColor(
                    android.graphics.Color.parseColor("#E8F5E9")
                )
            } else {
                binding.ivVisited.setImageResource(
                    android.R.drawable.checkbox_off_background
                )
                binding.root.setCardBackgroundColor(
                    android.graphics.Color.WHITE
                )
            }

            binding.root.setOnClickListener { onCountryClick(country) }
        }

        private fun getFlagEmoji(countryCode: String): String {
            val firstLetter = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6
            val secondLetter = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6
            return String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountryViewHolder {
        val binding = ItemCountryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CountryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CountryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Country>() {
        override fun areItemsTheSame(oldItem: Country, newItem: Country) =
            oldItem.code == newItem.code
        override fun areContentsTheSame(oldItem: Country, newItem: Country) =
            oldItem == newItem
    }
}