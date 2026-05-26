package com.example.stamped.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cities",
    indices = [Index(value = ["countryCode", "name"], unique = true)]
)
data class City(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val countryCode: String,
    val name: String,
    val isCapital: Boolean = false,
    val visitedAt: Long = System.currentTimeMillis()
)
