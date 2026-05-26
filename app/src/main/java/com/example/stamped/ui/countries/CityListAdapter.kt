package com.example.stamped.ui.countries

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stamped.data.model.City
import com.example.stamped.data.model.Country
import com.example.stamped.databinding.ItemCityListBinding
import com.example.stamped.util.CountryLocale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CityListAdapter(
    private val onLongClick: (City) -> Unit
) : ListAdapter<City, CityListAdapter.CityListViewHolder>(DiffCallback()) {

    private var countriesMap: Map<String, Country> = emptyMap()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun setCountriesMap(map: Map<String, Country>) {
        countriesMap = map
        notifyDataSetChanged()
    }

    inner class CityListViewHolder(
        private val binding: ItemCityListBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(city: City) {
            val ctx = binding.root.context

            binding.tvCityEmoji.text = if (city.isCapital) "🏛️" else "🏙️"
            binding.tvCityName.text = city.name

            val country = countriesMap[city.countryCode]
            val flag = flagEmoji(city.countryCode)
            val countryName = country?.let {
                CountryLocale.getLocalizedName(it.code, it.name, ctx)
            } ?: city.countryCode
            val date = dateFormat.format(Date(city.visitedAt))
            binding.tvCountryInfo.text = "$flag $countryName · $date"

            binding.tvCapitalBadge.visibility = if (city.isCapital) View.VISIBLE else View.GONE

            binding.root.setOnLongClickListener {
                onLongClick(city)
                true
            }
        }

        private fun flagEmoji(code: String): String {
            if (code.length < 2) return ""
            val first = Character.codePointAt(code, 0) - 0x41 + 0x1F1E6
            val second = Character.codePointAt(code, 1) - 0x41 + 0x1F1E6
            return String(Character.toChars(first)) + String(Character.toChars(second))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityListViewHolder {
        val binding = ItemCityListBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CityListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CityListViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<City>() {
        override fun areItemsTheSame(oldItem: City, newItem: City) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: City, newItem: City) = oldItem == newItem
    }
}
