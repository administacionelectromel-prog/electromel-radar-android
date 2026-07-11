package com.electromel.radar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electromel.radar.domain.ExportEngine

@Composable
fun ExportarScreen(
    state: TerrenoState,
    onExportar: (formato: String, filtro: ExportEngine.Filtro) -> Unit,
    onImportar: (merge: Boolean) -> Unit
) {
    var filtro by remember { mutableStateOf(ExportEngine.Filtro.TODOS) }
    val leadsFiltrados = ExportEngine.filtrar(state.leads.map { it.lead }, filtro)

    Column(Modifier.fillMaxSize().background(RadarColors.bg).padding(12.dp)) {
        Text("⬇ EXPORTAR DATOS", color = RadarColors.accent, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(12.dp))

        Text("¿QUÉ EXPORTAR?", color = RadarColors.textDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ExportEngine.Filtro.values().toList().chunked(2).forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    fila.forEach { fOpt ->
                        FiltroChip(fOpt.label, fOpt == filtro, Modifier.weight(1f)) { filtro = fOpt }
                    }
                    if (fila.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("${leadsFiltrados.size} leads seleccionados",
             color = RadarColors.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        Text("FORMATO", color = RadarColors.textDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FormatoBtn("📦", "JSON", "Backup completo, importable en otro dispositivo",
                       "json", filtro, onExportar, Modifier.weight(1f))
            FormatoBtn("📊", "CSV", "Excel / Google Sheets — una fila por lead",
                       "csv", filtro, onExportar, Modifier.weight(1f))
            FormatoBtn("📋", "TXT", "Lista de contactos: nombre + teléfono",
                       "txt", filtro, onExportar, Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = RadarColors.border)
        Spacer(Modifier.height(16.dp))

        Text("IMPORTAR DATOS", color = RadarColors.textDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ImportBtn("🔀", "MERGE", "Agrega sin borrar", RadarColors.border,
                      Modifier.weight(1f)) { onImportar(true) }
            ImportBtn("♻️", "REEMPLAZAR", "Sobreescribe todo",
                      RadarColors.red.copy(alpha = 0.5f),
                      Modifier.weight(1f)) { onImportar(false) }
        }
    }
}

@Composable
private fun FiltroChip(label: String, activo: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.background(if (activo) RadarColors.accent else RadarColors.bgPanel, RoundedCornerShape(8.dp))
        .clickable { onClick() }.padding(vertical = 10.dp),
        contentAlignment = Alignment.Center) {
        Text(label, color = if (activo) RadarColors.bg else RadarColors.text,
             fontSize = 12.sp, fontWeight = if (activo) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun FormatoBtn(ico: String, label: String, desc: String, formato: String,
                       filtro: ExportEngine.Filtro,
                       onExportar: (String, ExportEngine.Filtro) -> Unit, modifier: Modifier) {
    Column(modifier.background(RadarColors.bgPanel, RoundedCornerShape(8.dp))
        .clickable { onExportar(formato, filtro) }
        .padding(horizontal = 6.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(ico, fontSize = 14.sp)
        Text(label, color = RadarColors.text, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        Text(desc, color = RadarColors.textDim, fontSize = 9.sp, lineHeight = 11.sp,
             textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun ImportBtn(ico: String, label: String, desc: String,
                      borde: androidx.compose.ui.graphics.Color,
                      modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.background(RadarColors.bgPanel, RoundedCornerShape(8.dp))
        .border(1.dp, borde, RoundedCornerShape(8.dp))
        .clickable { onClick() }
        .padding(horizontal = 6.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(ico, fontSize = 13.sp)
        Text(label, color = RadarColors.text, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        Text(desc, color = RadarColors.textDim, fontSize = 9.sp)
    }
}
