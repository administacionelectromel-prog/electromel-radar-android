package com.electromel.radar.ui

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
 * LEADS — réplica de sec-leads de la PWA: filtros (10) + buscador + lista.
 */
private val FILTROS = listOf(
    "todos" to "TODOS", "urgente" to "URGENTE", "alta" to "ALTA", "media" to "MEDIA",
    "baja" to "BAJA", "no-contactado" to "SIN CONTACTO", "contactado" to "CONTACTADO",
    "cliente" to "CLIENTE", "recurrente" to "RECURRENTE", "mantenimiento" to "MANTENIMIENTO"
)

@Composable
fun LeadsScreen(state: TerrenoState, onLeadClick: (String) -> Unit) {
    var filtro by remember { mutableStateOf("todos") }
    var busqueda by remember { mutableStateOf("") }

    val filtrados = state.leads.filter { ui ->
        val l = ui.lead
        val pasaFiltro = when (filtro) {
            "todos"        -> true
            "urgente"      -> l.estado == "urgente"
            "alta"         -> l.prioridad == "alta"
            "media"        -> l.prioridad == "media"
            "baja"         -> l.prioridad == "baja"
            "no-contactado"-> l.estado == "no-contactado"
            "contactado"   -> l.estado == "contactado"
            "cliente"      -> l.estado == "cliente"
            "recurrente"   -> l.estado == "recurrente"
            "mantenimiento"-> l.estado == "mantenimiento"
            else           -> true
        }
        val pasaBusqueda = busqueda.isBlank() ||
            l.nombre.contains(busqueda, true) || l.direccion.contains(busqueda, true)
        pasaFiltro && pasaBusqueda
    }

    Column(Modifier.fillMaxSize().background(RadarColors.bg).padding(12.dp)) {
        // Filtros scrolleables
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FILTROS.forEach { (key, label) ->
                val activo = filtro == key
                Box(Modifier.clip(RoundedCornerShape(16.dp))
                    .background(if (activo) RadarColors.accent else RadarColors.bgCard)
                    .border(1.dp, if (activo) RadarColors.accent else RadarColors.border, RoundedCornerShape(16.dp))
                    .clickable { filtro = key }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(label, color = if (activo) RadarColors.bg else RadarColors.textDim,
                         fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = busqueda, onValueChange = { busqueda = it },
            placeholder = { Text("Buscar por nombre o dirección...", color = RadarColors.textDim) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = RadarColors.text, unfocusedTextColor = RadarColors.text,
                focusedBorderColor = RadarColors.accent, unfocusedBorderColor = RadarColors.border,
                cursorColor = RadarColors.accent))
        Spacer(Modifier.height(8.dp))
        Text("${filtrados.size} leads", color = RadarColors.textDim, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))

        if (filtrados.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Sin leads con ese filtro.", color = RadarColors.textDim, fontSize = 13.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filtrados, key = { it.lead.id }) { ui -> LeadRowLeads(ui, onLeadClick) }
            }
        }
    }
}

@Composable
private fun LeadRowLeads(item: LeadUi, onLeadClick: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().background(RadarColors.bgCard, RoundedCornerShape(8.dp))
        .border(1.dp, RadarColors.border, RoundedCornerShape(8.dp))
        .clickable { onLeadClick(item.lead.id) }.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(item.lead.nombre, color = RadarColors.text, fontSize = 14.sp,
                 fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${item.prioridad.ico} ${item.prioridad.label} · ${item.lead.estado}",
                 color = RadarColors.prioridadColor(item.prioridad.nivel), fontSize = 11.sp,
                 maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.background(RadarColors.orange.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)) {
            Text("IUT ${item.iut}", color = RadarColors.orange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}
