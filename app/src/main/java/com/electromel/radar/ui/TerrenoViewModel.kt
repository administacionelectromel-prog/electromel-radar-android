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
    val rutaDistanciaKm: Double = 0.0,
    val statsResumen: StatsEngine.Resumen = StatsEngine.Resumen(0,0,0,0,0,0),
    val statsConversion: List<StatsEngine.ConversionRubro> = emptyList(),
    val statsRevisitas: List<StatsEngine.Revisita> = emptyList(),
    val googleKey: String = "",
    val msgPrimero: String = "",
    val msgSeguimiento: String = "",
    val msgCierre: String = ""
)

/**
 * AndroidViewModel para tener Context (necesario para Room).
 * Carga los leads persistidos al abrir; el import los guarda en disco.
 */
class TerrenoViewModel(app: Application) : AndroidViewModel(app) {

    private val db = RadarDatabase.get(app)
    private val store = LeadStore(db.leadDao(), db.configDao())
    private val _state = MutableStateFlow(TerrenoState())
    val state: StateFlow<TerrenoState> = _state

    init {
        viewModelScope.launch {
            store.cargar()
            cargarConfig()
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

    private suspend fun cargarConfig() {
        val cfg = store.getConfig()
        _state.value = _state.value.copy(
            googleKey = cfg["googleKey"] ?: "",
            msgPrimero = cfg["msgPrimero"] ?: Mensajes.primerContacto(Lead(id="", nombre="{nombre}")),
            msgSeguimiento = cfg["msgSeguimiento"] ?: Mensajes.seguimiento(Lead(id="", nombre="{nombre}")),
            msgCierre = cfg["msgCierre"] ?: Mensajes.cierre(Lead(id="", nombre="{nombre}"))
        )
    }

    fun guardarConfig(key: String, primero: String, seguimiento: String, cierre: String) {
        viewModelScope.launch {
            store.setConfig("googleKey", key)
            store.setConfig("msgPrimero", primero)
            store.setConfig("msgSeguimiento", seguimiento)
            store.setConfig("msgCierre", cierre)
            _state.value = _state.value.copy(
                googleKey = key, msgPrimero = primero,
                msgSeguimiento = seguimiento, msgCierre = cierre
            )
        }
    }

    /** Captura un lead en terreno usando el GPS actual. */
    fun capturarLead(nombre: String, tipo: String, equipos: List<String>, tags: List<String>, tel: String) {
        val s = _state.value
        val lead = CapturaEngine.construir(
            nombre = nombre, tipo = tipo, equipos = equipos, tags = tags, telefono = tel,
            lat = s.userLat, lon = s.userLon,
            idGenerado = java.util.UUID.randomUUID().toString(),
            ahoraIso = java.time.Instant.now().toString()
        )
        viewModelScope.launch {
            store.upsert(lead)
            recomputar("Capturado: " + lead.nombre)
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
        val stResumen = StatsEngine.resumen(store.all(), ahora)
        val stConv = StatsEngine.conversionPorRubro(store.all())
        val stRev = StatsEngine.revisitasPendientes(store.all(), ahora)
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
            statsResumen = stResumen,
            statsConversion = stConv,
            statsRevisitas = stRev,
            googleKey = prev.googleKey,
            msgPrimero = prev.msgPrimero,
            msgSeguimiento = prev.msgSeguimiento,
            msgCierre = prev.msgCierre,
            mensaje = when {
                ui.isNotEmpty() && msg.isNotEmpty() -> msg
                ui.isEmpty() && store.count() == 0  -> "Importá el JSON exportado desde la PWA para ver tus leads."
                ui.isEmpty()                        -> "El backup no tenía leads activos."
                else                                -> ""
            }
        )
    }
}
