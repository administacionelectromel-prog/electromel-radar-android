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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electromel.radar.domain.BuscarEngine
import com.electromel.radar.domain.IutEngine
import com.electromel.radar.domain.Lead

/**
 * BUSCAR — réplica 1:1 de sec-buscar: Ciudad/Zona con autosuggest
 * (prefijo→contiene→levenshtein, top 5) · multi-rubro (chips naranjas ✕ +
 * 8 RUBROS FRECUENTES con emojis) · Fuente OSM/Google · BUSCAR + ⏹ ·
 * progreso por zona · barra masiva (nuevos/ya · Solo nuevos · ⬇GUARDAR N) ·
 * card resultado (IUT, ★, 📞 3 estados, MAPS/WA/+GUARDAR/✓GUARDADO/🗑).
 */
private val RUBROS_FRECUENTES = listOf(
    "taller" to "⚙️ Taller", "metalurgica" to "🏭 Metalúrgica",
    "soldadora" to "⚡ Soldadora", "gym" to "🏋️ Gym", "hotel" to "🏨 Hotel",
    "constructora" to "🏗️ Constructora", "lavadero" to "🌀 Lavadero",
    "industrial" to "🔧 Industrial"
)

@Composable
fun BuscarScreen(
    state: TerrenoState,
    rubroInicial: String = "",
    onRubroInicialConsumido: () -> Unit = {},
    onBuscar: (ciudad: String, rubros: List<String>, fuente: String) -> Unit,
    onFrenar: () -> Unit,
    onGuardarResultado: (BuscarEngine.Resultado) -> Unit,
    onGuardarTodos: () -> Unit,
    onMapsResultado: (BuscarEngine.Resultado) -> Unit,
    onWaResultado: (BuscarEngine.Resultado) -> Unit,
    onEliminarLead: (String) -> Unit,
    onAviso: (String) -> Unit
) {
    var ciudad by remember { mutableStateOf("") }
    var rubroInput by remember { mutableStateOf("") }
    var rubrosActivos by remember { mutableStateOf(listOf<String>()) }
    var fuente by remember { mutableStateOf("osm") }
    var soloNuevos by remember { mutableStateOf(false) }

    fun agregarRubro(r: String) {
        val v = r.trim().lowercase()
        if (v.isNotEmpty() && v !in rubrosActivos) rubrosActivos = rubrosActivos + v
    }
    fun disparar() {
        if (rubroInput.isNotBlank()) {
            parsearRubros(rubroInput).forEach { agregarRubro(it) }
            rubroInput = ""
        }
        val rubros = rubrosActivos
        if (rubros.isEmpty()) { onAviso("Escribí un rubro o elegí uno de los sugeridos"); return }
        if (ciudad.isBlank()) { onAviso("Ingresá una ciudad"); return }
        onBuscar(ciudad.trim(), rubros, fuente)
    }

    // Búsqueda rápida del top-bar: precarga el rubro y dispara (port 1:1)
    LaunchedEffect(rubroInicial) {
        if (rubroInicial.isNotBlank()) {
            parsearRubros(rubroInicial).forEach { agregarRubro(it) }
            onRubroInicialConsumido()
            disparar()   // valida y avisa si falta ciudad (port exacto)
        }
    }

    // yaGuardado por resultado (port de encontrarLeadExistente)
    val leads = state.leadsTodos.map { it.lead }
    fun leadDe(r: BuscarEngine.Resultado): Lead? {
        r.googleId?.let { g -> leads.find { it.googleId == g }?.let { return it } }
        r.osmId?.let { o -> leads.find { it.osmId == o }?.let { return it } }
        val k = IutEngine.normalizar(r.nombre) + "|" + IutEngine.normalizar(r.direccion).take(20)
        return leads.find {
            IutEngine.normalizar(it.nombre) + "|" +
                IutEngine.normalizar(it.direccion).take(20) == k
        }
    }

    Column(Modifier.fillMaxSize().background(RadarColors.bg).padding(12.dp)) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            item {
                // ── Ciudad / Zona + autosuggest ──
                Text("Ciudad / Zona", color = RadarColors.textDim, fontSize = 11.sp)
                Spacer(Modifier.height(3.dp))
                OutlinedTextField(value = ciudad, onValueChange = { ciudad = it },
                    placeholder = { Text("Ej: Neuquén, Bariloche, Roca...",
                                         color = RadarColors.textDim, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, colors = campoBuscar())
                val sugs = sugerirCiudades(ciudad)
                if (sugs.isNotEmpty() && ciudad.isNotBlank() &&
                    sugs.firstOrNull() != ciudad) {
                    Column(Modifier.fillMaxWidth()
                        .background(RadarColors.bgCard, RoundedCornerShape(8.dp))
                        .border(1.dp, RadarColors.border, RoundedCornerShape(8.dp))) {
                        sugs.forEach { c ->
                            Text("📍 $c", color = RadarColors.text, fontSize = 13.sp,
                                modifier = Modifier.fillMaxWidth()
                                    .clickable { ciudad = c }
                                    .padding(horizontal = 10.dp, vertical = 8.dp))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))

                // ── Tipo de negocio / equipo + chips activos + sugeridos ──
                Text("Tipo de negocio / equipo", color = RadarColors.textDim, fontSize = 11.sp)
                Spacer(Modifier.height(3.dp))
                OutlinedTextField(value = rubroInput, onValueChange = { rubroInput = it },
                    placeholder = { Text("gym, hotel, metalurgica, soldadora, taller...",
                                         color = RadarColors.textDim, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, colors = campoBuscar())

                if (rubrosActivos.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        rubrosActivos.forEach { r ->
                            Row(Modifier.clip(RoundedCornerShape(14.dp))
                                .background(RadarColors.orange.copy(alpha = 0.15f))
                                .border(1.dp, RadarColors.orange.copy(alpha = 0.4f),
                                        RoundedCornerShape(14.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(r, color = RadarColors.orange, fontSize = 12.sp,
                                     fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(4.dp))
                                Text("✕", color = RadarColors.orange, fontSize = 12.sp,
                                     fontWeight = FontWeight.Bold,
                                     modifier = Modifier.clickable {
                                         rubrosActivos = rubrosActivos - r })
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("RUBROS FRECUENTES:", color = RadarColors.textDim, fontSize = 10.sp,
                     letterSpacing = 0.5.sp)
                Spacer(Modifier.height(5.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    RUBROS_FRECUENTES.forEach { (id, label) ->
                        Box(Modifier.clip(RoundedCornerShape(16.dp))
                            .background(RadarColors.bgCard)
                            .border(1.dp, RadarColors.border, RoundedCornerShape(16.dp))
                            .clickable { agregarRubro(id) }
                            .padding(horizontal = 11.dp, vertical = 6.dp)) {
                            Text(label, color = RadarColors.text, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))

                // ── Fuente de datos (select 1:1) ──
                Text("Fuente de datos", color = RadarColors.textDim, fontSize = 11.sp)
                Spacer(Modifier.height(3.dp))
                Column(Modifier.fillMaxWidth()
                    .background(RadarColors.bgCard, RoundedCornerShape(8.dp))
                    .border(1.dp, RadarColors.border, RoundedCornerShape(8.dp))) {
                    FuenteOpcion("OpenStreetMap (gratis, offline-ready)",
                        fuente == "osm") { fuente = "osm" }
                    HorizontalDivider(color = RadarColors.border)
                    FuenteOpcion("Google Places (requiere API Key)",
                        fuente == "google") { fuente = "google" }
                }
                Spacer(Modifier.height(10.dp))

                // ── BUSCAR OBJETIVOS + ⏹ FRENAR ──
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(onClick = { disparar() }, modifier = Modifier.weight(1f),
                        enabled = !state.buscando,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RadarColors.accent,
                            disabledContainerColor = RadarColors.accent.copy(alpha = 0.4f))) {
                        Text("BUSCAR OBJETIVOS", color = RadarColors.bg,
                             fontWeight = FontWeight.ExtraBold)
                    }
                    if (state.buscando)
                        Button(onClick = onFrenar,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RadarColors.red)) {
                            Text("⏹", color = Color.White, fontSize = 14.sp)
                        }
                }

                // ── buscar-info: progreso / resumen final ──
                if (state.buscarProgreso.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(state.buscarProgreso, color = RadarColors.textDim, fontSize = 11.sp,
                         lineHeight = 15.sp, modifier = Modifier.fillMaxWidth(),
                         textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                if (state.buscarError.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(state.buscarError, color = RadarColors.red, fontSize = 12.sp)
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = RadarColors.border)
                Spacer(Modifier.height(10.dp))

                // ── Barra de acciones masivas ──
                val resultados = state.resultadosBusqueda
                if (resultados.isNotEmpty()) {
                    val nuevosN = resultados.count { leadDe(it) == null }
                    val yaN = resultados.size - nuevosN
                    Row(Modifier.fillMaxWidth()
                        .background(RadarColors.bgPanel, RoundedCornerShape(10.dp))
                        .border(1.dp, RadarColors.border, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.weight(1f)) {
                            Text("$nuevosN", color = RadarColors.accent, fontSize = 11.sp,
                                 fontWeight = FontWeight.Bold)
                            Text(" nuevos · ", color = RadarColors.textDim, fontSize = 11.sp)
                            Text("$yaN", color = RadarColors.textDim, fontSize = 11.sp,
                                 fontWeight = FontWeight.Bold)
                            Text(" ya guardados", color = RadarColors.textDim, fontSize = 11.sp)
                        }
                        Box(Modifier.clip(RoundedCornerShape(6.dp))
                            .background(if (soloNuevos) RadarColors.accent.copy(alpha = 0.2f)
                                        else RadarColors.bgCard)
                            .border(1.dp, if (soloNuevos) RadarColors.accent
                                          else RadarColors.border, RoundedCornerShape(6.dp))
                            .clickable { soloNuevos = !soloNuevos }
                            .padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(if (soloNuevos) "✓ Solo nuevos" else "Solo nuevos",
                                 color = if (soloNuevos) RadarColors.accent
                                         else RadarColors.text,
                                 fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(Modifier.clip(RoundedCornerShape(6.dp))
                            .background(if (nuevosN > 0) RadarColors.orange
                                        else RadarColors.orange.copy(alpha = 0.4f))
                            .clickable(enabled = nuevosN > 0) { onGuardarTodos() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text("⬇ GUARDAR $nuevosN", color = RadarColors.bg,
                                 fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ── Resultados ──
            val visibles = if (soloNuevos)
                state.resultadosBusqueda.filter { leadDe(it) == null }
            else state.resultadosBusqueda
            if (state.resultadosBusqueda.isNotEmpty() && visibles.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(20.dp),
                        contentAlignment = Alignment.Center) {
                        Text("✅ Todos los resultados ya están guardados.",
                             color = RadarColors.textDim, fontSize = 12.sp)
                    }
                }
            }
            items(visibles) { r ->
                val yaLead = leadDe(r)
                ResultadoCard(r, yaLead != null,
                    onGuardar = { onGuardarResultado(r) },
                    onMaps = { onMapsResultado(r) },
                    onWa = { onWaResultado(r) },
                    onDel = { yaLead?.let { onEliminarLead(it.id) } })
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun FuenteOpcion(label: String, activa: Boolean, onSel: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onSel() }
        .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(if (activa) "◉" else "○",
             color = if (activa) RadarColors.accent else RadarColors.textDim, fontSize = 13.sp)
        Spacer(Modifier.width(8.dp))
        Text(label, color = if (activa) RadarColors.text else RadarColors.textDim,
             fontSize = 12.sp, fontWeight = if (activa) FontWeight.Bold else FontWeight.Normal)
    }
}

/* Card resultado 1:1 de buildCardResultadoHTML */
@Composable
private fun ResultadoCard(
    r: BuscarEngine.Resultado, yaLead: Boolean,
    onGuardar: () -> Unit, onMaps: () -> Unit, onWa: () -> Unit, onDel: () -> Unit
) {
    val tieneTel = r.telefono.filter { it.isDigit() || it == '+' }.length >= 6
    Column(Modifier.fillMaxWidth()
        .background(RadarColors.bgCard, RoundedCornerShape(10.dp))
        .border(1.dp, RadarColors.border, RoundedCornerShape(10.dp))
        .padding(10.dp)) {

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(r.nombre, color = RadarColors.text, fontSize = 14.sp,
                     fontWeight = FontWeight.ExtraBold, maxLines = 1,
                     overflow = TextOverflow.Ellipsis,
                     modifier = Modifier.weight(1f, false))
                Spacer(Modifier.width(6.dp))
                IutBadge(r.iut)
            }
            r.rating?.let {
                Spacer(Modifier.width(6.dp))
                Text("★ $it", color = RadarColors.yellow, fontSize = 11.sp)
            }
        }

        if (r.direccion.isNotBlank())
            Text("📍 ${r.direccion}", color = RadarColors.textDim, fontSize = 11.sp,
                 maxLines = 1, overflow = TextOverflow.Ellipsis,
                 modifier = Modifier.padding(top = 3.dp))

        // 📞 3 estados: tel / Cargando (google) / Sin teléfono
        when {
            tieneTel -> Row(Modifier.padding(top = 2.dp)) {
                Text("📞 ", color = RadarColors.textDim, fontSize = 11.sp)
                Text(r.telefono, color = RadarColors.text, fontSize = 11.sp,
                     fontWeight = FontWeight.Bold)
            }
            r.fuente == "google" && r.googleId != null ->
                Text("Cargando...", color = RadarColors.textDim, fontSize = 11.sp,
                     modifier = Modifier.padding(top = 2.dp))
            else -> Text("Sin teléfono", color = RadarColors.textDim, fontSize = 11.sp,
                     modifier = Modifier.padding(top = 2.dp))
        }

        if (r.tipo.isNotBlank())
            Text(r.tipo, color = RadarColors.textDim, fontSize = 10.sp,
                 modifier = Modifier.padding(top = 2.dp))

        // rc-actions: MAPS · WA(si tel) · +GUARDAR/✓GUARDADO · 🗑(si guardado)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            BtnRc("MAPS", RadarColors.bgPanel, RadarColors.text) { onMaps() }
            if (tieneTel) BtnRc("WA", RadarColors.blue, Color.White) { onWa() }
            if (yaLead) {
                BtnRc("✓ GUARDADO", RadarColors.bgPanel,
                      RadarColors.text.copy(alpha = 0.5f)) { }
                BtnRc("🗑", RadarColors.bgPanel, RadarColors.red) { onDel() }
            } else {
                BtnRc("+ GUARDAR", RadarColors.orange, RadarColors.bg) { onGuardar() }
            }
        }
    }
}

@Composable
private fun BtnRc(label: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(bg)
        .border(1.dp, RadarColors.border, RoundedCornerShape(6.dp))
        .clickable { onClick() }
        .padding(horizontal = 10.dp, vertical = 7.dp)) {
        Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun campoBuscar() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = RadarColors.text, unfocusedTextColor = RadarColors.text,
    focusedBorderColor = RadarColors.accent, unfocusedBorderColor = RadarColors.border,
    cursorColor = RadarColors.accent
)

/* ── Helpers puros (port 1:1) ── */
private fun parsearRubros(v: String): List<String> =
    v.split(Regex("[,;]+")).map { it.trim().lowercase() }.filter { it.isNotEmpty() }

/** sugerirCiudades: prefijo → contiene → levenshtein (d ≤ max(2, 40%)), top 5. */
private fun sugerirCiudades(q: String): List<String> {
    if (q.isBlank()) return emptyList()
    val qn = IutEngine.normalizar(q)
    val pref = mutableListOf<String>(); val cont = mutableListOf<String>()
    val sim = mutableListOf<Pair<String, Int>>()
    for (c in BuscarEngine.CIUDADES_ZONA) {
        val n = IutEngine.normalizar(c)
        when {
            n.startsWith(qn) -> pref.add(c)
            n.contains(qn) -> cont.add(c)
            else -> {
                val d = levenshtein(qn, n)
                if (d <= maxOf(2, (qn.length * 0.4).toInt())) sim.add(c to d)
            }
        }
    }
    sim.sortBy { it.second }
    return (pref + cont + sim.map { it.first }).take(5)
}

private fun levenshtein(a: String, b: String): Int {
    val m = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) m[i][0] = i
    for (j in 0..b.length) m[0][j] = j
    for (i in 1..a.length) for (j in 1..b.length)
        m[i][j] = minOf(m[i - 1][j] + 1, m[i][j - 1] + 1,
                        m[i - 1][j - 1] + if (a[i - 1] != b[j - 1]) 1 else 0)
    return m[a.length][b.length]
}
