package com.electromel.radar.domain

/**
 * Motor "Arrancar Día" — port fiel de calcularScoreDia() y
 * generarObjetivosDia() de la PWA. Lógica pura, sin UI ni estado global.
 *
 * Score compuesto para elegir los mejores objetivos del día:
 *   seguimiento vencido +100, IUT×0.8, urgente +60, cliente +35,
 *   sin contactar con tel +40, días sin contacto +20/+15,
 *   proximidad GPS hasta +30.
 */
object DiaEngine {

    private fun limpiarTel(t: String) = t.filter { it.isDigit() || it == '+' }

    fun calcularScore(lead: Lead, userLat: Double?, userLon: Double?,
                      ahoraMs: Long = System.currentTimeMillis()): Int {
        var score = 0

        // Seguimiento vencido — máxima prioridad
        lead.seguimientoFecha?.let { seg ->
            parseIsoMs(seg)?.let { if (ahoraMs - it >= 0) score += 100 }
        }

        // IUT
        score += Math.round(IutEngine.calcular(lead) * 0.8).toInt()

        // Estado
        when (lead.estado) {
            "urgente"     -> score += 60
            "presupuesto" -> score += 25
            "esperando"   -> score += 20
        }
        if (lead.estado in listOf("cliente", "recurrente", "mantenimiento")) score += 35

        // Sin contacto nunca + tiene teléfono
        if (lead.estado == "no-contactado" && limpiarTel(lead.telefono).length >= 6) score += 40

        // Días desde último contacto
        if (lead.historial.isNotEmpty()) {
            val ultimo = parseIsoMs(lead.historial.last().fecha)
            if (ultimo != null) {
                val diasSin = (ahoraMs - ultimo) / 86_400_000.0
                if (diasSin > 7)  score += 20
                if (diasSin > 30) score += 15
            }
        } else {
            score += 20 // sin historial = nunca tocado
        }

        // Proximidad GPS — variables locales para smart-cast seguro (lat/lon son var)
        val lLat = lead.lat; val lLon = lead.lon
        if (userLat != null && userLon != null && lLat != null && lLon != null) {
            val dist = GeoUtils.distKm(userLat, userLon, lLat, lLon)
            if (dist <= 5) score += Math.round((1 - dist / 5) * 30).toInt()
        }

        return score
    }

    /** Los mejores [max] objetivos del día, ordenados por score desc. */
    fun generarObjetivos(leads: List<Lead>, userLat: Double?, userLon: Double?,
                         max: Int = 8, ahoraMs: Long = System.currentTimeMillis()): List<Pair<Lead, Int>> =
        leads
            .filter { it.lat != null && it.lon != null && it.estado != "descartado" }
            .map { it to calcularScore(it, userLat, userLon, ahoraMs) }
            .sortedByDescending { it.second }
            .take(max)

    private fun parseIsoMs(iso: String): Long? = try {
        java.time.Instant.parse(
            if (iso.endsWith("Z") || iso.contains("+")) iso else iso + "Z"
        ).toEpochMilli()
    } catch (e: Exception) { null }
}
