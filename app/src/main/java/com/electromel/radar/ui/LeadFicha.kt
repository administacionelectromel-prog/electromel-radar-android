package com.electromel.radar.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.electromel.radar.domain.Estados
import com.electromel.radar.domain.Lead
import com.electromel.radar.domain.Mensajes
import com.electromel.radar.domain.StatsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * FICHA — réplica 1:1 del modal-lead de la PWA (buildLeadFormHTML +
 * buildLeadSubtitleHTML + bindListenersModalLead):
 * subtitle IUT/fuente/nivel · Estado(12) · Rubro(5) · Nivel(4) · Tel ·
 * Dir · Zona · Equipos(12 checks) · Fotos(grid+📸, resize 800 q0.75,
 * guarda inmediato) · Notas + 8 notas rápidas · Seguimiento(date) ·
 * Ciclo(1-24) · HISTORIAL(últimos 5) · Intentos · ELIMINAR/GUARDAR ·
 * MAPS/+RUTA · PRIMER CONTACTO/SEGUIMIENTO (si tel).
 */
private val NOTAS_RAPIDAS = listOf(
    "✅ Visitado", "❌ Sin interés", "🔧 Equipo visible", "⚡ Soldadora detectada",
    "🏋️ Cintas dañadas", "💰 Buen potencial", "⭐ MUY INTERESANTE", "📅 Volver"
)
private val RUBROS_FICHA = listOf(
    "gimnasio" to "Gimnasio", "hotel" to "Hotel", "constructora" to "Constructora",
    "industrial" to "Industrial", "comercio" to "Comercio"
)
private val NIVELES_FICHA = listOf(
    "bajo" to "🔴 Bajo", "medio" to "🟡 Medio",
    "alto" to "🟢 Alto", "estrategico" to "🔥 Estratégico"
)

