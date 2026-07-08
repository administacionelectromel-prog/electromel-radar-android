package com.electromel.radar.domain

import java.text.Normalizer

/**
 * Índice de Urgencia Técnica — port fiel de calcularIUT() de app.js.
 * Misma tabla de keywords, mismos pesos, mismo tope de 99.
 */
object IutEngine {

    private val KEYWORDS = mapOf(
        "soldadora" to 20, "inverter" to 20, "variador" to 18, "tablero" to 15, "motor" to 15,
        "bomba" to 15, "compresor" to 12, "generador" to 18, "horno" to 15, "placa" to 12,
        "gym" to 18, "gimnasio" to 18, "fitness" to 18, "cinta" to 15,
        "hotel" to 20, "hostel" to 16, "motel" to 14, "alojamiento" to 12,
        "industrial" to 22, "industria" to 20, "fabrica" to 18, "metalurgica" to 20,
        "taller" to 16, "constructora" to 14, "corralon" to 12,
        "lavadero" to 15, "laundry" to 15, "mantenimiento" to 12, "reparacion" to 12,
        "24hs" to 10, "24h" to 10, "urgente" to 15, "falla" to 18
    )

    fun normalizar(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "").trim()

    fun calcular(lead: Lead): Int {
        var score = 0

        val texto = normalizar(listOf(
            lead.nombre, lead.tipo, lead.rubro, lead.notas,
            lead.equipos.joinToString(" "), lead.tags.joinToString(" ")
        ).joinToString(" "))

        for ((key, value) in KEYWORDS) if (texto.contains(key)) score += value

        score += lead.equipos.size * 8
        if ("soldadora" in lead.equipos) score += 15
        if ("cinta" in lead.equipos)     score += 12
        if ("variador" in lead.equipos)  score += 12
        if ("tablero" in lead.equipos)   score += 10

        if ("urgente" in lead.tags)           score += 25
        if ("alto_potencial" in lead.tags)    score += 20
        if ("equipo_detectado" in lead.tags)  score += 15
        if ("maquinas_viejas" in lead.tags)   score += 20
        if ("buen_cliente" in lead.tags)      score += 15
        if ("sin_mantenimiento" in lead.tags) score += 18
        if ("no_sirve" in lead.tags)          score = maxOf(0, score - 40)

        if (lead.web.isEmpty())              score += 8
        if (lead.fuente == "terreno")        score += 18
        when (lead.estado) {
            "cliente"       -> score += 15
            "recurrente"    -> score += 25
            "mantenimiento" -> score += 20
            "urgente"       -> score += 30
        }
        when (lead.nivel) {
            "estrategico" -> score += 20
            "alto"        -> score += 10
        }

        return minOf(score, 99)
    }
}
