package com.example.stamped.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

interface CountriesNowApi {

    /**
     * Враќа листа на сите градови во земјата.
     * Body: { "country": "Serbia" }
     * Response: { "error": false, "data": ["Belgrade", "Novi Sad", ...] }
     */
    @POST("countries/cities")
    suspend fun citiesByCountry(@Body body: CountriesNowRequest): CountriesNowResponse
}
