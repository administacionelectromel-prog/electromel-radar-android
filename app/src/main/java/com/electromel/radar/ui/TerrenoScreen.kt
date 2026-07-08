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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electromel.radar.domain.IutEngine

@Composable
fun TerrenoScreen(
    state: TerrenoState,
    onImportarClick: () -> Unit,
    onLeadClick: (String) -> Unit = {}
) {
    Column(
        Modifier.fillMaxSize().background(RadarColors.bg).padding(12.dp)
    ) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("⚡ TERRENO", color = RadarColors.accent,
                     fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text("${state.leads.size} objetivos", color = RadarColors.textDim, fontSize = 12.sp)
            }
            Button(
                onClick = onImportarClick,
                colors = ButtonDefaults.buttonColors(containerColor = RadarColors.orange)
            ) { Text("⬇ IMPORTAR", fontWeight = FontWeight.Bold) }
        }

        if (state.cargando) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RadarColors.accent)
            }
        } else if (state.leads.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.mensaje, color = RadarColors.textDim,
                     fontSize = 14.sp, modifier = Modifier.padding(24.dp))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.leads, key = { it.lead.id }) { LeadCard(it, onLeadClick) }
            }
        }
    }
}

@Composable
private fun LeadCard(item: LeadUi, onLeadClick: (String) -> Unit) {
    val prioColor = RadarColors.prioridadColor(item.prioridad.nivel)

    Column(
        Modifier.fillMaxWidth()
            .background(RadarColors.bgCard, RoundedCornerShape(10.dp))
            .border(1.dp, RadarColors.border, RoundedCornerShape(10.dp))
            .clickable { onLeadClick(item.lead.id) }
            .padding(12.dp)
    ) {
        // Barra de prioridad táctica
        Row(
            Modifier.fillMaxWidth()
                .background(prioColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                .border(1.dp, prioColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${item.prioridad.ico} ${item.prioridad.label}",
                 color = prioColor, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        }

        Spacer(Modifier.height(7.dp))

        // Nombre + IUT
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(item.lead.nombre, color = RadarColors.text,
                 fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                 maxLines = 1, overflow = TextOverflow.Ellipsis,
                 modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            IutBadge(item.iut)
        }

        // Dirección
        if (item.lead.direccion.isNotEmpty()) {
            Text("📍 ${item.lead.direccion}", color = RadarColors.textDim,
                 fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        // Teléfono
        if (item.lead.telefono.isNotEmpty()) {
            Text("📞 ${item.lead.telefono}", color = RadarColors.textDim, fontSize = 12.sp)
        }
    }
}

@Composable
private fun IutBadge(iut: Int) {
    val color = when {
        iut >= 70 -> RadarColors.red
        iut >= 45 -> RadarColors.yellow
        else      -> RadarColors.blue
    }
    Box(
        Modifier.background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text("IUT $iut", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
