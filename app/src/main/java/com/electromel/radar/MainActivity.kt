package com.electromel.radar

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.electromel.radar.ui.*
import com.electromel.radar.domain.ExportEngine

/**
 * Fase 3: mapa nativo (osmdroid) + GPS (LocationManager del sistema).
 */
class MainActivity : ComponentActivity() {

    private var ubicacion: UbicacionProvider? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = RadarColors.bg,
                primary    = RadarColors.accent,
                secondary  = RadarColors.orange
            )) {
                val vm: TerrenoViewModel = viewModel()
                val state by vm.state.collectAsState()

                // Import de archivo
                val picker = rememberLauncherForActivityResult(
                    ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    uri?.let {
                        val texto = contentResolver.openInputStream(it)
                            ?.bufferedReader()?.use { r -> r.readText() } ?: ""
                        if (texto.isNotBlank()) vm.importarBackup(texto)
                    }
                }

                // Permiso de ubicación
                val permLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { concedido -> if (concedido) arrancarGps(vm) }

                LaunchedEffect(Unit) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) arrancarGps(vm)
                    else permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }

                Surface(color = RadarColors.bg) {
                    RadarApp(
                        state = state,
                        onImportarClick = { picker.launch("application/json") },
                        onLeadClick = { id -> vm.abrirLead(id) },
                        onCerrarLead = { vm.cerrarLead() },
                        onGuardarFicha = { lead ->
                            vm.guardarFicha(lead); vm.cerrarLead()
                            android.widget.Toast.makeText(this@MainActivity,
                                "Lead actualizado ✓", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onEliminarLead = { id ->
                            vm.eliminarLead(id); vm.cerrarLead()
                            android.widget.Toast.makeText(this@MainActivity,
                                "Lead eliminado", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onAgregarFoto = { id, dataUrl ->
                            vm.agregarFotoLead(id, dataUrl)
                            android.widget.Toast.makeText(this@MainActivity,
                                "Foto guardada ✓", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onQuitarFoto = { id, idx ->
                            vm.quitarFotoLead(id, idx)
                            android.widget.Toast.makeText(this@MainActivity,
                                "Foto eliminada", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onGenerarRuta = { vm.generarRuta() },
                        onAgregarManual = { t -> vm.agregarParadaManual(t) },
                        onMoverParada = { i, d -> vm.moverParada(i, d) },
                        onQuitarParada = { i -> vm.quitarParada(i) },
                        onLimpiarRuta = { vm.limpiarRuta() },
                        onIniciarRecorrido = {
                            val url = vm.iniciarRecorrido()
                            if (url == null) {
                                android.widget.Toast.makeText(this@MainActivity,
                                    "La ruta está vacía", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                startActivity(android.content.Intent(
                                    android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                            }
                        },
                        onGuardarApiKey = { k -> vm.guardarApiKey(k) },
                        onAgregarZona = { z -> vm.agregarZona(z) },
                        onQuitarZona = { z -> vm.quitarZona(z) },
                        onGuardarPlantillas = { r, p1, p2, p3 -> vm.guardarPlantillas(r, p1, p2, p3) },
                        onRestaurarPlantillas = { vm.restaurarPlantillas() },
                        onRecalcularZonas = { modo, radio -> vm.recalcularZonas(modo, radio) },
                        onZonaARuta = { z -> vm.rutaDesdeZona(z) },
                        onWhatsAppSeguimiento = { lead ->
                            com.electromel.radar.ui.AccionesNativas.whatsapp(
                                this@MainActivity, lead,
                                com.electromel.radar.domain.Mensajes.build(lead, "seguimiento", state.mensajes))
                            vm.registrarWhatsApp(lead.id, "seguimiento")
                        },
                        onWhatsAppPrimero = { lead ->
                            com.electromel.radar.ui.AccionesNativas.whatsapp(
                                this@MainActivity, lead,
                                com.electromel.radar.domain.Mensajes.build(lead, "primero", state.mensajes))
                            vm.registrarWhatsApp(lead.id, "primero")
                        },
                        onMaps = { lead ->
                            val url = com.electromel.radar.domain.RutaEngine.urlRecorrido(listOf(
                                com.electromel.radar.domain.RutaEngine.Parada(
                                    id = lead.id, nombre = lead.nombre,
                                    direccion = lead.direccion, lat = lead.lat, lon = lead.lon)))
                            url?.let { startActivity(android.content.Intent(
                                android.content.Intent.ACTION_VIEW, android.net.Uri.parse(it))) }
                        },
                        onLlamar = { lead ->
                            val tel = lead.telefono.filter { it.isDigit() || it == '+' }
                            if (tel.isNotEmpty()) startActivity(android.content.Intent(
                                android.content.Intent.ACTION_DIAL,
                                android.net.Uri.parse("tel:" + tel)))
                        },
                        onVisitado = { id ->
                            vm.marcarVisitado(id)
                            android.widget.Toast.makeText(this@MainActivity,
                                "✓ Marcado como visitado", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onUrgente = { id ->
                            vm.marcarUrgente(id)
                            android.widget.Toast.makeText(this@MainActivity,
                                "🚨 Marcado como URGENTE", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onGuardarNota = { id, nota ->
                            vm.guardarNotaRapida(id, nota)
                            android.widget.Toast.makeText(this@MainActivity,
                                "Nota guardada ✓", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onIniciarDia = {
                            vm.generarRuta()
                            android.widget.Toast.makeText(this@MainActivity,
                                "✓ Ruta del día: " + state.objetivosDia.size +
                                " objetivos optimizados",
                                android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onRegenerarDia = { vm.refrescar() },
                        onAgregarARuta = { id ->
                            val dup = state.ruta.any { it.leadId == id }
                            vm.agregarLeadARuta(id)
                            if (dup) android.widget.Toast.makeText(this@MainActivity,
                                "Ya está en la ruta", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onPostergar = { id ->
                            vm.postergarSeguimiento(id, 2)
                            android.widget.Toast.makeText(this@MainActivity,
                                "Postergado +2 días", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onCapturar = { nom, tipo, eq, tags, tel -> vm.capturarLead(nom, tipo, eq, tags, tel) },
                        onExportar = { formato, filtro ->
                            val leads = ExportEngine.filtrar(state.leads.map { it.lead }, filtro)
                            val (contenido, archivo, mime) = when (formato) {
                                "json" -> Triple(ExportEngine.toJson(leads), "electromel_leads.json", "application/json")
                                "csv"  -> Triple(ExportEngine.toCsv(leads), "electromel_leads.csv", "text/csv")
                                else   -> Triple(ExportEngine.toTxt(leads), "electromel_contactos.txt", "text/plain")
                            }
                            ExportShare.compartir(this@MainActivity, contenido, archivo, mime)
                        },
                        onBuscar = { ciudad, rubros, fuente -> vm.buscar(ciudad, rubros, fuente) },
                        onFrenar = { vm.frenarBusqueda() },
                        onMapsResultado = { r ->
                            val url = com.electromel.radar.domain.RutaEngine.urlRecorrido(listOf(
                                com.electromel.radar.domain.RutaEngine.Parada(
                                    id = "res", nombre = r.nombre, direccion = r.direccion,
                                    lat = r.lat, lon = r.lon)))
                            url?.let { startActivity(android.content.Intent(
                                android.content.Intent.ACTION_VIEW, android.net.Uri.parse(it))) }
                        },
                        onWaResultado = { r ->
                            val lead = vm.waDesdeResultado(r)
                            com.electromel.radar.ui.AccionesNativas.whatsapp(
                                this@MainActivity, lead,
                                com.electromel.radar.domain.Mensajes.build(lead, "primero", state.mensajes))
                            vm.registrarWhatsApp(lead.id, "primero")
                        },
                        onAviso = { msg -> android.widget.Toast.makeText(
                            this@MainActivity, msg, android.widget.Toast.LENGTH_SHORT).show() },
                        onGuardarResultado = { r -> vm.guardarResultado(r) },
                        onGuardarTodos = { vm.guardarTodosResultados() },
                        onCampanaWhatsapp = {
                            // Campaña: abre WhatsApp con el primer lead con teléfono pendiente
                            val conTel = state.leads.map { it.lead }.firstOrNull {
                                it.telefono.filter { c -> c.isDigit() }.length >= 6 &&
                                it.estado == "no-contactado"
                            }
                            if (conTel != null)
                                AccionesNativas.whatsapp(this@MainActivity, conTel,
                                    com.electromel.radar.domain.Mensajes.build(conTel, "primero", state.mensajes))
                        },
                        onBorrarTodo = { vm.borrarTodo() }
                    )
                }
            }
        }
    }

    private fun arrancarGps(vm: TerrenoViewModel) {
        if (ubicacion == null) ubicacion = UbicacionProvider(this)
        ubicacion?.iniciar { lat, lon -> vm.setUbicacion(lat, lon) }
    }

    override fun onDestroy() {
        super.onDestroy()
        ubicacion?.detener()
    }
}
