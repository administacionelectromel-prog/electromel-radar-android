package com.electromel.radar.ui

import androidx.compose.foundation.background
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
    onImportar: () -> Unit
) {
    var filtro by remember { mutableStateOf(ExportEngine.Filtro.TODOS) }
    val leadsFiltrados = ExportEngine.filtrar(state.leads.map { it.lead }, filtro)

    Column(Modifier.fillMaxSize().background(RadarColors.bg).padding(12.dp)) {
        Text("⬇ EXPORTAR / IMPORTAR", color = RadarColors.accent, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
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
            FormatoBtn("📦 JSON", "json", filtro, onExportar, Modifier.weight(1f))
            FormatoBtn("📊 CSV", "csv", filtro, onExportar, Modifier.weight(1f))
            FormatoBtn("📋 TXT", "txt", filtro, onExportar, Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = RadarColors.border)
        Spacer(Modifier.height(16.dp))

        Text("IMPORTAR", color = RadarColors.textDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Button(onClick = onImportar, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = RadarColors.orange)) {
            Text("⬇ IMPORTAR JSON DE LA PWA", fontWeight = FontWeight.Bold, color = RadarColors.bg)
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
private fun FormatoBtn(label: String, formato: String, filtro: ExportEngine.Filtro,
                       onExportar: (String, ExportEngine.Filtro) -> Unit, modifier: Modifier) {
    Button(onClick = { onExportar(formato, filtro) }, modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = RadarColors.bgPanel)) {
        Text(label, color = RadarColors.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
