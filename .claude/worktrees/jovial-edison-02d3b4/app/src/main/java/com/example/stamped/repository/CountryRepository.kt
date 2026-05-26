package com.example.stamped.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.example.stamped.data.database.AppDatabase
import com.example.stamped.data.model.Country
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CountryRepository(context: Context) {

    private val dao = AppDatabase.getDatabase(context).countryDao()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val allCountries: LiveData<List<Country>> = dao.getAllCountries()
    val visitedCountries: LiveData<List<Country>> = dao.getVisitedCountries()
    val visitedCount: LiveData<Int> = dao.getVisitedCount()

    // Означи земја како посетена/непосетена
    suspend fun toggleVisited(country: Country) {
        val now = System.currentTimeMillis()
        val newVisited = !country.isVisited
        val time = if (newVisited) now else null

        // Зачувај во Room локално
        dao.setVisited(country.code, newVisited, time)

        // Синхронизирај со Firestore
        syncWithFirestore(country.code, country.name, newVisited, time)
    }

    // Пополни ја базата со сите земји при прво стартување
    suspend fun populateIfEmpty() {
        if (dao.getTotalCount() == 0) {
            dao.insertAll(getAllCountriesList())
        }
    }

    fun searchCountries(query: String): LiveData<List<Country>> {
        return dao.searchCountries(query)
    }

    fun getCountriesByContinent(continent: String): LiveData<List<Country>> {
        return dao.getCountriesByContinent(continent)
    }

    // Синхронизација со Firestore
    private fun syncWithFirestore(code: String, name: String, visited: Boolean, time: Long?) {
        val userId = auth.currentUser?.uid ?: return
        val ref = db.collection("countries")
            .document(userId)
            .collection("visited")
            .document(code)
        if (visited) {
            ref.set(mapOf(
                "name" to name,
                "code" to code,
                "visitedAt" to time
            ))
        } else {
            ref.delete()
        }
    }

    // Листа на сите 30 земји (скратена — ќе ја прошируваме)
    private fun getAllCountriesList(): List<Country> = listOf(
        Country("MK", "North Macedonia", "Europe"),
        Country("DE", "Germany", "Europe"),
        Country("FR", "France", "Europe"),
        Country("IT", "Italy", "Europe"),
        Country("ES", "Spain", "Europe"),
        Country("GB", "United Kingdom", "Europe"),
        Country("GR", "Greece", "Europe"),
        Country("HR", "Croatia", "Europe"),
        Country("RS", "Serbia", "Europe"),
        Country("AL", "Albania", "Europe"),
        Country("TR", "Turkey", "Europe"),
        Country("US", "United States", "North America"),
        Country("CA", "Canada", "North America"),
        Country("MX", "Mexico", "North America"),
        Country("BR", "Brazil", "South America"),
        Country("AR", "Argentina", "South America"),
        Country("CL", "Chile", "South America"),
        Country("CN", "China", "Asia"),
        Country("JP", "Japan", "Asia"),
        Country("KR", "South Korea", "Asia"),
        Country("IN", "India", "Asia"),
        Country("TH", "Thailand", "Asia"),
        Country("AE", "UAE", "Asia"),
        Country("AU", "Australia", "Oceania"),
        Country("NZ", "New Zealand", "Oceania"),
        Country("EG", "Egypt", "Africa"),
        Country("ZA", "South Africa", "Africa"),
        Country("MA", "Morocco", "Africa"),
        Country("NG", "Nigeria", "Africa"),
        Country("KE", "Kenya", "Africa")
    )
}