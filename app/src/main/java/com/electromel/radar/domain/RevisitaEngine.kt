package com.electromel.radar.domain

/** Port de clasificarRevisita — el mapeo a colores va en la capa Compose. */
object RevisitaEngine {
    enum class Nivel { ATRASADA, HOY, PRONTO, FUTURA }
    data class Clasificacion(val nivel: Nivel, val textoFecha: String)

    fun clasificar(diasHasta: Int): Clasificacion = when {
        diasHasta < 0  -> Clasificacion(Nivel.ATRASADA, "Hace ${-diasHasta} días")
        diasHasta == 0 -> Clasificacion(Nivel.HOY, "HOY")
        diasHasta <= 7 -> Clasificacion(Nivel.PRONTO, "En $diasHasta días")
        else           -> Clasificacion(Nivel.FUTURA, "En $diasHasta días")
    }
}
