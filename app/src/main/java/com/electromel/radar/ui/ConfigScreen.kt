package com.electromel.radar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConfigScreen(
    googleKey: String,
    msgPrimero: String,
    msgSeguimiento: String,
    msgCierre: String,
    onGuardar: (key: String, primero: String, seguimiento: String, cierre: String) -> Unit
) {
    var key by remember(googleKey) { mutableStateOf(googleKey) }
    var m1 by remember(msgPrimero) { mutableStateOf(msgPrimero) }
    var m2 by remember(msgSeguimiento) { mutableStateOf(msgSeguimiento) }
    var m3 by remember(msgCierre) { mutableStateOf(msgCierre) }

    Column(Modifier.fillMaxSize().background(RadarColors.bg)
        .verticalScroll(rememberScrollState()).padding(12.dp)) {

        Text("⚙️ CONFIGURACIÓN", color = RadarColors.accent, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(12.dp))

        Text("Google Places API Key (opcional)", color = RadarColors.textDim, fontSize = 11.sp)
        OutlinedTextField(value = key, onValueChange = { key = it },
            placeholder = { Text("Pegá tu API Key", color = RadarColors.textDim) },
            modifier = Modifier.fillMaxWidth(), colors = campoColorsCfg(), singleLine = true)
        Text("Sin key usa OSM gratis. Con key obtenés teléfonos y datos extra.",
             color = RadarColors.textDim, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = RadarColors.border)
        Spacer(Modifier.height(12.dp))

        Text("PLANTILLAS WHATSAPP", color = RadarColors.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text("Usá {nombre} — se reemplaza automáticamente.",
             color = RadarColors.textDim, fontSize = 10.sp, modifier = Modifier.padding(vertical = 4.dp))

        CampoMsg("Primer contacto", m1) { m1 = it }
        CampoMsg("Seguimiento", m2) { m2 = it }
        CampoMsg("Cierre / Propuesta", m3) { m3 = it }

        Spacer(Modifier.height(16.dp))
        Button(onClick = { onGuardar(key.trim(), m1, m2, m3) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = RadarColors.accent)) {
            Text("GUARDAR CONFIGURACIÓN", color = RadarColors.bg, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun CampoMsg(label: String, value: String, onChange: (String) -> Unit) {
    Spacer(Modifier.height(8.dp))
    Text(label, color = RadarColors.textDim, fontSize = 11.sp)
    OutlinedTextField(value = value, onValueChange = onChange,
        modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
        colors = campoColorsCfg(), maxLines = 5)
}

@Composable
private fun campoColorsCfg() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = RadarColors.text, unfocusedTextColor = RadarColors.text,
    focusedBorderColor = RadarColors.accent, unfocusedBorderColor = RadarColors.border,
    cursorColor = RadarColors.accent
)
