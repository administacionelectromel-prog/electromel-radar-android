package com.electromel.radar.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Estructura del JSON que exporta la PWA:
 * { version, fecha, leads: [...], ruta: [...], mensajes: {...} }
 * Solo nos interesan los leads en esta fase.
 */
@Serializable
data class BackupPwa(
    val version: Int = 6,
    val fecha: String = "",
    val leads: List<Lead> = emptyList()
)

object BackupParser {
    private val json = Json {
        ignoreUnknownKeys = true   // tolera campos que no mapeamos (ruta, mensajes, fotos)
        coerceInputValues = true   // null → valor por defecto
        isLenient = true
    }

    /** Devuelve los leads del backup, o lista vacía si el archivo es inválido. */
    fun parsearLeads(contenido: String): Result<List<Lead>> = try {
        Result.success(json.decodeFromString<BackupPwa>(contenido).leads)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
