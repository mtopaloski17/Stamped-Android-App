package com.example.stamped.data.remote

data class CountriesNowRequest(
    val country: String
)

data class CountriesNowResponse(
    val error: Boolean = false,
    val msg: String? = null,
    val data: List<String> = emptyList()
)
