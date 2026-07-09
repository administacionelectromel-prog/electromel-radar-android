package com.electromel.radar.domain

/**
 * Construcción de lead en terreno — port de construirLeadCaptura() de la PWA.
 * Recibe datos planos del formulario, deriva estado/prioridad/nivel.
 * Lógica pura: sin UI, sin GPS (las coords se pasan como parámetro).
 */
object CapturaEngine {

    fun construir(
        nombre: String,
        tipo: String,
        equipos: List<String>,
        tags: List<String>,
        telefono: String,
        lat: Double?,
        lon: Double?,
        idGenerado: String,
        ahoraIso: String
    ): Lead {
        val esUrgente = "urgente" in tags
        val esNoSirve = "no_sirve" in tags

        var lead = Lead(
            id = idGenerado,
            nombre = nombre,
            rubro = tipo,
            tipo = tipo,
            telefono = telefono,
            direccion = "",
            zona = "",
            notas = tags.joinToString(", "),
            lat = lat, lon = lon,
            fuente = if (lat != null) "terreno" else "manual",
            estado = when {
                esUrgente -> "urgente"
                esNoSirve -> "descartado"
                else      -> "no-contactado"
            },
            equipos = equipos,
            tags = tags,
            nivel = "bajo",
            prioridad = "media",
            intentosContacto = 0,
            cicloMantenimiento = null,
            seguimientoFecha = null,
            creado = ahoraIso,
            historial = listOf(EventoHistorial(ahoraIso, "Lead creado en terreno"))
        )
        // Derivar nivel según equipos (aprox. de la PWA: 2+ equipos = medio)
        val nivel = if (equipos.size >= 2) "medio" else "bajo"
        val prioridad = if (lead.estado == "urgente") "alta" else "media"
        return lead.copy(nivel = nivel, prioridad = prioridad)
    }
}
