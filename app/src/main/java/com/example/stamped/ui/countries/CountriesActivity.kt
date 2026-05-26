package com.example.stamped.ui.countries

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stamped.R
import com.example.stamped.data.model.City
import com.example.stamped.data.model.Country
import com.example.stamped.databinding.ActivityCountriesBinding
import com.example.stamped.repository.CountryRepository
import com.example.stamped.ui.base.BaseActivity
import com.example.stamped.ui.map.MapActivity
import com.example.stamped.ui.profile.ProfileActivity
import kotlinx.coroutines.launch

class CountriesActivity : BaseActivity() {

    private lateinit var binding: ActivityCountriesBinding
    private lateinit var repository: CountryRepository
    private lateinit var countryAdapter: CountryAdapter
    private lateinit var cityAdapter: CityListAdapter
    private var currentSource: LiveData<List<Country>>? = null

    private enum class Filter { ALL, VISITED, BUCKET, CITIES }
    private var activeFilter = Filter.ALL
    private var currentQuery: String = ""
    private var allCitiesCache: List<City> = emptyList()
    private var countriesCache: Map<String, Country> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCountriesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = CountryRepository(this)
        setSupportActionBar(binding.toolbar)

        setupAdapters()
        setupSearch()
        setupFilters()
        observeAllCountries()
        observeAllCities()
        applyFilter()
        populateDatabase()
        setupBottomNav()
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNav.selectedItemId = R.id.nav_countries
    }

    private fun setupAdapters() {
        countryAdapter = CountryAdapter { country ->
            startActivity(Intent(this, CountryDetailActivity::class.java).apply {
                putExtra(CountryDetailActivity.EXTRA_CODE, country.code)
            })
        }
        cityAdapter = CityListAdapter(onLongClick = { city ->
            confirmDeleteCity(city)
        })
        binding.recyclerCountries.layoutManager = LinearLayoutManager(this)
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            currentQuery = text?.toString() ?: ""
            applyFilter()
        }
    }

    private fun setupFilters() {
        binding.chipAll.setOnClickListener { activeFilter = Filter.ALL; applyFilter() }
        binding.chipVisited.setOnClickListener { activeFilter = Filter.VISITED; applyFilter() }
        binding.chipBucket.setOnClickListener { activeFilter = Filter.BUCKET; applyFilter() }
        binding.chipCities.setOnClickListener { activeFilter = Filter.CITIES; applyFilter() }
    }

    private fun observeAllCountries() {
        repository.allCountries.observe(this) { countries ->
            countriesCache = countries.associateBy { it.code }
            cityAdapter.setCountriesMap(countriesCache)
        }
    }

    private fun observeAllCities() {
        repository.allCities.observe(this) { cities ->
            allCitiesCache = cities
            if (activeFilter == Filter.CITIES) {
                showCitiesList()
            }
        }
    }

    private fun applyFilter() {
        if (activeFilter == Filter.CITIES) {
            // Префрли се на CityListAdapter
            binding.recyclerCountries.adapter = cityAdapter
            currentSource?.removeObservers(this)
            currentSource = null
            showCitiesList()
            return
        }

        // Се враќаме на CountryAdapter
        if (binding.recyclerCountries.adapter !== countryAdapter) {
            binding.recyclerCountries.adapter = countryAdapter
        }

        val source: LiveData<List<Country>> = when {
            currentQuery.isNotEmpty() -> repository.searchCountries(currentQuery)
            activeFilter == Filter.VISITED -> repository.visitedCountries
            activeFilter == Filter.BUCKET -> repository.bucketList
            else -> repository.allCountries
        }
        observeCountries(source)
    }

    private fun showCitiesList() {
        val filtered = if (currentQuery.isEmpty()) {
            allCitiesCache
        } else {
            allCitiesCache.filter {
                it.name.contains(currentQuery, ignoreCase = true) ||
                    countriesCache[it.countryCode]?.name?.contains(currentQuery, ignoreCase = true) == true
            }
        }
        cityAdapter.submitList(filtered)
    }

    private fun observeCountries(source: LiveData<List<Country>>) {
        currentSource?.removeObservers(this)
        currentSource = source
        source.observe(this) { countries ->
            val filtered = when {
                currentQuery.isNotEmpty() && activeFilter == Filter.VISITED -> countries.filter { it.isVisited }
                currentQuery.isNotEmpty() && activeFilter == Filter.BUCKET -> countries.filter { it.wantToVisit }
                else -> countries
            }
            countryAdapter.submitList(filtered)
        }
    }

    private fun confirmDeleteCity(city: City) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_city)
            .setMessage(getString(R.string.delete_city_confirm, city.name))
            .setPositiveButton(R.string.confirm_delete) { _, _ ->
                lifecycleScope.launch { repository.deleteCity(city) }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun populateDatabase() {
        lifecycleScope.launch { repository.populateIfEmpty() }
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_map -> {
                    startActivity(Intent(this, MapActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    finish()
                    true
                }
                R.id.nav_countries -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}
