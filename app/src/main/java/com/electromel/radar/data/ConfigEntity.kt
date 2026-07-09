package com.electromel.radar.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Config key-value persistida (API key, plantillas WhatsApp). */
@Entity(tableName = "config")
data class ConfigEntity(
    @PrimaryKey val clave: String,
    val valor: String
)
