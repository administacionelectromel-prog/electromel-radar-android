package com.electromel.radar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LeadEntity::class], version = 1, exportSchema = false)
abstract class RadarDatabase : RoomDatabase() {
    abstract fun leadDao(): LeadDao

    companion object {
        @Volatile private var INSTANCE: RadarDatabase? = null

        fun get(context: Context): RadarDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RadarDatabase::class.java,
                    "radar.db"
                ).build().also { INSTANCE = it }
            }
    }
}
