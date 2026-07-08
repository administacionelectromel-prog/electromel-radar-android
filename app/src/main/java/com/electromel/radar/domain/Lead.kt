package com.electromel.radar.domain

/**
 * Modelo de dominio — equivalente 1:1 del objeto lead de la PWA.
 * Mismos nombres de campos para que import/export JSON sea directo
 * entre la PWA y la app nativa.
 */
data class Lead(
    val id: String,
    var nombre: String,
    var rubro: String = "comercio",
    var tipo: String = "",
    var telefono: String = "",
    var direccion: String = "",
    var zona: String = "",
    var notas: String = "",
    var lat: Double? = null,
    var lon: Double? = null,
    var fuente: String = "manual",        // manual | terreno | google | osm
    var estado: String = "no-contactado",
    var equipos: List<String> = emptyList(),
    var tags: List<String> = emptyList(),
    var web: String = "",
    var nivel: String = "bajo",
    var prioridad: String = "media",
    var googleId: String? = null,
    var osmId: Long? = null,
    var rating: Double = 0.0,
    var intentosContacto: Int = 0,
    var cicloMantenimiento: Int? = null,
    var seguimientoFecha: String? = null,  // ISO 8601
    var creado: String = "",
    var historial: List<EventoHistorial> = emptyList()
)

data class EventoHistorial(val fecha: String, val accion: String)
