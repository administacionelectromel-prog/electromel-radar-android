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
import com.electromel.radar.domain.GeoUtils
import com.electromel.radar.domain.IutEngine
import com.electromel.radar.domain.Lead
import com.electromel.radar.domain.PrioridadEngine
import com.electromel.radar.domain.StatsEngine

/**
 * TERRENO — réplica 1:1 de sec-terreno:
 * Panel ⚡ARRANCAR DÍA (resumen 3 stats + objetivos + REGENERAR/INICIAR) ·
 * header DETECCIÓN TÉCNICA ACTIVA + botón ⚡ · terreno-status ·
 * lista radio 1.5km (IUT desc → dist asc) · card completa (prioBar con
 * días sin contacto, ◈dist, src, nivel, equipos, 📝, NAVEGAR/WHATSAPP +
 * 📞✅📝🗺️🚨✏️) · sheet NOTA RÁPIDA (9 chips + textarea).
 */
private const val TERRENO_RADIO = 1.5

private val NOTAS_TERRENO = listOf(
    "✅ Visitado", "❌ Sin interés", "🔧 Equipo visto", "⚡ Soldadora",
    "🏋️ Cintas dañadas", "💰 Buen potencial", "⭐ MUY INTERESANTE",
    "📅 Volver después", "🚨 URGENTE"
)

@Composable
fun TerrenoScreen(
    state: TerrenoState,
    onLeadClick: (String) -> Unit,
    onMaps: (Lead) -> Unit,
    onWhatsAppPrimero: (Lead) -> Unit,
    onLlamar: (Lead) -> Unit,
    onVisitado: (String) -> Unit,
    onUrgente: (String) -> Unit,
    onGuardarNota: (String, String) -> Unit,
    onAgregarARuta: (String) -> Unit,
    onIniciarDia: () -> Unit,
    onRegenerarDia: () -> Unit
) {
    var panelDia by remember { mutableStateOf(false) }
    var notaDe by remember { mutableStateOf<Lead?>(null) }

    // Port de renderTerreno(): filtro coords + !descartado + radio, orden IUT/dist
    val cercanos = remember(state.leads, state.userLat, state.userLon) {
        val uLat = state.userLat; val uLon = state.userLon
        if (uLat == null || uLon == null) emptyList()
        else state.leads
            .filter { it.lead.lat != null && it.lead.lon != null }
            .map { it to GeoUtils.distKm(uLat, uLon, it.lead.lat!!, it.lead.lon!!) }
            .filter { it.second <= TERRENO_RADIO }
            .sortedWith(compareByDescending<Pair<LeadUi, Double>> { it.first.iut }
                .thenBy { it.second })
    }

    Column(Modifier.fillMaxSize().background(RadarColors.bg).padding(12.dp)) {

        // Panel ⚡ ARRANCAR DÍA (display:none → toggle)
        if (panelDia) {
            PanelArrancarDia(state,
                onCerrar = { panelDia = false },
                onRegenerar = onRegenerarDia,
                onIniciar = { panelDia = false; onIniciarDia() })
            Spacer(Modifier.height(10.dp))
        }

        // sec-header
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("DETECCIÓN TÉCNICA ACTIVA", color = RadarColors.accent, fontSize = 14.sp,
                 fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(RadarColors.orange)
                .clickable { panelDia = !panelDia; if (panelDia) onRegenerarDia() }
                .padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text("⚡ ARRANCAR DÍA", color = RadarColors.bg, fontSize = 11.sp,
                     fontWeight = FontWeight.ExtraBold)
            }
        }
        Spacer(Modifier.height(6.dp))

        // terreno-status (3 textos exactos)
        val statusTxt = when {
            state.userLat == null -> "Activá GPS para detectar oportunidades técnicas"
            cercanos.isEmpty() -> "Sin objetivos en radio. Buscá o agregá negocios."
            else -> "${cercanos.size} objetivo(s) en radio de ${TERRENO_RADIO}km"
        }
        Text(statusTxt, color = if (cercanos.isNotEmpty()) RadarColors.accent
                                else RadarColors.textDim,
             fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
             textAlign = androidx.compose.ui.text.style.TextAlign.Center)

        // lista-terreno
        when {
            state.userLat == null -> EmptyTerreno("📡",
                "Tocá el botón GPS para detectar tu posición y ver objetivos cercanos.")
            cercanos.isEmpty() -> EmptyTerreno("🔍",
                "No hay leads en ${TERRENO_RADIO}km.\nBuscá negocios o tocá el mapa.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cercanos, key = { it.first.lead.id }) { (ui, dist) ->
                    TerrenoCard(ui, dist,
                        onLeadClick, onMaps, onWhatsAppPrimero, onLlamar,
                        onVisitado, onUrgente, { notaDe = ui.lead }, onAgregarARuta)
                }
            }
        }
    }

    // Sheet NOTA RÁPIDA (overlay bottom, port de abrirNotasRapidas)
    notaDe?.let { lead ->
        NotaRapidaSheet(lead,
            onGuardar = { txt -> onGuardarNota(lead.id, txt); notaDe = null },
            onCancelar = { notaDe = null })
    }
}

