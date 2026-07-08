package com.electromel.radar.ui

import androidx.compose.ui.graphics.Color

/** Paleta portada de radar.css */
object RadarColors {
    val bg        = Color(0xFF04080F)
    val bgCard    = Color(0xFF0A1220)
    val bgPanel   = Color(0xFF0D1626)
    val accent    = Color(0xFF00E8A0)  // verde
    val orange    = Color(0xFFFF6B1A)
    val red        = Color(0xFFFF3355)
    val yellow    = Color(0xFFF5C400)
    val blue      = Color(0xFF4A6888)
    val textDim   = Color(0xFF6B7D95)
    val text      = Color(0xFFE0E8F0)
    val border    = Color(0xFF1A2740)

    /** Color del nivel de prioridad — reemplaza el CSS inline de la PWA. */
    fun prioridadColor(nivel: com.electromel.radar.domain.PrioridadEngine.Nivel): Color =
        when (nivel) {
            com.electromel.radar.domain.PrioridadEngine.Nivel.HOY      -> red
            com.electromel.radar.domain.PrioridadEngine.Nivel.SEMANA   -> yellow
            com.electromel.radar.domain.PrioridadEngine.Nivel.REVISITA -> accent
            com.electromel.radar.domain.PrioridadEngine.Nivel.BAJA     -> blue
        }
}
