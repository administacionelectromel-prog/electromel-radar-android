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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electromel.radar.domain.StatsEngine
import com.electromel.radar.domain.ZonasEngine

/**
 * ZONAS — réplica de sec-zonas de la PWA:
 * MAPA CALOR INDUSTRIAL · banner score · selector agrupación ·
 * slider radio 200-3000 (auto) · RECALCULAR · lista de zonas con
 * temp 🔥/🌡️/❄️, IUT prom, top equipo, barra, stats y VER/+RUTA/MAPA.
 */
@Composable
fun ZonasScreen(
    state: TerrenoState,
    onRecalcular: (ZonasEngine.Modo, Int) -> Unit,
    onLeadClick: (String) -> Unit,
    onZonaARuta: (ZonasEngine.Zona) -> Unit,
    onVerEnMapa: (Double, Double) -> Unit
) {
    var modo by remember { mutableStateOf(state.zonasModo) }
    var radio by remember { mutableStateOf(state.zonasRadio.toFloat()) }
    var detalleDe by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(RadarColors.bg).padding(12.dp)) {

        Text("MAPA CALOR INDUSTRIAL", color = RadarColors.orange,
             fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(8.dp))

        // Banner (info-banner em)
        Row(Modifier.fillMaxWidth()
            .background(RadarColors.orange.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
            .border(1.dp, RadarColors.orange.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(10.dp)) {
            Text("Score = (clientes×3)+(respondieron×2)+contactados. Zonas industriales priorizadas.",
                 color = RadarColors.textDim, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Spacer(Modifier.height(10.dp))

        // Agrupación
        Text("Agrupación", color = RadarColors.textDim, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ModoOpcion("Por cercanía (radio)", modo == ZonasEngine.Modo.AUTO, Modifier.weight(1f)) {
                modo = ZonasEngine.Modo.AUTO
            }
            ModoOpcion("Por zona / barrio", modo == ZonasEngine.Modo.BARRIO, Modifier.weight(1f)) {
                modo = ZonasEngine.Modo.BARRIO
            }
        }

        // Radio (solo en modo auto, como el original)
        if (modo == ZonasEngine.Modo.AUTO) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Radio: ", color = RadarColors.textDim, fontSize = 11.sp)
                Text("${radio.toInt()}", color = RadarColors.text,
                     fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(" m", color = RadarColors.textDim, fontSize = 11.sp)
            }
            Slider(
                value = radio, onValueChange = { radio = it },
                valueRange = 200f..3000f, steps = 27,   // pasos de 100 m
                colors = SliderDefaults.colors(
                    thumbColor = RadarColors.orange,
                    activeTrackColor = RadarColors.orange,
                    inactiveTrackColor = RadarColors.border
                )
            )
        }

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = { detalleDe = null; onRecalcular(modo, radio.toInt()) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = RadarColors.bgPanel)
        ) { Text("RECALCULAR ZONAS", color = RadarColors.text, fontWeight = FontWeight.Bold) }

        Spacer(Modifier.height(10.dp))

        if (state.zonas.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                Text("📍 Agregá leads para ver zonas.", color = RadarColors.textDim, fontSize = 12.sp)
            }
        } else {
            val scoreMax = (state.zonas.firstOrNull()?.score ?: 1).coerceAtLeast(1)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.zonas, key = { it.nombre + it.leads.size }) { z ->
                    ZonaCard(
                        z = z, scoreMax = scoreMax,
                        expandida = detalleDe == z.nombre,
                        onVer = { detalleDe = if (detalleDe == z.nombre) null else z.nombre },
                        onRuta = { onZonaARuta(z) },
                        onMapa = { onVerEnMapa(z.latCentro, z.lonCentro) },
                        onLeadClick = onLeadClick
                    )
                }
            }
        }
    }
}

