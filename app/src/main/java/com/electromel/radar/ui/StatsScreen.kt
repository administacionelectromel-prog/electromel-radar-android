package com.electromel.radar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatsScreen(state: TerrenoState, onLeadClick: (String) -> Unit) {
    Column(Modifier.fillMaxSize().background(RadarColors.bg)
        .verticalScroll(rememberScrollState()).padding(12.dp)) {

        Text("📊 ESTADÍSTICAS", color = RadarColors.accent, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(10.dp))

        // Grid de resumen
        val r = state.statsResumen
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatCell("${r.total}", "TOTAL", RadarColors.text, Modifier.weight(1f))
            StatCell("${r.contactados}", "CONTACT.", RadarColors.blue, Modifier.weight(1f))
            StatCell("${r.clientes}", "CLIENTES", RadarColors.accent, Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatCell("${r.urgentes}", "URGENTES", RadarColors.red, Modifier.weight(1f))
            StatCell("${r.respondio}", "RESPOND.", RadarColors.yellow, Modifier.weight(1f))
            StatCell("${r.pendSeg}", "SEG. HOY", RadarColors.orange, Modifier.weight(1f))
        }

        // Conversión por rubro
        if (state.statsConversion.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("CONVERSIÓN POR RUBRO", color = RadarColors.textDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            state.statsConversion.take(6).forEach { c ->
                Column(Modifier.fillMaxWidth()
                    .background(RadarColors.bgPanel, RoundedCornerShape(8.dp))
                    .padding(10.dp).padding(bottom = 2.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(c.rubro.replaceFirstChar { it.uppercase() },
                             color = RadarColors.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("${c.clientes} cli · ${c.tasaContacto}% cont.",
                             color = RadarColors.textDim, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        // Alertas de revisita
        if (state.statsRevisitas.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text("ALERTAS DE REVISITA", color = RadarColors.textDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            state.statsRevisitas.take(8).forEach { rev ->
                val color = when {
                    rev.diasHasta < 0  -> RadarColors.red
                    rev.diasHasta == 0 -> RadarColors.orange
                    rev.diasHasta <= 7 -> RadarColors.yellow
                    else               -> RadarColors.blue
                }
                val txtFecha = when {
                    rev.diasHasta < 0  -> "Hace ${-rev.diasHasta} días"
                    rev.diasHasta == 0 -> "HOY"
                    else               -> "En ${rev.diasHasta} días"
                }
                Row(Modifier.fillMaxWidth()
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .clickable { onLeadClick(rev.lead.id) }
                    .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(rev.lead.nombre, color = RadarColors.text, fontSize = 13.sp,
                             fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(rev.motivo, color = RadarColors.textDim, fontSize = 10.sp,
                             maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(txtFecha, color = color, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun StatCell(num: String, label: String, color: Color, modifier: Modifier) {
    Column(modifier.background(RadarColors.bgCard, RoundedCornerShape(8.dp)).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(num, color = color, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = RadarColors.textDim, fontSize = 9.sp)
    }
}
