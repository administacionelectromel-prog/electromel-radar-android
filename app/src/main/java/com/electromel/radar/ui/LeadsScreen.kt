package com.electromel.radar.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electromel.radar.domain.Lead
import com.electromel.radar.domain.StatsEngine
import java.util.Calendar

/**
 * LEADS — réplica 1:1 de sec-leads: 10 filtros + buscador multicampo +
 * leadsFiltrados() (descartados solo en 'todos', orden IUT desc → creado
 * desc) + card completa de renderLeads() con CONTACTAR/MAPS/+RUTA/ABRIR.
 */
private val FILTROS = listOf(
    "todos" to "TODOS", "urgente" to "URGENTE", "alta" to "ALTA", "media" to "MEDIA",
    "baja" to "BAJA", "no-contactado" to "SIN CONTACTO", "contactado" to "CONTACTADO",
    "cliente" to "CLIENTE", "recurrente" to "RECURRENTE", "mantenimiento" to "MANTENIMIENTO"
)
private val FILTROS_ESTADO = listOf("no-contactado","contactado","respondio",
    "cliente","recurrente","mantenimiento","urgente")

@Composable
fun LeadsScreen(
    state: TerrenoState,
    onLeadClick: (String) -> Unit,
    onWhatsAppPrimero: (Lead) -> Unit,
    onMaps: (Lead) -> Unit,
    onAgregarARuta: (String) -> Unit
) {
    var filtro by remember { mutableStateOf("todos") }
    var busqueda by remember { mutableStateOf("") }

    // Port 1:1 de leadsFiltrados()
    val q = busqueda.lowercase().trim()
    val filtrados = state.leadsTodos.filter { ui ->
        val l = ui.lead
        if (l.estado == "descartado" && filtro != "todos") return@filter false
        if (filtro == "alta" && l.prioridad != "alta") return@filter false
        if (filtro == "media" && l.prioridad != "media") return@filter false
        if (filtro == "baja" && l.prioridad != "baja") return@filter false
        if (filtro in FILTROS_ESTADO && l.estado != filtro) return@filter false
        if (q.isNotEmpty()) {
            val blob = listOf(l.nombre, l.direccion, l.tipo, l.notas,
                l.equipos.joinToString(" ")).joinToString(" ").lowercase()
            if (!blob.contains(q)) return@filter false
        }
        true
    }.sortedWith(compareByDescending<LeadUi> { it.iut }
        .thenByDescending { parseCreado(it.lead.creado) })

    Column(Modifier.fillMaxSize().background(RadarColors.bg).padding(12.dp)) {

        // filtros-leads (urgente con estilo em naranja, como .filtro-btn.em)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FILTROS.forEach { (key, label) ->
                val activo = filtro == key
                val em = key == "urgente"
                val colorActivo = if (em) RadarColors.orange else RadarColors.accent
                Box(Modifier.clip(RoundedCornerShape(16.dp))
                    .background(if (activo) colorActivo else RadarColors.bgCard)
                    .border(1.dp, if (activo) colorActivo
                            else if (em) RadarColors.orange.copy(alpha = 0.5f) else RadarColors.border,
                            RoundedCornerShape(16.dp))
                    .clickable { filtro = key }
                    .padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(label,
                        color = if (activo) RadarColors.bg
                                else if (em) RadarColors.orange else RadarColors.textDim,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        // inp-buscar-leads
        OutlinedTextField(value = busqueda, onValueChange = { busqueda = it },
            placeholder = { Text("Filtrar leads...", color = RadarColors.textDim) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = RadarColors.text, unfocusedTextColor = RadarColors.text,
                focusedBorderColor = RadarColors.accent, unfocusedBorderColor = RadarColors.border,
                cursorColor = RadarColors.accent))
        Spacer(Modifier.height(8.dp))

        when {
            state.leadsTodos.isEmpty() ->
                EmptyLeads("📡", "Sin leads. Buscá objetivos o tocá el mapa para agregar.")
            filtrados.isEmpty() ->
                EmptyLeads("🔍", "Sin leads con estos filtros.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtrados, key = { it.lead.id }) { ui ->
                    LeadCard(ui, onLeadClick, onWhatsAppPrimero, onMaps, onAgregarARuta)
                }
            }
        }
    }
}

@Composable
private fun EmptyLeads(ico: String, texto: String) {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Text("$ico $texto", color = RadarColors.textDim, fontSize = 12.sp)
    }
}

