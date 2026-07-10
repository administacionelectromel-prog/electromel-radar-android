package com.electromel.radar.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electromel.radar.domain.BuscarEngine
import com.electromel.radar.domain.Mensajes

/**
 * CONFIGURACIÓN — réplica de sec-config de la PWA:
 * API Key + GUARDAR API KEY · ZONAS DE BÚSQUEDA (defaults + extras + agregar)
 * · PLANTILLAS WHATSAPP por rubro + GUARDAR + RESTAURAR POR DEFECTO.
 */
@Composable
fun ConfigScreen(
    googleKey: String,
    zonasExtra: List<String>,
    mensajes: Map<String, Map<String, String>>,
    onGuardarApiKey: (String) -> Unit,
    onAgregarZona: (String) -> Unit,
    onQuitarZona: (String) -> Unit,
    onGuardarPlantillas: (rubro: String, primero: String, seguimiento: String, cierre: String) -> Unit,
    onRestaurarPlantillas: () -> Unit
) {
    var key by remember(googleKey) { mutableStateOf(googleKey) }
    var nuevaZona by remember { mutableStateOf("") }
    var rubroSel by remember { mutableStateOf("gimnasio") }

    // Textareas del rubro seleccionado (se recargan al cambiar rubro o mensajes)
    val tpl = mensajes[rubroSel] ?: Mensajes.DEFAULT[rubroSel] ?: emptyMap()
    var m1 by remember(rubroSel, mensajes) { mutableStateOf(tpl["primero"] ?: "") }
    var m2 by remember(rubroSel, mensajes) { mutableStateOf(tpl["seguimiento"] ?: "") }
    var m3 by remember(rubroSel, mensajes) { mutableStateOf(tpl["cierre"] ?: "") }

    Column(Modifier.fillMaxSize().background(RadarColors.bg)
        .verticalScroll(rememberScrollState()).padding(12.dp)) {

        Text("CONFIGURACIÓN", color = RadarColors.accent, fontSize = 16.sp,
             fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(12.dp))

        // ── GOOGLE PLACES API KEY ──
        Text("Google Places API Key (opcional)", color = RadarColors.textDim, fontSize = 11.sp)
        OutlinedTextField(value = key, onValueChange = { key = it },
            placeholder = { Text("Pegá tu API Key aquí", color = RadarColors.textDim) },
            modifier = Modifier.fillMaxWidth(), colors = campoColorsCfg(), singleLine = true)
        Text("Sin key usa OSM gratis. Con key obtenés teléfonos y datos extra.",
             color = RadarColors.textDim, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(8.dp))
        Button(onClick = { onGuardarApiKey(key.trim()) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = RadarColors.accent)) {
            Text("GUARDAR API KEY", color = RadarColors.bg, fontWeight = FontWeight.ExtraBold)
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = RadarColors.border)
        Spacer(Modifier.height(12.dp))

        // ── ZONAS DE BÚSQUEDA ──
        Text("ZONAS DE BÚSQUEDA", color = RadarColors.text, fontSize = 13.sp,
             fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(6.dp))
        Banner("Cada zona genera una búsqueda extra. Más zonas = más resultados únicos.")
        Spacer(Modifier.height(8.dp))

        // Lista: defaults marcadas "(default)" + extras con quitar (✕)
        Column(Modifier.fillMaxWidth().heightIn(max = 200.dp)
            .verticalScroll(rememberScrollState())) {
            BuscarEngine.ZONAS_INDUSTRIALES.filter { it.isNotBlank() }.forEach { z ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(z, color = RadarColors.text, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("(default)", color = RadarColors.textDim, fontSize = 10.sp)
                }
            }
            zonasExtra.forEach { z ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(z, color = RadarColors.accent, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("✕", color = RadarColors.red, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                         modifier = Modifier.clickable { onQuitarZona(z) }.padding(horizontal = 6.dp))
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = nuevaZona, onValueChange = { nuevaZona = it },
                placeholder = { Text("Ej: zona sur, ruta 7, área logística...",
                                     color = RadarColors.textDim, fontSize = 12.sp) },
                modifier = Modifier.weight(1f), colors = campoColorsCfg(), singleLine = true)
            Button(onClick = {
                    if (nuevaZona.isNotBlank()) { onAgregarZona(nuevaZona.trim().lowercase()); nuevaZona = "" }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RadarColors.accent),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                Text("+ AGREGAR", color = RadarColors.bg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = RadarColors.border)
        Spacer(Modifier.height(12.dp))

        // ── PLANTILLAS WHATSAPP (por rubro) ──
        Text("PLANTILLAS WHATSAPP", color = RadarColors.text, fontSize = 13.sp,
             fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(6.dp))
        Banner("Usá {nombre} — se reemplaza automáticamente.")
        Spacer(Modifier.height(8.dp))

        Text("Rubro", color = RadarColors.textDim, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Mensajes.RUBROS_EDIT.forEach { (id, label) ->
                Row(Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (rubroSel == id) RadarColors.accent.copy(alpha = 0.15f) else RadarColors.bgPanel)
                    .border(1.dp, if (rubroSel == id) RadarColors.accent else RadarColors.border,
                            RoundedCornerShape(8.dp))
                    .clickable { rubroSel = id }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(label, color = if (rubroSel == id) RadarColors.accent else RadarColors.text,
                         fontSize = 12.sp,
                         fontWeight = if (rubroSel == id) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        CampoMsg("Primer contacto", m1) { m1 = it }
        CampoMsg("Seguimiento técnico", m2) { m2 = it }
        CampoMsg("Cierre / Propuesta", m3) { m3 = it }

        Spacer(Modifier.height(12.dp))
        Button(onClick = { onGuardarPlantillas(rubroSel, m1, m2, m3) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = RadarColors.accent)) {
            Text("GUARDAR PLANTILLAS", color = RadarColors.bg, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(6.dp))
        Button(onClick = onRestaurarPlantillas,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = RadarColors.bgPanel)) {
            Text("RESTAURAR POR DEFECTO", color = RadarColors.text, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun Banner(texto: String) {
    Row(Modifier.fillMaxWidth()
        .background(RadarColors.accent.copy(alpha = 0.07f), RoundedCornerShape(6.dp))
        .border(1.dp, RadarColors.accent.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
        .padding(10.dp)) {
        Text(texto, color = RadarColors.textDim, fontSize = 11.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun CampoMsg(label: String, value: String, onChange: (String) -> Unit) {
    Spacer(Modifier.height(8.dp))
    Text(label, color = RadarColors.textDim, fontSize = 11.sp)
    OutlinedTextField(value = value, onValueChange = onChange,
        modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp),
        colors = campoColorsCfg(), maxLines = 6,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp))
}

@Composable
private fun campoColorsCfg() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = RadarColors.text, unfocusedTextColor = RadarColors.text,
    focusedBorderColor = RadarColors.accent, unfocusedBorderColor = RadarColors.border,
    cursorColor = RadarColors.accent
)
