package com.example.stamped.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.stamped.data.model.Country

@Dao
interface CountryDao {

    // Земи ги сите земји
    @Query("SELECT * FROM countries ORDER BY name ASC")
    fun getAllCountries(): LiveData<List<Country>>

    // Земи само посетените
    @Query("SELECT * FROM countries WHERE isVisited = 1 ORDER BY visitedAt DESC")
    fun getVisitedCountries(): LiveData<List<Country>>

    // Земи само непосетените
    @Query("SELECT * FROM countries WHERE isVisited = 0 ORDER BY name ASC")
    fun getUnvisitedCountries(): LiveData<List<Country>>

    // Пребарување по ime
    @Query("SELECT * FROM countries WHERE name LIKE '%' || :query || '%'")
    fun searchCountries(query: String): LiveData<List<Country>>

    // Брои посетени (LiveData за UI)
    @Query("SELECT COUNT(*) FROM countries WHERE isVisited = 1")
    fun getVisitedCount(): LiveData<Int>

    // Вкупен број на земји (suspend за логика)
    @Query("SELECT COUNT(*) FROM countries")
    suspend fun getTotalCount(): Int

    // Земи по континент
    @Query("SELECT * FROM countries WHERE continent = :continent ORDER BY name ASC")
    fun getCountriesByContinent(continent: String): LiveData<List<Country>>

    // Вметни или ажурирај земја
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCountry(country: Country)

    // Вметни листа на земји
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(countries: List<Country>)

    // Ажурирај земја
    @Update
    suspend fun updateCountry(country: Country)

    // Означи како посетена
    @Query("UPDATE countries SET isVisited = :visited, visitedAt = :time WHERE code = :code")
    suspend fun setVisited(code: String, visited: Boolean, time: Long?)

    // Избриши сè
    @Query("DELETE FROM countries")
    suspend fun deleteAll()
}