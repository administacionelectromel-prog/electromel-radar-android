package com.electromel.radar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electromel.radar.domain.BuscarEngine

private val RUBROS = listOf("taller","metalúrgica","soldadora","gym","hotel","constructora","lavadero","industrial")

@Composable
fun BuscarScreen(
    state: TerrenoState,
    onBuscar: (ciudad: String, rubro: String, usarGoogle: Boolean) -> Unit,
    onGuardarResultado: (BuscarEngine.Resultado) -> Unit,
    onGuardarTodos: () -> Unit
) {
    var ciudad by remember { mutableStateOf("") }
    var rubro by remember { mutableStateOf("") }
    var usarGoogle by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(RadarColors.bg).padding(12.dp)) {
        Text("🔍 BUSCAR OBJETIVOS", color = RadarColors.accent, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(value = ciudad, onValueChange = { ciudad = it },
            placeholder = { Text("Ciudad (ej: Neuquén, Cipolletti)", color = RadarColors.textDim) },
            modifier = Modifier.fillMaxWidth(), colors = campoBuscar(), singleLine = true)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = rubro, onValueChange = { rubro = it },
            placeholder = { Text("Rubro (gym, hotel, taller...)", color = RadarColors.textDim) },
            modifier = Modifier.fillMaxWidth(), colors = campoBuscar(), singleLine = true)

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RUBROS.forEach { r ->
                Box(Modifier.background(RadarColors.bgPanel, RoundedCornerShape(16.dp))
                    .border(1.dp, RadarColors.border, RoundedCornerShape(16.dp))
                    .clickable { rubro = r }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(r, color = RadarColors.text, fontSize = 12.sp)
                }
            }
        }

        // Toggle Google (solo si hay API key configurada)
        if (state.googleKey.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = usarGoogle, onCheckedChange = { usarGoogle = it },
                    colors = CheckboxDefaults.colors(checkedColor = RadarColors.accent))
                Text("Incluir Google Places (más datos, usa tu API key)",
                     color = RadarColors.textDim, fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { if (ciudad.isNotBlank() && rubro.isNotBlank()) onBuscar(ciudad.trim(), rubro.trim(), usarGoogle) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.buscando,
            colors = ButtonDefaults.buttonColors(containerColor = RadarColors.accent)
        ) {
            Text(if (state.buscando) "BUSCANDO..." else "BUSCAR OBJETIVOS",
                 color = RadarColors.bg, fontWeight = FontWeight.ExtraBold)
        }

        Spacer(Modifier.height(10.dp))

        when {
            state.buscando -> Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RadarColors.accent)
            }
            state.buscarError.isNotEmpty() -> Text(state.buscarError, color = RadarColors.red, fontSize = 12.sp)
            state.resultadosBusqueda.isNotEmpty() -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("${state.resultadosBusqueda.size} resultados", color = RadarColors.textDim, fontSize = 11.sp)
                    Button(onClick = onGuardarTodos,
                        colors = ButtonDefaults.buttonColors(containerColor = RadarColors.accent),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                        Text("+ GUARDAR TODOS", color = RadarColors.bg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(6.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(state.resultadosBusqueda) { r -> ResultadoCard(r, onGuardarResultado) }
                }
            }
        }
    }
}

@Composable
private fun ResultadoCard(r: BuscarEngine.Resultado, onGuardar: (BuscarEngine.Resultado) -> Unit) {
    Row(Modifier.fillMaxWidth().background(RadarColors.bgCard, RoundedCornerShape(8.dp))
        .border(1.dp, RadarColors.border, RoundedCornerShape(8.dp)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(r.nombre, color = RadarColors.text, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                     maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                Spacer(Modifier.width(6.dp))
                Text(if (r.fuente == "google") "G" else "OSM",
                     color = if (r.fuente == "google") RadarColors.yellow else RadarColors.blue, fontSize = 9.sp)
            }
            if (r.direccion.isNotEmpty())
                Text("📍 ${r.direccion}", color = RadarColors.textDim, fontSize = 11.sp,
                     maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (r.telefono.isNotEmpty())
                Text("📞 ${r.telefono}", color = RadarColors.textDim, fontSize = 11.sp)
        }
        Button(onClick = { onGuardar(r) },
            colors = ButtonDefaults.buttonColors(containerColor = RadarColors.orange),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
            Text("+ GUARDAR", color = RadarColors.bg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun campoBuscar() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = RadarColors.text, unfocusedTextColor = RadarColors.text,
    focusedBorderColor = RadarColors.accent, unfocusedBorderColor = RadarColors.border,
    cursorColor = RadarColors.accent
)
