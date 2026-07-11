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
import androidx.compose.material3.Text
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
import androidx.compose.ui.zIndex
import com.electromel.radar.domain.Lead
import com.electromel.radar.domain.Mensajes
import com.electromel.radar.domain.PrioridadEngine

/**
 * CAMPAÑA WHATSAPP — port 1:1 de abrirCampanaWhatsApp():
 * header azul + ✕ · tipo (Primer contacto / Seguimiento / Cierre-Propuesta)
 * · filtro (6 opciones exactas, aplicado con APLICAR) · 'N contacto(s)
 * seleccionados' · card (nombre, 📞, IUT + prioridad, preview 3 líneas,
 * 📤 ENVIAR / SALTAR) · enviado → '✓ ENVIADO' opaca · empty 📭.
 */
private val TIPOS_CAMPANA = listOf(
    "primero" to "Primer contacto",
    "seguimiento" to "Seguimiento",
    "cierre" to "Cierre / Propuesta"
)
private val FILTROS_CAMPANA = listOf(
    "todos" to "Todos con tel.", "no-contactado" to "Sin contactar",
    "contactado" to "Contactados", "seguimiento-hoy" to "Seguimiento hoy",
    "alta" to "Prioridad alta", "cliente" to "Clientes"
)

@Composable
fun CampanaScreen(
    state: TerrenoState,
    onEnviar: (Lead, String) -> Unit,
    onCerrar: () -> Unit
) {
    var tipo by remember { mutableStateOf("primero") }
    var filtroSel by remember { mutableStateOf("todos") }
    var filtroAplicado by remember { mutableStateOf("todos") }
    var enviados by remember { mutableStateOf(setOf<String>()) }
    var saltados by remember { mutableStateOf(setOf<String>()) }

    // Base: tel ≥ 6 (state.leads ya excluye descartados) — port exacto
    val conTel = state.leads.filter {
        it.lead.telefono.filter { c -> c.isDigit() || c == '+' }.length >= 6
    }
    val ahora = System.currentTimeMillis()
    val filtrados = conTel.filter { ui ->
        val l = ui.lead
        when (filtroAplicado) {
            "no-contactado" -> l.estado == "no-contactado"
            "contactado" -> l.estado in listOf("contactado", "respondio", "visitado")
            "seguimiento-hoy" -> l.seguimientoFecha?.let { s ->
                try { java.time.Instant.parse(
                        if (s.endsWith("Z") || s.contains("+")) s else s + "Z")
                        .toEpochMilli() <= ahora } catch (e: Exception) { false }
            } ?: false
            "alta" -> l.prioridad == "alta" || ui.iut >= 45
            "cliente" -> l.estado in listOf("cliente", "recurrente", "mantenimiento")
            else -> true
        }
    }.sortedByDescending { it.iut }
     .filter { it.lead.id !in saltados }

    Column(Modifier.fillMaxSize().zIndex(35f).background(RadarColors.bg)
        .statusBarsPadding().navigationBarsPadding()) {

        // Header (bg-card, borde inferior)
        Column(Modifier.fillMaxWidth().background(RadarColors.bgCard).padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("📱 CAMPAÑA WHATSAPP", color = RadarColors.blue, fontSize = 17.sp,
                     fontWeight = FontWeight.ExtraBold)
                Text("✕", color = RadarColors.textDim, fontSize = 18.sp,
                     fontWeight = FontWeight.Bold,
                     modifier = Modifier.clickable { onCerrar() }.padding(4.dp))
            }
            Spacer(Modifier.height(10.dp))

            // Tipo de mensaje (cambia el preview en vivo, como la PWA)
            FilaChips(TIPOS_CAMPANA, tipo) { tipo = it }
            Spacer(Modifier.height(6.dp))
            // Filtro (se aplica con APLICAR)
            FilaChips(FILTROS_CAMPANA, filtroSel) { filtroSel = it }
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("${filtrados.size} contacto(s) seleccionados",
                     color = RadarColors.textDim, fontSize = 11.sp)
                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(RadarColors.blue)
                    .clickable { filtroAplicado = filtroSel }
                    .padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("APLICAR", color = Color.White, fontSize = 11.sp,
                         fontWeight = FontWeight.Bold)
                }
            }
        }
        HorizontalDividerLinea()

        if (filtrados.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("📭 Sin leads con ese filtro.", color = RadarColors.textDim, fontSize = 12.sp)
            }
        } else {
            LazyColumn(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtrados, key = { it.lead.id }) { ui ->
                    CampanaCard(ui, tipo, state.mensajes,
                        enviado = ui.lead.id in enviados,
                        onEnviar = {
                            onEnviar(ui.lead, tipo)
                            enviados = enviados + ui.lead.id
                        },
                        onSaltar = { saltados = saltados + ui.lead.id })
                }
            }
        }
    }
}

