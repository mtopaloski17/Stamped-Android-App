package com.example.stamped.ui.countries

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stamped.R
import com.example.stamped.data.model.Country
import com.example.stamped.databinding.ItemCountryBinding
import com.example.stamped.util.CountryLocale

class CountryAdapter(
    private val onCountryClick: (Country) -> Unit
) : ListAdapter<Country, CountryAdapter.CountryViewHolder>(DiffCallback()) {

    inner class CountryViewHolder(
        private val binding: ItemCountryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(country: Country) {
            val ctx = binding.root.context
            binding.tvCountryName.text = CountryLocale.getLocalizedName(country.code, country.name, ctx)
            binding.tvContinent.text = CountryLocale.getLocalizedContinent(country.continent, ctx).uppercase()
            binding.tvFlag.text = getFlagEmoji(country.code)

            // Mutual exclusion: можна е само една состојба
            when {
                country.isVisited -> {
                    binding.tvStatusIcon.text = "✅"
                    binding.root.setCardBackgroundColor(
                        ContextCompat.getColor(ctx, R.color.status_visited_bg)
                    )
                }
                country.wantToVisit -> {
                    binding.tvStatusIcon.text = "⭐"
                    binding.root.setCardBackgroundColor(
                        ContextCompat.getColor(ctx, R.color.status_wishlist_bg)
                    )
                }
                else -> {
                    binding.tvStatusIcon.text = ""
                    binding.root.setCardBackgroundColor(
                        ContextCompat.getColor(ctx, R.color.bg_card)
                    )
                }
            }

            binding.root.setOnClickListener { onCountryClick(country) }
        }

        private fun getFlagEmoji(code: String): String {
            if (code.length < 2) return ""
            val first = Character.codePointAt(code, 0) - 0x41 + 0x1F1E6
            val second = Character.codePointAt(code, 1) - 0x41 + 0x1F1E6
            return String(Character.toChars(first)) + String(Character.toChars(second))
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