/* ── Panel ⚡ ARRANCAR DÍA ── */
@Composable
private fun PanelArrancarDia(
    state: TerrenoState, onCerrar: () -> Unit,
    onRegenerar: () -> Unit, onIniciar: () -> Unit
) {
    Column(Modifier.fillMaxWidth()
        .background(RadarColors.bgPanel, RoundedCornerShape(10.dp))
        .border(1.dp, RadarColors.border, RoundedCornerShape(10.dp))
        .padding(12.dp)) {

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("⚡ ARRANCAR DÍA", color = RadarColors.orange, fontSize = 15.sp,
                 fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
            Text("✕", color = RadarColors.textDim, fontSize = 15.sp,
                 fontWeight = FontWeight.Bold,
                 modifier = Modifier.clickable { onCerrar() }.padding(4.dp))
        }
        Spacer(Modifier.height(10.dp))

        // dia-resumen (3 stats: seguim hoy / urgentes / sin contactar)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DiaStat("${state.diaSeguimientos}", "seguim. hoy", RadarColors.orange, Modifier.weight(1f))
            DiaStat("${state.diaUrgentes}", "urgentes", RadarColors.red, Modifier.weight(1f))
            DiaStat("${state.diaSinContacto}", "sin contactar", RadarColors.accent, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))

        Text("OBJETIVOS SUGERIDOS HOY", color = RadarColors.textDim, fontSize = 10.sp,
             fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))

        if (state.objetivosDia.isEmpty()) {
            Text("Sin leads con coordenadas todavía.", color = RadarColors.textDim,
                 fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(8.dp),
                 textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        } else {
            state.objetivosDia.forEach { ui ->
                ObjetivoRow(ui, state.userLat, state.userLon)
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = onRegenerar, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = RadarColors.bgCard)) {
                Text("↺ REGENERAR", color = RadarColors.text, fontSize = 11.sp,
                     fontWeight = FontWeight.Bold)
            }
            Button(onClick = onIniciar, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = RadarColors.orange)) {
                Text("INICIAR RECORRIDO", color = RadarColors.bg, fontSize = 11.sp,
                     fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun DiaStat(num: String, label: String, color: Color, modifier: Modifier) {
    Column(modifier.background(RadarColors.bgCard, RoundedCornerShape(8.dp))
        .padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(num, color = color, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = RadarColors.textDim, fontSize = 9.sp)
    }
}

/* Objetivo: nombre + motivo · dist | IUT chico (port de la card del panel) */
@Composable
private fun ObjetivoRow(ui: LeadUi, uLat: Double?, uLon: Double?) {
    val l = ui.lead
    val ahora = System.currentTimeMillis()
    val segHoy = l.seguimientoFecha?.let { s ->
        try { java.time.Instant.parse(if (s.endsWith("Z") || s.contains("+")) s else s + "Z")
                .toEpochMilli() <= ahora } catch (e: Exception) { false }
    } ?: false
    val motivo = when {
        segHoy -> "⏰ Seguimiento hoy"
        l.estado == "urgente" -> "🚨 Urgente"
        l.estado == "presupuesto" -> "💰 Presupuesto pendiente"
        l.estado == "no-contactado" -> "📞 Sin contactar"
        l.estado in listOf("cliente", "recurrente", "mantenimiento") -> "⭐ Cliente — revisita"
        else -> "🎯 IUT ${ui.iut}"
    }
    val dist = if (uLat != null && uLon != null && l.lat != null && l.lon != null)
        GeoUtils.distKm(uLat, uLon, l.lat!!, l.lon!!) else null

    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(l.nombre, color = RadarColors.text, fontSize = 13.sp,
                 fontWeight = FontWeight.ExtraBold, maxLines = 1,
                 overflow = TextOverflow.Ellipsis)
            Text(motivo + (dist?.let { " · ${fmtDist(it)}" } ?: ""),
                 color = RadarColors.textDim, fontSize = 11.sp)
        }
        Spacer(Modifier.width(6.dp))
        IutBadge(ui.iut)
    }
    HorizontalDivider(color = RadarColors.border)
}

