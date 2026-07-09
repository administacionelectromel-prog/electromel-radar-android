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
                        onGuardarConfig = { k, p1, p2, p3 -> vm.guardarConfig(k, p1, p2, p3) },
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
                        onGuardarResultado = { r -> vm.guardarResultado(r) }
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