/* .card de renderLeads — completa, 1:1 */
@Composable
private fun LeadCard(
    ui: LeadUi,
    onLeadClick: (String) -> Unit,
    onWhatsAppPrimero: (Lead) -> Unit,
    onMaps: (Lead) -> Unit,
    onAgregarARuta: (String) -> Unit
) {
    val l = ui.lead
    val tieneTel = l.telefono.filter { it.isDigit() || it == '+' }.length >= 6
    val srcLbl = l.fuente.ifBlank { "manual" }.uppercase()

    Column(Modifier.fillMaxWidth()
        .background(RadarColors.bgCard, RoundedCornerShape(10.dp))
        .border(1.dp, RadarColors.border, RoundedCornerShape(10.dp))
        .padding(10.dp)) {

        // card-header: nombre | columna derecha (IUT + estado)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Text(l.nombre, color = RadarColors.text, fontSize = 14.sp,
                 fontWeight = FontWeight.ExtraBold, maxLines = 1,
                 overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(6.dp))
            Column(horizontalAlignment = Alignment.End,
                   verticalArrangement = Arrangement.spacedBy(3.dp)) {
                IutBadge(ui.iut)
                EstadoBadge(l.estado)
            }
        }

        // fila: src-badge + nivel + equipos (≤3, solo íconos)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(srcLbl, color = RadarColors.textDim, fontSize = 9.sp,
                 fontWeight = FontWeight.Bold,
                 modifier = Modifier.clip(RoundedCornerShape(4.dp))
                     .background(RadarColors.bgPanel)
                     .border(1.dp, RadarColors.border, RoundedCornerShape(4.dp))
                     .padding(horizontal = 5.dp, vertical = 1.dp))
            NivelBadgeUi(l.nivel.ifBlank { "bajo" })
            l.equipos.take(3).forEach { e ->
                StatsEngine.EQUIPOS_CATALOGO[e]?.let { (ico, _) ->
                    Text(ico, fontSize = 12.sp)
                }
            }
        }

        // 📍 dirección · 📞 teléfono (bold)
        if (l.direccion.isNotBlank())
            Text("📍 ${l.direccion}", color = RadarColors.textDim, fontSize = 11.sp,
                 maxLines = 1, overflow = TextOverflow.Ellipsis,
                 modifier = Modifier.padding(top = 3.dp))
        if (l.telefono.isNotBlank())
            Row(Modifier.padding(top = 2.dp)) {
                Text("📞 ", color = RadarColors.textDim, fontSize = 11.sp)
                Text(l.telefono, color = RadarColors.text, fontSize = 11.sp,
                     fontWeight = FontWeight.Bold)
            }

        // nota-chip (55 chars)
        if (l.notas.isNotBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(l.notas.take(55) + if (l.notas.length > 55) "…" else "",
                 color = RadarColors.yellow, fontSize = 11.sp, fontStyle = FontStyle.Italic,
                 maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        // ⏰ SEGUIMIENTO HOY (naranja) si vencido
        if (!l.seguimientoFecha.isNullOrBlank() && esHoyOAtrasadoLeads(l.seguimientoFecha)) {
            Text("⏰ SEGUIMIENTO HOY", color = RadarColors.orange, fontSize = 10.sp,
                 fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 3.dp))
        }

        // card-actions: CONTACTAR (em, si tel) / MAPS / + RUTA + ABRIR/EDITAR (block)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            if (tieneTel)
                BtnLeads("CONTACTAR", RadarColors.orange, RadarColors.bg) { onWhatsAppPrimero(l) }
            BtnLeads("MAPS", RadarColors.bgPanel, RadarColors.text) { onMaps(l) }
            BtnLeads("+ RUTA", RadarColors.bgPanel, RadarColors.text) { onAgregarARuta(l.id) }
        }
        Spacer(Modifier.height(5.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
            .background(RadarColors.bgPanel)
            .border(1.dp, RadarColors.border, RoundedCornerShape(6.dp))
            .clickable { onLeadClick(l.id) }
            .padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
            Text("ABRIR / EDITAR", color = RadarColors.text, fontSize = 11.sp,
                 fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BtnLeads(label: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(bg)
        .border(1.dp, RadarColors.border, RoundedCornerShape(6.dp))
        .clickable { onClick() }
        .padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

private fun esHoyOAtrasadoLeads(iso: String?): Boolean {
    val m = parseCreado(iso ?: return false).takeIf { it > 0 } ?: return false
    val finDia = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
    }.timeInMillis
    return m <= finDia
}

private fun parseCreado(iso: String?): Long = try {
    if (iso.isNullOrBlank()) 0L
    else java.time.Instant.parse(
        if (iso.endsWith("Z") || iso.contains("+")) iso else iso + "Z"
    ).toEpochMilli()
} catch (e: Exception) { 0L }
