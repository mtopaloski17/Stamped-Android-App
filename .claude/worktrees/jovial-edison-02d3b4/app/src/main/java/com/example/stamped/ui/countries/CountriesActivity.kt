package com.example.stamped.ui.countries

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stamped.data.model.Country
import com.example.stamped.databinding.ActivityCountriesBinding
import com.example.stamped.repository.CountryRepository
import kotlinx.coroutines.launch

class CountriesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCountriesBinding
    private lateinit var repository: CountryRepository
    private lateinit var adapter: CountryAdapter
    private var currentSource: LiveData<List<Country>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCountriesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = CountryRepository(this)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        setupSearch()
        loadCountries()
        populateDatabase()
    }

    private fun setupRecyclerView() {
        adapter = CountryAdapter { country ->
            lifecycleScope.launch {
                repository.toggleVisited(country)
            }
        }
        binding.recyclerCountries.layoutManager = LinearLayoutManager(this)
        binding.recyclerCountries.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            val query = text?.toString() ?: ""
            observeCountries(
                if (query.isEmpty()) repository.allCountries
                else repository.searchCountries(query)
            )
        }
    }

    private fun observeCountries(source: LiveData<List<Country>>) {
        currentSource?.removeObservers(this)
        currentSource = source
        source.observe(this) { countries ->
            adapter.submitList(countries)
        }
    }

    private fun loadCountries() {
        observeCountries(repository.allCountries)
    }

    private fun populateDatabase() {
        lifecycleScope.launch {
            repository.populateIfEmpty()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}