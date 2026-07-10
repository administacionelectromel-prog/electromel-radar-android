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
                        onCambiarEstado = { id, estado -> vm.cambiarEstado(id, estado) },
                        onGenerarRuta = { vm.generarRuta() },
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
                        onBuscar = { ciudad, rubro, google -> vm.buscar(ciudad, rubro, google) },
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
