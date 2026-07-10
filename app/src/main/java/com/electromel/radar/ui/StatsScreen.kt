package com.electromel.radar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * STATS — réplica de la sección sec-stats de la PWA:
 * MÉTRICAS OPERATIVAS (8) · EQUIPOS MÁS DETECTADOS · CONVERSIONES POR RUBRO
 * · ALERTAS DE REVISITA · BACKUPS Y DATOS.
 */
@Composable
fun StatsScreen(
    state: TerrenoState,
    onLeadClick: (String) -> Unit,
    onCampanaWhatsapp: () -> Unit,
    onExportarAvanzado: () -> Unit,
    onExportarJson: () -> Unit,
    onImportar: () -> Unit,
    onBorrarTodo: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(RadarColors.bg)
        .verticalScroll(rememberScrollState()).padding(12.dp)) {

        Text("📊 MÉTRICAS OPERATIVAS", color = RadarColors.accent,
             fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(10.dp))

        // Grid 8 métricas (2 columnas × 4 filas)
        val r = state.statsResumen
        MetricRow(
            "${r.total}" to ("TOTAL LEADS" to RadarColors.accent),
            "${r.contactados}" to ("CONTACTADOS" to RadarColors.blue)
        )
        Spacer(Modifier.height(6.dp))
        MetricRow(
            "${r.respondio}" to ("RESPONDIERON" to RadarColors.accent),
            "${r.clientes}" to ("CLIENTES" to RadarColors.orange)
        )
        Spacer(Modifier.height(6.dp))
        MetricRow(
            "${r.urgentes}" to ("URGENTES" to RadarColors.red),
            "${r.estrategicos}" to ("ESTRATÉGICOS" to RadarColors.orange)
        )
        Spacer(Modifier.height(6.dp))
        MetricRow(
            "${r.pendSeg}" to ("SEGUIM. HOY" to RadarColors.orange),
            "${r.conFotos}" to ("CON FOTOS" to RadarColors.blue)
        )

        // EQUIPOS MÁS DETECTADOS
        Spacer(Modifier.height(16.dp))
        Seccion("EQUIPOS MÁS DETECTADOS")
        if (state.statsEquipos.isEmpty()) {
            Vacio("Agregá equipos a los leads para ver estadísticas.")
        } else {
            state.statsEquipos.forEach { eq ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(eq.label, color = RadarColors.text, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Box(Modifier.weight(2f).height(8.dp)
                        .background(RadarColors.bgPanel, RoundedCornerShape(4.dp))) {
                        Box(Modifier.fillMaxWidth(eq.pct / 100f).fillMaxHeight()
                            .background(RadarColors.accent, RoundedCornerShape(4.dp)))
                    }
                    Text(" ${eq.cnt}", color = RadarColors.textDim, fontSize = 11.sp)
                }
            }
        }

        // CONVERSIONES POR RUBRO
        Spacer(Modifier.height(14.dp))
        Seccion("CONVERSIONES POR RUBRO")
        if (state.statsConversion.isEmpty()) {
            Vacio("Sin datos suficientes todavía.")
        } else {
            state.statsConversion.take(6).forEach { c ->
                Row(Modifier.fillMaxWidth().background(RadarColors.bgPanel, RoundedCornerShape(8.dp))
                    .padding(10.dp).padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(c.rubro.replaceFirstChar { it.uppercase() },
                         color = RadarColors.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("${c.clientes} cli · ${c.tasaContacto}% cont.",
                         color = RadarColors.textDim, fontSize = 11.sp)
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        // ALERTAS DE REVISITA
        Spacer(Modifier.height(14.dp))
        Seccion("ALERTAS DE REVISITA")
        if (state.statsRevisitas.isEmpty()) {
            Vacio("Sin revisitas pendientes en los próximos 30 días. ✓")
        } else {
            state.statsRevisitas.take(8).forEach { rev ->
                val color = when {
                    rev.diasHasta < 0  -> RadarColors.red
                    rev.diasHasta == 0 -> RadarColors.orange
                    rev.diasHasta <= 7 -> RadarColors.yellow
                    else               -> RadarColors.blue
                }
                val txt = when {
                    rev.diasHasta < 0  -> "Hace ${-rev.diasHasta} días"
                    rev.diasHasta == 0 -> "HOY"
                    else               -> "En ${rev.diasHasta} días"
                }
                Row(Modifier.fillMaxWidth().background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .clickable { onLeadClick(rev.lead.id) }.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(rev.lead.nombre, color = RadarColors.text, fontSize = 13.sp,
                             fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(rev.motivo, color = RadarColors.textDim, fontSize = 10.sp,
                             maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(txt, color = color, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        // BACKUPS Y DATOS
        Spacer(Modifier.height(16.dp))
        Seccion("BACKUPS Y DATOS")
        Spacer(Modifier.height(4.dp))
        BtnBlock("📱 CAMPAÑA WHATSAPP", RadarColors.blue, onCampanaWhatsapp)
        Spacer(Modifier.height(8.dp))
        BtnBlock("⬇ EXPORTAR / SINCRONIZAR", RadarColors.accent, onExportarAvanzado)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BtnSm("JSON completo", Modifier.weight(1f), onExportarJson)
            BtnSm("IMPORTAR", Modifier.weight(1f), onImportar)
        }
        Spacer(Modifier.height(12.dp))
        // Borrar todo (rojo, con confirmación)
        var confirmar by remember { androidx.compose.runtime.mutableStateOf(false) }
        OutlinedButton(
            onClick = { if (confirmar) { onBorrarTodo(); confirmar = false } else confirmar = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = RadarColors.red)
        ) { Text(if (confirmar) "¿SEGURO? TOCÁ DE NUEVO" else "BORRAR TODO", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun MetricRow(
    a: Pair<String, Pair<String, Color>>,
    b: Pair<String, Pair<String, Color>>
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        MetricCell(a.first, a.second.first, a.second.second, Modifier.weight(1f))
        MetricCell(b.first, b.second.first, b.second.second, Modifier.weight(1f))
    }
}

@Composable
private fun MetricCell(num: String, label: String, color: Color, modifier: Modifier) {
    Column(modifier.background(RadarColors.bgCard, RoundedCornerShape(10.dp))
        .padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(num, color = color, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = RadarColors.textDim, fontSize = 10.sp)
    }
}

@Composable
private fun Seccion(titulo: String) {
    Text(titulo, color = RadarColors.text, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun Vacio(txt: String) {
    Text(txt, color = RadarColors.textDim, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun BtnBlock(label: String, color: Color, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = color)) {
        Text(label, color = RadarColors.bg, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun BtnSm(label: String, modifier: Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = RadarColors.bgPanel)) {
        Text(label, color = RadarColors.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
