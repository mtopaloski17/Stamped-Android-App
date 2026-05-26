package com.example.stamped.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RestCountriesApi {

    @GET("v3.1/alpha/{code}")
    suspend fun getCountryByCode(
        @Path("code") code: String,
        @Query("fields") fields: String = "name,capital,population,currencies,languages,region,subregion,area,timezones,borders,flags,maps"
    ): CountryInfoDto
}
