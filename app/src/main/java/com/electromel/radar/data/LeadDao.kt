package com.electromel.radar.data

import androidx.room.*

@Dao
interface LeadDao {
    @Query("SELECT * FROM leads")
    suspend fun getAll(): List<LeadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(lead: LeadEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(leads: List<LeadEntity>)

    @Query("DELETE FROM leads WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM leads")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM leads")
    suspend fun count(): Int
}
