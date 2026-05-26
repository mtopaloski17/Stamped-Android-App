package com.example.stamped.data.remote

data class CountryInfoDto(
    val name: NameDto,
    val capital: List<String>?,
    val population: Long,
    val currencies: Map<String, CurrencyDto>?,
    val languages: Map<String, String>?,
    val region: String?,
    val subregion: String?,
    val area: Double,
    val timezones: List<String>?,
    val borders: List<String>?,
    val flags: FlagsDto?,
    val maps: MapsDto?
)

data class NameDto(
    val common: String,
    val official: String
)

data class CurrencyDto(
    val name: String,
    val symbol: String?
)

data class FlagsDto(
    val png: String?,
    val svg: String?
)

data class MapsDto(
    val googleMaps: String?,
    val openStreetMaps: String?
)