@Composable
private fun ModoOpcion(label: String, activo: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier
        .clip(RoundedCornerShape(8.dp))
        .background(if (activo) RadarColors.orange else RadarColors.bgPanel)
        .border(1.dp, if (activo) RadarColors.orange else RadarColors.border, RoundedCornerShape(8.dp))
        .clickable { onClick() }
        .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center) {
        Text(label, color = if (activo) RadarColors.bg else RadarColors.text,
             fontSize = 11.sp, fontWeight = if (activo) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun ZonaCard(
    z: ZonasEngine.Zona, scoreMax: Int, expandida: Boolean,
    onVer: () -> Unit, onRuta: () -> Unit, onMapa: () -> Unit,
    onLeadClick: (String) -> Unit
) {
    val tempColor = when (z.temp) {
        "hot" -> RadarColors.red
        "warm" -> RadarColors.orange
        else -> RadarColors.blue
    }
    val ico = when (z.temp) { "hot" -> "🔥"; "warm" -> "🌡️"; else -> "❄️" }
    val barPct = ((z.score.toFloat() / scoreMax) * 100f).coerceAtLeast(5f) / 100f

    Column(Modifier.fillMaxWidth()
        .background(RadarColors.bgCard, RoundedCornerShape(10.dp))
        .border(1.dp, tempColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
        .padding(10.dp)) {

        // Top: nombre + IUT promedio
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("$ico ${z.nombre}", color = RadarColors.text, fontSize = 13.sp,
                 fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                 modifier = Modifier.weight(1f, false))
            Spacer(Modifier.width(6.dp))
            Text("IUT·${if (z.leads.isNotEmpty()) Math.round(z.iutTotal.toDouble() / z.leads.size) else 0}",
                 color = RadarColors.accent, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        }

        // Top equipo de la zona (como el original)
        z.topEquipo?.let { (eqId, cnt) ->
            val cat = StatsEngine.EQUIPOS_CATALOGO[eqId]
            if (cat != null) {
                Spacer(Modifier.height(2.dp))
                Text("${cat.first} ${cat.second} × $cnt", color = RadarColors.orange,
                     fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(6.dp))
        // Barra proporcional al score
        Box(Modifier.fillMaxWidth().height(5.dp)
            .background(RadarColors.border, RoundedCornerShape(3.dp))) {
            Box(Modifier.fillMaxWidth(barPct).fillMaxHeight()
                .background(tempColor, RoundedCornerShape(3.dp)))
        }

        Spacer(Modifier.height(6.dp))
        // Stats: total / contactados / clientes
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ZStat("${z.leads.size}", "leads", RadarColors.text)
            ZStat("${z.contactados}", "contact.", RadarColors.blue)
            ZStat("${z.clientes}", "clientes", RadarColors.accent)
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            BtnZona(if (expandida) "OCULTAR" else "VER", RadarColors.bgPanel, RadarColors.text, onVer)
            BtnZona("+RUTA", RadarColors.orange, RadarColors.bg, onRuta)
            BtnZona("MAPA", RadarColors.bgPanel, RadarColors.text, onMapa)
        }

        // Detalle: leads de la zona (VER)
        if (expandida) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = RadarColors.border)
            z.leads.take(20).forEach { l ->
                Row(Modifier.fillMaxWidth()
                    .clickable { onLeadClick(l.id) }
                    .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(l.nombre, color = RadarColors.text, fontSize = 12.sp,
                         maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(l.estado, color = RadarColors.textDim, fontSize = 10.sp)
                }
            }
            if (z.leads.size > 20)
                Text("… y ${z.leads.size - 20} más", color = RadarColors.textDim, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ZStat(num: String, label: String, color: Color) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(num, color = color, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.width(3.dp))
        Text(label, color = RadarColors.textDim, fontSize = 10.sp)
    }
}

@Composable
private fun BtnZona(label: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(bg)
        .border(1.dp, RadarColors.border, RoundedCornerShape(6.dp))
        .clickable { onClick() }
        .padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
