package com.example.stamped.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.stamped.data.dao.CityDao
import com.example.stamped.data.dao.CountryDao
import com.example.stamped.data.model.City
import com.example.stamped.data.model.Country

@Database(
    entities = [Country::class, City::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun countryDao(): CountryDao
    abstract fun cityDao(): CityDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private var currentUserId: String? = null

        fun getDatabase(context: Context, userId: String): AppDatabase {
            if (INSTANCE != null && currentUserId == userId) return INSTANCE!!
            return synchronized(this) {
                if (currentUserId != userId) {
                    INSTANCE?.close()
                    INSTANCE = null
                }
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stamped_db_$userId"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                currentUserId = userId
                instance
            }
        }
    }
}
