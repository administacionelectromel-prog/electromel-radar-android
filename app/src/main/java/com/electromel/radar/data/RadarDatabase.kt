package com.electromel.radar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [LeadEntity::class, ConfigEntity::class, BackupEntity::class], version = 4, exportSchema = false)
abstract class RadarDatabase : RoomDatabase() {
    abstract fun leadDao(): LeadDao
    abstract fun configDao(): ConfigDao
    abstract fun backupDao(): BackupDao

    companion object {
        @Volatile private var INSTANCE: RadarDatabase? = null

        /** v3→v4: agrega la tabla backups SIN tocar leads/config (aditiva). */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS backups (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "fecha TEXT NOT NULL, tipo TEXT NOT NULL, " +
                    "datos TEXT NOT NULL, cantLeads INTEGER NOT NULL)")
            }
        }

        fun get(context: Context): RadarDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RadarDatabase::class.java,
                    "radar.db"
                ).addMigrations(MIGRATION_3_4)
                 .fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
