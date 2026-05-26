package com.example.stamped.ui.countries

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stamped.R
import com.example.stamped.data.model.City
import com.example.stamped.data.model.Country
import com.example.stamped.data.remote.CountryInfoDto
import com.example.stamped.data.remote.RestCountriesClient
import com.example.stamped.databinding.ActivityCountryDetailBinding
import com.example.stamped.databinding.ItemInfoRowBinding
import com.example.stamped.repository.CountryRepository
import com.example.stamped.ui.base.BaseActivity
import com.example.stamped.util.CountryLocale
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CountryDetailActivity : BaseActivity() {

    companion object {
        const val EXTRA_CODE = "country_code"
    }

    private lateinit var binding: ActivityCountryDetailBinding
    private lateinit var repository: CountryRepository
    private lateinit var cityAdapter: CityAdapter
    private var current: Country? = null
    private var suppressToggleEvents = false
    private var loadedCapital: String? = null
    private var lastCitiesList: List<City> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCountryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = CountryRepository(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val code = intent.getStringExtra(EXTRA_CODE) ?: run { finish(); return }
        setupCities(code)
        observeCountry(code)
        setupListeners()
        fetchCountryInfo(code)
    }

    private fun setupCities(code: String) {
        cityAdapter = CityAdapter(onLongClick = { city ->
            confirmDeleteCity(city)
        })
        binding.recyclerCities.layoutManager = LinearLayoutManager(this)
        binding.recyclerCities.adapter = cityAdapter

        repository.getCitiesForCountry(code).observe(this) { cities ->
            cityAdapter.submitList(cities)
            lastCitiesList = cities
            updateCityVisibility(cities)
        }

        binding.btnAddCity.setOnClickListener {
            val country = current ?: return@setOnClickListener
            if (!country.isVisited) {
                Toast.makeText(this, R.string.cities_need_visited, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val localizedName = CountryLocale.getLocalizedName(country.code, country.name, this)
            val sheet = AddCityBottomSheet.newInstance(
                country.code,
                localizedName,
                country.name,   // English name за API повикот
                loadedCapital
            )
            sheet.onCityAdded = { name, isCapital, date ->
                lifecycleScope.launch {
                    repository.addCity(country.code, name, isCapital, date)
                    runOnUiThread {
                        Toast.makeText(
                            this@CountryDetailActivity,
                            getString(R.string.city_added, name),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            sheet.show(supportFragmentManager, "add_city")
        }
    }

    private fun updateCityVisibility(cities: List<City>) {
        val isVisited = current?.isVisited == true

        if (cities.isEmpty()) {
            binding.tvNoCities.visibility = View.VISIBLE
            binding.tvCityCount.visibility = View.GONE
            // Различна порака во зависност од состојбата
            binding.tvNoCities.text = if (isVisited) {
                getString(R.string.no_cities_yet)
            } else {
                getString(R.string.cities_need_visited)
            }
        } else {
            binding.tvNoCities.visibility = View.GONE
            binding.tvCityCount.visibility = View.VISIBLE
            binding.tvCityCount.text = cities.size.toString()
        }
    }

    private fun confirmDeleteCity(city: City) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_city)
            .setMessage(getString(R.string.delete_city_confirm, city.name))
            .setPositiveButton(R.string.confirm_delete) { _, _ ->
                lifecycleScope.launch {
                    repository.deleteCity(city)
                    runOnUiThread {
                        Toast.makeText(
                            this@CountryDetailActivity,
                            getString(R.string.city_removed, city.name),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun observeCountry(code: String) {
        repository.getCountryByCode(code).observe(this) { country ->
            country ?: return@observe
            current = country
            bindCountry(country)
        }
    }

    private fun bindCountry(c: Country) {
        suppressToggleEvents = true

        val localizedName = CountryLocale.getLocalizedName(c.code, c.name, this)
        val localizedContinent = CountryLocale.getLocalizedContinent(c.continent, this)

        binding.toolbar.title = localizedName
        binding.tvFlag.text = flagEmoji(c.code)
        binding.tvCountryName.text = localizedName
        binding.tvContinent.text = localizedContinent

        binding.switchVisited.isChecked = c.isVisited
        binding.switchBucket.isChecked = c.wantToVisit

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        if (c.isVisited && c.visitedAt != null) {
            binding.tvVisitedDate.visibility = View.VISIBLE
            binding.tvVisitedDate.text = getString(
                R.string.visited_on, dateFormat.format(Date(c.visitedAt))
            )
        } else {
            binding.tvVisitedDate.visibility = View.GONE
        }

        if (c.wantToVisit && c.wantToVisitAt != null) {
            binding.tvBucketDate.visibility = View.VISIBLE
            binding.tvBucketDate.text = getString(
                R.string.added_to_bucket, dateFormat.format(Date(c.wantToVisitAt))
            )
        } else {
            binding.tvBucketDate.visibility = View.GONE
        }

        if (binding.etNotes.text?.toString() != c.notes) {
            binding.etNotes.setText(c.notes)
        }

        // Add city е достапно само ако земјата е посетена
        binding.btnAddCity.isEnabled = c.isVisited
        binding.btnAddCity.alpha = if (c.isVisited) 1.0f else 0.5f

        // Освежи го пораката во cities секцијата врз основа на новата состојба
        updateCityVisibility(lastCitiesList)

        suppressToggleEvents = false
    }

    private fun setupListeners() {
        binding.switchVisited.setOnCheckedChangeListener { _, _ ->
            if (suppressToggleEvents) return@setOnCheckedChangeListener
            val c = current ?: return@setOnCheckedChangeListener
            lifecycleScope.launch { repository.toggleVisited(c) }
        }

        binding.switchBucket.setOnCheckedChangeListener { _, isChecked ->
            if (suppressToggleEvents) return@setOnCheckedChangeListener
            val c = current ?: return@setOnCheckedChangeListener
            lifecycleScope.launch {
                repository.toggleBucketList(c)
                val localizedName = CountryLocale.getLocalizedName(c.code, c.name, this@CountryDetailActivity)
                val msg = if (isChecked)
                    getString(R.string.bucket_added, localizedName)
                else
                    getString(R.string.bucket_removed, localizedName)
                runOnUiThread { Toast.makeText(this@CountryDetailActivity, msg, Toast.LENGTH_SHORT).show() }
            }
        }

        binding.btnSaveNotes.setOnClickListener {
            val c = current ?: return@setOnClickListener
            val notes = binding.etNotes.text?.toString()?.trim() ?: ""
            lifecycleScope.launch {
                repository.updateNotes(c.code, c.name, notes)
                runOnUiThread {
                    Toast.makeText(
                        this@CountryDetailActivity,
                        getString(R.string.notes_saved),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun fetchCountryInfo(code: String) {
        binding.infoProgress.visibility = View.VISIBLE
        binding.infoContent.visibility = View.GONE
        binding.infoError.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val info = RestCountriesClient.api.getCountryByCode(code)
                loadedCapital = info.capital?.firstOrNull()
                bindInfo(info)
            } catch (e: Exception) {
                android.util.Log.e("RestCountries", "Failed for code=$code", e)
                showInfoError(e.javaClass.simpleName + ": " + (e.message ?: ""))
            }
        }
    }

    private fun bindInfo(info: CountryInfoDto) {
        binding.infoProgress.visibility = View.GONE
        binding.infoError.visibility = View.GONE
        binding.infoContent.visibility = View.VISIBLE

        val nf = NumberFormat.getNumberInstance()

        setInfoRow(binding.rowCapital, R.string.info_capital,
            info.capital?.joinToString(", ").orDash())
        setInfoRow(binding.rowPopulation, R.string.info_population,
            nf.format(info.population))
        setInfoRow(binding.rowCurrency, R.string.info_currency,
            info.currencies?.values?.firstOrNull()?.let { c ->
                if (c.symbol.isNullOrEmpty()) c.name else "${c.name} (${c.symbol})"
            }.orDash())
        setInfoRow(binding.rowLanguages, R.string.info_languages,
            info.languages?.values?.joinToString(", ").orDash())
        setInfoRow(binding.rowRegion, R.string.info_region,
            listOfNotNull(info.region, info.subregion)
                .filter { it.isNotEmpty() }
                .joinToString(" • ")
                .ifEmpty { "—" })
        setInfoRow(binding.rowArea, R.string.info_area,
            getString(R.string.info_area_value, nf.format(info.area)))
        setInfoRow(binding.rowTimezones, R.string.info_timezone,
            info.timezones?.take(3)?.joinToString(", ").orDash())
        setInfoRow(binding.rowBorders, R.string.info_neighbors,
            info.borders?.joinToString(", ").orDash())
    }

    private fun setInfoRow(row: ItemInfoRowBinding, labelRes: Int, value: String) {
        row.tvLabel.text = getString(labelRes)
        row.tvValue.text = value
    }

    private fun showInfoError(detail: String? = null) {
        binding.infoProgress.visibility = View.GONE
        binding.infoContent.visibility = View.GONE
        binding.infoError.visibility = View.VISIBLE
        binding.infoError.text = if (detail.isNullOrBlank()) {
            getString(R.string.info_unavailable)
        } else {
            "${getString(R.string.info_unavailable)}\n($detail)"
        }
    }

    private fun String?.orDash(): String = if (this.isNullOrEmpty()) "—" else this

    private fun flagEmoji(code: String): String {
        if (code.length < 2) return ""
        val first = Character.codePointAt(code, 0) - 0x41 + 0x1F1E6
        val second = Character.codePointAt(code, 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(first)) + String(Character.toChars(second))
    }
}
