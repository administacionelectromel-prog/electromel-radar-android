package com.electromel.radar.data

import androidx.room.*

@Dao
interface ConfigDao {
    @Query("SELECT * FROM config")
    suspend fun getAll(): List<ConfigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(config: ConfigEntity)
}
