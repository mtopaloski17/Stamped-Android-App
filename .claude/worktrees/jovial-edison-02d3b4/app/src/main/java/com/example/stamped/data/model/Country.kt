package com.example.stamped.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "countries")
data class Country(
    @PrimaryKey
    val code: String,        // ISO код пр. "MK"
    val name: String,        // Ime "North Macedonia"
    val continent: String,   // "Europe"
    val isVisited: Boolean = false,
    val visitedAt: Long? = null,
    val notes: String = ""
)