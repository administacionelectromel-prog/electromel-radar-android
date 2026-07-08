package com.electromel.radar.domain

/**
 * Prioridad táctica — port fiel de calcularPrioridadTactica() de la PWA.
 * Devuelve el nivel semántico; el color se resuelve en la capa Compose
 * (Theme), no acá. Esa separación es la que pedía el LOOP 11.
 */
object PrioridadEngine {

    enum class Nivel { HOY, SEMANA, REVISITA, BAJA }
    data class Prioridad(val nivel: Nivel, val ico: String, val label: String)

    /** diasDesde: días desde el último contacto (null si nunca). */
    fun calcular(lead: Lead, ahoraMs: Long = System.currentTimeMillis()): Prioridad {
        val iut = IutEngine.calcular(lead)
        val segMs = lead.seguimientoFecha?.let { parseIsoMs(it) }

        // CONTACTAR HOY
        if (lead.estado == "urgente")
            return Prioridad(Nivel.HOY, "🔴", "CONTACTAR HOY")
        if (segMs != null && segMs <= ahoraMs)
            return Prioridad(Nivel.HOY, "⏰", "CONTACTAR HOY")
        if (iut >= 70)
            return Prioridad(Nivel.HOY, "⚡", "CONTACTAR HOY")

        // ESTA SEMANA
        if (lead.estado == "presupuesto" || lead.estado == "esperando")
            return Prioridad(Nivel.SEMANA, "💰", "ESTA SEMANA")
        if (segMs != null) {
            val diasHasta = (segMs - ahoraMs) / 86_400_000.0
            if (diasHasta <= 7) return Prioridad(Nivel.SEMANA, "📅", "ESTA SEMANA")
        }
        if (iut >= 45)
            return Prioridad(Nivel.SEMANA, "🎯", "ESTA SEMANA")
        if (lead.estado in listOf("cliente", "recurrente", "mantenimiento"))
            return Prioridad(Nivel.REVISITA, "⭐", "REVISITA")

        // SIN URGENCIA
        return Prioridad(Nivel.BAJA, "🔵", "SIN URGENCIA")
    }

    /** Parsea ISO 8601 sin depender de librerías extra (formato de la PWA). */
    private fun parseIsoMs(iso: String): Long? = try {
        val instant = java.time.Instant.parse(
            if (iso.endsWith("Z") || iso.contains("+")) iso else iso + "Z"
        )
        instant.toEpochMilli()
    } catch (e: Exception) { null }
}
