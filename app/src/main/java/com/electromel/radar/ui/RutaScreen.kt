package com.electromel.radar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pantalla RUTA: recorrido optimizado por cercanía (RutaEngine).
 * Cada parada numerada; tap navega hasta ella. Botón regenerar desde los objetivos del día.
 */
@Composable
fun RutaScreen(
    state: TerrenoState,
    onGenerarRuta: () -> Unit,
    onLeadClick: (String) -> Unit
) {
    val ctx = LocalContext.current

    Column(Modifier.fillMaxSize().background(RadarColors.bg).padding(12.dp)) {

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("🧭 RUTA DEL DÍA", color = RadarColors.accent,
                     fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                if (state.ruta.isNotEmpty())
                    Text("${state.ruta.size} paradas · ${"%.1f".format(state.rutaDistanciaKm)} km",
                         color = RadarColors.textDim, fontSize = 12.sp)
            }
            Button(onClick = onGenerarRuta,
                   colors = ButtonDefaults.buttonColors(containerColor = RadarColors.orange)) {
                Text("↺ GENERAR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(10.dp))

        if (state.ruta.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tocá GENERAR para armar la ruta óptima\ncon los objetivos del día.",
                     color = RadarColors.textDim, fontSize = 13.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(state.ruta, key = { _, l -> l.lead.id }) { i, obj ->
                    ParadaRow(i + 1, obj) { onLeadClick(obj.lead.id) }
                }
            }
        }
    }
}

@Composable
private fun ParadaRow(orden: Int, obj: LeadUi, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(RadarColors.bgCard, RoundedCornerShape(8.dp))
            .border(1.dp, RadarColors.border, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Número de orden
        Box(
            Modifier.size(28.dp).background(RadarColors.accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("$orden", color = RadarColors.bg, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(obj.lead.nombre, color = RadarColors.text,
                 fontSize = 14.sp, fontWeight = FontWeight.Bold,
                 maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (obj.lead.direccion.isNotEmpty())
                Text("📍 ${obj.lead.direccion}", color = RadarColors.textDim,
                     fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("→", color = RadarColors.orange, fontSize = 18.sp)
    }
}
