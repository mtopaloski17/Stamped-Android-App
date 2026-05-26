package com.example.stamped.ui.countries

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stamped.R
import com.example.stamped.data.remote.CountriesNowClient
import com.example.stamped.data.remote.CountriesNowRequest
import com.example.stamped.databinding.BottomSheetAddCityBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddCityBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_COUNTRY_CODE = "country_code"
        private const val ARG_DISPLAY_NAME = "display_name"
        private const val ARG_ENGLISH_NAME = "english_name"
        private const val ARG_CAPITAL = "capital"

        fun newInstance(
            countryCode: String,
            displayName: String,
            englishName: String,
            capital: String?
        ): AddCityBottomSheet {
            return AddCityBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_COUNTRY_CODE, countryCode)
                    putString(ARG_DISPLAY_NAME, displayName)
                    putString(ARG_ENGLISH_NAME, englishName)
                    putString(ARG_CAPITAL, capital)
                }
            }
        }
    }

    var onCityAdded: ((name: String, isCapital: Boolean, date: Long) -> Unit)? = null

    private var _binding: BottomSheetAddCityBinding? = null
    private val binding get() = _binding!!
    private var selectedDate: Long = System.currentTimeMillis()
    private var capitalName: String? = null
    private var englishCountryName: String = ""
    private var allCities: List<String> = emptyList()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    private lateinit var suggestedAdapter: SuggestedCityAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddCityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val displayName = arguments?.getString(ARG_DISPLAY_NAME) ?: ""
        englishCountryName = arguments?.getString(ARG_ENGLISH_NAME) ?: ""
        capitalName = arguments?.getString(ARG_CAPITAL)

        binding.tvBottomSheetTitle.text = getString(R.string.add_city_to, displayName)
        updateDateText()

        setupSuggestedList()
        loadCities()
        setupSearch()

        binding.btnDate.setOnClickListener { showDatePicker() }
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnAdd.setOnClickListener { tryAdd() }
    }

    private fun setupSuggestedList() {
        suggestedAdapter = SuggestedCityAdapter(capitalName) { cityName ->
            binding.etCityName.setText(cityName)
            binding.etCityName.setSelection(cityName.length)
        }
        binding.recyclerCities.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCities.adapter = suggestedAdapter
    }

    private fun loadCities() {
        if (englishCountryName.isBlank()) {
            showError(getString(R.string.cities_load_error))
            return
        }
        showLoading()
        lifecycleScope.launch {
            try {
                val response = CountriesNowClient.api.citiesByCountry(
                    CountriesNowRequest(englishCountryName)
                )
                if (response.error || response.data.isEmpty()) {
                    showError(getString(R.string.cities_none_found))
                } else {
                    // Главниот град прв, останатите алфабетски
                    val capital = capitalName
                    val sorted = if (!capital.isNullOrBlank() &&
                        response.data.any { it.equals(capital, ignoreCase = true) }) {
                        val cap = response.data.first { it.equals(capital, ignoreCase = true) }
                        listOf(cap) + response.data.filter { !it.equals(capital, ignoreCase = true) }
                            .sorted()
                    } else {
                        response.data.sorted()
                    }
                    allCities = sorted
                    showResults(allCities)
                }
            } catch (e: Exception) {
                android.util.Log.e("CountriesNow", "Failed to load cities", e)
                showError(getString(R.string.cities_load_error))
            }
        }
    }

    private fun setupSearch() {
        binding.etCityName.addTextChangedListener { text ->
            val query = text?.toString()?.trim().orEmpty()
            val filtered = if (query.isEmpty()) {
                allCities
            } else {
                allCities.filter { it.contains(query, ignoreCase = true) }
            }
            showResults(filtered)
        }
    }

    private fun showLoading() {
        binding.citiesProgress.visibility = View.VISIBLE
        binding.citiesCardContainer.visibility = View.GONE
        binding.citiesEmpty.visibility = View.GONE
    }

    private fun showResults(cities: List<String>) {
        binding.citiesProgress.visibility = View.GONE
        if (cities.isEmpty()) {
            binding.citiesCardContainer.visibility = View.GONE
            binding.citiesEmpty.visibility = View.VISIBLE
            binding.citiesEmpty.text = getString(R.string.cities_none_found)
        } else {
            binding.citiesCardContainer.visibility = View.VISIBLE
            binding.citiesEmpty.visibility = View.GONE
            suggestedAdapter.submitList(cities)
        }
    }

    private fun showError(message: String) {
        binding.citiesProgress.visibility = View.GONE
        binding.citiesCardContainer.visibility = View.GONE
        binding.citiesEmpty.visibility = View.VISIBLE
        binding.citiesEmpty.text = message
    }

    private fun tryAdd() {
        val name = binding.etCityName.text?.toString()?.trim().orEmpty()
        if (name.isEmpty()) {
            binding.etCityName.error = getString(R.string.city_name_hint)
            return
        }
        val isCapital = capitalName?.equals(name, ignoreCase = true) == true
        onCityAdded?.invoke(name, isCapital, selectedDate)
        dismiss()
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                cal.set(year, month, day)
                selectedDate = cal.timeInMillis
                updateDateText()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    private fun updateDateText() {
        binding.tvDate.text = dateFormat.format(Date(selectedDate))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
