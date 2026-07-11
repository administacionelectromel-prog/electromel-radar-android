package com.electromel.radar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electromel.radar.domain.Estados

/* Badges compartidos — colores 1:1 de radar.css. */

private val Cyan = Color(0xFF00C8FF)
private val Purple = Color(0xFF9D5CFF)
private val OrangeSoft = Color(0xFFFF9020)
private val ScoreBajo = Color(0xFF4A6888)

/** estado-badge — .est-* (12 estados). */
@Composable
fun EstadoBadge(estado: String) {
    val (bg, fg) = when (estado) {
        "visitado"      -> Color(0x1A00C8FF) to Cyan
        "contactado"    -> Color(0x1F2D8FFF) to RadarColors.blue
        "respondio"     -> Color(0x1F9D5CFF) to Purple
        "presupuesto"   -> Color(0x1FFF9020) to OrangeSoft
        "esperando"     -> Color(0x1FF5C400) to RadarColors.yellow
        "revisita"      -> Color(0x1F00C8FF) to Cyan
        "cliente"       -> Color(0x2600E8A0) to RadarColors.accent
        "recurrente"    -> Color(0x26FF6B1A) to RadarColors.orange
        "mantenimiento" -> Color(0x33FF6B1A) to RadarColors.orange
        "urgente"       -> Color(0x26FF3355) to RadarColors.red
        "descartado"    -> Color(0x801E283C) to RadarColors.textDim
        else            -> Color(0x1F4A6888) to RadarColors.textDim   // est-no
    }
    Badge(Estados.LABEL[estado] ?: "—", bg, fg, Color.Transparent)
}

/** iut-badge — emoji por rango (🔴70/🟠45/🟡20/⚪) + ·valor, colores .iut-*. */
@Composable
fun IutBadge(iut: Int, prefijo: String = "") {
    val (emoji, fg, bg, borde) = when {
        iut >= 70 -> Cuad4("🔴", RadarColors.red, Color(0x1AFF3355), Color(0x4DFF3355))
        iut >= 45 -> Cuad4("🟠", OrangeSoft, Color(0x1AFF9020), Color(0x4DFF9020))
        iut >= 20 -> Cuad4("🟡", RadarColors.yellow, Color(0x1AF5C400), Color(0x4DF5C400))
        else      -> Cuad4("⚪", ScoreBajo, Color(0x1A4A6888), Color(0x4D4A6888))
    }
    Badge("$emoji$prefijo·$iut", bg, fg, borde)
}

/** nivel-badge — .nivel-* (mismos colores que HoyScreen). */
@Composable
fun NivelBadgeUi(nivel: String) {
    val (lbl, bg, fg, borde) = when (nivel) {
        "estrategico" -> Cuad4("ESTRATÉGICO", Color(0x33FF6B1A), RadarColors.orange, Color(0x66FF6B1A))
        "alto"        -> Cuad4("ALTO VALOR", Color(0x1F00E8A0), RadarColors.accent, Color(0x4D00E8A0))
        "medio"       -> Cuad4("MEDIO", Color(0x1FF5C400), RadarColors.yellow, Color(0x4DF5C400))
        else          -> Cuad4("BAJO", Color(0x1A4A6888), RadarColors.textDim, RadarColors.border)
    }
    Badge(lbl, bg, fg, borde)
}

private data class Cuad4(val a: String, val b: Color, val c: Color, val d: Color)

@Composable
private fun Badge(texto: String, bg: Color, fg: Color, borde: Color) {
    Text(texto, color = fg, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(bg)
            .border(1.dp, borde, RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp))
}