@Composable
private fun FilaChips(opciones: List<Pair<String, String>>, sel: String,
                      onSel: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        opciones.forEach { (id, label) ->
            val activo = id == sel
            Box(Modifier.clip(RoundedCornerShape(8.dp))
                .background(if (activo) RadarColors.blue.copy(alpha = 0.2f)
                            else RadarColors.bgPanel)
                .border(1.dp, if (activo) RadarColors.blue else RadarColors.border,
                        RoundedCornerShape(8.dp))
                .clickable { onSel(id) }
                .padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text(label, color = if (activo) RadarColors.blue else RadarColors.text,
                     fontSize = 11.sp,
                     fontWeight = if (activo) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

/* Card 1:1 de buildCardCampanaHTML */
@Composable
private fun CampanaCard(
    ui: LeadUi, tipo: String, mensajes: Map<String, Map<String, String>>,
    enviado: Boolean, onEnviar: () -> Unit, onSaltar: () -> Unit
) {
    val l = ui.lead
    val msg = Mensajes.build(l, tipo, mensajes)
    val lineas = msg.split("\n")
    val preview = lineas.take(3).joinToString("\n") + if (lineas.size > 3) "\n..." else ""

    Column(Modifier.fillMaxWidth()
        .background(RadarColors.bgPanel, RoundedCornerShape(10.dp))
        .border(1.dp, RadarColors.border, RoundedCornerShape(10.dp))
        .padding(horizontal = 12.dp, vertical = 11.dp)
        .let { if (enviado) it else it }) {

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(l.nombre, color = RadarColors.text.copy(
                        alpha = if (enviado) 0.45f else 1f),
                     fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                     maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("📞 ${l.telefono}", color = RadarColors.textDim, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End,
                   verticalArrangement = Arrangement.spacedBy(3.dp)) {
                IutBadge(ui.iut)
                PrioBadgeMini(ui.prioridad)
            }
        }
        Spacer(Modifier.height(6.dp))

        // Preview del mensaje (italic, border-left azul)
        Row(Modifier.fillMaxWidth()
            .background(RadarColors.blue.copy(alpha = 0.06f),
                        RoundedCornerShape(topEnd = 5.dp, bottomEnd = 5.dp))) {
            Box(Modifier.width(2.dp).height(52.dp).background(RadarColors.blue))
            Text(preview, color = RadarColors.textDim, fontSize = 11.sp,
                 fontStyle = FontStyle.Italic, lineHeight = 15.sp, maxLines = 4,
                 overflow = TextOverflow.Ellipsis,
                 modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
        }
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.weight(1f).clip(RoundedCornerShape(6.dp))
                .background(if (enviado) RadarColors.bgCard else RadarColors.blue)
                .clickable(enabled = !enviado) { onEnviar() }
                .padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Text(if (enviado) "✓ ENVIADO" else "📤 ENVIAR",
                     color = if (enviado) RadarColors.textDim else Color.White,
                     fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            }
            if (!enviado)
                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(RadarColors.bgCard)
                    .border(1.dp, RadarColors.border, RoundedCornerShape(6.dp))
                    .clickable { onSaltar() }
                    .padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Text("SALTAR", color = RadarColors.text, fontSize = 11.sp,
                         fontWeight = FontWeight.Bold)
                }
        }
    }
}

@Composable
private fun PrioBadgeMini(prio: PrioridadEngine.Prioridad) {
    val fg = when (prio.nivel) {
        PrioridadEngine.Nivel.HOY -> RadarColors.red
        PrioridadEngine.Nivel.SEMANA -> RadarColors.yellow
        PrioridadEngine.Nivel.REVISITA -> RadarColors.accent
        else -> Color(0xFF4A6888)
    }
    Text("${prio.ico} ${prio.label}", color = fg, fontSize = 9.sp,
         fontWeight = FontWeight.Bold,
         modifier = Modifier.clip(RoundedCornerShape(4.dp))
             .background(fg.copy(alpha = 0.1f))
             .border(1.dp, fg.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
             .padding(horizontal = 6.dp, vertical = 2.dp))
}

@Composable
private fun HorizontalDividerLinea() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(RadarColors.border))
}
