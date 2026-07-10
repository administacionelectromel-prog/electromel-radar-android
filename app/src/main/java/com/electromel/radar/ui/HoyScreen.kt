package com.electromel.radar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * HOY — réplica 1:1 de renderSeguimientos() (sec-seguimientos de la PWA).
 * Filtro: seguimientoFecha hoy-o-atrasada, excluye cliente-ok/descartado.
 * Orden: fecha ascendente. Badge contador. Card: nombre + nivelBadge,
 * ⏰ fecha, 📍 dirección, notas. Botones: SEGUIMIENTO WA / ABRIR / +2 DÍAS.
 */
@Composable
fun HoyScreen(
    state: TerrenoState,
    onLeadClick: (String) -> Unit,
    onWhatsAppSeguimiento: (Lead) -> Unit,
    onPostergar: (String) -> Unit
) {
    // Filtro y orden — port exacto de renderSeguimientos()
    val pendientes = state.leads
        .map { it.lead }
        .filter { l ->
            !l.seguimientoFecha.isNullOrBlank() &&
            esHoyOAtrasado(l.seguimientoFecha) &&
            l.estado !in listOf("cliente-ok", "descartado")
        }
        .sortedBy { parseMs(it.seguimientoFecha) ?: 0L }

    Column(Modifier.fillMaxSize().background(RadarColors.bg).padding(12.dp)) {

        // sec-header: título + badge rojo contador (visible solo si hay pendientes)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("SEGUIMIENTOS HOY", color = RadarColors.accent, fontSize = 16.sp,
                 fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
            if (pendientes.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(RadarColors.red)
                    .padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("${pendientes.size}", color = Color.White, fontSize = 11.sp,
                         fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        if (pendientes.isEmpty()) {
            // empty-state: "✅ Sin seguimientos pendientes."
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("✅ Sin seguimientos pendientes.",
                     color = RadarColors.textDim, fontSize = 13.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(pendientes, key = { it.id }) { l ->
                    SegCard(l, onLeadClick, onWhatsAppSeguimiento, onPostergar)
                }
            }
        }
    }
}

/* seg-card: bg-panel, borde, radius, padding 10x12 */
@Composable
private fun SegCard(
    l: Lead,
    onLeadClick: (String) -> Unit,
    onWhatsAppSeguimiento: (Lead) -> Unit,
    onPostergar: (String) -> Unit
) {
    val tieneTel = l.telefono.filter { it.isDigit() || it == '+' }.length >= 6

    Column(Modifier.fillMaxWidth()
        .background(RadarColors.bgPanel, RoundedCornerShape(10.dp))
        .border(1.dp, RadarColors.border, RoundedCornerShape(10.dp))
        .padding(horizontal = 12.dp, vertical = 10.dp)) {

        // nombre (bold) + nivelBadge
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top) {
            Text(l.nombre, color = RadarColors.text, fontSize = 14.sp,
                 fontWeight = FontWeight.ExtraBold, maxLines = 1,
                 overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
            Spacer(Modifier.width(6.dp))
            NivelBadge(l.nivel.ifBlank { "bajo" })
        }

        // ⏰ fecha (mono chico)
        Text("⏰ ${fmtFecha(l.seguimientoFecha)}", color = RadarColors.textDim, fontSize = 10.sp)

        // 📍 dirección (si hay)
        if (l.direccion.isNotBlank())
            Text("📍 ${l.direccion}", color = RadarColors.textDim, fontSize = 11.sp,
                 maxLines = 1, overflow = TextOverflow.Ellipsis)

        // notas: amarillas, cursiva, 60 chars
        if (l.notas.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(l.notas.take(60), color = RadarColors.yellow, fontSize = 11.sp,
                 fontStyle = FontStyle.Italic, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        // card-actions: SEGUIMIENTO WA (em, solo con tel) / ABRIR / +2 DÍAS
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            if (tieneTel)
                BtnSeg("SEGUIMIENTO WA", RadarColors.orange, RadarColors.bg) {
                    onWhatsAppSeguimiento(l)
                }
            BtnSeg("ABRIR", RadarColors.bgCard, RadarColors.text) { onLeadClick(l.id) }
            BtnSeg("+2 DÍAS", RadarColors.bgCard, RadarColors.text) { onPostergar(l.id) }
        }
    }
}

/* nivelBadge — port 1:1: label + colores del CSS (.nivel-*) */
@Composable
private fun NivelBadge(nivel: String) {
    val (lbl, bg, fg, borde) = when (nivel) {
        "estrategico" -> Cuad("ESTRATÉGICO", Color(0x33FF6B1A), RadarColors.orange, Color(0x66FF6B1A))
        "alto"        -> Cuad("ALTO VALOR", Color(0x1F00E8A0), RadarColors.accent, Color(0x4D00E8A0))
        "medio"       -> Cuad("MEDIO", Color(0x1FF5C400), RadarColors.yellow, Color(0x4DF5C400))
        else          -> Cuad("BAJO", Color(0x1A4A6888), RadarColors.textDim, RadarColors.border)
    }
    Box(Modifier.clip(RoundedCornerShape(5.dp)).background(bg)
        .border(1.dp, borde, RoundedCornerShape(5.dp))
        .padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(lbl, color = fg, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
    }
}

private data class Cuad(val a: String, val b: Color, val c: Color, val d: Color)

@Composable
private fun BtnSeg(label: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(bg)
        .border(1.dp, RadarColors.border, RoundedCornerShape(6.dp))
        .clickable { onClick() }
        .padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

/* esHoyOAtrasado — port 1:1: fecha <= fin del día local (23:59:59.999) */
private fun esHoyOAtrasado(iso: String?): Boolean {
    val m = parseMs(iso) ?: return false
    val finDia = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
    }.timeInMillis
    return m <= finDia
}

/* fmtFecha — port 1:1: dd/MM/yyyy (es-AR) */
private fun fmtFecha(iso: String?): String {
    val m = parseMs(iso) ?: return ""
    return SimpleDateFormat("dd/MM/yyyy", Locale("es", "AR")).format(Date(m))
}

private fun parseMs(iso: String?): Long? = try {
    if (iso.isNullOrBlank()) null
    else java.time.Instant.parse(
        if (iso.endsWith("Z") || iso.contains("+")) iso else iso + "Z"
    ).toEpochMilli()
} catch (e: Exception) { null }
