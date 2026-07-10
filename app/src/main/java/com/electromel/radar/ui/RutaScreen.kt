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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electromel.radar.domain.RutaEngine

/**
 * RUTA — réplica 1:1 de sec-ruta + renderRuta() de la PWA:
 * banner · parada manual + AGREGAR · lista numerada con ↑ ↓ ✕ ·
 * LIMPIAR (confirm) + INICIAR RECORRIDO (abre Maps con todas las paradas).
 */
@Composable
fun RutaScreen(
    state: TerrenoState,
    onAgregarManual: (String) -> Unit,
    onMover: (Int, Int) -> Unit,
    onQuitar: (Int) -> Unit,
    onLimpiar: () -> Unit,
    onIniciar: () -> Unit
) {
    var manual by remember { mutableStateOf("") }
    var confirmarLimpiar by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(RadarColors.bg).padding(12.dp)) {

        Text("PLANIFICADOR DE RUTA", color = RadarColors.accent, fontSize = 16.sp,
             fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(8.dp))

        // info-banner em
        Row(Modifier.fillMaxWidth()
            .background(RadarColors.orange.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
            .border(1.dp, RadarColors.orange.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(10.dp)) {
            Text("Orden optimizado por cercanía. Abrí en Maps con todas las paradas.",
                 color = RadarColors.textDim, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Spacer(Modifier.height(10.dp))

        // Agregar parada manual
        Text("Agregar parada manual", color = RadarColors.textDim, fontSize = 11.sp)
        OutlinedTextField(value = manual, onValueChange = { manual = it },
            placeholder = { Text("Ej: Av. Colón 1234, Neuquén", color = RadarColors.textDim) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = RadarColors.text, unfocusedTextColor = RadarColors.text,
                focusedBorderColor = RadarColors.accent, unfocusedBorderColor = RadarColors.border,
                cursorColor = RadarColors.accent))
        Spacer(Modifier.height(6.dp))
        Button(onClick = { if (manual.isNotBlank()) { onAgregarManual(manual); manual = "" } },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = RadarColors.bgPanel)) {
            Text("+ AGREGAR PARADA", color = RadarColors.text, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = RadarColors.border)
        Spacer(Modifier.height(10.dp))

        // lista-ruta
        if (state.ruta.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f).padding(20.dp),
                contentAlignment = Alignment.Center) {
                Text("🗺️ Sin paradas. Agregá leads desde sus fichas o desde Terreno.",
                     color = RadarColors.textDim, fontSize = 12.sp)
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(state.ruta, key = { _, p -> p.id }) { i, p ->
                    ParadaRow(i, p, state.ruta.size, onMover, onQuitar)
                }
            }
        }

        // row-2: LIMPIAR (rojo) + INICIAR RECORRIDO (em naranja)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = { confirmarLimpiar = true }, modifier = Modifier.weight(0.4f),
                colors = ButtonDefaults.buttonColors(containerColor = RadarColors.bgPanel)) {
                Text("LIMPIAR", color = RadarColors.red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Button(onClick = onIniciar, modifier = Modifier.weight(0.6f),
                colors = ButtonDefaults.buttonColors(containerColor = RadarColors.orange)) {
                Text("INICIAR RECORRIDO", color = RadarColors.bg, fontWeight = FontWeight.ExtraBold)
            }
        }
    }

    // confirm('¿Limpiar la ruta?')
    if (confirmarLimpiar) {
        AlertDialog(
            onDismissRequest = { confirmarLimpiar = false },
            containerColor = RadarColors.bgCard,
            title = { Text("¿Limpiar la ruta?", color = RadarColors.text, fontSize = 15.sp) },
            confirmButton = {
                TextButton(onClick = { onLimpiar(); confirmarLimpiar = false }) {
                    Text("LIMPIAR", color = RadarColors.red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarLimpiar = false }) {
                    Text("Cancelar", color = RadarColors.textDim)
                }
            }
        )
    }
}

/* .parada: número circular + info + controles ↑ ↓ ✕ */
@Composable
private fun ParadaRow(
    i: Int, p: RutaEngine.Parada, total: Int,
    onMover: (Int, Int) -> Unit, onQuitar: (Int) -> Unit
) {
    Row(Modifier.fillMaxWidth()
        .background(RadarColors.bgPanel, RoundedCornerShape(8.dp))
        .border(1.dp, RadarColors.border, RoundedCornerShape(8.dp))
        .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically) {

        // parada-num
        Box(Modifier.size(26.dp).clip(CircleShape).background(RadarColors.accent),
            contentAlignment = Alignment.Center) {
            Text("${i + 1}", color = RadarColors.bg, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(8.dp))

        // parada-info: nombre + dirección
        Column(Modifier.weight(1f)) {
            Text(p.nombre, color = RadarColors.text, fontSize = 13.sp,
                 fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (p.direccion.isNotBlank() && p.direccion != p.nombre)
                Text(p.direccion, color = RadarColors.textDim, fontSize = 10.sp,
                     maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        // parada-ctrl: ↑ ↓ ✕ (extremos deshabilitados con opacidad 0.3)
        CtrlBtn("↑", i > 0) { onMover(i, -1) }
        CtrlBtn("↓", i < total - 1) { onMover(i, 1) }
        CtrlBtn("✕", true, RadarColors.red) { onQuitar(i) }
    }
}

@Composable
private fun CtrlBtn(label: String, habilitado: Boolean,
                    color: Color = RadarColors.text, onClick: () -> Unit) {
    Box(Modifier.padding(start = 4.dp).clip(RoundedCornerShape(6.dp))
        .background(RadarColors.bgCard)
        .border(1.dp, RadarColors.border, RoundedCornerShape(6.dp))
        .clickable(enabled = habilitado) { onClick() }
        .padding(horizontal = 9.dp, vertical = 5.dp)) {
        Text(label, color = if (habilitado) color else color.copy(alpha = 0.3f),
             fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
