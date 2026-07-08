package com.electromel.radar.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.electromel.radar.data.LeadStore
import com.electromel.radar.data.RadarDatabase
import com.electromel.radar.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LeadUi(
    val lead: Lead,
    val iut: Int,
    val prioridad: PrioridadEngine.Prioridad
)

data class TerrenoState(
    val leads: List<LeadUi> = emptyList(),
    val cargando: Boolean = true,
    val mensaje: String = "",
    val userLat: Double? = null,
    val userLon: Double? = null,
    val leadSeleccionado: LeadUi? = null,
    val objetivosDia: List<LeadUi> = emptyList(),
    val diaSeguimientos: Int = 0,
    val diaUrgentes: Int = 0,
    val diaSinContacto: Int = 0,
    val ruta: List<LeadUi> = emptyList(),
    val rutaDistanciaKm: Double = 0.0
)

/**
 * AndroidViewModel para tener Context (necesario para Room).
 * Carga los leads persistidos al abrir; el import los guarda en disco.
 */
class TerrenoViewModel(app: Application) : AndroidViewModel(app) {

    private val store = LeadStore(RadarDatabase.get(app).leadDao())
    private val _state = MutableStateFlow(TerrenoState())
    val state: StateFlow<TerrenoState> = _state

    init {
        viewModelScope.launch {
            store.cargar()
            recomputar()
        }
    }

    /** Import con modo reemplazar (default). Persiste en Room. */
    fun importarBackup(contenido: String, merge: Boolean = false) {
        BackupParser.parsearLeads(contenido)
            .onSuccess { leads ->
                viewModelScope.launch {
                    val n = if (merge) store.merge(leads) else store.replaceAll(leads)
                    recomputar(
                        if (merge) "Merge: +$n leads nuevos"
                        else "Importados $n leads"
                    )
                }
            }
            .onFailure {
                _state.value = _state.value.copy(
                    cargando = false,
                    mensaje = "No se pudo leer el archivo. ¿Es un backup de ELECTROMEL RADAR?"
                )
            }
    }

    fun abrirLead(id: String) {
        val ui = _state.value.leads.find { it.lead.id == id }
        _state.value = _state.value.copy(leadSeleccionado = ui)
    }

    fun cerrarLead() {
        _state.value = _state.value.copy(leadSeleccionado = null)
    }

    /** Cambia el estado del lead, persiste, y recalcula prioridad/orden. */
    fun cambiarEstado(id: String, nuevoEstado: String) {
        val lead = store.byId(id) ?: return
        val actualizado = lead.copy(
            estado = nuevoEstado,
            historial = lead.historial + EventoHistorial(
                fecha = java.time.Instant.now().toString(),
                accion = "Estado → $nuevoEstado"
            )
        )
        viewModelScope.launch {
            store.upsert(actualizado)
            recomputar()
        }
    }

    /** Genera la ruta óptima con los objetivos del día (RutaEngine). */
    fun generarRuta() {
        val s = _state.value
        val leadsObjetivo = s.objetivosDia.map { it.lead }
        val optimizada = RutaEngine.optimizar(leadsObjetivo, s.userLat, s.userLon)
        val dist = RutaEngine.distanciaTotal(optimizada, s.userLat, s.userLon)
        val rutaUi = optimizada.mapNotNull { lead -> s.leads.find { it.lead.id == lead.id } }
        _state.value = s.copy(ruta = rutaUi, rutaDistanciaKm = dist)
    }

    fun setUbicacion(lat: Double, lon: Double) {
        _state.value = _state.value.copy(userLat = lat, userLon = lon)
    }

    private fun parseIsoMs(iso: String): Long? = try {
        java.time.Instant.parse(if (iso.endsWith("Z") || iso.contains("+")) iso else iso + "Z").toEpochMilli()
    } catch (e: Exception) { null }

    private fun recomputar(msg: String = "") {
        val ahora = System.currentTimeMillis()
        val ui = store.all()
            .filter { it.estado != "descartado" }
            .map { LeadUi(it, IutEngine.calcular(it), PrioridadEngine.calcular(it, ahora)) }
            .sortedWith(compareBy<LeadUi> { it.prioridad.nivel.ordinal }.thenByDescending { it.iut })
        val prev = _state.value

        // Objetivos del día (DiaEngine) + resumen
        val objetivos = DiaEngine.generarObjetivos(
            store.all(), prev.userLat, prev.userLon, 8, ahora
        ).mapNotNull { (lead, _) -> ui.find { it.lead.id == lead.id } }

        val seguHoy = store.all().count {
            it.seguimientoFecha?.let { s -> parseIsoMs(s)?.let { m -> m <= ahora } } == true &&
            it.estado != "descartado"
        }
        val urgentes = store.all().count { it.estado == "urgente" }
        val sinContacto = store.all().count {
            it.estado == "no-contactado" && it.telefono.filter { c -> c.isDigit() }.length >= 6
        }

        val selRefrescado = prev.leadSeleccionado?.let { sel ->
            ui.find { it.lead.id == sel.lead.id }
        }
        _state.value = TerrenoState(
            leads = ui,
            cargando = false,
            userLat = prev.userLat,
            userLon = prev.userLon,
            leadSeleccionado = selRefrescado,
            objetivosDia = objetivos,
            diaSeguimientos = seguHoy,
            diaUrgentes = urgentes,
            diaSinContacto = sinContacto,
            ruta = prev.ruta.mapNotNull { r -> ui.find { it.lead.id == r.lead.id } },
            rutaDistanciaKm = prev.rutaDistanciaKm,
            mensaje = when {
                ui.isNotEmpty() && msg.isNotEmpty() -> msg
                ui.isEmpty() && store.count() == 0  -> "Importá el JSON exportado desde la PWA para ver tus leads."
                ui.isEmpty()                        -> "El backup no tenía leads activos."
                else                                -> ""
            }
        )
    }
}