@Composable
fun LeadFicha(
    item: LeadUi,
    mensajes: Map<String, Map<String, String>> = Mensajes.DEFAULT,
    onCerrar: () -> Unit,
    onGuardar: (Lead) -> Unit,
    onEliminar: (String) -> Unit,
    onAgregarFoto: (String, String) -> Unit,
    onQuitarFoto: (String, Int) -> Unit,
    onMaps: (Lead) -> Unit,
    onAgregarARuta: (String) -> Unit,
    onWhatsAppPrimero: (Lead) -> Unit,
    onWhatsAppSeguimiento: (Lead) -> Unit
) {
    val l = item.lead
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // Estado del formulario (inicializado del lead; se refresca si cambia el lead)
    var estado by remember(l.id, l.estado) { mutableStateOf(l.estado) }
    var rubro by remember(l.id) { mutableStateOf(l.rubro.ifBlank { "comercio" }) }
    var nivel by remember(l.id) { mutableStateOf(l.nivel.ifBlank { "bajo" }) }
    var tel by remember(l.id) { mutableStateOf(l.telefono) }
    var dir by remember(l.id) { mutableStateOf(l.direccion) }
    var zona by remember(l.id) { mutableStateOf(l.zona) }
    var notas by remember(l.id) { mutableStateOf(l.notas) }
    var seg by remember(l.id) { mutableStateOf(l.seguimientoFecha?.take(10) ?: "") }
    var ciclo by remember(l.id) { mutableStateOf(l.cicloMantenimiento?.toString() ?: "") }
    var equiposSel by remember(l.id) { mutableStateOf(l.equipos.toSet()) }
    var confirmarEliminar by remember { mutableStateOf(false) }

    val tieneTel = l.telefono.filter { it.isDigit() || it == '+' }.length >= 6

    // 📸 FOTO — picker → resize 800px q0.75 → dataURL → guarda inmediato
    val fotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) scope.launch(Dispatchers.IO) {
            val dataUrl = redimensionarImagen(ctx, uri, 800, 75)
            withContext(Dispatchers.Main) {
                if (dataUrl != null) onAgregarFoto(l.id, dataUrl)
            }
        }
    }

    Box(Modifier.fillMaxSize().zIndex(40f).background(RadarColors.bg)
        .statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {

            // Header: nombre + ✕
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(l.nombre, color = RadarColors.text, fontSize = 17.sp,
                     fontWeight = FontWeight.ExtraBold, maxLines = 1,
                     overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text("✕", color = RadarColors.textDim, fontSize = 20.sp,
                     fontWeight = FontWeight.Bold,
                     modifier = Modifier.clickable { onCerrar() }.padding(6.dp))
            }

            // Subtitle: IUT·n + FUENTE + nivel (buildLeadSubtitleHTML)
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IutBadge(item.iut, prefijo = " IUT")
                Text(l.fuente.ifBlank { "manual" }.uppercase(),
                     color = RadarColors.textDim, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                     modifier = Modifier.clip(RoundedCornerShape(4.dp))
                         .background(RadarColors.bgPanel)
                         .border(1.dp, RadarColors.border, RoundedCornerShape(4.dp))
                         .padding(horizontal = 5.dp, vertical = 1.dp))
                NivelBadgeUi(l.nivel.ifBlank { "bajo" })
            }
            Spacer(Modifier.height(12.dp))

            // Estado (select 12)
            CampoLabel("Estado")
            SelectorFicha(Estados.LISTA, estado) { estado = it }

            // Rubro + Nivel (row-2)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    CampoLabel("Rubro")
                    SelectorFicha(RUBROS_FICHA, rubro) { rubro = it }
                }
                Column(Modifier.weight(1f)) {
                    CampoLabel("Nivel cliente")
                    SelectorFicha(NIVELES_FICHA, nivel) { nivel = it }
                }
            }

            CampoLabel("Teléfono")
            CampoTexto(tel, { tel = it })
            CampoLabel("Dirección")
            CampoTexto(dir, { dir = it })
            CampoLabel("Zona / Barrio")
            CampoTexto(zona, { zona = it }, "Ej: Centro, Parque Industrial...")

            // Equipos detectados (12 checkboxes, grid 2 col)
            CampoLabel("Equipos detectados")
            Column(Modifier.fillMaxWidth()
                .background(RadarColors.bgPanel, RoundedCornerShape(10.dp))
                .border(1.dp, RadarColors.border, RoundedCornerShape(10.dp))
                .padding(8.dp)) {
                StatsEngine.EQUIPOS_CATALOGO.entries.chunked(2).forEach { fila ->
                    Row(Modifier.fillMaxWidth()) {
                        fila.forEach { (id, par) ->
                            Row(Modifier.weight(1f)
                                .clickable {
                                    equiposSel = if (id in equiposSel) equiposSel - id
                                                 else equiposSel + id
                                }
                                .padding(vertical = 5.dp, horizontal = 3.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = id in equiposSel,
                                    onCheckedChange = {
                                        equiposSel = if (id in equiposSel) equiposSel - id
                                                     else equiposSel + id
                                    },
                                    modifier = Modifier.size(22.dp),
                                    colors = CheckboxDefaults.colors(checkedColor = RadarColors.accent))
                                Spacer(Modifier.width(6.dp))
                                Text("${par.first} ${par.second}", color = RadarColors.text,
                                     fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                     maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        if (fila.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            // Fotos (grid + ✕ + 📸FOTO — guarda inmediato)
            CampoLabel("Fotos del local / equipos")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                l.fotos.forEachIndexed { i, dataUrl ->
                    Box(Modifier.size(72.dp).clip(RoundedCornerShape(8.dp))
                        .background(RadarColors.bgPanel)) {
                        decodeDataUrl(dataUrl)?.let { bmp ->
                            Image(bmp.asImageBitmap(), contentDescription = "foto",
                                  modifier = Modifier.fillMaxSize(),
                                  contentScale = ContentScale.Crop)
                        }
                        Text("✕", color = Color.White, fontSize = 12.sp,
                             fontWeight = FontWeight.ExtraBold,
                             modifier = Modifier.align(Alignment.TopEnd)
                                 .clip(RoundedCornerShape(bottomStart = 6.dp))
                                 .background(RadarColors.red)
                                 .clickable { onQuitarFoto(l.id, i) }
                                 .padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Column(Modifier.size(72.dp).clip(RoundedCornerShape(8.dp))
                    .background(RadarColors.bgPanel)
                    .border(1.dp, RadarColors.border, RoundedCornerShape(8.dp))
                    .clickable {
                        fotoLauncher.launch(PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    Text("📸", fontSize = 20.sp)
                    Text("FOTO", color = RadarColors.textDim, fontSize = 10.sp,
                         fontWeight = FontWeight.Bold)
                }
            }

            // Notas técnicas + notas rápidas (appendean con \n)
            CampoLabel("Notas técnicas")
            OutlinedTextField(value = notas, onValueChange = { notas = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                colors = camposFicha(), maxLines = 6)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                NOTAS_RAPIDAS.forEach { n ->
                    Box(Modifier.clip(RoundedCornerShape(14.dp))
                        .background(RadarColors.bgPanel)
                        .border(1.dp, RadarColors.border, RoundedCornerShape(14.dp))
                        .clickable { notas = if (notas.isBlank()) n else notas + "\n" + n }
                        .padding(horizontal = 10.dp, vertical = 5.dp)) {
                        Text(n, color = RadarColors.text, fontSize = 11.sp)
                    }
                }
            }

            // Próximo seguimiento (yyyy-MM-dd) + Ciclo mantenimiento
            CampoLabel("Próximo seguimiento")
            CampoTexto(seg, { seg = it }, "aaaa-mm-dd (ej: 2026-07-15)")
            CampoLabel("Ciclo mantenimiento (meses)")
            CampoTexto(ciclo, { if (it.length <= 2 && it.all { c -> c.isDigit() }) ciclo = it }, "Ej: 3")

            // HISTORIAL (últimos 5, más reciente arriba)
            if (l.historial.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Column(Modifier.fillMaxWidth()
                    .background(RadarColors.accent.copy(alpha = 0.07f), RoundedCornerShape(6.dp))
                    .border(1.dp, RadarColors.accent.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                    .padding(10.dp)) {
                    Text("HISTORIAL", color = RadarColors.text, fontSize = 10.sp,
                         fontWeight = FontWeight.ExtraBold)
                    l.historial.takeLast(5).reversed().forEach { h ->
                        Text("• ${fmtFechaFicha(h.fecha)} — ${h.accion}",
                             color = RadarColors.textDim, fontSize = 10.sp, lineHeight = 15.sp)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row {
                Text("Intentos contacto: ", color = RadarColors.textDim, fontSize = 10.sp)
                Text("${l.intentosContacto}", color = RadarColors.text, fontSize = 10.sp,
                     fontWeight = FontWeight.Bold)
            }

            // ELIMINAR / GUARDAR
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = { confirmarEliminar = true }, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = RadarColors.bgPanel)) {
                    Text("ELIMINAR", color = RadarColors.red, fontSize = 12.sp,
                         fontWeight = FontWeight.Bold)
                }
                Button(onClick = {
                        onGuardar(l.copy(
                            estado = estado, rubro = rubro, nivel = nivel,
                            telefono = tel.trim(), direccion = dir.trim(), zona = zona.trim(),
                            notas = notas,
                            seguimientoFecha = segAIso(seg),
                            cicloMantenimiento = ciclo.toIntOrNull(),
                            equipos = equiposSel.toList()
                        ))
                    }, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = RadarColors.orange)) {
                    Text("GUARDAR", color = RadarColors.bg, fontSize = 12.sp,
                         fontWeight = FontWeight.ExtraBold)
                }
            }
            // MAPS / + RUTA
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BtnFicha("MAPS", Modifier.weight(1f)) { onMaps(l) }
                BtnFicha("+ RUTA", Modifier.weight(1f)) { onAgregarARuta(l.id); onCerrar() }
            }
            // PRIMER CONTACTO / SEGUIMIENTO (si tel)
            if (tieneTel) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(onClick = { onWhatsAppPrimero(l) }, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = RadarColors.blue)) {
                        Text("PRIMER CONTACTO", color = Color.White, fontSize = 11.sp,
                             fontWeight = FontWeight.Bold)
                    }
                    BtnFicha("SEGUIMIENTO", Modifier.weight(1f)) { onWhatsAppSeguimiento(l) }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    // confirm('¿Eliminar este lead?')
    if (confirmarEliminar) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            containerColor = RadarColors.bgCard,
            title = { Text("¿Eliminar este lead?", color = RadarColors.text, fontSize = 15.sp) },
            confirmButton = {
                TextButton(onClick = { confirmarEliminar = false; onEliminar(l.id) }) {
                    Text("ELIMINAR", color = RadarColors.red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarEliminar = false }) {
                    Text("Cancelar", color = RadarColors.textDim)
                }
            }
        )
    }
}

/* ── Helpers UI ── */
@Composable
private fun CampoLabel(texto: String) {
    Text(texto, color = RadarColors.textDim, fontSize = 11.sp,
         modifier = Modifier.padding(top = 10.dp, bottom = 3.dp))
}

@Composable
private fun CampoTexto(valor: String, onChange: (String) -> Unit, placeholder: String = "") {
    OutlinedTextField(value = valor, onValueChange = onChange,
        placeholder = { if (placeholder.isNotEmpty())
            Text(placeholder, color = RadarColors.textDim, fontSize = 13.sp) },
        modifier = Modifier.fillMaxWidth(), singleLine = true, colors = camposFicha())
}

/** Select de la PWA → fila de opciones deslizable (una activa). */
@Composable
private fun SelectorFicha(opciones: List<Pair<String, String>>, sel: String,
                          onSel: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        opciones.forEach { (id, label) ->
            val activo = id == sel
            Box(Modifier.clip(RoundedCornerShape(8.dp))
                .background(if (activo) RadarColors.accent.copy(alpha = 0.18f) else RadarColors.bgPanel)
                .border(1.dp, if (activo) RadarColors.accent else RadarColors.border,
                        RoundedCornerShape(8.dp))
                .clickable { onSel(id) }
                .padding(horizontal = 10.dp, vertical = 7.dp)) {
                Text(label, color = if (activo) RadarColors.accent else RadarColors.text,
                     fontSize = 11.sp,
                     fontWeight = if (activo) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun BtnFicha(label: String, modifier: Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = RadarColors.bgPanel)) {
        Text(label, color = RadarColors.text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun camposFicha() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = RadarColors.text, unfocusedTextColor = RadarColors.text,
    focusedBorderColor = RadarColors.accent, unfocusedBorderColor = RadarColors.border,
    cursorColor = RadarColors.accent
)

/* ── Helpers puros ── */

/** yyyy-MM-dd → ISO (port: new Date(v).toISOString()); vacío → null. */
private fun segAIso(v: String): String? {
    val s = v.trim()
    if (s.isEmpty()) return null
    return try {
        java.time.LocalDate.parse(s).atStartOfDay(java.time.ZoneOffset.UTC)
            .toInstant().toString()
    } catch (e: Exception) { null }
}

private fun fmtFechaFicha(iso: String): String = try {
    val ms = java.time.Instant.parse(
        if (iso.endsWith("Z") || iso.contains("+")) iso else iso + "Z").toEpochMilli()
    SimpleDateFormat("dd/MM/yyyy", Locale("es", "AR")).format(Date(ms))
} catch (e: Exception) { iso.take(10) }

/** Port de redimensionarImagen(file, 800, 0.75): max 800px, JPEG 75%,
 *  devuelve dataURL "data:image/jpeg;base64,..." (mismo formato que la PWA). */
private fun redimensionarImagen(ctx: android.content.Context, uri: Uri,
                                maxPx: Int, calidad: Int): String? = try {
    val input = ctx.contentResolver.openInputStream(uri)
    val original = BitmapFactory.decodeStream(input)
    input?.close()
    if (original == null) null else {
        val ratio = minOf(maxPx.toFloat() / original.width,
                          maxPx.toFloat() / original.height, 1f)
        val bmp = if (ratio < 1f)
            Bitmap.createScaledBitmap(original,
                (original.width * ratio).toInt(), (original.height * ratio).toInt(), true)
        else original
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, calidad, out)
        "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
} catch (e: Exception) { null }

/** Decodifica un dataURL base64 a Bitmap para mostrar el thumb. */
private fun decodeDataUrl(dataUrl: String): Bitmap? = try {
    val b64 = dataUrl.substringAfter("base64,", "")
    if (b64.isEmpty()) null else {
        val bytes = Base64.decode(b64, Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
} catch (e: Exception) { null }