/* ── Card de terreno (renderTerrenoCard 1:1) ── */
@Composable
private fun TerrenoCard(
    ui: LeadUi, dist: Double,
    onLeadClick: (String) -> Unit, onMaps: (Lead) -> Unit,
    onWhatsAppPrimero: (Lead) -> Unit, onLlamar: (Lead) -> Unit,
    onVisitado: (String) -> Unit, onUrgente: (String) -> Unit,
    onNota: () -> Unit, onAgregarARuta: (String) -> Unit
) {
    val l = ui.lead
    val tieneTel = l.telefono.filter { it.isDigit() || it == '+' }.length >= 6
    val srcLbl = when (l.fuente) {
        "google" -> "GOOGLE"; "manual", "" -> "MANUAL"; "osm" -> "OSM"; else -> "TERRENO"
    }
    val prio = ui.prioridad
    val urgente = l.estado == "urgente" || ui.iut >= 70

    // Colores del prioBar (css 1:1 de calcularPrioridadTactica)
    val (pFg, pBg, pBorde) = when {
        prio.nivel == PrioridadEngine.Nivel.HOY && prio.ico == "⏰" ->
            Triple(RadarColors.orange, Color(0x1FFF6B1A), Color(0x66FF6B1A))
        prio.nivel == PrioridadEngine.Nivel.HOY ->
            Triple(RadarColors.red, Color(0x1FFF3355), Color(0x66FF3355))
        prio.nivel == PrioridadEngine.Nivel.SEMANA ->
            Triple(RadarColors.yellow, Color(0x1AF5C400), Color(0x59F5C400))
        prio.nivel == PrioridadEngine.Nivel.REVISITA ->
            Triple(RadarColors.accent, Color(0x1A00E8A0), Color(0x5900E8A0))
        else -> Triple(Color(0xFF4A6888), Color(0x144A6888), Color(0x404A6888))
    }

    Column(Modifier.fillMaxWidth()
        .background(RadarColors.bgCard, RoundedCornerShape(10.dp))
        .border(1.dp, if (urgente) RadarColors.red.copy(alpha = 0.5f) else RadarColors.border,
                RoundedCornerShape(10.dp))
        .padding(10.dp)) {

        // prioBar: prioridad + días desde último contacto
        Row(Modifier.fillMaxWidth()
            .background(pBg, RoundedCornerShape(6.dp))
            .border(1.dp, pBorde, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("${prio.ico} ${prio.label}", color = pFg, fontSize = 11.sp,
                 fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
            Text(diasSinContacto(l), color = pFg.copy(alpha = 0.7f), fontSize = 10.sp)
        }
        Spacer(Modifier.height(7.dp))

        // tc-top: nombre + IUT (con label emoji + ' IUT·')
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Text(l.nombre, color = RadarColors.text, fontSize = 14.sp,
                 fontWeight = FontWeight.ExtraBold, maxLines = 1,
                 overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(6.dp))
            IutBadge(ui.iut, prefijo = " IUT")
        }

        // tc-meta: ◈ dist + src + nivel
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("◈ ${fmtDist(dist)}", color = RadarColors.accent, fontSize = 11.sp,
                 fontWeight = FontWeight.Bold)
            Text(srcLbl, color = RadarColors.textDim, fontSize = 9.sp,
                 fontWeight = FontWeight.Bold,
                 modifier = Modifier.clip(RoundedCornerShape(4.dp))
                     .background(RadarColors.bgPanel)
                     .border(1.dp, RadarColors.border, RoundedCornerShape(4.dp))
                     .padding(horizontal = 5.dp, vertical = 1.dp))
            NivelBadgeUi(l.nivel.ifBlank { "bajo" })
        }

        // equipos-chips (TODOS, ico+label)
        if (l.equipos.isNotEmpty()) {
            Spacer(Modifier.height(5.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                l.equipos.forEach { e ->
                    StatsEngine.EQUIPOS_CATALOGO[e]?.let { (ico, label) ->
                        Text("$ico $label", color = RadarColors.text, fontSize = 10.sp,
                             fontWeight = FontWeight.Bold,
                             modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                 .background(RadarColors.bgPanel)
                                 .border(1.dp, RadarColors.border, RoundedCornerShape(10.dp))
                                 .padding(horizontal = 7.dp, vertical = 3.dp))
                    }
                }
            }
        }

        // 📝 nota (60)
        if (l.notas.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text("📝 ${l.notas.take(60)}${if (l.notas.length > 60) "…" else ""}",
                 color = RadarColors.yellow, fontSize = 11.sp, fontStyle = FontStyle.Italic,
                 maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        // acciones primarias: NAVEGAR + (WHATSAPP | VER FICHA)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.weight(1f).clip(RoundedCornerShape(6.dp))
                .background(RadarColors.accent)
                .clickable { onMaps(l) }.padding(vertical = 8.dp),
                contentAlignment = Alignment.Center) {
                Text("NAVEGAR", color = RadarColors.bg, fontSize = 11.sp,
                     fontWeight = FontWeight.ExtraBold)
            }
            if (tieneTel)
                Box(Modifier.weight(1f).clip(RoundedCornerShape(6.dp))
                    .background(RadarColors.blue)
                    .clickable { onWhatsAppPrimero(l) }.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center) {
                    Text("WHATSAPP", color = Color.White, fontSize = 11.sp,
                         fontWeight = FontWeight.ExtraBold)
                }
            else
                Box(Modifier.weight(1f).clip(RoundedCornerShape(6.dp))
                    .background(RadarColors.bgPanel)
                    .border(1.dp, RadarColors.border, RoundedCornerShape(6.dp))
                    .clickable { onLeadClick(l.id) }.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center) {
                    Text("VER FICHA", color = RadarColors.text, fontSize = 11.sp,
                         fontWeight = FontWeight.Bold)
                }
        }

        // secundarias: 📞(tel) ✅ 📝 🗺️ 🚨 ✏️
        Spacer(Modifier.height(5.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            if (tieneTel) IcoBtn("📞") { onLlamar(l) }
            IcoBtn("✅") { onVisitado(l.id) }
            IcoBtn("📝") { onNota() }
            IcoBtn("🗺️") { onAgregarARuta(l.id) }
            IcoBtn("🚨", RadarColors.red.copy(alpha = 0.4f)) { onUrgente(l.id) }
            IcoBtn("✏️") { onLeadClick(l.id) }
        }
    }
}

@Composable
private fun IcoBtn(ico: String, borde: Color = RadarColors.border, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(RadarColors.bgPanel)
        .border(1.dp, borde, RoundedCornerShape(6.dp))
        .clickable { onClick() }.padding(horizontal = 9.dp, vertical = 5.dp)) {
        Text(ico, fontSize = 13.sp)
    }
}

@Composable
private fun EmptyTerreno(ico: String, texto: String) {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(ico, fontSize = 26.sp)
            Spacer(Modifier.height(6.dp))
            Text(texto, color = RadarColors.textDim, fontSize = 12.sp,
                 textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

/* ── Sheet NOTA RÁPIDA (port de abrirNotasRapidas) ── */
@Composable
private fun NotaRapidaSheet(lead: Lead, onGuardar: (String) -> Unit, onCancelar: () -> Unit) {
    var texto by remember(lead.id) { mutableStateOf(lead.notas) }
    Box(Modifier.fillMaxSize().background(Color(0xBF000000))
        .clickable { onCancelar() }, contentAlignment = Alignment.BottomCenter) {
        Column(Modifier.fillMaxWidth()
            .clickable(enabled = false) {}
            .background(RadarColors.bgCard,
                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .border(2.dp, RadarColors.orange,
                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(start = 14.dp, end = 14.dp, top = 18.dp, bottom = 30.dp)) {

            Text("NOTA RÁPIDA — ${lead.nombre}", color = RadarColors.orange,
                 fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                 letterSpacing = 0.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                NOTAS_TERRENO.forEach { n ->
                    Box(Modifier.clip(RoundedCornerShape(14.dp))
                        .background(RadarColors.bgPanel)
                        .border(1.dp, RadarColors.border, RoundedCornerShape(14.dp))
                        .clickable { texto = if (texto.isBlank()) n else texto + "\n" + n }
                        .padding(horizontal = 10.dp, vertical = 5.dp)) {
                        Text(n, color = RadarColors.text, fontSize = 11.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(value = texto, onValueChange = { texto = it },
                placeholder = { Text("O escribí tu nota...", color = RadarColors.textDim) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 70.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = RadarColors.text, unfocusedTextColor = RadarColors.text,
                    focusedBorderColor = RadarColors.accent,
                    unfocusedBorderColor = RadarColors.border,
                    cursorColor = RadarColors.accent),
                maxLines = 4)
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onGuardar(texto) }, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = RadarColors.orange)) {
                    Text("GUARDAR", color = RadarColors.bg, fontWeight = FontWeight.ExtraBold)
                }
                Button(onClick = onCancelar,
                    colors = ButtonDefaults.buttonColors(containerColor = RadarColors.bgPanel)) {
                    Text("CANCELAR", color = RadarColors.text, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/* ── Helpers puros (port 1:1) ── */
private fun fmtDist(km: Double): String =
    if (km < 1) "${Math.round(km * 1000)}m" else String.format(java.util.Locale.US, "%.1fkm", km)

private fun diasSinContacto(l: Lead): String {
    if (l.historial.isEmpty()) return "Sin contacto previo"
    val ultimo = try {
        val f = l.historial.last().fecha
        java.time.Instant.parse(if (f.endsWith("Z") || f.contains("+")) f else f + "Z").toEpochMilli()
    } catch (e: Exception) { return "Sin contacto previo" }
    val dias = Math.round((System.currentTimeMillis() - ultimo) / 86_400_000.0).toInt()
    return when (dias) { 0 -> "Contactado hoy"; 1 -> "Ayer"; else -> "Hace $dias días" }
}
