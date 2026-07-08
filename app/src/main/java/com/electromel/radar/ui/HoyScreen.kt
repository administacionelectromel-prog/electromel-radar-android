package com.electromel.radar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pantalla "Arrancar Día": resumen del día + los 8 mejores objetivos
 * segun DiaEngine. Tocar un objetivo abre su ficha.
 */
@Composable
fun HoyScreen(
    state: TerrenoState,
    onLeadClick: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().background(RadarColors.bg).padding(12.dp)) {

        Text("⚡ ARRANCAR DÍA", color = RadarColors.orange,
             fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))

        // Resumen del día (3 números)
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ResumenCard("${state.diaSeguimientos}", "seguim. hoy", RadarColors.orange, Modifier.weight(1f))
            ResumenCard("${state.diaUrgentes}", "urgentes", RadarColors.red, Modifier.weight(1f))
            ResumenCard("${state.diaSinContacto}", "sin contactar", RadarColors.accent, Modifier.weight(1f))
        }

        Text("OBJETIVOS SUGERIDOS HOY", color = RadarColors.textDim,
             fontSize = 10.sp, fontWeight = FontWeight.Bold,
             modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))

        if (state.objetivosDia.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Sin objetivos con coordenadas todavía.",
                     color = RadarColors.textDim, fontSize = 13.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(state.objetivosDia, key = { it.lead.id }) { obj ->
                    ObjetivoCard(obj, onLeadClick)
                }
            }
        }
    }
}

@Composable
private fun ResumenCard(num: String, label: String,
                        color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(
        modifier.background(RadarColors.bgPanel, RoundedCornerShape(8.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(num, color = color, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = RadarColors.textDim, fontSize = 10.sp)
    }
}

@Composable
private fun ObjetivoCard(obj: LeadUi, onLeadClick: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(RadarColors.bgCard, RoundedCornerShape(8.dp))
            .border(1.dp, RadarColors.border, RoundedCornerShape(8.dp))
            .clickable { onLeadClick(obj.lead.id) }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(obj.lead.nombre, color = RadarColors.text,
                 fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                 maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${obj.prioridad.ico} ${obj.prioridad.label}",
                 color = RadarColors.prioridadColor(obj.prioridad.nivel), fontSize = 11.sp)
        }
        Box(
            Modifier.background(RadarColors.orange.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                .padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Text("IUT ${obj.iut}", color = RadarColors.orange,
                 fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}
