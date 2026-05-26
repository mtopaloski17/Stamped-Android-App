package com.example.stamped.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.stamped.data.model.Country

@Dao
interface CountryDao {

    @Query("SELECT * FROM countries ORDER BY name ASC")
    fun getAllCountries(): LiveData<List<Country>>

    @Query("SELECT * FROM countries WHERE isVisited = 1 ORDER BY visitedAt DESC")
    fun getVisitedCountries(): LiveData<List<Country>>

    @Query("SELECT * FROM countries WHERE wantToVisit = 1 ORDER BY wantToVisitAt DESC")
    fun getBucketList(): LiveData<List<Country>>

    @Query("SELECT * FROM countries WHERE isVisited = 0 ORDER BY name ASC")
    fun getUnvisitedCountries(): LiveData<List<Country>>

    @Query("SELECT * FROM countries WHERE name LIKE '%' || :query || '%'")
    fun searchCountries(query: String): LiveData<List<Country>>

    @Query("SELECT * FROM countries WHERE code = :code LIMIT 1")
    fun getCountryByCode(code: String): LiveData<Country>

    @Query("SELECT * FROM countries WHERE code = :code LIMIT 1")
    suspend fun getCountryByCodeOnce(code: String): Country?

    @Query("SELECT COUNT(*) FROM countries WHERE isVisited = 1")
    fun getVisitedCount(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM countries WHERE wantToVisit = 1")
    fun getBucketListCount(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM countries")
    suspend fun getTotalCount(): Int

    @Query("SELECT * FROM countries WHERE continent = :continent ORDER BY name ASC")
    fun getCountriesByContinent(continent: String): LiveData<List<Country>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCountry(country: Country)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(countries: List<Country>)

    @Update
    suspend fun updateCountry(country: Country)

    @Query("UPDATE countries SET isVisited = :visited, visitedAt = :time WHERE code = :code")
    suspend fun setVisited(code: String, visited: Boolean, time: Long?)

    @Query("UPDATE countries SET wantToVisit = :wanted, wantToVisitAt = :time WHERE code = :code")
    suspend fun setWantToVisit(code: String, wanted: Boolean, time: Long?)

    @Query("UPDATE countries SET notes = :notes WHERE code = :code")
    suspend fun setNotes(code: String, notes: String)

    @Query("DELETE FROM countries")
    suspend fun deleteAll()
}
