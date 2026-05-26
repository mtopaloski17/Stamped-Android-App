package com.example.stamped.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "countries")
data class Country(
    @PrimaryKey
    val code: String,
    val name: String,
    val continent: String,
    val isVisited: Boolean = false,
    val visitedAt: Long? = null,
    val wantToVisit: Boolean = false,
    val wantToVisitAt: Long? = null,
    val notes: String = ""
)
