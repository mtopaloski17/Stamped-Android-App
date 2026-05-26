package com.example.stamped.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.stamped.data.model.City

@Dao
interface CityDao {

    @Query("SELECT * FROM cities WHERE countryCode = :code ORDER BY visitedAt DESC")
    fun getCitiesForCountry(code: String): LiveData<List<City>>

    @Query("SELECT * FROM cities ORDER BY visitedAt DESC")
    fun getAllCities(): LiveData<List<City>>

    @Query("SELECT COUNT(*) FROM cities")
    fun getCityCount(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM cities WHERE isCapital = 1")
    fun getCapitalCount(): LiveData<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(city: City): Long

    @Delete
    suspend fun delete(city: City)

    @Query("DELETE FROM cities WHERE countryCode = :code AND name = :name")
    suspend fun deleteByName(code: String, name: String)
}
