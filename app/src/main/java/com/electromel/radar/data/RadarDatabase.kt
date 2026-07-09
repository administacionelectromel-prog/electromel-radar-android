package com.electromel.radar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LeadEntity::class, ConfigEntity::class], version = 2, exportSchema = false)
abstract class RadarDatabase : RoomDatabase() {
    abstract fun leadDao(): LeadDao
    abstract fun configDao(): ConfigDao

    companion object {
        @Volatile private var INSTANCE: RadarDatabase? = null

        fun get(context: Context): RadarDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RadarDatabase::class.java,
                    "radar.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
