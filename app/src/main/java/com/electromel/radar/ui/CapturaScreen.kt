package com.electromel.radar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TIPOS = listOf("comercio","industrial","gimnasio","hotel","constructora","taller","lavadero")
private val TAGS = listOf(
    "urgente" to "🚨 Urgente", "alto_potencial" to "⭐ Potencial",
    "equipo_detectado" to "⚙️ Equipo", "maquinas_viejas" to "🔧 Máq. viejas",
    "no_sirve" to "❌ No sirve"
)

@Composable
fun CapturaScreen(
    tieneGps: Boolean,
    onGuardar: (nombre: String, tipo: String, equipos: List<String>, tags: List<String>, tel: String) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var tel by remember { mutableStateOf("") }
    var tipoSel by remember { mutableStateOf("comercio") }
    var tagsSel by remember { mutableStateOf(setOf<String>()) }

    Column(Modifier.fillMaxSize().background(RadarColors.bg)
        .verticalScroll(rememberScrollState()).padding(12.dp)) {

        Text("➕ CAPTURA RÁPIDA", color = RadarColors.orange, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text(if (tieneGps) "◈ Se guardará con tu ubicación" else "Sin GPS (se guarda sin coordenadas)",
             color = RadarColors.textDim, fontSize = 11.sp)
        Spacer(Modifier.height(12.dp))

        Text("Nombre del negocio *", color = RadarColors.textDim, fontSize = 11.sp)
        OutlinedTextField(
            value = nombre, onValueChange = { nombre = it },
            placeholder = { Text("Ej: GymFit, Metal Sur...", color = RadarColors.textDim) },
            modifier = Modifier.fillMaxWidth(),
            colors = campoColors(), singleLine = true
        )
        Spacer(Modifier.height(10.dp))

        Text("Tipo de negocio", color = RadarColors.textDim, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        FlowChips(TIPOS, setOf(tipoSel)) { tipoSel = it }
        Spacer(Modifier.height(10.dp))

        Text("Etiquetas", color = RadarColors.textDim, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TAGS.forEach { (key, label) ->
                Chip(label, key in tagsSel) {
                    tagsSel = if (key in tagsSel) tagsSel - key else tagsSel + key
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        Text("Teléfono (opcional)", color = RadarColors.textDim, fontSize = 11.sp)
        OutlinedTextField(
            value = tel, onValueChange = { tel = it },
            placeholder = { Text("+54 299...", color = RadarColors.textDim) },
            modifier = Modifier.fillMaxWidth(), colors = campoColors(), singleLine = true
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                if (nombre.isBlank()) return@Button
                onGuardar(nombre.trim(), tipoSel, emptyList(), tagsSel.toList(), tel.trim())
                nombre = ""; tel = ""; tagsSel = emptySet(); tipoSel = "comercio"
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = RadarColors.accent)
        ) { Text("GUARDAR LEAD", color = RadarColors.bg, fontWeight = FontWeight.ExtraBold) }
    }
}

@Composable
private fun FlowChips(items: List<String>, sel: Set<String>, onClick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(3).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                fila.forEach { Chip(it, it in sel) { onClick(it) } }
            }
        }
    }
}

@Composable
private fun Chip(label: String, activo: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.background(
            if (activo) RadarColors.accent else RadarColors.bgPanel, RoundedCornerShape(16.dp))
            .border(1.dp, if (activo) RadarColors.accent else RadarColors.border, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(label, color = if (activo) RadarColors.bg else RadarColors.text,
             fontSize = 12.sp, fontWeight = if (activo) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun campoColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = RadarColors.text, unfocusedTextColor = RadarColors.text,
    focusedBorderColor = RadarColors.accent, unfocusedBorderColor = RadarColors.border,
    cursorColor = RadarColors.accent
)
