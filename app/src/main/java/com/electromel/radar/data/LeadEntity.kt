package com.electromel.radar.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.electromel.radar.domain.EventoHistorial
import com.electromel.radar.domain.Lead
import kotlinx.serialization.json.Json

/**
 * Entidad de persistencia. Campos escalares como columnas; listas
 * (equipos, tags, historial) serializadas a JSON en columnas TEXT.
 * El mapeo to/from dominio mantiene la capa de datos separada de la lógica.
 */
@Entity(tableName = "leads")
data class LeadEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val rubro: String,
    val tipo: String,
    val telefono: String,
    val direccion: String,
    val zona: String,
    val notas: String,
    val lat: Double?,
    val lon: Double?,
    val fuente: String,
    val estado: String,
    val equiposJson: String,
    val tagsJson: String,
    val web: String,
    val nivel: String,
    val prioridad: String,
    val googleId: String?,
    val osmId: Long?,
    val rating: Double,
    val intentosContacto: Int,
    val cicloMantenimiento: Int?,
    val seguimientoFecha: String?,
    val creado: String,
    val historialJson: String
) {
    fun toDomain(): Lead = Lead(
        id = id, nombre = nombre, rubro = rubro, tipo = tipo,
        telefono = telefono, direccion = direccion, zona = zona, notas = notas,
        lat = lat, lon = lon, fuente = fuente, estado = estado,
        equipos = JSON.decodeFromString(equiposJson),
        tags = JSON.decodeFromString(tagsJson),
        web = web, nivel = nivel, prioridad = prioridad,
        googleId = googleId, osmId = osmId, rating = rating,
        intentosContacto = intentosContacto, cicloMantenimiento = cicloMantenimiento,
        seguimientoFecha = seguimientoFecha, creado = creado,
        historial = JSON.decodeFromString<List<EventoHistorial>>(historialJson)
    )

    companion object {
        val JSON = Json { ignoreUnknownKeys = true }

        fun from(l: Lead): LeadEntity = LeadEntity(
            id = l.id, nombre = l.nombre, rubro = l.rubro, tipo = l.tipo,
            telefono = l.telefono, direccion = l.direccion, zona = l.zona, notas = l.notas,
            lat = l.lat, lon = l.lon, fuente = l.fuente, estado = l.estado,
            equiposJson = JSON.encodeToString(l.equipos),
            tagsJson = JSON.encodeToString(l.tags),
            web = l.web, nivel = l.nivel, prioridad = l.prioridad,
            googleId = l.googleId, osmId = l.osmId, rating = l.rating,
            intentosContacto = l.intentosContacto, cicloMantenimiento = l.cicloMantenimiento,
            seguimientoFecha = l.seguimientoFecha, creado = l.creado,
            historialJson = JSON.encodeToString(l.historial)
        )
    }
}
