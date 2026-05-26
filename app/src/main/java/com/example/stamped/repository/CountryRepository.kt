package com.example.stamped.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.example.stamped.data.database.AppDatabase
import com.example.stamped.data.model.City
import com.example.stamped.data.model.Country
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class CountryRepository(context: Context) {

    private val auth = FirebaseAuth.getInstance()
    private val isAnonymous = auth.currentUser?.isAnonymous ?: true
    // За анонимни корисници користиме СТАБИЛЕН локален ID,
    // не Firebase анонимниот UID (кој се менува со секоја сесија).
    // Така податоците остануваат во истиот Room DB меѓу анонимни сесии.
    private val userId = when {
        auth.currentUser == null -> "local"
        isAnonymous -> "anonymous_local"
        else -> auth.currentUser!!.uid
    }

    private val database = AppDatabase.getDatabase(context, userId)
    private val dao = database.countryDao()
    private val cityDao = database.cityDao()
    private val db = FirebaseFirestore.getInstance()

    val allCountries: LiveData<List<Country>> = dao.getAllCountries()
    val visitedCountries: LiveData<List<Country>> = dao.getVisitedCountries()
    val bucketList: LiveData<List<Country>> = dao.getBucketList()
    val visitedCount: LiveData<Int> = dao.getVisitedCount()
    val bucketListCount: LiveData<Int> = dao.getBucketListCount()

    fun getCountryByCode(code: String): LiveData<Country> = dao.getCountryByCode(code)

    // ===== Cities =====
    val allCities: LiveData<List<City>> = cityDao.getAllCities()
    val cityCount: LiveData<Int> = cityDao.getCityCount()
    val capitalCount: LiveData<Int> = cityDao.getCapitalCount()

    fun getCitiesForCountry(code: String): LiveData<List<City>> =
        cityDao.getCitiesForCountry(code)

    suspend fun addCity(countryCode: String, name: String, isCapital: Boolean, visitedAt: Long) {
        val city = City(
            countryCode = countryCode,
            name = name.trim(),
            isCapital = isCapital,
            visitedAt = visitedAt
        )
        cityDao.insert(city)
        syncCityWithFirestore(city, exists = true)
    }

    suspend fun deleteCity(city: City) {
        cityDao.delete(city)
        syncCityWithFirestore(city, exists = false)
    }

    suspend fun addCityFromRemote(
        countryCode: String,
        name: String,
        isCapital: Boolean,
        visitedAt: Long
    ) {
        cityDao.insert(
            City(countryCode = countryCode, name = name, isCapital = isCapital, visitedAt = visitedAt)
        )
    }

    private fun firestoreCityId(countryCode: String, name: String): String {
        val sanitized = name.lowercase().replace(Regex("[^a-z0-9]"), "_")
        return "${countryCode}_${sanitized}"
    }

    private fun syncCityWithFirestore(city: City, exists: Boolean) {
        if (isAnonymous) return
        val ref = db.collection("countries")
            .document(userId)
            .collection("cities")
            .document(firestoreCityId(city.countryCode, city.name))
        if (exists) {
            ref.set(
                mapOf(
                    "name" to city.name,
                    "countryCode" to city.countryCode,
                    "isCapital" to city.isCapital,
                    "visitedAt" to toTimestamp(city.visitedAt)
                )
            )
        } else {
            ref.delete()
        }
    }

    // Постави посетена/непосетена по код (за Map и Firestore sync)
    suspend fun setVisitedByCode(code: String, name: String, visited: Boolean) {
        val time = if (visited) System.currentTimeMillis() else null
        dao.setVisited(code, visited, time)
        syncWithFirestore(code, name, visited, time)

        // Mutual exclusion — посетена земја не може истовремено да биде во wishlist
        if (visited) {
            val current = dao.getCountryByCodeOnce(code)
            if (current?.wantToVisit == true) {
                dao.setWantToVisit(code, false, null)
                syncBucketWithFirestore(code, name, false, null)
            }
        }
    }

    // Ажурирај Room од Firestore без write-back (за почетна синхронизација)
    suspend fun markVisitedFromRemote(code: String, visitedAt: Long) {
        dao.setVisited(code, true, visitedAt)
    }

    suspend fun markBucketFromRemote(code: String, wantedAt: Long) {
        dao.setWantToVisit(code, true, wantedAt)
    }

    suspend fun setNotesFromRemote(code: String, notes: String) {
        dao.setNotes(code, notes)
    }

    // Означи земја како посетена/непосетена
    suspend fun toggleVisited(country: Country) {
        val now = System.currentTimeMillis()
        val newVisited = !country.isVisited
        val time = if (newVisited) now else null

        dao.setVisited(country.code, newVisited, time)
        syncWithFirestore(country.code, country.name, newVisited, time)

        // Mutual exclusion
        if (newVisited && country.wantToVisit) {
            dao.setWantToVisit(country.code, false, null)
            syncBucketWithFirestore(country.code, country.name, false, null)
        }
    }

    // Означи земја како желба за посета / отстрани од листата
    suspend fun toggleBucketList(country: Country) {
        val now = System.currentTimeMillis()
        val newWanted = !country.wantToVisit
        val time = if (newWanted) now else null

        dao.setWantToVisit(country.code, newWanted, time)
        syncBucketWithFirestore(country.code, country.name, newWanted, time)

        // Mutual exclusion
        if (newWanted && country.isVisited) {
            dao.setVisited(country.code, false, null)
            syncWithFirestore(country.code, country.name, false, null)
        }
    }

    // Зачувај белешки за земја
    suspend fun updateNotes(code: String, name: String, notes: String) {
        dao.setNotes(code, notes)
        syncNotesWithFirestore(code, name, notes)
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

    // Помошна функција — Long → Firebase Timestamp
    private fun toTimestamp(time: Long?): Timestamp? {
        return time?.let { Timestamp(Date(it)) }
    }

    // Синхронизација со Firestore (само за автентицирани корисници)
    private fun syncWithFirestore(code: String, name: String, visited: Boolean, time: Long?) {
        if (isAnonymous) return
        val ref = db.collection("countries")
            .document(userId)
            .collection("visited")
            .document(code)
        if (visited) {
            ref.set(mapOf(
                "name" to name,
                "code" to code,
                "visitedAt" to toTimestamp(time)
            ))
        } else {
            ref.delete()
        }
    }

    private fun syncBucketWithFirestore(code: String, name: String, wanted: Boolean, time: Long?) {
        if (isAnonymous) return
        val ref = db.collection("countries")
            .document(userId)
            .collection("bucketList")
            .document(code)
        if (wanted) {
            ref.set(mapOf(
                "name" to name,
                "code" to code,
                "wantToVisitAt" to toTimestamp(time)
            ))
        } else {
            ref.delete()
        }
    }

    private fun syncNotesWithFirestore(code: String, name: String, notes: String) {
        if (isAnonymous) return
        val ref = db.collection("countries")
            .document(userId)
            .collection("notes")
            .document(code)
        if (notes.isBlank()) {
            ref.delete()
        } else {
            ref.set(mapOf(
                "name" to name,
                "code" to code,
                "notes" to notes,
                "updatedAt" to Timestamp.now()
            ))
        }
    }

    private fun getAllCountriesList(): List<Country> = listOf(
        // Africa
        Country("DZ", "Algeria", "Africa"),
        Country("AO", "Angola", "Africa"),
        Country("BJ", "Benin", "Africa"),
        Country("BW", "Botswana", "Africa"),
        Country("BF", "Burkina Faso", "Africa"),
        Country("BI", "Burundi", "Africa"),
        Country("CM", "Cameroon", "Africa"),
        Country("CV", "Cape Verde", "Africa"),
        Country("CF", "Central African Republic", "Africa"),
        Country("TD", "Chad", "Africa"),
        Country("KM", "Comoros", "Africa"),
        Country("CG", "Republic of Congo", "Africa"),
        Country("CD", "DR Congo", "Africa"),
        Country("CI", "Ivory Coast", "Africa"),
        Country("DJ", "Djibouti", "Africa"),
        Country("EG", "Egypt", "Africa"),
        Country("GQ", "Equatorial Guinea", "Africa"),
        Country("ER", "Eritrea", "Africa"),
        Country("SZ", "Eswatini", "Africa"),
        Country("ET", "Ethiopia", "Africa"),
        Country("GA", "Gabon", "Africa"),
        Country("GM", "Gambia", "Africa"),
        Country("GH", "Ghana", "Africa"),
        Country("GN", "Guinea", "Africa"),
        Country("GW", "Guinea-Bissau", "Africa"),
        Country("KE", "Kenya", "Africa"),
        Country("LS", "Lesotho", "Africa"),
        Country("LR", "Liberia", "Africa"),
        Country("LY", "Libya", "Africa"),
        Country("MG", "Madagascar", "Africa"),
        Country("MW", "Malawi", "Africa"),
        Country("ML", "Mali", "Africa"),
        Country("MR", "Mauritania", "Africa"),
        Country("MU", "Mauritius", "Africa"),
        Country("MA", "Morocco", "Africa"),
        Country("MZ", "Mozambique", "Africa"),
        Country("NA", "Namibia", "Africa"),
        Country("NE", "Niger", "Africa"),
        Country("NG", "Nigeria", "Africa"),
        Country("RW", "Rwanda", "Africa"),
        Country("ST", "São Tomé and Príncipe", "Africa"),
        Country("SN", "Senegal", "Africa"),
        Country("SL", "Sierra Leone", "Africa"),
        Country("SO", "Somalia", "Africa"),
        Country("ZA", "South Africa", "Africa"),
        Country("SS", "South Sudan", "Africa"),
        Country("SD", "Sudan", "Africa"),
        Country("TZ", "Tanzania", "Africa"),
        Country("TG", "Togo", "Africa"),
        Country("TN", "Tunisia", "Africa"),
        Country("UG", "Uganda", "Africa"),
        Country("ZM", "Zambia", "Africa"),
        Country("ZW", "Zimbabwe", "Africa"),
        // Asia
        Country("AF", "Afghanistan", "Asia"),
        Country("AM", "Armenia", "Asia"),
        Country("AZ", "Azerbaijan", "Asia"),
        Country("BH", "Bahrain", "Asia"),
        Country("BD", "Bangladesh", "Asia"),
        Country("BT", "Bhutan", "Asia"),
        Country("BN", "Brunei", "Asia"),
        Country("KH", "Cambodia", "Asia"),
        Country("CN", "China", "Asia"),
        Country("CY", "Cyprus", "Asia"),
        Country("GE", "Georgia", "Asia"),
        Country("IN", "India", "Asia"),
        Country("ID", "Indonesia", "Asia"),
        Country("IR", "Iran", "Asia"),
        Country("IQ", "Iraq", "Asia"),
        Country("IL", "Israel", "Asia"),
        Country("JP", "Japan", "Asia"),
        Country("JO", "Jordan", "Asia"),
        Country("KZ", "Kazakhstan", "Asia"),
        Country("KW", "Kuwait", "Asia"),
        Country("KG", "Kyrgyzstan", "Asia"),
        Country("LA", "Laos", "Asia"),
        Country("LB", "Lebanon", "Asia"),
        Country("MY", "Malaysia", "Asia"),
        Country("MV", "Maldives", "Asia"),
        Country("MN", "Mongolia", "Asia"),
        Country("MM", "Myanmar", "Asia"),
        Country("NP", "Nepal", "Asia"),
        Country("KP", "North Korea", "Asia"),
        Country("OM", "Oman", "Asia"),
        Country("PK", "Pakistan", "Asia"),
        Country("PH", "Philippines", "Asia"),
        Country("QA", "Qatar", "Asia"),
        Country("SA", "Saudi Arabia", "Asia"),
        Country("SG", "Singapore", "Asia"),
        Country("KR", "South Korea", "Asia"),
        Country("LK", "Sri Lanka", "Asia"),
        Country("SY", "Syria", "Asia"),
        Country("TW", "Taiwan", "Asia"),
        Country("TJ", "Tajikistan", "Asia"),
        Country("TH", "Thailand", "Asia"),
        Country("TL", "Timor-Leste", "Asia"),
        Country("TR", "Turkey", "Asia"),
        Country("TM", "Turkmenistan", "Asia"),
        Country("AE", "UAE", "Asia"),
        Country("UZ", "Uzbekistan", "Asia"),
        Country("VN", "Vietnam", "Asia"),
        Country("YE", "Yemen", "Asia"),
        // Europe
        Country("AL", "Albania", "Europe"),
        Country("AD", "Andorra", "Europe"),
        Country("AT", "Austria", "Europe"),
        Country("BY", "Belarus", "Europe"),
        Country("BE", "Belgium", "Europe"),
        Country("BA", "Bosnia and Herzegovina", "Europe"),
        Country("BG", "Bulgaria", "Europe"),
        Country("HR", "Croatia", "Europe"),
        Country("CZ", "Czech Republic", "Europe"),
        Country("DK", "Denmark", "Europe"),
        Country("EE", "Estonia", "Europe"),
        Country("FI", "Finland", "Europe"),
        Country("FR", "France", "Europe"),
        Country("DE", "Germany", "Europe"),
        Country("GR", "Greece", "Europe"),
        Country("HU", "Hungary", "Europe"),
        Country("IS", "Iceland", "Europe"),
        Country("IE", "Ireland", "Europe"),
        Country("IT", "Italy", "Europe"),
        Country("XK", "Kosovo", "Europe"),
        Country("LV", "Latvia", "Europe"),
        Country("LI", "Liechtenstein", "Europe"),
        Country("LT", "Lithuania", "Europe"),
        Country("LU", "Luxembourg", "Europe"),
        Country("MT", "Malta", "Europe"),
        Country("MD", "Moldova", "Europe"),
        Country("MC", "Monaco", "Europe"),
        Country("ME", "Montenegro", "Europe"),
        Country("NL", "Netherlands", "Europe"),
        Country("MK", "North Macedonia", "Europe"),
        Country("NO", "Norway", "Europe"),
        Country("PL", "Poland", "Europe"),
        Country("PT", "Portugal", "Europe"),
        Country("RO", "Romania", "Europe"),
        Country("RU", "Russia", "Europe"),
        Country("SM", "San Marino", "Europe"),
        Country("RS", "Serbia", "Europe"),
        Country("SK", "Slovakia", "Europe"),
        Country("SI", "Slovenia", "Europe"),
        Country("ES", "Spain", "Europe"),
        Country("SE", "Sweden", "Europe"),
        Country("CH", "Switzerland", "Europe"),
        Country("UA", "Ukraine", "Europe"),
        Country("GB", "United Kingdom", "Europe"),
        Country("VA", "Vatican City", "Europe"),
        // North America
        Country("AG", "Antigua and Barbuda", "North America"),
        Country("BS", "Bahamas", "North America"),
        Country("BB", "Barbados", "North America"),
        Country("BZ", "Belize", "North America"),
        Country("CA", "Canada", "North America"),
        Country("CR", "Costa Rica", "North America"),
        Country("CU", "Cuba", "North America"),
        Country("DM", "Dominica", "North America"),
        Country("DO", "Dominican Republic", "North America"),
        Country("SV", "El Salvador", "North America"),
        Country("GD", "Grenada", "North America"),
        Country("GT", "Guatemala", "North America"),
        Country("HT", "Haiti", "North America"),
        Country("HN", "Honduras", "North America"),
        Country("JM", "Jamaica", "North America"),
        Country("MX", "Mexico", "North America"),
        Country("NI", "Nicaragua", "North America"),
        Country("PA", "Panama", "North America"),
        Country("KN", "Saint Kitts and Nevis", "North America"),
        Country("LC", "Saint Lucia", "North America"),
        Country("VC", "Saint Vincent and the Grenadines", "North America"),
        Country("TT", "Trinidad and Tobago", "North America"),
        Country("US", "United States", "North America"),
        // South America
        Country("AR", "Argentina", "South America"),
        Country("BO", "Bolivia", "South America"),
        Country("BR", "Brazil", "South America"),
        Country("CL", "Chile", "South America"),
        Country("CO", "Colombia", "South America"),
        Country("EC", "Ecuador", "South America"),
        Country("GY", "Guyana", "South America"),
        Country("PY", "Paraguay", "South America"),
        Country("PE", "Peru", "South America"),
        Country("SR", "Suriname", "South America"),
        Country("UY", "Uruguay", "South America"),
        Country("VE", "Venezuela", "South America"),
        // Oceania
        Country("AU", "Australia", "Oceania"),
        Country("FJ", "Fiji", "Oceania"),
        Country("KI", "Kiribati", "Oceania"),
        Country("MH", "Marshall Islands", "Oceania"),
        Country("FM", "Micronesia", "Oceania"),
        Country("NR", "Nauru", "Oceania"),
        Country("NZ", "New Zealand", "Oceania"),
        Country("PW", "Palau", "Oceania"),
        Country("PG", "Papua New Guinea", "Oceania"),
        Country("WS", "Samoa", "Oceania"),
        Country("SB", "Solomon Islands", "Oceania"),
        Country("TO", "Tonga", "Oceania"),
        Country("TV", "Tuvalu", "Oceania"),
        Country("VU", "Vanuatu", "Oceania")
    )
}